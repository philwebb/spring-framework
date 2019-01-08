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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;

/**
 * ASM {@link AnnotationVisitor} to collect attribute values.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see DeclaredAttributesAnnotationVisitor
 */
class AttributeValuesAnnotationVisitor extends AnnotationVisitor {

	private static final Object[] EMPTY_OBJECT_ARRAY = {};

	/**
	 * Mapping of object wrappers to their primitive equivalent.
	 */
	private static final Map<Class<?>, Class<?>> PRIMITIVES;
	static {
		Map<Class<?>, Class<?>> primitives = new LinkedHashMap<>();
		primitives.put(Byte.class, byte.class);
		primitives.put(Boolean.class, boolean.class);
		primitives.put(Character.class, char.class);
		primitives.put(Short.class, short.class);
		primitives.put(Integer.class, int.class);
		primitives.put(Long.class, long.class);
		primitives.put(Float.class, float.class);
		primitives.put(Double.class, double.class);
		PRIMITIVES = Collections.unmodifiableMap(primitives);
	}

	private final BiConsumer<String, Object> consumer;

	/**
	 * Create a new {@link DeclaredAttributesAnnotationVisitor} instance.
	 * @param consumer a {@link BiConsumer} that will called with the
	 * {@code name} and {@code value} of each attribute value.
	 */
	public AttributeValuesAnnotationVisitor(BiConsumer<String, Object> consumer) {
		super(SpringAsmInfo.ASM_VERSION);
		this.consumer = consumer;
	}

	protected AttributeValuesAnnotationVisitor() {
		super(SpringAsmInfo.ASM_VERSION);
		this.consumer = (name, value) -> {
			throw new IllegalStateException("Accept method not overridden");
		};
	}

	@Override
	public void visit(String name, Object value) {
		if (value instanceof Type) {
			value = ClassReference.of(((Type) value).getClassName());
		}
		accept(name, value);
	}

	@Override
	public void visitEnum(String name, String desc, String value) {
		String enumType = Type.getType(desc).getClassName();
		accept(name, EnumValueReference.of(enumType, value));
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, String desc) {
		return new DeclaredAttributesAnnotationVisitor(
				attributes -> accept(name, attributes));
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		return new ArrayVisitor(array -> accept(name, convertObjectArray(array)));
	}

	private Object convertObjectArray(Object[] array) {
		if (array.length == 0) {
			return EMPTY_OBJECT_ARRAY;
		}
		Class<?> componentType = array[0].getClass();
		componentType = PRIMITIVES.getOrDefault(componentType, componentType);
		Object converted = Array.newInstance(componentType, array.length);
		for (int i = 0; i < array.length; i++) {
			Array.set(converted, i, array[i]);
		}
		return converted;
	}

	protected void accept(String name, Object value) {
		this.consumer.accept(name, value);
	}

	/**
	 * {@link AnnotationVisitor} to deal with array attributes.
	 */
	private static class ArrayVisitor extends AnnotationVisitor {

		private final List<Object> elements = new ArrayList<>();

		private final Consumer<Object[]> consumer;

		ArrayVisitor(Consumer<Object[]> consumer) {
			super(SpringAsmInfo.ASM_VERSION);
			this.consumer = consumer;
		}

		@Override
		public void visit(String name, Object value) {
			if (value instanceof Type) {
				value = ClassReference.of(((Type) value).getClassName());
			}
			this.elements.add(value);
		}

		@Override
		public void visitEnum(String name, String desc, String value) {
			String enumType = Type.getType(desc).getClassName();
			this.elements.add(EnumValueReference.of(enumType, value));
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String desc) {
			return new DeclaredAttributesAnnotationVisitor(this.elements::add);
		}

		@Override
		public void visitEnd() {
			this.consumer.accept(this.elements.toArray());
		}

	}

}
