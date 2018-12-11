/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Strategy used to determine annotations that act as containers for other
 * annotations. The {@link #standardRepeatables()} method provides a default
 * strategy that respects Java's {@link Repeatable @Repeatable} support and
 * should be suitable for most situations.
 * <p>
 * The {@link #of} method can be used to register relationships for annotations
 * that do not wish to use {@link Repeatable @Repeatable}.
 * <p>
 * To completely disable repeatable support use {@link #none()}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public abstract class RepeatableContainers {

	private final RepeatableContainers parent;

	private RepeatableContainers(RepeatableContainers parent) {
		this.parent = parent;
	}

	public RepeatableContainers and(Class<? extends Annotation> container,
			Class<? extends Annotation> repeatable) {
		return new ExplicitRepeatableContainer(this, container, repeatable);
	}

	void visit(ClassLoader classLoader, DeclaredAnnotation annotation,
			BiConsumer<AnnotationType, DeclaredAttributes> consumer) {
		AnnotationType annotationType = AnnotationType.resolve(annotation.getType(),
				classLoader);
		if (annotationType != null) {
			DeclaredAttributes attributes = annotation.getAttributes();
			AnnotationType repeatableAnnotationType = findContainedRepeatable(classLoader,
					annotationType, attributes);
			if (repeatableAnnotationType == null) {
				consumer.accept(annotationType, attributes);
			}
			else {
				doWithRepeated(repeatableAnnotationType,
						(DeclaredAttributes[]) attributes.get("value"), consumer);
			}
		}
	}

	private void doWithRepeated(AnnotationType annotationType,
			DeclaredAttributes[] repeateAttributes,
			BiConsumer<AnnotationType, DeclaredAttributes> consumer) {
		for (DeclaredAttributes attributes : repeateAttributes) {
			consumer.accept(annotationType, attributes);
		}
	}

	@Nullable
	protected AnnotationType findContainedRepeatable(ClassLoader classLoader,
			AnnotationType annotationType, DeclaredAttributes attributes) {
		if (this.parent == null) {
			return null;
		}
		return this.parent.findContainedRepeatable(classLoader, annotationType,
				attributes);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		RepeatableContainers other = (RepeatableContainers) obj;
		return ObjectUtils.nullSafeEquals(this.parent, other.parent);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.parent);
	}

	/**
	 * Return a {@link RepeatableContainers} instance that searches using Java's
	 * {@link Repeatable @Repeatable} annotation.
	 * @return a {@link RepeatableContainers} instance
	 */
	public static RepeatableContainers standardRepeatables() {
		return StandardRepeatableContainers.INSTANCE;
	}

	public static RepeatableContainers of(Class<? extends Annotation> container,
			Class<? extends Annotation> repeatable) {
		return new ExplicitRepeatableContainer(null, container, repeatable);
	}

	public static RepeatableContainers none() {
		return NoRepeatableContainers.INSTANCE;
	}

	/**
	 * Standard {@link RepeatableContainers} implementation that searches using
	 * Java's {@link Repeatable @Repeatable} annotation.
	 */
	private static class StandardRepeatableContainers extends RepeatableContainers {

		private static StandardRepeatableContainers INSTANCE = new StandardRepeatableContainers();

		private static final String REPEATABLE = Repeatable.class.getName();

		StandardRepeatableContainers() {
			super(null);
		}

		@Override
		public AnnotationType findContainedRepeatable(ClassLoader classLoader,
				AnnotationType type, DeclaredAttributes attributes) {
			Object value = attributes.get("value");
			AttributeType valueType = type.getAttributeTypes().get("value");
			if (value != null && value instanceof DeclaredAttributes[]) {
				String elementType = valueType.getClassName().replace("[]", "");
				AnnotationType repeatableType = AnnotationType.resolve(elementType, classLoader);
				if (hasAnnotation(repeatableType, REPEATABLE)) {
					return repeatableType;
				}
			}
			return super.findContainedRepeatable(classLoader, type, attributes);
		}

		private boolean hasAnnotation(AnnotationType contained, String name) {
			return contained != null
					&& contained.getDeclaredAnnotations().find(name) != null;
		}

	}

	/**
	 * A single explicit mapping.
	 */
	private static class ExplicitRepeatableContainer extends RepeatableContainers {

		private Class<? extends Annotation> container;

		private Class<? extends Annotation> repeatable;

		ExplicitRepeatableContainer(RepeatableContainers parent,
				Class<? extends Annotation> container,
				Class<? extends Annotation> repeatable) {
			super(parent);
			validate(container, repeatable);
			this.container = container;
			this.repeatable = repeatable;
		}

		private void validate(Class<? extends Annotation> container,
				Class<? extends Annotation> repeatable) {
			Assert.notNull(container, "Container must not be null");
			Assert.notNull(repeatable, "Repeatable must not be null");
			try {
				Method method = container.getDeclaredMethod("value");
				Class<?> returnType = method.getReturnType();
				if (!returnType.isArray()
						|| returnType.getComponentType() != repeatable) {
					throw new AnnotationConfigurationException("Container type ["
							+ container.getName()
							+ "] must declare a 'value' attribute for an array of type ["
							+ repeatable.getName() + "]");
				}
			}
			catch (AnnotationConfigurationException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new AnnotationConfigurationException(
						"Invalid declaration of container type [" + container.getName()
								+ "] for repeatable annotation [" + repeatable.getName()
								+ "]",
						ex);
			}
		}

		@Override
		protected AnnotationType findContainedRepeatable(ClassLoader classLoader,
				AnnotationType type, DeclaredAttributes attributes) {
			if (type.getClassName().equals(this.container.getName())) {
				return AnnotationType.resolve(this.repeatable.getName(), classLoader);
			}
			return super.findContainedRepeatable(classLoader, type, attributes);
		}

		@Override
		public boolean equals(Object obj) {
			if (super.equals(obj)) {
				ExplicitRepeatableContainer other = (ExplicitRepeatableContainer) obj;
				return this.container.equals(other.container)
						&& this.repeatable.equals(other.repeatable);
			}
			return false;
		}

		@Override
		public int hashCode() {
			int hashCode = super.hashCode();
			hashCode = 31 * hashCode + this.container.hashCode();
			hashCode = 31 * hashCode + this.repeatable.hashCode();
			return hashCode;
		}

	}

	/**
	 * No repeatable containers.
	 */
	private static class NoRepeatableContainers extends RepeatableContainers {

		private static NoRepeatableContainers INSTANCE = new NoRepeatableContainers();

		NoRepeatableContainers() {
			super(null);
		}

	}

}
