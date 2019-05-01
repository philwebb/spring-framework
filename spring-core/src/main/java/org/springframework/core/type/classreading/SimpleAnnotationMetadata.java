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

package org.springframework.core.type.classreading;

import java.util.Set;

import org.springframework.asm.Opcodes;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.lang.Nullable;

/**
 * {@link AnnotationMetadata} created from a
 * {@link SimpleAnnotationMetadataReadingVistor}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
final class SimpleAnnotationMetadata implements AnnotationMetadata {

	private final String className;

	private final int access;

	private final String enclosingClassName;

	private final String superClassName;

	private final boolean independentInnerClass;

	private final String[] interfaceNames;

	private final String[] memberClassNames;

	private final Set<MethodMetadata> annotatedMethods;

	private final MergedAnnotations annotations;

	SimpleAnnotationMetadata(String className, int access, String enclosingClassName,
			String superClassName, boolean independentInnerClass, String[] interfaceNames,
			String[] memberClassNames, Set<MethodMetadata> annotatedMethods,
			MergedAnnotations annotations) {
		this.className = className;
		this.access = access;
		this.enclosingClassName = enclosingClassName;
		this.superClassName = superClassName;
		this.independentInnerClass = independentInnerClass;
		this.interfaceNames = interfaceNames;
		this.memberClassNames = memberClassNames;
		this.annotatedMethods = annotatedMethods;
		this.annotations = annotations;
	}

	@Override
	public String getClassName() {
		return this.className;
	}

	@Override
	public boolean isInterface() {
		return (this.access & Opcodes.ACC_INTERFACE) != 0;
	}

	@Override
	public boolean isAnnotation() {
		return (this.access & Opcodes.ACC_ANNOTATION) != 0;
	}

	@Override
	public boolean isAbstract() {
		return (this.access & Opcodes.ACC_ABSTRACT) != 0;
	}

	@Override
	public boolean isFinal() {
		return (this.access & Opcodes.ACC_FINAL) != 0;
	}

	@Override
	public boolean isIndependent() {
		return this.enclosingClassName == null || this.independentInnerClass;
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
	public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
		return this.annotatedMethods;
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

}
