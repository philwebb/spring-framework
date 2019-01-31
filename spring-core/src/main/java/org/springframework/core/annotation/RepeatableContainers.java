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

	/**
	 * Add an additional explicit relationship between a contained and
	 * repeatable annotation.
	 * @param container the container type
	 * @param repeatable the contained repeatable type
	 * @return a new {@link RepeatableContainers} instance
	 */
	public RepeatableContainers and(Class<? extends Annotation> container,
			Class<? extends Annotation> repeatable) {
		return new ExplicitRepeatableContainer(this, repeatable, container);
	}


	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		RepeatableContainers other = (RepeatableContainers) obj;
		return (this.parent == other.parent)
				|| (this.parent != null && this.parent.equals(other.parent));
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

	/**
	 * Return a {@link RepeatableContainers} instance that uses a defined
	 * container and repeatable type.
	 * @param repeatable the contained repeatable annotation
	 * @param container the container annotation or {@code null}. If specified,
	 * this annotation must declare a {@code value} attribute returning an array
	 * of repeatable annotations. If not specified, the container will be
	 * deduced by inspecting the {@code @Repeatable} annotation on
	 * {@code repeatable}.
	 * @return a {@link RepeatableContainers} instance
	 */
	public static RepeatableContainers of(Class<? extends Annotation> repeatable,
			@Nullable Class<? extends Annotation> container) {
		return new ExplicitRepeatableContainer(null, repeatable, container);
	}

	/**
	 * Return a {@link RepeatableContainers} instance that does not expand any
	 * repeatable annotations.
	 * @return a {@link RepeatableContainers} instance
	 */
	public static RepeatableContainers none() {
		return NoRepeatableContainers.INSTANCE;
	}

	/**
	 * Standard {@link RepeatableContainers} implementation that searches using
	 * Java's {@link Repeatable @Repeatable} annotation.
	 */
	private static class StandardRepeatableContainers extends RepeatableContainers {

		private static StandardRepeatableContainers INSTANCE = new StandardRepeatableContainers();

		StandardRepeatableContainers() {
			super(null);
		}

	}

	/**
	 * A single explicit mapping.
	 */
	private static class ExplicitRepeatableContainer extends RepeatableContainers {

		private final Class<? extends Annotation> repeatable;

		@Nullable
		private final Class<? extends Annotation> container;

		ExplicitRepeatableContainer(RepeatableContainers parent,
				Class<? extends Annotation> repeatable,
				@Nullable Class<? extends Annotation> container) {
			super(parent);
			Assert.notNull(repeatable, "Repeatable must not be null");
			if(container == null) {
				container = deduceContainer(repeatable);
			}
			validate(repeatable, container);
			this.repeatable = repeatable;
			this.container = container;
		}

		private void validate(Class<? extends Annotation> repeatable,
				@Nullable Class<? extends Annotation> container) {
			try {
				Method method = container.getDeclaredMethod("value");
				Assert.state(method != null, "No value method found");
				Class<?> returnType = method != null ? method.getReturnType() : null;
				if (returnType == null || !returnType.isArray()
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

		private Class<? extends Annotation> deduceContainer(
				Class<? extends Annotation> repeatable) {
			Repeatable annotation = repeatable.getAnnotation(Repeatable.class);
			Assert.notNull(annotation, "Annotation type must be a repeatable annotation: "
					+ "failed to resolve container type for " + repeatable.getName());
			return annotation.value();
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
