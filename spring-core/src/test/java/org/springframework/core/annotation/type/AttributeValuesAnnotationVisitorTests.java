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

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.junit.Test;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.SpringAsmInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link AttributeValuesAnnotationVisitor}.
 *
 * @author Phillip Webb
 */
public class AttributeValuesAnnotationVisitorTests {

	@Test
	public void visitConsumesSimpleTypes() throws Exception {
		Map<String, Object> attributes = new LinkedHashMap<>();
		visit(WithSimpleTypes.class, attributes::put);
		assertThat(attributes).containsOnly(entry("stringValue", "a"),
				entry("intValue", 1), entry("charValue", 'c'));
	}

	@Test
	public void vistWhenHasClassAttributeConsumesAsClassReference() throws Exception {
		Map<String, Object> attributes = new LinkedHashMap<>();
		visit(WithClassType.class, attributes::put);
		assertThat(attributes).containsOnly(
				entry("classValue", ClassReference.from(StringBuilder.class)));
	}

	@Test
	public void vistWhenHasEnumAttributeConsumesAsEnumReference() throws Exception {
		Map<String, Object> attributes = new LinkedHashMap<>();
		visit(WithEnumType.class, attributes::put);
		assertThat(attributes).containsOnly(
				entry("enumValue", EnumValueReference.from(ExampleEnum.ONE)));
	}

	@Test
	public void visitConsumesSimpleArrayTypes() throws Exception {
		Map<String, Object> attributes = new LinkedHashMap<>();
		visit(WithSimpleArrayTypes.class, attributes::put);
		assertThat(attributes).containsOnly(entry("byteArray", new byte[] { 1 }),
				entry("booleanArray", new boolean[] { false }),
				entry("charArray", new char[] { 'c' }),
				entry("shortArray", new short[] { 1 }),
				entry("intArray", new int[] { 1 }), entry("longArray", new long[] { 1 }),
				entry("floatArray", new float[] { 0.1f }),
				entry("doubleArray", new double[] { 0.1 }));
	}

	@Test
	public void visitConsumesEmptySimpleArrayTypes() throws Exception {
		Map<String, Object> attributes = new LinkedHashMap<>();
		visit(WithEmptySimpleArrayTypes.class, attributes::put);
		// We can't easily detect the primitive array type so we just use
		// an empty object array
		assertThat(attributes).containsOnly(entry("byteArray", new Object[] {}),
				entry("booleanArray", new Object[] {}),
				entry("charArray", new Object[] {}), entry("shortArray", new Object[] {}),
				entry("intArray", new Object[] {}), entry("longArray", new Object[] {}),
				entry("floatArray", new Object[] {}),
				entry("doubleArray", new Object[] {}));
	}

	@Test
	public void vistWhenHasClassArrayAttributeConsumesAsClassReferenceArray()
			throws Exception {
		Map<String, Object> attributes = new LinkedHashMap<>();
		visit(WithClassArrayType.class, attributes::put);
		assertThat(attributes).containsOnly(entry("classArray",
				new ClassReference[] { ClassReference.from(StringBuilder.class) }));
	}

	@Test
	public void vistWhenHasEnumArrayAttributeConsumesAsEnumReferenceArray()
			throws IOException {
		Map<String, Object> attributes = new LinkedHashMap<>();
		visit(WithEnumArrayType.class, attributes::put);
		assertThat(attributes).containsOnly(entry("enumArray",
				new EnumValueReference[] { EnumValueReference.from(ExampleEnum.ONE) }));
	}

	private void visit(Class<?> type, BiConsumer<String, Object> consumer)
			throws IOException {
		AnnotationVisitor annotationVisitor = new AttributeValuesAnnotationVisitor(
				consumer);
		ClassVisitor classVisitor = new ClassVisitor(SpringAsmInfo.ASM_VERSION) {

			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				return annotationVisitor;
			}

		};
		ClassReader reader = new ClassReader(type.getName());
		reader.accept(classVisitor, ClassReader.SKIP_DEBUG);
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface SimpleTypes {

		String stringValue();

		int intValue();

		char charValue();

	}

	@SimpleTypes(stringValue = "a", intValue = 1, charValue = 'c')
	private static class WithSimpleTypes {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface ClassType {

		Class<? extends CharSequence> classValue();

	}

	@ClassType(classValue = StringBuilder.class)
	private static class WithClassType {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface EnumType {

		ExampleEnum enumValue();

	}

	@EnumType(enumValue = ExampleEnum.ONE)
	private static class WithEnumType {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface SimpleArrayTypes {

		byte[] byteArray();

		boolean[] booleanArray();

		char[] charArray();

		short[] shortArray();

		int[] intArray();

		long[] longArray();

		float[] floatArray();

		double[] doubleArray();

	}

	@SimpleArrayTypes(byteArray = { 1 }, booleanArray = { false }, charArray = {
		'c' }, shortArray = { 1 }, intArray = {
			1 }, longArray = { 1 }, floatArray = { 0.1f }, doubleArray = { 0.1 })
	private static class WithSimpleArrayTypes {

	}

	@SimpleArrayTypes(byteArray = {}, booleanArray = {}, charArray = {}, shortArray = {}, intArray = {}, longArray = {}, floatArray = {}, doubleArray = {})
	private static class WithEmptySimpleArrayTypes {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface ClassArrayType {

		Class<? extends CharSequence>[] classArray();

	}

	@ClassArrayType(classArray = { StringBuilder.class })
	private static class WithClassArrayType {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface EnumArrayType {

		ExampleEnum[] enumArray();

	}

	@EnumArrayType(enumArray = { ExampleEnum.ONE })
	private static class WithEnumArrayType {

	}

	private static enum ExampleEnum {

		ONE, TWO, THREE

	}
}
