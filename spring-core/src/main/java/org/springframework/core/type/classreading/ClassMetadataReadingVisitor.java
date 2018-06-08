/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.Attribute;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.core.type.ClassMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * ASM class visitor which looks only for the class name and implemented types,
 * exposing them through the {@link org.springframework.core.type.ClassMetadata}
 * interface.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Mark Fisher
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @since 2.5
 * @deprecated
 */
@Deprecated
class ClassMetadataReadingVisitor extends ClassVisitor implements ClassMetadata {

	private String className = "";

	private EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);

	@Nullable
	private String enclosingClassName;

	@Nullable
	private String superClassName;

	private String[] interfaces = new String[0];

	private Set<String> memberClassNames = new LinkedHashSet<>(4);


	public ClassMetadataReadingVisitor() {
		super(SpringAsmInfo.ASM_VERSION);
	}


	@Override
	public void visit(
			int version, int access, String name, String signature, @Nullable String supername, String[] interfaces) {

		this.className = ClassUtils.convertResourcePathToClassName(name);
		toggle(Flag.INTERFACE, ((access & Opcodes.ACC_INTERFACE) != 0));
		toggle(Flag.ANNOTATION, ((access & Opcodes.ACC_ANNOTATION) != 0));
		toggle(Flag.ABSTRACT, ((access & Opcodes.ACC_ABSTRACT) != 0));
		toggle(Flag.FINAL, ((access & Opcodes.ACC_FINAL) != 0));
		if (supername != null && !flags.contains(Flag.INTERFACE)) {
			this.superClassName = ClassUtils.convertResourcePathToClassName(supername);
		}
		this.interfaces = new String[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			this.interfaces[i] = ClassUtils.convertResourcePathToClassName(interfaces[i]);
		}
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		this.enclosingClassName = ClassUtils.convertResourcePathToClassName(owner);
	}

	@Override
	public void visitInnerClass(String name, @Nullable String outerName, String innerName, int access) {
		if (outerName != null) {
			String fqName = ClassUtils.convertResourcePathToClassName(name);
			String fqOuterName = ClassUtils.convertResourcePathToClassName(outerName);
			if (this.className.equals(fqName)) {
				this.enclosingClassName = fqOuterName;
				toggle(Flag.INDEPENDENT_INNER_CLASS, ((access & Opcodes.ACC_STATIC) != 0));
			}
			else if (this.className.equals(fqOuterName)) {
				this.memberClassNames.add(fqName);
			}
		}
	}

	@Override
	public void visitSource(String source, String debug) {
		// no-op
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		// no-op
		return new EmptyAnnotationVisitor();
	}

	@Override
	public void visitAttribute(Attribute attr) {
		// no-op
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		// no-op
		return new EmptyFieldVisitor();
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		// no-op
		return new EmptyMethodVisitor();
	}

	@Override
	public void visitEnd() {
		// no-op
	}

	private void toggle(Flag flag, boolean add) {
		if (add) {
			this.flags.add(flag);
		}
		else {
			this.flags.remove(flag);
		}
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
		return this.interfaces;
	}

	@Override
	public String[] getMemberClassNames() {
		return StringUtils.toStringArray(this.memberClassNames);
	}


	private static class EmptyAnnotationVisitor extends AnnotationVisitor {

		public EmptyAnnotationVisitor() {
			super(SpringAsmInfo.ASM_VERSION);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String desc) {
			return this;
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			return this;
		}
	}


	private static class EmptyMethodVisitor extends MethodVisitor {

		public EmptyMethodVisitor() {
			super(SpringAsmInfo.ASM_VERSION);
		}
	}


	private static class EmptyFieldVisitor extends FieldVisitor {

		public EmptyFieldVisitor() {
			super(SpringAsmInfo.ASM_VERSION);
		}
	}

	private static enum Flag {

		INTERFACE, ANNOTATION, ABSTRACT, FINAL, INDEPENDENT_INNER_CLASS

	}

}
