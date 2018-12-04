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

package org.springframework.core.annotation.type;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * {@link AnnotationTypeResolver} implementation based on an ASM
 * {@link org.springframework.asm.ClassReader}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class SimpleAnnotationTypeResolver {

	private static final Map<ClassLoader, SimpleAnnotationTypeResolver> cache = new ConcurrentReferenceHashMap<>(
			4);

	private static final Map<Resource, AnnotationType> resourceCache = new ConcurrentReferenceHashMap<>(
			4);

	private final ResourceLoader resourceLoader;

	private SimpleAnnotationTypeResolver(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	public AnnotationType resolve(String className) {
		Assert.notNull(className, "ClassName must not be null");
		return resolve(className, true);
	}

	public AnnotationType resolve(String className, boolean useCache) {
		String resourcePath = getResourcePath(className);
		Resource resource = this.resourceLoader.getResource(resourcePath);
		return resolve(resource, useCache);
	}

	private AnnotationType resolve(Resource resource, boolean useCache) {
		if (!useCache) {
			return load(resource);
		}
		return resourceCache.computeIfAbsent(resource, this::load);
	}

	private AnnotationType load(Resource resource) {
		try {
			try (InputStream inputStream = new BufferedInputStream(
					resource.getInputStream())) {
				return load(inputStream);
			}
		}
		catch (IOException ex) {
			return null;
		}
	}

	private AnnotationType load(InputStream inputStream) throws IOException {
		ClassReader classReader = new ClassReader(inputStream);
		AnnotationTypeClassVisitor visitor = new AnnotationTypeClassVisitor();
		classReader.accept(visitor, ClassReader.SKIP_DEBUG);
		return visitor.getAnnotationType();

	}

	private String getResourcePath(String className) {
		return ResourceLoader.CLASSPATH_URL_PREFIX
				+ ClassUtils.convertClassNameToResourcePath(className)
				+ ClassUtils.CLASS_FILE_SUFFIX;
	}

	public static SimpleAnnotationTypeResolver get(ClassLoader classLoader) {
		if (classLoader == null) {
			return createResolver(classLoader);
		}
		return cache.computeIfAbsent(classLoader,
				SimpleAnnotationTypeResolver::createResolver);
	}

	private static SimpleAnnotationTypeResolver createResolver(ClassLoader classLoader) {
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader(classLoader);
		return new SimpleAnnotationTypeResolver(resourceLoader);
	}

	/**
	 * {@link ClassVisitor} used to parse the annotation class bytecode.
	 */
	private static class AnnotationTypeClassVisitor extends ClassVisitor {

		private final List<DeclaredAnnotation> declaredAnnotations = new ArrayList<>();

		private final List<AttributeType> attributeTypes = new ArrayList<>();

		private String type;

		public AnnotationTypeClassVisitor() {
			super(SpringAsmInfo.ASM_VERSION);
		}

		@Override
		public void visit(int version, int access, String name, String signature,
				String superName, String[] interfaces) {
			this.type = Type.getObjectType(name).getClassName();
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			return DeclaredAnnotationAnnotationVisitor.forTypeDescriptor(desc,
					this.declaredAnnotations::add);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc,
				String signature, String[] exceptions) {
			String returnType = Type.getReturnType(desc).getClassName();
			return new AnnotationTypeMethodVisitor(this.type, name, returnType,
					this.attributeTypes::add);
		}

		public AnnotationType getAnnotationType() {
			return new SimpleAnnotationType(this.type,
					new SimpleDeclaredAnnotations(new Source(this.type),
							this.declaredAnnotations),
					new SimpleAttributeTypes(this.attributeTypes));
		}

	}

	/**
	 * {@link MethodVisitor} used to parse the annotation method bytecode.
	 */
	private static class AnnotationTypeMethodVisitor extends MethodVisitor {

		private final String declaringClass;

		private final String name;

		private final String type;

		private final Consumer<AttributeType> consumer;

		private final List<DeclaredAnnotation> declaredAnnotations = new ArrayList<>();

		private Object defaultValue;

		public AnnotationTypeMethodVisitor(String declaringClass, String name,
				String type, Consumer<AttributeType> consumer) {
			super(SpringAsmInfo.ASM_VERSION);
			this.declaringClass = declaringClass;
			this.name = name;
			this.type = type;
			this.consumer = consumer;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			return DeclaredAnnotationAnnotationVisitor.forTypeDescriptor(desc,
					this.declaredAnnotations::add);
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			return new AttributeValuesAnnotationVisitor(this::visitDefaultValue);
		}

		private void visitDefaultValue(String name, Object value) {
			this.defaultValue = value;
		}

		@Override
		public void visitEnd() {
			Source source = new Source(
					this.type + ":" + this.declaringClass + "." + this.name);
			AttributeType attributeType = new SimpleAttributeType(this.name, this.type,
					new SimpleDeclaredAnnotations(source, this.declaredAnnotations),
					this.defaultValue);
			this.consumer.accept(attributeType);
		}

	}

	/**
	 * ASM source for use with {@link DeclaredAnnotations}.
	 */
	private static class Source {

		private final String name;

		public Source(String name) {
			this.name = name;
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			return this.name.equals(((Source) obj).name);
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

}
