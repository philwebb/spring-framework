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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.Attribute;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;

/**
 * ASM class visitor to create {@link SimpleAnnotationMetadata}.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.1
 */
class SimpleAnnotationMetadataReadingVistor extends ClassVisitor {

	@Nullable
	private final ClassLoader classLoader;

	private String className = "";

	private int access;

	private String enclosingClassName;

	private boolean independentInnerClass;

	private String superClassName;

	private String[] interfaceNames;

	private Set<String> memberClassNames = new LinkedHashSet<>(4);

	private final Set<MethodMetadataVisitor> methodVistors = new LinkedHashSet<>(4);

	SimpleAnnotationMetadataReadingVistor(@Nullable ClassLoader classLoader) {
		super(SpringAsmInfo.ASM_VERSION);
		this.classLoader = classLoader;
	}

	@Override
	public void visit(int version, int access, String name, String signature,
			@Nullable String supername, String[] interfaces) {
		this.className = ClassUtils.convertResourcePathToClassName(name);
		this.access = access;
		if (supername != null && !((access & Opcodes.ACC_INTERFACE) != 0)) {
			this.superClassName = ClassUtils.convertResourcePathToClassName(supername);
		}
		this.interfaceNames = new String[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			this.interfaceNames[i] = ClassUtils.convertResourcePathToClassName(
					interfaces[i]);
		}
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		this.enclosingClassName = ClassUtils.convertResourcePathToClassName(owner);
	}

	@Override
	public void visitInnerClass(String name, @Nullable String outerName, String innerName,
			int access) {
		if (outerName != null) {
			String className = ClassUtils.convertResourcePathToClassName(name);
			String outerClassName = ClassUtils.convertResourcePathToClassName(outerName);
			if (this.className.equals(className)) {
				this.enclosingClassName = outerClassName;
				this.independentInnerClass = ((access & Opcodes.ACC_STATIC) != 0);
			}
			else if (this.className.equals(outerClassName)) {
				this.memberClassNames.add(className);
			}
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return MergedAnnotationMetadataVisitor.get(desc, visible);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		// Skip bridge methods - we're only interested in original
		// annotation-defining
		// user methods.
		// On JDK 8, we'd otherwise run into double detection of the same
		// annotated
		// method...
		if ((access & Opcodes.ACC_BRIDGE) != 0) {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
		return new MethodMetadataVisitor(access, name, desc);
	}

	@Override
	public void visitEnd() {
	}

	public SimpleAnnotationMetadata getMetadata() {
		Set<SimpleMethodMetadata> methodMetadata = getMethodMetadata();
		String[] memberClassNames = StringUtils.toStringArray(this.memberClassNames);
		// return new SimpleAnnotationMetadata(this.classLoader,
		// this.annotationAttributes,
		// this.metaAnnotations, this.className, flags, this.enclosingClassName,
		// this.superClassName,
		// this.interfaceNames, memberClassNames, this.annotationTypes,
		// methodMetadata);
		return null;
	}

	private Set<SimpleMethodMetadata> getMethodMetadata() {
		Set<SimpleMethodMetadata> methodMetadata = new LinkedHashSet<>(
				this.methodVistors.size());
		for (MethodMetadataVisitor methodVistor : this.methodVistors) {
			methodMetadata.add(methodVistor.getMetadata());
		}
		return methodMetadata;
	}

	private class MethodMetadataVisitor extends MethodVisitor {

		private final int access;

		private final String name;

		private final String desc;

		private MethodMetadataVisitor(int access, String name, String desc) {
			super(SpringAsmInfo.ASM_VERSION);
			this.access = access;
			this.name = name;
			this.desc = desc;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			return MergedAnnotationMetadataVisitor.get(desc, visible);
		}

		@Override
		public void visitEnd() {
		}

		public SimpleMethodMetadata getMetadata() {
			// return new SimpleMethodMetadata(
			// SimpleAnnotationMetadataReadingVistor.this.classLoader,
			// this.annotationAttributes, this.metaAnnotations, this.methodName,
			// flags,
			// declaringClassName, returnTypeName);
			return null;
		}

	}

}
