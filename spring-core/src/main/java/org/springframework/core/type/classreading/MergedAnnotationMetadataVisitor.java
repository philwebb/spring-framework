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

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationFilter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link AnnotationVisitor} that can be used to construct a
 * {@link MergedAnnotation}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class MergedAnnotationMetadataVisitor<A extends Annotation> extends AnnotationVisitor {

	@Nullable
	private final ClassLoader classLoader;

	@Nullable
	private final Object source;

	private final Class<A> annotationType;

	private final Consumer<MergedAnnotation<A>> consumer;

	private final Map<String, Object> attributes = new LinkedHashMap<>(4);

	public MergedAnnotationMetadataVisitor(ClassLoader classLoader,
			@Nullable Object source, Class<A> annotationType,
			Consumer<MergedAnnotation<A>> consumer) {
		super(SpringAsmInfo.ASM_VERSION);
		this.classLoader = classLoader;
		this.source = source;
		this.annotationType = annotationType;
		this.consumer = consumer;
	}

	@Override
	public void visit(String name, Object value) {
		this.attributes.put(name, value);
	}

	@Override
	public void visitEnum(String name, String descriptor, String value) {
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, String descriptor) {
		return null;
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		return null;
	}

	@Override
	public void visitEnd() {
		MergedAnnotation<A> annotation = MergedAnnotation.from(this.source,
				this.annotationType, this.attributes);
		this.consumer.accept(annotation);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> AnnotationVisitor get(
			@Nullable ClassLoader classLoader, @Nullable Object source, String descriptor,
			boolean visible, Consumer<MergedAnnotation<A>> consumer) {
		if (!visible) {
			return null;
		}
		String typeName = Type.getType(descriptor).getClassName();
		if (AnnotationFilter.PLAIN.matches(typeName)) {
			return null;
		}
		try {
			Class<A> annotationType = (Class<A>) ClassUtils.forName(typeName,
					classLoader);
			return new MergedAnnotationMetadataVisitor<>(classLoader, source,
					annotationType, consumer);
		}
		catch (ClassNotFoundException | LinkageError ex) {
			return null;
		}
	}

}
