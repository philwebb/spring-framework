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
import java.util.Map;
import java.util.Set;

import org.springframework.asm.Opcodes;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.LinkedMultiValueMap;

/**
 * {@link MethodMetadata} returned from a {@link SimpleMetadataReader}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public class SimpleMethodMetadata implements MethodMetadata {

	// FIXME make package private

	private final String methodName;

	private final Set<Flag> flags;

	private final String declaringClassName;

	private final String returnTypeName;

	SimpleMethodMetadata(ClassLoader classLoader,
			LinkedMultiValueMap<String, AnnotationAttributes> annotationAttributes,
			Map<String, Set<String>> metaAnnotations, String methodName, Set<Flag> flags,
			String declaringClassName, String returnTypeName) {
		this.methodName = methodName;
		this.flags = flags;
		this.declaringClassName = declaringClassName;
		this.returnTypeName = returnTypeName;
	}

	Set<Flag> getFlags() {
		return flags;
	}

	@Override
	public String getMethodName() {
		return this.methodName;
	}

	@Override
	public boolean isAbstract() {
		return flags.contains(Flag.ABSTRACT);
	}

	@Override
	public boolean isStatic() {
		return flags.contains(Flag.STATIC);
	}

	@Override
	public boolean isFinal() {
		return flags.contains(Flag.FINAL);
	}

	@Override
	public boolean isOverridable() {
		return this.flags.contains(Flag.OVERRIDABLE);
	}

	@Override
	public String getDeclaringClassName() {
		return this.declaringClassName;
	}

	@Override
	public String getReturnTypeName() {
		return this.returnTypeName;
	}

	@Override
	public MergedAnnotations getAnnotations() {
		// FIXME
		return null;
	}

	public enum Flag {

		ABSTRACT, STATIC, FINAL, OVERRIDABLE

	}

	private Set<SimpleMethodMetadata.Flag> getFlags() {
		EnumSet<SimpleMethodMetadata.Flag> flags = EnumSet.noneOf(SimpleMethodMetadata.Flag.class);
		setAccessFlag(flags, Opcodes.ACC_ABSTRACT, SimpleMethodMetadata.Flag.ABSTRACT);
		setAccessFlag(flags, Opcodes.ACC_STATIC, SimpleMethodMetadata.Flag.STATIC);
		setAccessFlag(flags, Opcodes.ACC_FINAL, SimpleMethodMetadata.Flag.FINAL);
		if ((this.access & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE)) == 0) {
			flags.add(SimpleMethodMetadata.Flag.OVERRIDABLE);
		}
		return flags;
	}

	private void setAccessFlag(EnumSet<SimpleMethodMetadata.Flag> flags,
			int accessCode, SimpleMethodMetadata.Flag flag) {
		if ((this.access & accessCode) != 0) {
			flags.add(flag);
		}
	}


}
