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
import java.util.Set;
import java.util.function.BiFunction;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
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

	void afterAllMappingsSet() {
		this.claimedAliases = null;
		this.aliasTargetToAttributes = null;
	}

	private void processAttributeAnnotations() {
		for (Method attribute : this.attributes) {
			AliasFor aliasFor = attribute.getDeclaredAnnotation(AliasFor.class);
			if (aliasFor != null) {
				Method target = resolveAliasTarget(attribute, aliasFor);
				List<Method> targetAttributes = this.aliasTargetToAttributes.computeIfAbsent(
						target, key -> new ArrayList<>());
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

	public AnnotationTypeMapping getParent() {
		return this.parent;
	}

	public int getDepth() {
		return this.depth;
	}

	public Class<? extends Annotation> getAnnotationType() {
		return this.annotationType;
	}

	/**
	 * Return mapped attributes for the given root annotation.
	 * @param rootAnnotation the root annotation
	 * @return the mapped attributes
	 */
	public MappedAttributes mapAttributes(Annotation rootAnnotation) {
		return mapAttributes(rootAnnotation,
				AnnotationTypeMapping::extractAnnotationValue);
	}

	/**
	 * Return mapped attributes for the given root annotation. The method is
	 * designed to take alternative representations of an annotation (for
	 * example ASM parsed bytecode). As long as a suitable value can be
	 * extracted, any source object can be used.
	 * <p>
	 * The value extractor must return a type that is compatible with the method
	 * return type, namely:
	 * <ul>
	 * <li>If the method return type is an {@link Annotation} then the extracted
	 * value must be of type {@code <A>} (so that further extactions can be
	 * applied)</li>
	 * <li>If the method return type is {@code Class} then the value can either
	 * be a {@code Class} or {@code String}</li>
	 * <li>If the method return type is {@code Class[]} then the value can
	 * either be a {@code Class[]} or {@code String[]}</li>
	 * <li>For all other method return types the value must be an exact instance
	 * match</li>
	 * </ul>
	 * @param rootAnnotation the root annotation that can have values extracted
	 * from it
	 * @param valueExtractor a function to extract values from the root
	 * annotation.
	 * @return the mapped attributes
	 */
	public <A> MappedAttributes mapAttributes(A rootAnnotation,
			BiFunction<A, Method, Object> valueExtractor) {
		return new MappedAttributes(rootAnnotation, valueExtractor);
	}

	private static Object extractAnnotationValue(Annotation annotation,
			Method attributeMethod) {
		try {
			return attributeMethod.invoke(annotation);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Allows access to annotation attributes with mapping rules applied.
	 */
	public class MappedAttributes {

		private final Method[] attributeToMappedAttribute = new Method[attributes.size()];

		private final Object rootAnnotation;

		private final BiFunction<Object, Method, Object> valueExtractor;

		@SuppressWarnings("unchecked")
		MappedAttributes(Object rootAnnotation,
				BiFunction<?, Method, Object> valueExtractor) {
			this.rootAnnotation = rootAnnotation;
			this.valueExtractor = (BiFunction<Object, Method, Object>) valueExtractor;
			setupMappings();
		}

		private void setupMappings() {
			for (int i = 0; i < attributes.size(); i++) {
				List<Method> mappedAttributes = attributeToMappedAttributes.get(i);
				if (mappedAttributes != null) {
					this.attributeToMappedAttribute[i] = getMappedAttribute(i,
							mappedAttributes);
				}
			}
		}

		private Method getMappedAttribute(int attributeIndex,
				List<Method> mappedAttributes) {
			if (mappedAttributes.isEmpty()) {
				return null;
			}
			if (mappedAttributes.size() == 1) {
				return mappedAttributes.get(0);
			}
			Method result = null;
			for (int i = 0; i < mappedAttributes.size(); i++) {
				Method candidate = mappedAttributes.get(i);
				if (!hasDefaultValue(candidate)) {
					result = candidate;
				}
			}
			return result;
		}

		private boolean hasDefaultValue(Method candidate) {
			return equals(candidate.getDefaultValue(), getValue(candidate));
		}

		private boolean equals(Object value, Object extractedValue) {
			if (ObjectUtils.nullSafeEquals(value, extractedValue)) {
				return true;
			}
			if (value instanceof Class && extractedValue instanceof String) {
				// FIXME
			}
			if (value instanceof Class[] && extractedValue instanceof String[]) {
				// FIXME
			}
			if (value instanceof Annotation && !(extractedValue instanceof Annotation)) {
				Annotation annotation = (Annotation) value;
				equals(annotation, extractedValue);
			}
			return false;
		}

		private boolean equals(Annotation value, Object extractedValue) {
			for (Method attribute : AnnotationAttributeMethods.forAnnotationType(
					value.annotationType())) {
				if (equals(extractAnnotationValue(value, attribute),
						this.valueExtractor.apply(extractedValue, attribute))) {
					return true;
				}
			}
			return false;
		}

		public AnnotationTypeMapping getMapping() {
			return AnnotationTypeMapping.this;
		}

		public Object getValue(String attributeName) {
			int attributeIndex = attributes.indexOf(attributeName);
			Assert.isTrue(attributeIndex >= 0, "Attribute not found");
			Method mapped = this.attributeToMappedAttribute[attributeIndex];
			if (mapped != null) {
				return getValue(mapped);
			}
			Method attribute = attributes.get(attributeIndex);
			if (depth == 0) {
				return getValue(attribute);
			}
			return extractAnnotationValue(annotation, attribute);
		}

		private Object getValue(Method method) {
			return this.valueExtractor.apply(this.rootAnnotation, method);
		}

	}

}
