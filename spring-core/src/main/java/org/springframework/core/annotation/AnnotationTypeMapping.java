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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AnnotationTypeResolver;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAttributes;
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
 * <ul>
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

	private final List<List<Reference>> mirrors = new ArrayList<>();

	AnnotationTypeMapping(AnnotationTypeResolver resolver, AnnotationTypeMapping parent,
			MappableAnnotation source) {
		this.resolver = resolver;
		this.parent = parent;
		this.source = source;
		this.depth = (parent == null ? 0 : parent.depth + 1);
	}

	boolean isDescendant(String className) {
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

	void addMirrors(List<Reference> mirrors) {
		this.mirrors.add(mirrors);
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
		DeclaredAttributes mappedAttributes = mapAttributes(annotation.getAttributes());
		return new MappedAnnotation<>(this, mappedAttributes, inherited, null);
	}

	private DeclaredAttributes mapAttributes(DeclaredAttributes rootAttributes) {
		DeclaredAttributes parentAttributes = (getParent() == null ? rootAttributes
				: getParent().mapAttributes(rootAttributes));
		return new MappedAttributes(parentAttributes);
	}

	/**
	 * Mapped {@link DeclaredAttributes}.
	 */
	private class MappedAttributes implements DeclaredAttributes {

		private final DeclaredAttributes parentAttributes;

		private final Map<String, Reference> mirrors = new LinkedHashMap<>();

		public MappedAttributes(DeclaredAttributes parentAttributes) {
			this.parentAttributes = parentAttributes;
			processMirrors();
		}

		private void processMirrors() {
			for (List<Reference> mirrors : AnnotationTypeMapping.this.mirrors) {
				processMirrors(mirrors);
			}
		}

		private void processMirrors(List<Reference> mirrors) {
			Reference inUse = getMirrorAttributeInUse(mirrors);
			for (Reference mirror : mirrors) {
				this.mirrors.put(mirror.getAttribute().getAttributeName(), inUse);
			}
		}

		private Reference getMirrorAttributeInUse(List<Reference> candidates) {
			Reference result = null;
			for (Reference candidate : candidates) {
				AttributeType attribute = candidate.getAttribute();
				String name = attribute.getAttributeName();
				Object value = this.parentAttributes.get(name);
				Object defaultValue = attribute.getDefaultValue();
				if (value != null && !Objects.equals(value, defaultValue)) {
					if (result != null) {
						throw new AnnotationConfigurationException(String.format(
								"Different @AliasFor mirror values defined in attributes '%s' and '%s' on [%s].",
								result.getAttribute().getAttributeName(), name,
								result.getMapping().getAnnotationType().getClassName()));
					}
					result = candidate;
				}
			}
			return result;
		}

		@Override
		public Object get(String name) {
			if (getParent() != null) {
				Reference alias = AnnotationTypeMapping.this.aliases.get(name);
				if (alias == null && isConventionRestricted(name)) {
					return null;
				}
				if (alias != null) {
					name = alias.getAttribute().getAttributeName();
				}
			}
			Reference mirror = this.mirrors.get(name);
			if (mirror != null) {
				name = mirror.getAttribute().getAttributeName();
			}
			return this.parentAttributes.get(name);
		}

		private boolean isConventionRestricted(String name) {
			return "value".equals(name);
		}

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

}
