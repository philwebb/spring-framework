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

package org.springframework.core.type.classreading;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;

/**
 * {@link AnnotationMetadata} returned from a {@link SimpleMetadataReader}.
 *
 * @author Phillip Webb
 * @since 5.1
 */
public final class SimpleAnnotationMetadata extends SimpleAnnotatedTypeMetadata implements AnnotationMetadata {

	private final String className;

	private final EnumSet<Flag> flags;

	@Nullable
	private final String enclosingClassName;

	@Nullable
	private final String superClassName;

	private final String[] interfaceNames;

	private final String[] memberClassNames;

	private final Set<String> annotationTypes;

	private final Set<SimpleMethodMetadata> methodMetadata;


	SimpleAnnotationMetadata(ClassLoader classLoader,
			LinkedMultiValueMap<String, AnnotationAttributes> annotationAttributes,
			Map<String, Set<String>> metaAnnotations, String className,
			EnumSet<Flag> flags, String enclosingClassName, String superClassName,
			String[] interfaceNames, String[] memberClassNames,
			Set<String> annotationTypes, Set<SimpleMethodMetadata> methodMetadata) {
		super(classLoader, annotationAttributes, metaAnnotations);
		this.className = className;
		this.flags = flags;
		this.enclosingClassName = enclosingClassName;
		this.superClassName = superClassName;
		this.interfaceNames = interfaceNames;
		this.memberClassNames = memberClassNames;
		this.annotationTypes = annotationTypes;
		this.methodMetadata = methodMetadata;
	}


	@Override
	public String getClassName() {
		return this.className;
	}

	@Override
	public boolean isInterface() {
		return this.flags.contains(Flag.INTERFACE);
	}

	@Override
	public boolean isAnnotation() {
		return this.flags.contains(Flag.ANNOTATION);
	}

	@Override
	public boolean isAbstract() {
		return this.flags.contains(Flag.ABSTRACT);

	}

	@Override
	public boolean isFinal() {
		return this.flags.contains(Flag.FINAL);
	}

	@Override
	public boolean isIndependent() {
		return (getEnclosingClassName() == null || this.flags.contains(Flag.INDEPENDENT_INNER_CLASS));
	}

	@Override
	@Nullable
	public String getEnclosingClassName() {
		return this.enclosingClassName;
	}

	@Override
	@Nullable
	public String getSuperClassName() {
		return this.superClassName;
	}

	@Override
	public String[] getInterfaceNames() {
		return this.interfaceNames;
	}

	@Override
	public String[] getMemberClassNames() {
		return this.memberClassNames;
	}


	@Override
	public Set<String> getAnnotationTypes() {
		return this.annotationTypes;
	}

	@Override
	public Set<String> getMetaAnnotationTypes(String annotationName) {
		return super.getMetaAnnotationTypes(annotationName);
	}

	@Override
	public boolean hasMetaAnnotation(String metaAnnotationType) {
		return super.hasMetaAnnotation(metaAnnotationType);
	}

	@Override
	public boolean hasAnnotatedMethods(String annotationName) {
		for (MethodMetadata methodMetadata : this.methodMetadata) {
			if (methodMetadata.isAnnotated(annotationName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
		Set<MethodMetadata> annotatedMethods = new LinkedHashSet<>(4);
		for (MethodMetadata methodMetadata : this.methodMetadata) {
			if (methodMetadata.isAnnotated(annotationName)) {
				annotatedMethods.add(methodMetadata);
			}
		}
		return annotatedMethods;
	}

	@Override
	protected String getDescription() {
		return "class '" + this.className + "'";
	}

	public enum Flag {

		INTERFACE, ANNOTATION, ABSTRACT, FINAL, INDEPENDENT_INNER_CLASS
	}

}
