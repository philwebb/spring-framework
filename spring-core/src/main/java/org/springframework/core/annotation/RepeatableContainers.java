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

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AnnotationTypeResolver;
import org.springframework.core.annotation.type.AttributeType;
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
 * The {@link #and(Class, Class)} method can be used to register additional
 * relationships for annotations that do not wish to use
 * {@link Repeatable @Repeatable}.
 *
 * @author Phillip Webb
 * @since 5.1
 */
public abstract class RepeatableContainers {

	private final RepeatableContainers parent;

	private RepeatableContainers(RepeatableContainers parent) {
		this.parent = parent;
	}

	public RepeatableContainers and(Class<? extends Annotation> container,
			Class<? extends Annotation> repeatable) {
		Assert.notNull(container, "Container must not be null");
		Assert.notNull(repeatable, "Repeatable must not be null");
		return new ExplicitRepeatableContainer(this, container, repeatable);
	}

	@Nullable
	AnnotationType findContainedRepeatableType(AnnotationTypeResolver resolver,
			AnnotationType type, DeclaredAttributes attributes) {
		return (this.parent != null)
				? parent.findContainedRepeatableType(resolver, type, attributes)
				: null;
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
	 * {@link Repeatable @Repeatable} annotation
	 * @return a {@link RepeatableContainers} instance
	 */
	public static RepeatableContainers standardRepeatables() {
		return StandardRepeatableContainers.INSTANCE;
	}

	/**
	 * Standard {@link RepeatableContainers} implementation that searches using
	 * Java's {@link Repeatable @Repeatable} annotation.
	 */
	private static class StandardRepeatableContainers extends RepeatableContainers {

		private static StandardRepeatableContainers INSTANCE = new StandardRepeatableContainers(
				null);

		private static final String REPEATABLE = Repeatable.class.getName();

		StandardRepeatableContainers(RepeatableContainers parent) {
			super(parent);
		}

		@Override
		public AnnotationType findContainedRepeatableType(AnnotationTypeResolver resolver,
				AnnotationType type, DeclaredAttributes attributes) {
			Object value = attributes.get("value");
			AttributeType valueType = type.getAttributeTypes().get("value");
			if (value != null && value instanceof DeclaredAttributes[]) {
				String elementType = valueType.getClassName().replace("[]", "");
				AnnotationType repeatableType = resolver.resolve(elementType);
				if (hasAnnotation(repeatableType, REPEATABLE)) {
					return repeatableType;
				}
			}
			return super.findContainedRepeatableType(resolver, type, attributes);
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

		private Class<?> container;

		private Class<?> repeatable;

		ExplicitRepeatableContainer(RepeatableContainers parent, Class<?> container,
				Class<?> repeatable) {
			super(parent);
			this.container = container;
			this.repeatable = repeatable;
		}

		@Override
		AnnotationType findContainedRepeatableType(AnnotationTypeResolver resolver,
				AnnotationType type, DeclaredAttributes attributes) {
			if (type.getClassName().equals(this.container.getName())) {
				return resolver.resolve(repeatable.getName());
			}
			return super.findContainedRepeatableType(resolver, type, attributes);
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

}
