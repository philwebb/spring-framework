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
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSet;
import org.springframework.core.annotation.AnnotationTypeMapping.MirroredAttributes;
import org.springframework.core.annotation.AnnotationTypeMapping.ParentMappedAttributes;
import org.springframework.core.annotation.AnnotationTypeMapping.Reference;
import org.springframework.core.annotation.type.AbstractDeclaredAttributes;
import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 *
 * @author pwebb
 * @since 5.0
 */
public class XDunno {


	// FIXME perhaps return MappedAnnotation?
	public <A extends Annotation> TypeMappedAnnotation<A> map(
			DeclaredAttributes attributes, boolean inherited) {
		DeclaredAttributes mappedAttributes = mapAttributes(attributes);
		return new TypeMappedAnnotation<>(this, mappedAttributes, inherited, null);
	}

	// FIXME
	// public <A extends Annotation> MergedAnnotation<A> map(MappableAnnotation
	// annotation,
	// boolean inherited) {
	// if (annotation == null) {
	// return MergedAnnotation.missing();
	// }
	// try {
	// DeclaredAttributes mappedAttributes = mapAttributes(
	// annotation.getAttributes());
	// return new MappedAnnotation<>(this, mappedAttributes, inherited, null);
	// }
	// catch (Exception ex) {
	// throw new AnnotationConfigurationException("Unable to map attributes of "
	// + annotation.getAnnotationType().getClassName(), ex);
	// }
	// }

	private DeclaredAttributes mapAttributes(DeclaredAttributes rootAttributes) {
		DeclaredAttributes mappedAttributes = rootAttributes;
		if (this.parent != null) {
			DeclaredAttributes parentAttributes = this.parent.mapAttributes(
					rootAttributes);
			mappedAttributes = new ParentMappedAttributes(this.annotationType,
					this.annotationAttributes, parentAttributes, this.aliases);
		}
		if (!this.mirrorSets.isEmpty()) {
			mappedAttributes = new MirroredAttributes(this.annotationAttributes,
					mappedAttributes, this.mirrorSets);
		}
		return mappedAttributes;
	}



	/**
	 * {@link DeclaredAttributes} decorator to apply mapping rules.
	 */
	private class ParentMappedAttributes extends AbstractDeclaredAttributes {

		private final AnnotationType annotationType;

		private final DeclaredAttributes annotationAttributes;

		private final DeclaredAttributes parentAttributes;

		private final Map<String, Reference> aliases;

		public ParentMappedAttributes(AnnotationType annotationType,
				DeclaredAttributes annotationAttributes,
				DeclaredAttributes parentAttributes, Map<String, Reference> aliases) {
			this.annotationType = annotationType;
			this.annotationAttributes = annotationAttributes;
			this.parentAttributes = parentAttributes;
			this.aliases = aliases;
		}

		@Override
		public Object get(String name) {
			Assert.notNull(name, "Name must not be null");
			AttributeType type = this.annotationType.getAttributeTypes().get(name);
			Object result = null;
			Reference alias = this.aliases.get(name);
			if (alias != null) {
				String aliasName = alias.getAttribute().getAttributeName();
				result = this.parentAttributes.get(aliasName);
			}
			if (result == null && !isConventionRestricted(name)) {
				result = this.parentAttributes.get(name);
			}
			if (result == null) {
				result = this.annotationAttributes.get(name);
			}
			if (result != null && isArrayAttributeType(type)
					&& !ObjectUtils.isArray(result)) {
				result = wrapInArray(result);
			}
			return result;
		}

		private boolean isConventionRestricted(String name) {
			return "value".equals(name);
		}

		private boolean isArrayAttributeType(AttributeType type) {
			return type != null && type.getClassName().endsWith("[]");
		}

		private Object wrapInArray(Object result) {
			Object array = Array.newInstance(result.getClass(), 1);
			Array.set(array, 0, result);
			return array;
		}

		@Override
		public Set<String> names() {
			return this.annotationType.getAttributeTypes().names();
		}

	}

	/**
	 * {@link DeclaredAttributes} decorator to apply mirroring rules.
	 */
	private static class MirroredAttributes extends AbstractDeclaredAttributes {

		private final DeclaredAttributes annotationAttributes;

		private final DeclaredAttributes sourceAttributes;

		private final Map<String, Reference> mirrors;

		public MirroredAttributes(DeclaredAttributes annotationAttributes,
				DeclaredAttributes sourceAttributes, List<MirrorSet> mirrorSets) {
			this.annotationAttributes = annotationAttributes;
			this.sourceAttributes = sourceAttributes;
			this.mirrors = getMirrors(mirrorSets);
		}

		private Map<String, Reference> getMirrors(List<MirrorSet> mirrorSets) {
			Map<String, Reference> mirrors = new HashMap<>();
			for (MirrorSet mirrorSet : mirrorSets) {
				addMirrors(mirrors, mirrorSet);
			}
			return Collections.unmodifiableMap(mirrors);
		}

		private void addMirrors(Map<String, Reference> mirrors, MirrorSet mirrorSet) {
			Reference inUse = getMirrorAttributeInUse(mirrorSet);
			for (Reference mirror : mirrorSet) {
				mirrors.put(mirror.getAttribute().getAttributeName(), inUse);
			}
		}

		private Reference getMirrorAttributeInUse(MirrorSet mirrorSet) {
			Reference result = null;
			Object lastValue = null;
			for (Reference candidate : mirrorSet) {
				AttributeType attribute = candidate.getAttribute();
				String name = attribute.getAttributeName();
				Object value = this.sourceAttributes.get(name);
				if (value != null && !isSameAsDefaultValue(value, attribute)) {
					if (result != null) {
						checkMirrorPossibleAttributeResult(name, result, value,
								lastValue);
					}
					result = candidate;
					lastValue = value;
				}
			}
			return result;
		}

		private void checkMirrorPossibleAttributeResult(String name, Reference result,
				Object value, Object lastValue) {
			if (ObjectUtils.nullSafeEquals(value, lastValue)
					|| isShadow(result, value, lastValue)) {
				return;
			}
			// FIXME this.source.getDeclaringClass();
			Class<?> declaringClass = null;
			String on = (declaringClass != null)
					? " declared on " + declaringClass.getName()
					: "";
			String annotationType = result.getMapping().getAnnotationType().getClassName();
			String lastName = result.getAttribute().getAttributeName();
			throw new AnnotationConfigurationException(String.format(
					"Different @AliasFor mirror values for annotation [%s]%s, "
							+ "attribute '%s' and its alias '%s' are declared with values of [%s] and [%s].",
					annotationType, on, lastName, name,
					ObjectUtils.nullSafeToString(lastValue),
					ObjectUtils.nullSafeToString(value)));
		}

		private boolean isShadow(Reference result, Object value, Object lastValue) {
			Object attributeValue = this.annotationAttributes.get(
					result.getAttribute().getAttributeName());
			return ObjectUtils.nullSafeEquals(lastValue, attributeValue);
		}

		private boolean isSameAsDefaultValue(Object value, AttributeType attribute) {
			Object defaultValue = attribute.getDefaultValue();
			if (ObjectUtils.nullSafeEquals(value, defaultValue)) {
				return true;
			}
			if (isZeroLengthArray(defaultValue) && isZeroLengthArray(value)) {
				return true;
			}
			return false;
		}

		private boolean isZeroLengthArray(Object defaultValue) {
			return ObjectUtils.isArray(defaultValue)
					&& Array.getLength(defaultValue) == 0;
		}

		@Override
		public Set<String> names() {
			return this.sourceAttributes.names();
		}

		@Override
		public Object get(String name) {
			Reference mirror = this.mirrors.get(name);
			if (mirror != null) {
				name = mirror.getAttribute().getAttributeName();
			}
			return this.sourceAttributes.get(name);
		}

	}


}
