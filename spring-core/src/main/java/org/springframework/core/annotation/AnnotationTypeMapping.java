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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSets.MirrorSet;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
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

	private final AnnotationTypeMapping root;

	private final int depth;

	private final Class<? extends Annotation> annotationType;

	@Nullable
	private final Annotation annotation;

	private final AttributeMethods attributes;

	private final MirrorSets mirrorSets;

	@Nullable
	private Map<Method, List<Method>> aliasesFrom = new HashMap<>();

	private final int[] attributeMappings;

	@Nullable
	private Set<Method> claimedAliases = new HashSet<>();

	private int[] resolvedAnnotationMirrors;

	AnnotationTypeMapping(Class<? extends Annotation> annotationType) {
		this(null, annotationType, null);
	}

	AnnotationTypeMapping(AnnotationTypeMapping parent, Annotation annotation) {
		this(parent, annotation.annotationType(), annotation);
	}

	private AnnotationTypeMapping(@Nullable AnnotationTypeMapping parent,
			Class<? extends Annotation> annotationType, @Nullable Annotation annotation) {
		this.parent = parent;
		this.root = parent != null ? parent.getRoot() : this;
		this.depth = parent == null ? 0 : parent.getDepth() + 1;
		this.annotationType = annotationType;
		this.annotation = annotation;
		this.attributes = AttributeMethods.forAnnotationType(annotationType);
		this.mirrorSets = new MirrorSets();
		this.attributeMappings = new int[this.attributes.size()];
		addAliasesFromEntries();
		addAttributeMappings();
		updateMirrorSets();
	}

	private void addAliasesFromEntries() {
		for (Method attribute : this.attributes) {
			AliasFor aliasFor = attribute.getDeclaredAnnotation(AliasFor.class);
			if (aliasFor != null) {
				Method target = resolveAliasTarget(attribute, aliasFor);
				List<Method> aliasFrom = this.aliasesFrom.computeIfAbsent(target,
						key -> new ArrayList<>());
				aliasFrom.add(attribute);
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
					AttributeMethods.describe(attribute), aliasFor.attribute(),
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
		Method target = AttributeMethods.get(targetAnnotation, targetAttribute);
		if (target == null) {
			if (targetAnnotation == this.annotationType) {
				throw new AnnotationConfigurationException(String.format(
						"@AliasFor declaration on %s declares an "
								+ "alias for '%s' which is not present.",
						AttributeMethods.describe(attribute), targetAttribute));
			}
			throw new AnnotationConfigurationException(String.format(
					"%s is declared as an @AliasFor nonexistent %s.",
					StringUtils.capitalize(AttributeMethods.describe(attribute)),
					AttributeMethods.describe(targetAnnotation, targetAttribute)));
		}
		if (target == attribute) {
			throw new AnnotationConfigurationException(String.format(
					"@AliasFor declaration on %s points to itself. "
							+ "Specify 'annotation' to point to a same-named "
							+ "attribute on a meta-annotation.",
					AttributeMethods.describe(attribute)));
		}
		if (!isCompatibleReturnType(attribute.getReturnType(), target.getReturnType())) {
			throw new AnnotationConfigurationException(String.format(
					"Misconfigured aliases: %s and %s must declare the same return type.",
					AttributeMethods.describe(attribute),
					AttributeMethods.describe(target)));
		}
		if (isAliasPair(target) && checkAliasPair) {
			AliasFor targetAliasFor = target.getAnnotation(AliasFor.class);
			if (targetAliasFor == null) {
				throw new AnnotationConfigurationException(
						String.format("%s must be declared as an @AliasFor '%s'.",
								StringUtils.capitalize(AttributeMethods.describe(target)),
								attribute.getName()));
			}
			Method mirror = resolveAliasTarget(target, targetAliasFor, false);
			if (mirror != attribute) {
				throw new AnnotationConfigurationException(String.format(
						"%s must be declared as an @AliasFor '%s', not '%s'.",
						StringUtils.capitalize(AttributeMethods.describe(target)),
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

	private void addAttributeMappings() {
		AnnotationTypeMapping root = getRoot();
		Arrays.fill(this.attributeMappings, -1);
		if (root != this) {
			for (int i = 0; i < this.attributes.size(); i++) {
				Method mapped = findMapping(this.attributes.get(i));
				if (mapped != null) {
					this.attributeMappings[i] = root.attributes.indexOf(mapped);
				}
			}
		}
	}

	@Nullable
	private Method findMapping(Method attribute) {
		Method mapping = findAliasTargetMapping(attribute);
		if (mapping == null) {
			mapping = findConventionMapping(attribute);
		}
		return mapping;
	}

	@Nullable
	private Method findAliasTargetMapping(Method attribute) {
		AnnotationTypeMapping typeMapping = this;
		while (typeMapping.parent != null) {
			List<Method> aliasesFrom = typeMapping.parent.aliasesFrom.get(attribute);
			if (aliasesFrom != null) {
				attribute = aliasesFrom.get(0);
			}
			typeMapping = typeMapping.parent;
		}
		if (attribute.getDeclaringClass() == typeMapping.annotationType) {
			return attribute;
		}
		return null;
	}

	@Nullable
	protected final Method findConventionMapping(Method attribute) {
		if (MergedAnnotation.VALUE.equals(attribute.getName())) {
			return null;
		}
		AnnotationTypeMapping typeMapping = this;
		while (typeMapping.parent != null) {
			Method parentAttribute = typeMapping.parent.attributes.get(
					attribute.getName());
			if (parentAttribute != null) {
				return typeMapping.parent.findMapping(parentAttribute);
			}
			typeMapping = typeMapping.parent;
		}
		if (attribute.getDeclaringClass() == typeMapping.annotationType) {
			return attribute;
		}
		return null;
	}

	private void updateMirrorSets() {
		for (Method attribute : this.attributes) {
			updateMirrorSets(attribute);
		}
		if(getDepth() > 0) {
			this.resolvedAnnotationMirrors = this.mirrorSets.resolve(
					getAnnotationType().getName(), this.annotation,
					ReflectionUtils::invokeMethod);
		}
	}

	private void updateMirrorSets(Method attribute) {
		Set<Method> aliases = new HashSet<>();
		aliases.add(attribute);
		AnnotationTypeMapping typeMapping = this;
		while (typeMapping != null) {
			boolean aliasesChanged = false;
			for (Method alias : new LinkedHashSet<>(aliases)) {
				List<Method> additionalAliases = typeMapping.aliasesFrom.get(alias);
				if (additionalAliases != null) {
					typeMapping.claimedAliases.addAll(additionalAliases);
					if (aliases.addAll(additionalAliases)) {
						aliasesChanged = true;
					}
				}
			}
			if (aliasesChanged) {
				typeMapping.mirrorSets.updateFrom(aliases);
			}
			typeMapping = typeMapping.parent;
		}
	}

	/**
	 * Method called after all mappings have been set. At this point no further
	 * lookups from child mappings will occur.
	 */
	void afterAllMappingsSet() {
		validateAllAliasesClaimed();
		for (int i = 0; i < this.mirrorSets.size(); i++) {
			validateMirrorSet(this.mirrorSets.get(i));
		}
		this.claimedAliases = null;
		this.aliasesFrom = null;
	}

	private void validateAllAliasesClaimed() {
		for (Method attribute : this.attributes) {
			AliasFor aliasFor = attribute.getDeclaredAnnotation(AliasFor.class);
			if (aliasFor != null && !this.claimedAliases.contains(attribute)) {
				Method target = resolveAliasTarget(attribute, aliasFor);
				throw new AnnotationConfigurationException(String.format(
						"@AliasFor declaration on %s declares an alias for %s which is not meta-present.",
						AttributeMethods.describe(attribute),
						AttributeMethods.describe(target)));
			}
		}
	}

	private void validateMirrorSet(MirrorSet mirrorSet) {
		Method firstAttribute = mirrorSet.get(0);
		Object firstDefaultValue = firstAttribute.getDefaultValue();
		for (int i = 1; i <= mirrorSet.size() - 1; i++) {
			Method mirrorAttribute = mirrorSet.get(i);
			Object mirrorDefaultValue = mirrorAttribute.getDefaultValue();
			if (firstDefaultValue == null || mirrorDefaultValue == null) {
				throw new AnnotationConfigurationException(String.format(
						"Misconfigured aliases: %s and %s must declare default values.",
						AttributeMethods.describe(firstAttribute),
						AttributeMethods.describe(mirrorAttribute)));
			}
			if (!ObjectUtils.nullSafeEquals(firstDefaultValue, mirrorDefaultValue)) {
				throw new AnnotationConfigurationException(String.format(
						"Misconfigured aliases: %s and %s must declare the same default value.",
						AttributeMethods.describe(firstAttribute),
						AttributeMethods.describe(mirrorAttribute)));
			}
		}
	}

	/**
	 * Return the root mapping.
	 * @return the root mapping
	 */
	public AnnotationTypeMapping getRoot() {
		return this.root;
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
	public AttributeMethods getAttributes() {
		return this.attributes;
	}

	/**
	 * Return mapped attribute for the given attribute index or {@code -1} if
	 * there is no mapping. The resulting value is the index of the attribute on
	 * the root annotation that can be invoked in order to obtain the actual
	 * mapped value.
	 * @param attributeIndex the attribute index of the source attribute
	 * @return the mapped attributes or {@code null}
	 */
	@Nullable
	public int getMappedAttribute(int attributeIndex) {
		return this.attributeMappings[attributeIndex];
	}

	/**
	 * Return the mirror sets for this type mapping.
	 * @return the mirrorSets the attribute mirror sets.
	 */
	public MirrorSets getMirrorSets() {
		return this.mirrorSets;
	}

	public class MirrorSets {

		private MirrorSet[] mirrorSets;

		private MirrorSet[] assigned;

		MirrorSets() {
			this.assigned = new MirrorSet[attributes.size()];
			this.mirrorSets = new MirrorSet[0];
		}

		void updateFrom(Set<Method> aliases) {
			MirrorSet mirrorSet = null;
			for (int i = 0; i < attributes.size(); i++) {
				Method attribute = attributes.get(i);
				if (aliases.contains(attribute)) {
					if (mirrorSet == null) {
						mirrorSet = this.assigned[i] != null ? this.assigned[i]
								: new MirrorSet();
					}
					assigned[i] = mirrorSet;
				}
			}
			if (mirrorSet != null) {
				mirrorSet.update();
				LinkedHashSet<MirrorSet> unique = new LinkedHashSet<>(
						Arrays.asList(this.assigned));
				unique.remove(null);
				this.mirrorSets = unique.toArray(new MirrorSet[0]);
			}
		}

		public int size() {
			return this.mirrorSets.length;
		}

		public MirrorSet get(int index) {
			return this.mirrorSets[index];
		}

		public int[] resolve(Object source, Object annotation,
				BiFunction<Method, Object, Object> valueExtractor) {
			int[] result = new int[attributes.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = i;
			}
			for (int i = 0; i < size(); i++) {
				MirrorSet mirrorSet = get(i);
				int resolved = mirrorSet.resolve(source, annotation, valueExtractor);
				for (int j = 0; j < mirrorSet.size; j++) {
					result[mirrorSet.indexes[j]] = resolved;
				}
			}
			return result;
		}

		public class MirrorSet {

			private int size;

			private int[] indexes = new int[attributes.size()];

			public void update() {
				this.size = 0;
				Arrays.fill(this.indexes, -1);
				for (int i = 0; i < assigned.length; i++) {
					if (assigned[i] == this) {
						this.indexes[this.size] = i;
						this.size++;
					}
				}
			}

			private int resolve(Object source, Object annotation,
					BiFunction<Method, Object, Object> valueExtractor) {
				int result = -1;
				Object lastValue = null;
				for (int i = 0; i < attributes.size(); i++) {
					Method attribute = attributes.get(i);
					Object value = valueExtractor.apply(attribute, annotation);
					if (ObjectUtils.nullSafeEquals(lastValue, value)
							|| AttributeValues.isDefault(attribute, value,
									valueExtractor)) {
						continue;
					}
					if (lastValue != null) {
						String on = (source != null) ? " declared on " + source : "";
						throw new AnnotationConfigurationException(String.format(
								"Different @AliasFor mirror values for annotation [%s]%s, "
										+ "attribute '%s' and its alias '%s' are declared with values of [%s] and [%s].",
								getAnnotationType().getName(), on,
								attributes.get(result).getName(), attribute.getName(),
								ObjectUtils.nullSafeToString(lastValue),
								ObjectUtils.nullSafeToString(value)));
					}
					result = i;
					lastValue = value;
				}
				return result != -1 ? result : this.indexes[0];
			}

			public int size() {
				return this.size;
			}

			public Method get(int index) {
				int attributeIndex = this.indexes[index];
				return attributes.get(attributeIndex);
			}

		}

	}

}
