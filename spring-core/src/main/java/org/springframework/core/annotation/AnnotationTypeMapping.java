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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AnnotationTypeResolver;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Provides mapping information for a single annotation (or meta-annotation) in
 * the context of a source {@link AnnotationType}. Each mapping includes:
 * <ul>
 * <li>The source {@link MappableAnnotation}</li>
 * <li>The parent meta-annotation that defined it (if any)</li>
 * <li>Any aliases defined against it (i.e. the if this meta-annotation is a
 * {@code @AliasFor} target)</li>
 * <li>Any implicit or explicit mirrored attributes (i.e. attributes that all
 * should return the same value</li>
 * </ul>
 * <p>
 * An {@link AnnotationTypeMapping} can be used to
 * {@link #map(DeclaredAnnotation) map} a {@link MappableAnnotation} to a
 * {@link MergedAnnotation}.
 *
 * @author Phillip Webb
 * @since 5.1
 * @see #map(MappableAnnotation)
 */
class AnnotationTypeMapping {

	private final AnnotationTypeResolver resolver;

	private final AnnotationTypeMapping parent;

	private final MappableAnnotation source;

	private final int depth;

	private final Map<String, Reference> aliases = new LinkedHashMap<>();

	private final List<MirrorSet> mirrorSets = new ArrayList<>();

	AnnotationTypeMapping(AnnotationTypeResolver resolver, AnnotationTypeMapping parent,
			MappableAnnotation source) {
		this.resolver = resolver;
		this.parent = parent;
		this.source = source;
		this.depth = (parent == null ? 0 : parent.depth + 1);
	}

	boolean isAlreadyMapped(String className) {
		AnnotationTypeMapping candidate = this;
		while (candidate != null) {
			if (candidate.getAnnotationType().getClassName().equals(className)) {
				return true;
			}
			candidate = candidate.getParent();
		}
		return false;
	}

	void addAlias(Reference from, Reference to) {
		this.aliases.putIfAbsent(from.getAttribute().getAttributeName(), to);
	}

	void addMirrorSet(Collection<Reference> references) {
		for (Reference reference : references) {
			Assert.state(equals(reference.getMapping()),
					"Invalid mirror mapping reference");
			String attributeName = reference.getAttribute().getAttributeName();
			Reference aliasTo = this.aliases.get(attributeName);
			if(aliasTo != null && equals(aliasTo.getMapping())) {
				this.aliases.remove(attributeName);
			}
		}
		this.mirrorSets.add(new MirrorSet(references));
	}

	AnnotationTypeResolver getResolver() {
		return this.resolver;
	}

	AnnotationTypeMapping getParent() {
		return this.parent;
	}

	public MappableAnnotation getSource() {
		return this.source;
	}

	AnnotationType getAnnotationType() {
		return this.source.getAnnotationType();
	}

	DeclaredAttributes getAttributes() {
		return this.source.getAttributes();
	}

	int getDepth() {
		return this.depth;
	}

	public <A extends Annotation> MergedAnnotation<A> map(MappableAnnotation annotation,
			boolean inherited) {
		if (annotation == null) {
			return MergedAnnotation.missing();
		}
		try {
			DeclaredAttributes mappedAttributes = mapAttributes(
					annotation.getAttributes());
			return new MappedAnnotation<>(this, mappedAttributes,
					annotation.getDeclaringClass(), inherited, null);
		}
		catch (Exception ex) {
			throw new AnnotationConfigurationException("Unable to map attributes of "
					+ annotation.getAnnotationType().getClassName(), ex);
		}
	}

	DeclaredAttributes mapAttributes(DeclaredAttributes rootAttributes) {
		DeclaredAttributes attributes = rootAttributes;
		if (getParent() != null) {
			DeclaredAttributes parentMappedAttributes = getParent().mapAttributes(rootAttributes);
			attributes = new MappedAttributes(parentMappedAttributes, this.source, this.aliases);
		}
		if (!this.mirrorSets.isEmpty()) {
			attributes = new MirroredAttributes(attributes, this.mirrorSets);
		}
		return attributes;
	}

	/**
	 * Holds a reference to an {@link AnnotationTypeMapping} and an
	 * {@link AttributeType}.
	 */
	static class Reference {

		private final AnnotationTypeMapping mapping;

		private final AttributeType attribute;

		Reference(AnnotationTypeMapping mapping, AttributeType attribute) {
			this.mapping = mapping;
			this.attribute = attribute;
		}

		public AnnotationTypeMapping getMapping() {
			return this.mapping;
		}

		public AttributeType getAttribute() {
			return this.attribute;
		}

		public boolean isForSameAnnotation(Reference other) {
			if (other == null) {
				return false;
			}
			return Objects.equals(getMapping().getAnnotationType().getClassName(),
					other.getMapping().getAnnotationType().getClassName());
		}

		public String toCapitalizedString() {
			return StringUtils.capitalize(toString());
		}

		@Override
		public String toString() {
			return String.format("attribute '%s' in annotation [%s]",
					this.attribute.getAttributeName(),
					this.mapping.getAnnotationType().getClassName());
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ this.mapping.getAnnotationType().getClassName().hashCode();
			result = prime * result + this.attribute.getAttributeName().hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			Reference other = (Reference) obj;
			return Objects.equals(this.mapping.getAnnotationType().getClassName(),
					other.mapping.getAnnotationType().getClassName())
					&& Objects.equals(this.attribute.getAttributeName(),
							other.attribute.getAttributeName());
		}

	}

	/**
	 * A set of mirror attribute references.
	 */
	private static class MirrorSet implements Iterable<Reference> {

		private final Set<Reference> references;

		public MirrorSet(Collection<Reference> references) {
			Iterator<Reference> iterator = references.iterator();
			Reference source = iterator.next();
			while (iterator.hasNext()) {
				Reference mirror = iterator.next();
				Object sourceDefault = source.getAttribute().getDefaultValue();
				Object mirrorDefault = mirror.getAttribute().getDefaultValue();
				if (sourceDefault == null || mirrorDefault == null) {
					throw new AnnotationConfigurationException(String.format(
							"Misconfigured aliases: %s and %s must declare default values.",
							mirror, source));
				}
				if (!ObjectUtils.nullSafeEquals(sourceDefault, mirrorDefault)) {
					throw new AnnotationConfigurationException(String.format(
							"Misconfigured aliases: %s and %s must declare the same default value.",
							mirror, source));
				}
			}
			this.references = new LinkedHashSet<>(references);
			Assert.isTrue(references.size() > 1,
					"Mirrors must contain more than one reference");
		}

		@Override
		public Iterator<Reference> iterator() {
			return this.references.iterator();
		}

	}


	/**
	 * {@link DeclaredAttributes} decorator to apply mapping rules.
	 */
	private static class MappedAttributes implements DeclaredAttributes {

		private final DeclaredAttributes attributes;

		private final MappableAnnotation source;

		private final Map<String, Reference> aliases;

		public MappedAttributes(DeclaredAttributes attributes,
				MappableAnnotation source,
				Map<String, Reference> aliases) {
			this.attributes = attributes;
			this.source = source;
			this.aliases = aliases;
		}

		@Override
		public Object get(String name) {
			Object result = null;
			Reference alias = this.aliases.get(name);
			if (alias != null) {
				String aliasName = alias.getAttribute().getAttributeName();
				result = this.attributes.get(aliasName);
			}
			if (result == null && !isConventionRestricted(name)) {
				result = this.attributes.get(name);
			}
			if (result == null) {
				result = this.source.getAttributes().get(name);
			}
			return result;
		}

		private boolean isConventionRestricted(String name) {
			return "value".equals(name);
		}

	}

	/**
	 * {@link DeclaredAttributes} decorator to apply mirroring rules.
	 */
	private class MirroredAttributes implements DeclaredAttributes {

		private final DeclaredAttributes attributes;

		private final Map<String, Reference> mirrors;

		public MirroredAttributes(DeclaredAttributes attributes,
				List<MirrorSet> mirrorSets) {
			this.attributes = attributes;
			this.mirrors = getMirrors(mirrorSets);
		}

		private Map<String, Reference> getMirrors(List<MirrorSet> mirrorSets) {
			Map<String, Reference> mirrors = new HashMap<>();
			for (MirrorSet mirrorSet : mirrorSets) {
				addMirrors(mirrors, mirrorSet);
			}
			return Collections.unmodifiableMap(mirrors);
		}

		private void addMirrors( Map<String, Reference> mirrors, MirrorSet mirrorSet) {
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
				Object value = this.attributes.get(name);
				if (value != null && !isSameAsDefaultValue(value, attribute)) {
					if (result != null && !ObjectUtils.nullSafeEquals(lastValue, value)) {
						Class<?> declaringClass = getSource().getDeclaringClass();
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
					result = candidate;
					lastValue = value;
				}
			}
			return result;
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
		public Object get(String name) {
			Reference mirror = this.mirrors.get(name);
			if (mirror != null) {
				name = mirror.getAttribute().getAttributeName();
			}
			return this.attributes.get(name);
		}

	}

}
