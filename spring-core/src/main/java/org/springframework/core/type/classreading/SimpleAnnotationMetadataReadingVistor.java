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
import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
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

	private final Set<String> annotationTypes = new LinkedHashSet<>(4);

	private final Map<String, Set<String>> metaAnnotations = new LinkedHashMap<>(4);

	private final LinkedMultiValueMap<String, AnnotationAttributes> annotationAttributes = new LinkedMultiValueMap<>(4);

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
	public void visitSource(String source, String debug) {
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
		String className = Type.getType(desc).getClassName();
		this.annotationTypes.add(className);
		return new AnnotationAttributesReadingVisitor(className,
				this.annotationAttributes, this.metaAnnotations, this.classLoader);
	}

	@Override
	public void visitAttribute(Attribute attr) {
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature,
			Object value) {
		return EmptyFieldVisitor.INSTANCE;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		// Skip bridge methods - we're only interested in original annotation-defining
		// user methods.
		// On JDK 8, we'd otherwise run into double detection of the same annotated
		// method...
		if ((access & Opcodes.ACC_BRIDGE) != 0) {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
		return new MethodMetadataVisitor(name, access, Type.getReturnType(desc).getClassName());
	}

	@Override
	public void visitEnd() {
	}

	public SimpleAnnotationMetadata getMetadata() {
		EnumSet<SimpleAnnotationMetadata.Flag> flags = getFlags();
		Set<SimpleMethodMetadata> methodMetadata = getMethodMetadata();
		String[] memberClassNames= StringUtils.toStringArray(this.memberClassNames);
		return new SimpleAnnotationMetadata(this.classLoader, this.annotationAttributes,
				this.metaAnnotations, this.className, flags, this.enclosingClassName, this.superClassName,
				this.interfaceNames, memberClassNames, this.annotationTypes, methodMetadata);
	}

	private EnumSet<SimpleAnnotationMetadata.Flag> getFlags() {
		EnumSet<SimpleAnnotationMetadata.Flag> flags = EnumSet.noneOf(
				SimpleAnnotationMetadata.Flag.class);
		setAccessFlag(flags, Opcodes.ACC_INTERFACE, SimpleAnnotationMetadata.Flag.INTERFACE);
		setAccessFlag(flags, Opcodes.ACC_ANNOTATION, SimpleAnnotationMetadata.Flag.ANNOTATION);
		setAccessFlag(flags, Opcodes.ACC_ABSTRACT, SimpleAnnotationMetadata.Flag.ABSTRACT);
		setAccessFlag(flags, Opcodes.ACC_INTERFACE, SimpleAnnotationMetadata.Flag.INTERFACE);
		setAccessFlag(flags, Opcodes.ACC_FINAL, SimpleAnnotationMetadata.Flag.FINAL);
		if (this.independentInnerClass) {
			flags.add(SimpleAnnotationMetadata.Flag.INDEPENDENT_INNER_CLASS);
		}
		return flags;
	}

	private void setAccessFlag(EnumSet<SimpleAnnotationMetadata.Flag> flags,
			int accessCode, SimpleAnnotationMetadata.Flag flag) {
		if ((this.access & accessCode) != 0) {
			flags.add(flag);
		}
	}

	private Set<SimpleMethodMetadata> getMethodMetadata() {
		Set<SimpleMethodMetadata> methodMetadata = new LinkedHashSet<>(
				this.methodVistors.size());
		for (MethodMetadataVisitor methodVistor : this.methodVistors) {
			methodMetadata.add(methodVistor.getMetadata());
		}
		return methodMetadata;
	}


	private static class EmptyFieldVisitor extends FieldVisitor {

		private static final FieldVisitor INSTANCE = new EmptyFieldVisitor();

		public EmptyFieldVisitor() {
			super(SpringAsmInfo.ASM_VERSION);
		}
	}


	private class MethodMetadataVisitor extends MethodVisitor {

		private final String methodName;

		private final int access;

		private final String returnTypeName;

		private final Map<String, Set<String>> metaAnnotations = new LinkedHashMap<>(4);

		private final LinkedMultiValueMap<String, AnnotationAttributes> annotationAttributes = new LinkedMultiValueMap<>(4);


		private MethodMetadataVisitor(String methodName, int access, String returnTypeName) {
			super(SpringAsmInfo.ASM_VERSION);
			this.methodName = methodName;
			this.access = access;
			this.returnTypeName = returnTypeName;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			SimpleAnnotationMetadataReadingVistor.this.methodVistors.add(this);
			String className = Type.getType(desc).getClassName();
			return new AnnotationAttributesReadingVisitor(className, this.annotationAttributes,
					this.metaAnnotations, SimpleAnnotationMetadataReadingVistor.this.classLoader);
		}

		public SimpleMethodMetadata getMetadata() {
			String declaringClassName = SimpleAnnotationMetadataReadingVistor.this.className;
			Set<SimpleMethodMetadata.Flag> flags = getFlags();
			return new SimpleMethodMetadata(
					SimpleAnnotationMetadataReadingVistor.this.classLoader,
					this.annotationAttributes, this.metaAnnotations, this.methodName, flags,
					declaringClassName, returnTypeName);
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

}
