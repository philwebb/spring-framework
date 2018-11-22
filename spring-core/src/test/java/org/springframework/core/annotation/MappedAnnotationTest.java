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

package org.springframework.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.Test;

import org.springframework.core.annotation.MergedAnnotation.MapValues;
import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AnnotationTypeResolver;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.AttributeTypes;
import org.springframework.core.annotation.type.ClassReference;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.core.annotation.type.EnumValueReference;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.fail;

/**
 * Tests for {@link MappedAnnotation}.
 *
 * @author Phillip Webb
 * @since 5.0
 */
public class MappedAnnotationTest {

	private AnnotationTypeResolver resolver = AnnotationTypeResolver.get(
			ClassUtils.getDefaultClassLoader());

	@Test
	public void getTypeReturnsType() {
		MergedAnnotation<?> annotation = create((byte) 123, byte.class, null);
		assertThat(annotation.getType()).isEqualTo(Example.class.getName());
	}

	@Test
	public void hasNonDefaultValueWhenHasDefaultValueReturnsFalse() {
		MergedAnnotation<?> annotation = create(123, int.class, 123);
		assertThat(annotation.hasNonDefaultValue("value")).isFalse();
	}

	@Test
	public void hasNoneDefaultValueWhenHasNonDefaultValueReturnsTrue() {
		MergedAnnotation<?> annotation = create(456, int.class, 123);
		assertThat(annotation.hasNonDefaultValue("value")).isTrue();
	}

	@Test
	public void hasDefaultValueWhenHasDefaultValueReturnsTrue() {
		MergedAnnotation<?> annotation = create(123, int.class, 123);
		assertThat(annotation.hasDefaultValue("value")).isTrue();
	}

	@Test
	public void hasDefaultValueWhenHasNonDefaultValueReturnsFalse() {
		MergedAnnotation<?> annotation = create(456, int.class, 123);
		assertThat(annotation.hasDefaultValue("value")).isFalse();
	}

	@Test
	public void getByteWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create((byte) 123, byte.class, null);
		assertThat(annotation.getByte("value")).isEqualTo((byte) 123);
	}

	@Test
	public void getByteWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, byte.class, (byte) 123);
		assertThat(annotation.getByte("value")).isEqualTo((byte) 123);
	}

	@Test
	public void getByteArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(new byte[] { 123 }, byte[].class, null);
		assertThat(annotation.getByteArray("value")).containsExactly(123);
	}

	@Test
	public void getByteArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, byte[].class, new byte[] { 123 });
		assertThat(annotation.getByteArray("value")).containsExactly(123);
	}

	@Test
	public void getBooleanWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(true, boolean.class, null);
		assertThat(annotation.getBoolean("value")).isTrue();
	}

	@Test
	public void getBooleanWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, boolean.class, true);
		assertThat(annotation.getBoolean("value")).isTrue();
	}

	@Test
	public void getBooleanArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(new boolean[] { true }, boolean[].class,
				null);
		assertThat(annotation.getBooleanArray("value")).containsExactly(true);
	}

	@Test
	public void getBooleanArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, boolean[].class,
				new boolean[] { true });
		assertThat(annotation.getBooleanArray("value")).containsExactly(true);
	}

	@Test
	public void getCharWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create('c', char.class, null);
		assertThat(annotation.getChar("value")).isEqualTo('c');
	}

	@Test
	public void getCharWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, char.class, 'c');
		assertThat(annotation.getChar("value")).isEqualTo('c');
	}

	@Test
	public void getCharArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(new char[] { 'c' }, char[].class, null);
		assertThat(annotation.getCharArray("value")).containsExactly('c');
	}

	@Test
	public void getCharArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, char[].class, new char[] { 'c' });
		assertThat(annotation.getCharArray("value")).containsExactly('c');
	}

	@Test
	public void getShortWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create((short) 123, short.class, null);
		assertThat(annotation.getShort("value")).isEqualTo((short) 123);
	}

	@Test
	public void getShortWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, short.class, (short) 123);
		assertThat(annotation.getShort("value")).isEqualTo((short) 123);
	}

	@Test
	public void getShortArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(new short[] { 123 }, short[].class, null);
		assertThat(annotation.getShortArray("value")).containsExactly((short) 123);
	}

	@Test
	public void getShortArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, short[].class, new short[] { 123 });
		assertThat(annotation.getShortArray("value")).containsExactly((short) 123);
	}

	@Test
	public void getIntWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(123, int.class, null);
		assertThat(annotation.getInt("value")).isEqualTo(123);
	}

	@Test
	public void getIntWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, int.class, 123);
		assertThat(annotation.getInt("value")).isEqualTo(123);
	}

	@Test
	public void getIntArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(new int[] { 123 }, int[].class, null);
		assertThat(annotation.getIntArray("value")).containsExactly(123);
	}

	@Test
	public void getIntArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, int[].class, new int[] { 123 });
		assertThat(annotation.getIntArray("value")).containsExactly(123);
	}

	@Test
	public void getLongWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(123L, long.class, null);
		assertThat(annotation.getLong("value")).isEqualTo(123L);
	}

	@Test
	public void getLongWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, long.class, 123L);
		assertThat(annotation.getLong("value")).isEqualTo(123L);
	}

	@Test
	public void getLongArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(new long[] { 123 }, long[].class, null);
		assertThat(annotation.getLongArray("value")).containsExactly(123L);
	}

	@Test
	public void getLongArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, long[].class, new long[] { 123 });
		assertThat(annotation.getLongArray("value")).containsExactly(123L);
	}

	@Test
	public void getDoubleWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(123.0, double.class, null);
		assertThat(annotation.getDouble("value")).isEqualTo(123.0);
	}

	@Test
	public void getDoubleWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, double.class, 123.0);
		assertThat(annotation.getDouble("value")).isEqualTo(123.0);
	}

	@Test
	public void getDoubleArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(new double[] { 123.0 }, double[].class,
				null);
		assertThat(annotation.getDoubleArray("value")).containsExactly(123.0);
	}

	@Test
	public void getDoubleArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, double[].class,
				new double[] { 123.0 });
		assertThat(annotation.getDoubleArray("value")).containsExactly(123.0);
	}

	@Test
	public void getFloatWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create((float) 123.0, float.class, null);
		assertThat(annotation.getFloat("value")).isEqualTo((float) 123.0);
	}

	@Test
	public void getFloatWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, float.class, (float) 123.0);
		assertThat(annotation.getFloat("value")).isEqualTo((float) 123.0);
	}

	@Test
	public void getFloatArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(new float[] { (float) 123.0 },
				float[].class, null);
		assertThat(annotation.getFloatArray("value")).containsExactly((float) 123.0);
	}

	@Test
	public void getFloatArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, float[].class,
				new float[] { (float) 123.0 });
		assertThat(annotation.getFloatArray("value")).containsExactly((float) 123.0);
	}

	@Test
	public void getStringWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create("abc", String.class, null);
		assertThat(annotation.getString("value")).isEqualTo("abc");
	}

	@Test
	public void getStringWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, String.class, "abc");
		assertThat(annotation.getString("value")).isEqualTo("abc");
	}

	@Test
	public void getStringArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(new String[] { "abc" }, String[].class,
				null);
		assertThat(annotation.getStringArray("value")).containsExactly("abc");
	}

	@Test
	public void getStringArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, String[].class,
				new String[] { "abc" });
		assertThat(annotation.getStringArray("value")).containsExactly("abc");
	}

	@Test
	public void getClassWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(ClassReference.of(String.class),
				Class.class, null);
		assertThat(annotation.getClass("value")).isEqualTo(String.class);
	}

	@Test
	public void getClassWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, String.class,
				ClassReference.of(String.class));
		assertThat(annotation.getClass("value")).isEqualTo(String.class);
	}

	@Test
	public void getClassArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(
				new ClassReference[] { ClassReference.of(String.class) }, Class[].class,
				null);
		assertThat(annotation.getClassArray("value")).containsExactly(String.class);
	}

	@Test
	public void getClassArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, ClassReference[].class,
				new ClassReference[] { ClassReference.of(String.class) });
		assertThat(annotation.getClassArray("value")).containsExactly(String.class);
	}

	@Test
	public void getClassAsStringWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(ClassReference.of(String.class),
				Class.class, null);
		assertThat(annotation.getString("value")).isEqualTo(String.class.getName());
	}

	@Test
	public void getClassAsStringWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, String.class,
				ClassReference.of(String.class));
		assertThat(annotation.getString("value")).isEqualTo(String.class.getName());
	}

	@Test
	public void getClassAsStringArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(
				new ClassReference[] { ClassReference.of(String.class) }, Class[].class,
				null);
		assertThat(annotation.getStringArray("value")).containsExactly(
				String.class.getName());
	}

	@Test
	public void getClassAsStringArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, ClassReference[].class,
				new ClassReference[] { ClassReference.of(String.class) });
		assertThat(annotation.getStringArray("value")).containsExactly(
				String.class.getName());
	}

	@Test
	public void getEnumWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(EnumValueReference.of(ExampleEnum.ONE),
				EnumValueReference.class, null);
		assertThat(annotation.getEnum("value", ExampleEnum.class)).isEqualTo(
				ExampleEnum.ONE);
	}

	@Test
	public void getEnumWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, EnumValueReference.class,
				EnumValueReference.of(ExampleEnum.ONE));
		assertThat(annotation.getEnum("value", ExampleEnum.class)).isEqualTo(
				ExampleEnum.ONE);
	}

	@Test
	public void getEnumArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(
				new EnumValueReference[] { EnumValueReference.of(ExampleEnum.ONE) },
				EnumValueReference[].class, null);
		assertThat(annotation.getEnumArray("value", ExampleEnum.class)).containsExactly(
				ExampleEnum.ONE);
	}

	@Test
	public void getEnumArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, EnumValueReference[].class,
				new EnumValueReference[] { EnumValueReference.of(ExampleEnum.ONE) });
		assertThat(annotation.getEnumArray("value", ExampleEnum.class)).containsExactly(
				ExampleEnum.ONE);
	}

	@Test
	public void getAnnotationWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(DeclaredAttributes.of("value", "test"),
				Example.class, null);
		assertThat(annotation.getAnnotation("value", Example.class).getString(
				"value")).isEqualTo("test");
	}

	@Test
	public void getAnnotationHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, Example.class,
				DeclaredAttributes.of("value", "test"));
		assertThat(annotation.getAnnotation("value", Example.class).getString(
				"value")).isEqualTo("test");
	}

	@Test
	public void getAnnotationArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") },
				Example[].class, null);
		MergedAnnotation<Example>[] array = annotation.getAnnotationArray("value",
				Example.class);
		assertThat(array).hasSize(1);
		assertThat(array[0].getString("value")).isEqualTo("test");
	}

	@Test
	public void getAnnotationArrayHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(null, Example[].class,
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") });
		MergedAnnotation<Example>[] array = annotation.getAnnotationArray("value",
				Example.class);
		assertThat(array).hasSize(1);
		assertThat(array[0].getString("value")).isEqualTo("test");
	}

	@Test
	public void getRequiredAttributeWhenMissingThrowsException() {
		MergedAnnotation<?> annotation = create((byte) 123, byte.class, null);
		assertNoSuchElement(() -> annotation.getByte("missing"));
		assertNoSuchElement(() -> annotation.getByteArray("missing"));
		assertNoSuchElement(() -> annotation.getBoolean("missing"));
		assertNoSuchElement(() -> annotation.getBooleanArray("missing"));
		assertNoSuchElement(() -> annotation.getChar("missing"));
		assertNoSuchElement(() -> annotation.getCharArray("missing"));
		assertNoSuchElement(() -> annotation.getShort("missing"));
		assertNoSuchElement(() -> annotation.getShortArray("missing"));
		assertNoSuchElement(() -> annotation.getInt("missing"));
		assertNoSuchElement(() -> annotation.getIntArray("missing"));
		assertNoSuchElement(() -> annotation.getLong("missing"));
		assertNoSuchElement(() -> annotation.getLongArray("missing"));
		assertNoSuchElement(() -> annotation.getDouble("missing"));
		assertNoSuchElement(() -> annotation.getDoubleArray("missing"));
		assertNoSuchElement(() -> annotation.getFloat("missing"));
		assertNoSuchElement(() -> annotation.getFloatArray("missing"));
		assertNoSuchElement(() -> annotation.getString("missing"));
		assertNoSuchElement(() -> annotation.getStringArray("missing"));
		assertNoSuchElement(() -> annotation.getClass("missing"));
		assertNoSuchElement(() -> annotation.getClassArray("missing"));
		assertNoSuchElement(() -> annotation.getEnum("missing", ExampleEnum.class));
		assertNoSuchElement(() -> annotation.getEnumArray("missing", ExampleEnum.class));
		assertNoSuchElement(() -> annotation.getAnnotation("missing", Example.class));
		assertNoSuchElement(
				() -> annotation.getAnnotationArray("missing", Example.class));
	}

	private void assertNoSuchElement(Runnable call) {
		try {
			call.run();
			fail("NoSuchElementException not thrown");
		}
		catch (NoSuchElementException ex) {
		}
	}

	@Test
	public void getAttributeWhenAvailableReturnsOptionalOf() {
		MergedAnnotation<?> annotation = create((byte) 123, byte.class, null);
		assertThat(annotation.getAttribute("value", Byte.class)).isEqualTo(
				Optional.of((byte) 123));
	}

	@Test
	public void getAttributeWhenMissingReturnsEmptyOptional() {
		MergedAnnotation<?> annotation = create((byte) 123, byte.class, null);
		assertThat(annotation.getAttribute("missing", Byte.class)).isEqualTo(
				Optional.empty());
	}

	@Test
	public void getClassReferenceAttributeAsClassAdapts() {
		MergedAnnotation<?> annotation = create(ClassReference.of(String.class),
				Class.class, null);
		assertThat(annotation.getAttribute("value", Class.class).get()).isEqualTo(
				String.class);
	}

	@Test
	public void getClassReferenceAttributeAsStringAdapts() {
		MergedAnnotation<?> annotation = create(ClassReference.of(String.class),
				Class.class, null);
		assertThat(annotation.getAttribute("value", String.class).get()).isEqualTo(
				"java.lang.String");
	}

	@Test
	public void getClassReferenceArrayAttributeAsClassArrayAdapts() {
		MergedAnnotation<?> annotation = create(
				new ClassReference[] { ClassReference.of(String.class) }, Class[].class,
				null);
		assertThat(annotation.getAttribute("value", Class[].class).get()).containsExactly(
				String.class);
	}

	@Test
	public void getClassReferenceArrayAttributeAsStringArrayAdapts() {
		MergedAnnotation<?> annotation = create(
				new ClassReference[] { ClassReference.of(String.class) }, Class[].class,
				null);
		assertThat(
				annotation.getAttribute("value", String[].class).get()).containsExactly(
						"java.lang.String");
	}

	@Test
	public void getEnumValueReferenceAttributeAsEnumAdapts() {
		MergedAnnotation<?> annotation = create(EnumValueReference.of(ExampleEnum.ONE),
				EnumValueReference.class, null);
		assertThat(annotation.getAttribute("value", ExampleEnum.class).get()).isEqualTo(
				ExampleEnum.ONE);
	}

	@Test
	public void getEnumValueReferenceArrayAttributeAsEnumArrayAdapts() {
		MergedAnnotation<?> annotation = create(
				new EnumValueReference[] { EnumValueReference.of(ExampleEnum.ONE) },
				EnumValueReference[].class, null);
		assertThat(annotation.getAttribute("value",
				ExampleEnum[].class).get()).containsExactly(ExampleEnum.ONE);
	}

	@Test
	public void getDeclaredAttributesValueAsAnnotationAdapts() {
		MergedAnnotation<?> annotation = create(DeclaredAttributes.of("value", "test"),
				Example.class, null);
		Example nested = annotation.getAttribute("value", Example.class).get();
		assertThat(nested.value()).isEqualTo("test");
	}

	@Test
	public void getDeclaredAttributesArrayValueAsAnnotationArrayAdapts() {
		MergedAnnotation<?> annotation = create(
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") },
				Example[].class, null);
		Example nested = annotation.getAttribute("value", Example[].class).get()[0];
		assertThat(nested.value()).isEqualTo("test");
	}

	@Test
	public void getFromEmptyObjectArraySupportsEveryArrayType() {
		MappedAnnotation<?> annotation = createTwoAttributeAnnotation();
		Object[] empty = {};
		assertThat(annotation.extract(empty, byte[].class)).isEqualTo(new byte[] {});
		assertThat(annotation.extract(empty, boolean[].class)).isEqualTo(
				new boolean[] {});
		assertThat(annotation.extract(empty, char[].class)).isEqualTo(new char[] {});
		assertThat(annotation.extract(empty, short[].class)).isEqualTo(new short[] {});
		assertThat(annotation.extract(empty, int[].class)).isEqualTo(new int[] {});
		assertThat(annotation.extract(empty, long[].class)).isEqualTo(new long[] {});
		assertThat(annotation.extract(empty, float[].class)).isEqualTo(new float[] {});
		assertThat(annotation.extract(empty, double[].class)).isEqualTo(new double[] {});
		assertThat(annotation.extract(empty, String[].class)).isEqualTo(new String[] {});
		assertThat(annotation.extract(empty, ClassReference[].class)).isEqualTo(
				new ClassReference[] {});
		assertThat(annotation.extract(empty, EnumValueReference[].class)).isEqualTo(
				new EnumValueReference[] {});
		assertThat(annotation.extract(empty, DeclaredAttributes[].class)).isEqualTo(
				new DeclaredAttributes[] {});
	}

	@Test
	public void filterDefaultValueFiltersDefaultValues() {
		MergedAnnotation<?> annotation = createTwoAttributeAnnotation();
		MergedAnnotation<?> filtered = annotation.filterDefaultValues();
		assertThat(filtered.getAttribute("one", Integer.class)).isEmpty();
		assertThat(filtered.getAttribute("two", Integer.class)).hasValue(2);
	}

	@Test
	public void filterAttributesAppliesFilter() {
		MergedAnnotation<?> annotation = createTwoAttributeAnnotation();
		MergedAnnotation<?> filtered = annotation.filterAttributes("one"::equals);
		assertThat(filtered.getAttribute("one", Integer.class)).hasValue(1);
		assertThat(filtered.getAttribute("two", Integer.class)).isEmpty();

	}

	@Test
	public void synthesizeCreatesAnnotation() {
		Example example = (Example) create("hello", String.class, null).synthesize();
		assertThat(example.value()).isEqualTo("hello");
	}

	@Test
	public void asMapCreatesMap() {
		MergedAnnotation<?> annotation = createTwoAttributeAnnotation();
		Map<String, Object> map = annotation.asMap();
		assertThat(map).containsExactly(entry("one", 1), entry("two", 2));
	}

	@Test
	public void asMapWhenClassAndMapClassToStringOptionContainsString() {
		AnnotationType annotationType = this.resolver.resolve(
				ClassExample.class.getName());
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				ClassReference.of(StringBuilder.class));
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		Map<String, Object> map = annotation.asMap(MapValues.CLASS_TO_STRING);
		assertThat(map).containsOnly(entry("value", StringBuilder.class.getName()));
	}

	@Test
	public void asMapWhenClassArrayAndMapClassToStringOptionContainsStringArray() {
		AnnotationType annotationType = this.resolver.resolve(
				ClassesExample.class.getName());
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				new ClassReference[] { ClassReference.of(StringBuffer.class) });
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		Map<String, Object> map = annotation.asMap(MapValues.CLASS_TO_STRING);
		assertThat(map).containsOnly(
				entry("value", new String[] { StringBuffer.class.getName() }));
	}

	@Test
	public void asMapWhenNestedContainsSynthesized() {
		AnnotationType annotationType = this.resolver.resolve(
				NestedExample.class.getName());
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				DeclaredAttributes.of("value", "test"));
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		Map<String, Object> map = annotation.asMap();
		assertThat(map).hasSize(1).containsKey("value");
		Example example = (Example) map.get("value");
		assertThat(example.value()).isEqualTo("test");
	}

	@Test
	public void asMapWhenNestedArrayContainsSynthesized() {
		AnnotationType annotationType = this.resolver.resolve(
				NestedArrayExample.class.getName());
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") });
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		Map<String, Object> map = annotation.asMap();
		assertThat(map).hasSize(1).containsKey("value");
		Example[] example = (Example[]) map.get("value");
		assertThat(example[0].value()).isEqualTo("test");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void asMapWhenNestedAndNestedAnnotationsToMapOptionContainsNestedMap() {
		AnnotationType annotationType = this.resolver.resolve(
				NestedExample.class.getName());
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				DeclaredAttributes.of("value", "test"));
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		Map<String, Object> map = annotation.asMap(MapValues.ANNOTATION_TO_MAP);
		assertThat(map).hasSize(1).containsKey("value");
		Map<String, Object> example = (Map<String, Object>) map.get("value");
		assertThat(example).containsOnly(entry("value", "test"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void asMapWhenNestedArrayAndNestedAnnotationsToMapOptionContainsNestedMap() {
		AnnotationType annotationType = this.resolver.resolve(
				NestedArrayExample.class.getName());
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") });
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		Map<String, Object> map = annotation.asMap(MapValues.ANNOTATION_TO_MAP);
		assertThat(map).hasSize(1).containsKey("value");
		Map<String, Object>[] example = (Map<String, Object>[]) map.get("value");
		assertThat(example[0]).containsOnly(entry("value", "test"));
	}

	@Test
	public void asSuppliedMapWhenNestedAndNestedAnnotationsToMapOptionContainsNestedMap() {
		AnnotationType annotationType = this.resolver.resolve(
				NestedExample.class.getName());
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				DeclaredAttributes.of("value", "test"));
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		AnnotationAttributes map = annotation.asMap(source -> new AnnotationAttributes(),
				MapValues.ANNOTATION_TO_MAP);
		assertThat(map).hasSize(1).containsKey("value");
		AnnotationAttributes example = (AnnotationAttributes) map.get("value");
		assertThat(example).containsOnly(entry("value", "test"));
	}

	private MappedAnnotation<?> createTwoAttributeAnnotation() {
		AttributeTypes attributeTypes = AttributeTypes.of(
				AttributeType.of("one", ClassUtils.getQualifiedName(Integer.class),
						DeclaredAnnotations.NONE, 1),
				AttributeType.of("two", ClassUtils.getQualifiedName(Integer.class),
						DeclaredAnnotations.NONE, null));
		AnnotationType annotationType = AnnotationType.of("com.example.Example",
				DeclaredAnnotations.NONE, attributeTypes);
		DeclaredAttributes attributes = DeclaredAttributes.of("one", 1, "two", 2);
		return create(annotationType, attributes);
	}

	private MappedAnnotation<?> create(Object value, Class<?> type, Object defaultValue) {
		AttributeType attributeType = AttributeType.of("value",
				ClassUtils.getQualifiedName(type), DeclaredAnnotations.NONE,
				defaultValue);
		AttributeTypes attributeTypes = AttributeTypes.of(attributeType);
		AnnotationType annotationType = AnnotationType.of(Example.class.getName(),
				DeclaredAnnotations.NONE, attributeTypes);
		DeclaredAttributes attributes = DeclaredAttributes.of("value", value);
		return create(annotationType, attributes);
	}

	private MappedAnnotation<?> create(AnnotationType type,
			DeclaredAttributes attributes) {
		MappableAnnotation source = new MappableAnnotation(this.resolver,
				RepeatableContainers.standardRepeatables(), type, attributes, null);
		AnnotationTypeMapping mapping = new AnnotationTypeMapping(this.resolver, null,
				source);
		return new MappedAnnotation<>(mapping, attributes, null, false, null);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Example {

		String value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AttributesExample {

		Example value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AttributesArrayExample {

		Example[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ClassExample {

		Class<?> value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ClassesExample {

		Class<?>[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NestedExample {

		Example value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NestedArrayExample {

		Example[] value();

	}

	@AttributesExample(@Example("test"))
	static class WithAttributesExample {

	}

	enum ExampleEnum {

		ONE, TWO, THREE

	}

}
