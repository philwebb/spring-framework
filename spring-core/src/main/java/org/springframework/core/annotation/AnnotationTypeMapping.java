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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

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

	private final Map<String, Method> attributeMethods;

	private Map<String, Set<Method>> mappedAttributes = new HashMap<>();

	@Nullable
	private Map<Method, Set<Method>> aliases;

	@Nullable
	private Set<Method> claimedAliases = new HashSet<>();

	AnnotationTypeMapping(Class<? extends Annotation> annotationType) {
		this.parent = null;
		this.depth = 0;
		this.annotationType = annotationType;
		this.annotation = null;
		this.attributeMethods = findAttributeMethods(annotationType);
		this.aliases = findAliases(this.attributeMethods);
	}

	AnnotationTypeMapping(AnnotationTypeMapping parent, Annotation annotation) {
		this.parent = parent;
		this.depth = parent.getDepth() + 1;
		this.annotationType = annotation.annotationType();
		this.annotation = annotation;
		this.attributeMethods = findAttributeMethods(annotationType);
		this.aliases = findAliases(this.attributeMethods);
	}

	private static Map<String, Method> findAttributeMethods(
			Class<? extends Annotation> annotationType) {
		Map<String, Method> attributeMethods = new HashMap<>();
		ReflectionUtils.doWithLocalMethods(annotationType,
				method -> attributeMethods.put(method.getName(), method),
				AnnotationTypeMapping::isAttributeMethod);
		return attributeMethods.isEmpty() ? Collections.emptyMap()
				: Collections.unmodifiableMap(attributeMethods);
	}

	private static boolean isAttributeMethod(Method method) {
		return method.getParameterCount() == 0 && method.getReturnType() != void.class;
	}

	private static Map<Method, Set<Method>> findAliases(
			Map<String, Method> attributeMethods) {
		return null;
	}

	public void setupMappings() {
		for (Method method : this.attributeMethods.values()) {
			Set<Method> mappings = findMappings(method);
			if (mappings != null) {
				this.mappedAttributes.put(method.getName(), mappings);
			}
		}
	}

	protected final Set<Method> findMappings(Method method) {
		Set<Method> mapping = findMappingsByAlias(method);
		return mapping != null ? mapping : findMappingsByConvention(method);
	}

	protected final Set<Method> findMappingsByAlias(Method method) {
		Set<Method> aliasedBy = this.aliases.get(method);
		if (aliasedBy != null) {
			this.claimedAliases.addAll(aliasedBy);
		}
		if (this.parent == null) {
			return aliasedBy;
		}
		if (aliasedBy == null) {
			return this.parent.findMappingsByAlias(method);
		}
		if (aliasedBy.size() == 1) {
			return this.parent.findMappingsByAlias(aliasedBy.iterator().next());
		}
		Set<Method> mappings = new LinkedHashSet<>();
		for (Method aliasedByMethod : aliasedBy) {
			Set<Method> aliasMappings = this.parent.findMappingsByAlias(aliasedByMethod);
			if (aliasMappings != null) {
				mappings.addAll(aliasMappings);
			}
		}
		return mappings.isEmpty() ? null : mappings;
	}

	protected final Set<Method> findMappingsByConvention(Method method) {
		if (this.parent == null || MergedAnnotation.VALUE.equals(method.getName())) {
			return null;
		}
		Method parentMethod = this.parent.attributeMethods.get(method.getName());
		return parentMethod != null ? this.parent.findMappings(parentMethod) : null;
	}

	public void validate() {
		this.mappedAttributes = this.mappedAttributes.isEmpty() ? Collections.emptyMap()
				: Collections.unmodifiableMap(this.mappedAttributes);
		this.claimedAliases = null;
		this.aliases = null;
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
		return null;
	}

	static class MappedAttributes {

		public Object getValue(String attributeName) {
			return null;
		}

	}

}
