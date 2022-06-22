/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aot.graalvm;

import java.lang.annotation.Annotation;

import com.oracle.svm.core.meta.ReadableJavaField;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * @author Phillip Webb
 */
class ConstantReadableJavaField implements ReadableJavaField {

	private final ResolvedJavaField original;

	private final JavaConstant constant;

	public ConstantReadableJavaField(ResolvedJavaField original, JavaConstant constant) {
		this.original = original;
		this.constant = constant;
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return this.original.getAnnotation(annotationClass);
	}

	public Annotation[] getAnnotations() {
		return this.original.getAnnotations();
	}

	public Annotation[] getDeclaredAnnotations() {
		return this.original.getDeclaredAnnotations();
	}

	public ResolvedJavaType getDeclaringClass() {
		return this.original.getDeclaringClass();
	}

	public int getModifiers() {
		return this.original.getModifiers();
	}

	public String getName() {
		return this.original.getName();
	}

	public int getOffset() {
		return this.original.getOffset();
	}

	public JavaType getType() {
		return this.original.getType();
	}

	public boolean isInternal() {
		return this.original.isInternal();
	}

	public boolean isSynthetic() {
		return this.original.isSynthetic();
	}

	@Override
	public JavaConstant readValue(MetaAccessProvider metaAccess, JavaConstant receiver) {
		return constant;
	}

	@Override
	public boolean allowConstantFolding() {
		return true;
	}

	@Override
	public boolean injectFinalForRuntimeCompilation() {
		return true;
	}

}