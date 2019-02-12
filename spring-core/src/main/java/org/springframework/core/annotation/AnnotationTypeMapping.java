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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
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
	private Map<Method, List<Method>> aliasTargetToAttributes = new HashMap<>();

	private final List<List<Method>> attributeToMappedAttributes;

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

	private void processAttributeAnnotations() {
		for (Method attribute : this.attributes) {
			AliasFor aliasFor = attribute.getDeclaredAnnotation(AliasFor.class);
			if (aliasFor != null) {
				Method target = resolveAliasTarget(attribute, aliasFor);
				List<Method> targetAttributes = this.aliasTargetToAttributes.computeIfAbsent(
						target, key -> new ArrayList<>());
				targetAttributes.add(attribute);
				if (isAliasPair(target)) {
					targetAttributes.add(target);
				}
			}
		}
	}

	private Method resolveAliasTarget(Method attribute, AliasFor aliasFor) {
		return resolveAliasTarget(attribute, aliasFor, true);
	}

	private Method resolveAliasTarget(Method attribute, AliasFor aliasFor,
			boolean checkAliasPair) {
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
			if (targetAnnotation == this.annotationType) {
				throw new AnnotationConfigurationException(String.format(
						"@AliasFor declaration on %s declares an "
								+ "alias for '%s' which is not present.",
						AnnotationAttributeMethods.describe(attribute), targetAttribute));
			}
			throw new AnnotationConfigurationException(
					String.format("%s is declared as an @AliasFor nonexistent %s.",
							StringUtils.capitalize(
									AnnotationAttributeMethods.describe(attribute)),
							AnnotationAttributeMethods.describe(targetAnnotation,
									targetAttribute)));
		}
		if (target == attribute) {
			throw new AnnotationConfigurationException(String.format(
					"@AliasFor declaration on %s points to itself. "
							+ "Specify 'annotation' to point to a same-named "
							+ "attribute on a meta-annotation.",
					AnnotationAttributeMethods.describe(attribute)));
		}
		if (!isCompatibleReturnType(attribute.getReturnType(), target.getReturnType())) {
			throw new AnnotationConfigurationException(String.format(
					"Misconfigured aliases: %s and %s must declare the same return type.",
					AnnotationAttributeMethods.describe(attribute),
					AnnotationAttributeMethods.describe(target)));
		}
		if (isAliasPair(target) && checkAliasPair) {
			AliasFor targetAliasFor = target.getAnnotation(AliasFor.class);
			if (targetAliasFor == null) {
				throw new AnnotationConfigurationException(
						String.format("%s must be declared as an @AliasFor '%s'.",
								StringUtils.capitalize(
										AnnotationAttributeMethods.describe(target)),
								attribute.getName()));
			}
			Method mirror = resolveAliasTarget(target, targetAliasFor, false);
			if (mirror != attribute) {
				throw new AnnotationConfigurationException(String.format(
						"%s must be declared as an @AliasFor '%s', not '%s'.",
						StringUtils.capitalize(
								AnnotationAttributeMethods.describe(target)),
						attribute.getName(), mirror.getName()));
			}
		}
		return target;
	}

	private boolean isAliasPair(Method target) {
		return target.getDeclaringClass().equals(this.annotationType);
	}

	private boolean isCompatibleReturnType(Class<?> attributeType, Class<?> targetType) {
		return Objects.equals(attributeType, targetType)
				|| Objects.equals(attributeType, targetType.getComponentType());
	}

	private void setupMappings() {
		for (Method attribute : this.attributes) {
			this.attributeToMappedAttributes.add(findMappings(attribute));
		}
	}

	@Nullable
	private List<Method> findMappings(Method attribute) {
		List<Method> mappings = findAliasTargetMappings(attribute);
		if (mappings == null) {
			mappings = findConventionMappings(attribute);
		}
		return mappings;
	}

	@Nullable
	private List<Method> findAliasTargetMappings(Method attribute) {
		List<Method> aliasedBy = this.aliasTargetToAttributes.get(attribute);
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
			return this.parent.findAliasTargetMappings(aliasedBy.get(0));
		}
		Set<Method> mappings = new LinkedHashSet<>();
		for (Method aliasedByMethod : aliasedBy) {
			List<Method> aliasMappings = this.parent.findAliasTargetMappings(
					aliasedByMethod);
			if (aliasMappings != null) {
				mappings.addAll(aliasMappings);
			}
		}
		return mappings.isEmpty() ? null : new ArrayList<>(mappings);
	}

	@Nullable
	protected final List<Method> findConventionMappings(Method attribute) {
		if (MergedAnnotation.VALUE.equals(attribute.getName())) {
			return null;
		}
		if (this.parent == null) {
			return this.annotationType.equals(attribute.getDeclaringClass())
					? Collections.singletonList(attribute)
					: null;
		}
		Method parentAttribute = this.parent.attributes.get(attribute.getName());
		return parentAttribute != null ? this.parent.findMappings(parentAttribute) : null;
	}

	/**
	 * Method called after all mappings have been set. At this point no further
	 * lookups from child mappings will occur.
	 */
	void afterAllMappingsSet() {
		for (Method attribute : this.attributes) {
			AliasFor aliasFor = attribute.getDeclaredAnnotation(AliasFor.class);
			if (aliasFor != null && !this.claimedAliases.contains(attribute)) {
				Method target = resolveAliasTarget(attribute, aliasFor);
				throw new AnnotationConfigurationException(String.format(
						"@AliasFor declaration on %s declares an alias for %s which is not meta-present.",
						AnnotationAttributeMethods.describe(attribute),
						AnnotationAttributeMethods.describe(target)));
			}
		}
		for (List<Method> attributes : this.aliasTargetToAttributes.values()) {
			if (attributes != null && attributes.size() > 1) {
				validateMirroredAttributes(attributes);
			}
		}
		this.claimedAliases = null;
		this.aliasTargetToAttributes = null;
	}

	private void validateMirroredAttributes(List<Method> attributes) {
		Method firstAttribute = attributes.get(0);
		Object firstDefaultValue = firstAttribute.getDefaultValue();
		for (int i = 1; i <= attributes.size() - 1; i++) {
			Method mirrorAttribute = attributes.get(i);
			Object mirrorDefaultValue = mirrorAttribute.getDefaultValue();
			if (firstDefaultValue == null || mirrorDefaultValue == null) {
				throw new AnnotationConfigurationException(String.format(
						"Misconfigured aliases: %s and %s must declare default values.",
						AnnotationAttributeMethods.describe(firstAttribute),
						AnnotationAttributeMethods.describe(mirrorAttribute)));
			}
			if (!ObjectUtils.nullSafeEquals(firstDefaultValue, mirrorDefaultValue)) {
				throw new AnnotationConfigurationException(String.format(
						"Misconfigured aliases: %s and %s must declare the same default value.",
						AnnotationAttributeMethods.describe(firstAttribute),
						AnnotationAttributeMethods.describe(mirrorAttribute)));
			}
		}
	}

	/**
	 * Return the parent mapping or {@code null}.
	 * @return the parent mapping
	 */
	@Nullable
	public AnnotationTypeMapping getParent() {
		return this.parent;
	}

	/**
	 * Return the depth of this mapping.
	 * @return the depth of the mapping
	 */
	public int getDepth() {
		return this.depth;
	}

	/**
	 * Return the type of the mapped annotation.
	 * @return the annotation type
	 */
	public Class<? extends Annotation> getAnnotationType() {
		return this.annotationType;
	}

	/**
	 * Return the source annotation for this mapping. This will be the
	 * meta-annotation, or {@code null} if this is the root mapping.
	 * @return the source annotation of the mapping
	 */
	@Nullable
	public Annotation getAnnotation() {
		return annotation;
	}

	/**
	 * Return the annotation attributes for the mapping annotation type.
	 * @return the attribute methods
	 */
	public AnnotationAttributeMethods getAttributes() {
		return this.attributes;
	}

	/**
	 * Return mapped attributes for the given attribute index or {@code null} if
	 * there are none. The resulting methods can be invoked against the root
	 * annotation in order to obtain the actual mapped value. If the returned
	 * list contains more than one element then the mapping is for a mirrored
	 * attribute.
	 * @param attributeIndex the attribute index of the source attribute
	 * @return the mapped attributes or {@code null}
	 */
	@Nullable
	public List<Method> getMappedAttributes(int attributeIndex) {
		return this.attributeToMappedAttributes.get(attributeIndex);
	}

}
