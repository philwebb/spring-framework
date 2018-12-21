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

import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Consumer;

import org.junit.Test;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.SpringAsmInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DeclaredAttributesAnnotationVisitor}.
 *
 * @author Phillip Webb
 */
public class DeclaredAttributesAnnotationVisitorTests {

	private DeclaredAttributes attributes;

	@Test
	public void createWhenConsumerIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new DeclaredAttributesAnnotationVisitor(null)).withMessage(
						"DeclaredAttributesConsumer must not be null");
	}

	@Test
	public void acceptCreatesExpectedAttributes() throws Exception {
		ClassReader reader = new ClassReader(ExampleClass.class.getName());
		reader.accept(new Visitor(this::saveAttributes), ClassReader.SKIP_DEBUG);
		DeclaredAttributes a = this.attributes;
		assertThat(a.get("byteValue")).isEqualTo((byte) 123);
		assertThat(a.get("byteArray")).isEqualTo(new byte[] { 123, 124 });
		assertThat(a.get("booleanValue")).isEqualTo(true);
		assertThat(a.get("booleanArray")).isEqualTo(new boolean[] { true, false });
		assertThat(a.get("charValue")).isEqualTo('c');
		assertThat(a.get("charArray")).isEqualTo(new char[] { 'c', 'h' });
		assertThat(a.get("shortValue")).isEqualTo((short) 234);
		assertThat(a.get("shortArray")).isEqualTo(new short[] { 234, 235 });
		assertThat(a.get("intValue")).isEqualTo(345);
		assertThat(a.get("intArray")).isEqualTo(new int[] { 345, 346 });
		assertThat(a.get("longValue")).isEqualTo(456L);
		assertThat(a.get("longArray")).isEqualTo(new long[] { 456, 457 });
		assertThat(a.get("floatValue")).isEqualTo(1.23F);
		assertThat(a.get("floatArray")).isEqualTo(new float[] { 1.23F, 1.24F });
		assertThat(a.get("doubleValue")).isEqualTo(2.34);
		assertThat(a.get("doubleArray")).isEqualTo(new double[] { 2.34, 2.35 });
		assertThat(a.get("stringValue")).isEqualTo("st");
		assertThat(a.get("stringArray")).isEqualTo(new String[] { "st", "ri" });
		assertThat(a.get("classValue")).isEqualTo(ClassReference.from(StringBuilder.class));
		assertThat(a.get("classArray")).isEqualTo(
				new ClassReference[] { ClassReference.from(StringBuilder.class),
					ClassReference.from(InputStream.class) });
		assertThat(a.get("enumValue")).isEqualTo(
				EnumValueReference.from(ExampleEnum.ONE));
		assertThat(a.get("enumArray")).isEqualTo(
				new EnumValueReference[] { EnumValueReference.from(ExampleEnum.ONE),
					EnumValueReference.from(ExampleEnum.TWO) });
		DeclaredAttributes nested = (DeclaredAttributes) a.get("nestedValue");
		assertThat(nested.get("value")).isEqualTo("n");
		DeclaredAttributes[] nestedArray = (DeclaredAttributes[]) a.get("nestedArray");
		assertThat(nestedArray).hasSize(2);
		assertThat(nestedArray[0].get("value")).isEqualTo("n");
		assertThat(nestedArray[1].get("value")).isEqualTo("e");
		assertThat(a.get("emptyNestedArray")).isNotNull();
	}

	private void saveAttributes(DeclaredAttributes attributes) {
		this.attributes = attributes;
	}

	private static class Visitor extends ClassVisitor {

		private final Consumer<DeclaredAttributes> consumer;

		public Visitor(Consumer<DeclaredAttributes> consumer) {
			super(SpringAsmInfo.ASM_VERSION);
			this.consumer = consumer;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			return new DeclaredAttributesAnnotationVisitor(this.consumer);
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface ExampleAnnotation {

		byte byteValue();

		byte[] byteArray();

		boolean booleanValue();

		boolean[] booleanArray();

		char charValue();

		char[] charArray();

		short shortValue();

		short[] shortArray();

		int intValue();

		int[] intArray();

		long longValue();

		long[] longArray();

		float floatValue();

		float[] floatArray();

		double doubleValue();

		double[] doubleArray();

		String stringValue();

		String[] stringArray();

		Class<?> classValue();

		Class<?>[] classArray();

		ExampleEnum enumValue();

		ExampleEnum[] enumArray();

		Nested nestedValue();

		Nested[] nestedArray();

		Nested[] emptyNestedArray();

	}

	@interface Nested {

		String value();

	}

	enum ExampleEnum {

		ONE, TWO, THREE

	}

	// @formatter:off
	@ExampleAnnotation(
			byteValue = 123,
			byteArray = { 123, 124 },
			booleanValue = true,
			booleanArray = { true, false },
			charValue = 'c',
			charArray = { 'c', 'h' },
			shortValue = 234,
			shortArray = { 234, 235 },
			intValue = 345,
			intArray = { 345, 346 },
			longValue = 456,
			longArray = { 456, 457 },
			floatValue = 1.23F,
			floatArray = { 1.23F, 1.24F },
			doubleValue = 2.34,
			doubleArray = { 2.34, 2.35 },
			stringValue = "st",
			stringArray = { "st", "ri" },
			classValue = StringBuilder.class,
			classArray = { StringBuilder.class, InputStream.class },
			enumValue = ExampleEnum.ONE,
			enumArray = { ExampleEnum.ONE, ExampleEnum.TWO },
			nestedValue = @Nested("n"),
			nestedArray = { @Nested("n"), @Nested("e") }, emptyNestedArray = {})
	static class ExampleClass {

	}
	// @formatter:on

}
