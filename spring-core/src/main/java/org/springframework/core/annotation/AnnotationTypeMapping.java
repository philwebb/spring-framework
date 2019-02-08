/*
 * Copyright 2002-2019 the original author or authors.
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Provides mapping information for a single annotation (or meta-annotation) in
 * the context of a source annotation type.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see AnnotationTypeMappings
 */
class AnnotationTypeMapping {

	@Nullable
	private final AnnotationTypeMapping parent;

	private final int depth;

	private final Class<? extends Annotation> annotationType;

	@Nullable
	private final Annotation annotation;

	private final AnnotationAttributeMethods attributes;

	@Nullable
	private Map<Method, Set<Method>> aliasTargetToAttributes = new HashMap<>();

	private final List<Set<Method>> attributeToMappedAttributes;

	@Nullable
	private Set<Method> claimedAliases = new HashSet<>();

	AnnotationTypeMapping(Class<? extends Annotation> annotationType) {
		this(null, annotationType, null);
	}

	AnnotationTypeMapping(AnnotationTypeMapping parent, Annotation annotation) {
		this(parent, annotation.annotationType(), annotation);
	}

	private AnnotationTypeMapping(@Nullable AnnotationTypeMapping parent,
			Class<? extends Annotation> annotationType, @Nullable Annotation annotation) {
		this.parent = parent;
		this.depth = parent == null ? 0 : parent.getDepth() + 1;
		this.annotationType = annotationType;
		this.annotation = annotation;
		this.attributes = AnnotationAttributeMethods.forAnnotationType(annotationType);
		this.attributeToMappedAttributes = new ArrayList<>(this.attributes.size());
		processAttributeAnnotations();
		setupMappings();
	}

	void afterAllMappingsSet() {
		this.claimedAliases = null;
		this.aliasTargetToAttributes = null;
	}

	private void processAttributeAnnotations() {
		for (Method attribute : this.attributes) {
			AliasFor aliasFor = attribute.getDeclaredAnnotation(AliasFor.class);
			if (aliasFor != null) {
				Method target = resolveAliasTarget(attribute, aliasFor);
				Set<Method> targetAttributes = this.aliasTargetToAttributes.computeIfAbsent(
						target, key -> new LinkedHashSet<>());
				targetAttributes.add(attribute);
				if (isImplicitMirror(target)) {
					targetAttributes.add(attribute);
				}
			}
		}
	}

	private boolean isImplicitMirror(Method target) {
		return target.getDeclaringClass().equals(this.annotationType);
	}

	private Method resolveAliasTarget(Method attribute, AliasFor aliasFor) {
		if (StringUtils.hasText(aliasFor.value())
				&& StringUtils.hasText(aliasFor.attribute())) {
			throw new AnnotationConfigurationException(String.format(
					"In @AliasFor declared on %s, attribute 'attribute' and its alias "
							+ "'value' are present with values of '%s' and '%s', but "
							+ "only one is permitted.",
					AnnotationAttributeMethods.describe(attribute), aliasFor.attribute(),
					aliasFor.value()));
		}
		Class<? extends Annotation> targetAnnotation = aliasFor.annotation();
		if (targetAnnotation == Annotation.class) {
			targetAnnotation = this.annotationType;
		}
		String targetAttribute = aliasFor.attribute();
		if (!StringUtils.hasLength(targetAttribute)) {
			targetAttribute = aliasFor.value();
		}
		if (!StringUtils.hasLength(targetAttribute)) {
			targetAttribute = attribute.getName();
		}
		Method target = AnnotationAttributeMethods.get(targetAnnotation, targetAttribute);
		if (target == null) {
			// FIXME throw
		}
		if (target == attribute) {
			// FIXME throw
		}
		// FIXME check return types match
		// FIXME check default values match
		return target;
	}

	private void setupMappings() {
		for (Method attribute : this.attributes) {
			this.attributeToMappedAttributes.add(findMappings(attribute));
		}
	}

	@Nullable
	protected final Set<Method> findMappings(Method attribute) {
		Set<Method> mappings = findAliasTargetMappings(attribute);
		if (mappings == null) {
			mappings = findConventionMappings(attribute);
		}
		return mappings;
	}

	@Nullable
	protected final Set<Method> findAliasTargetMappings(Method attribute) {
		Set<Method> aliasedBy = this.aliasTargetToAttributes.get(attribute);
		if (aliasedBy != null) {
			this.claimedAliases.addAll(aliasedBy);
		}
		if (this.parent == null) {
			return aliasedBy;
		}
		if (aliasedBy == null) {
			return this.parent.findAliasTargetMappings(attribute);
		}
		if (aliasedBy.size() == 1) {
			return this.parent.findAliasTargetMappings(aliasedBy.iterator().next());
		}
		Set<Method> mappings = new LinkedHashSet<>();
		for (Method aliasedByMethod : aliasedBy) {
			Set<Method> aliasMappings = this.parent.findAliasTargetMappings(
					aliasedByMethod);
			if (aliasMappings != null) {
				mappings.addAll(aliasMappings);
			}
		}
		return mappings.isEmpty() ? null : mappings;
	}

	@Nullable
	protected final Set<Method> findConventionMappings(Method method) {
		if (this.parent == null || MergedAnnotation.VALUE.equals(method.getName())) {
			return null;
		}
		Method parentMethod = this.parent.attributes.get(method.getName());
		return parentMethod != null ? this.parent.findMappings(parentMethod) : null;
	}

	public AnnotationTypeMapping getParent() {
		return this.parent;
	}

	public int getDepth() {
		return this.depth;
	}

	public Class<? extends Annotation> getAnnotationType() {
		return this.annotationType;
	}

	public MappedAttributes mapAttributes(Annotation rootAnnotation) {
		return new MappedAttributes(rootAnnotation);
	}

	class MappedAttributes {

		private final Annotation rootAnnotation;

		// FIXME this could be a simple array given the limited number of items
		private final Method[] attributeToMappedAttribute = new Method[attributes.size()];

		public MappedAttributes(Annotation rootAnnotation) {
			this.rootAnnotation = rootAnnotation;
			setupMappings();
		}

		private void setupMappings() {
			for (int i = 0; i < attributes.size(); i++) {
				Set<Method> mappedAttributes = attributeToMappedAttributes.get(i);
				if(mappedAttributes != null) {
					setupMapping(i, mappedAttributes);
				}
			}
		}

		private void setupMapping(int attributeIndex, Set<Method> mappedAttributes) {
			if (mappedAttributes.size() == 1) {
				this.attributeToMappedAttribute[attributeIndex] =
						mappedAttributes.iterator().next();
				return;
			}
			// FIXME find out which one is actually in use
		}

		public Object getValue(String attributeName) {
			try {
				int attributeIndex = attributes.indexOf(attributeName);
				Assert.isTrue(attributeIndex >= 0, "Attribute not found");
				Method mapped = this.attributeToMappedAttribute[attributeIndex];
				if (mapped != null) {
					return mapped.invoke(this.rootAnnotation);
				}
				Assert.notNull(annotation, "No meta-annotation found");
				return attributes.get(attributeIndex).invoke(annotation);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to retrieve value of attribute '"
						+ attributeName + "' in annotation ["
						+ getAnnotationType().getName() + "]", ex);
			}
		}

	}

}
