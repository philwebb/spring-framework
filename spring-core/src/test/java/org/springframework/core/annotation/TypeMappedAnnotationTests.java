/*
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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;

import org.springframework.core.annotation.MergedAnnotation.MapValues;
import org.springframework.core.annotation.SynthesizedMergedAnnotationInvocationHandlerTests.TestEnum;
import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.AttributeTypes;
import org.springframework.core.annotation.type.ClassReference;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.core.annotation.type.EnumValueReference;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link TypeMappedAnnotation}.
 *
 * @author Phillip Webb
 * @since 5.0
 */
public class TypeMappedAnnotationTests {

	private final Object source = "TypeMappedAnnotationTests";

	private int aggregateIndex = 0;

	@Test
	public void getTypeReturnsType() {
		MergedAnnotation<?> annotation = create(byte.class, (byte) 123, null);
		assertThat(annotation.getType()).isEqualTo("com.example.Component");
	}

	@Test
	public void isPresentReturnsTrue() {
		MergedAnnotation<?> annotation = create(String.class, "test", null);
		assertThat(annotation.isPresent()).isTrue();
	}

	@Test
	public void isDirectlyPresentWhenDirectAnnotationReturnsTrue() {
		MergedAnnotation<?> annotation = create(String.class, "test", null);
		assertThat(annotation.isDirectlyPresent()).isTrue();
	}

	@Test
	public void isDirectlyPresentWhenMetaAnnotationReturnsFalse() {
		TypeMappedAnnotation<Annotation> annotation = createMetaAnnotation(2);
		assertThat(annotation.isDirectlyPresent()).isFalse();
	}

	@Test
	public void isMetaPresentWhenDirectAnnotationReturnsFalse() {
		MergedAnnotation<?> annotation = create(String.class, "test", null);
		assertThat(annotation.isMetaPresent()).isFalse();
	}

	@Test
	public void isMetaPresentWhenMetaAnnotationReturnsTrue() {
		TypeMappedAnnotation<Annotation> annotation = createMetaAnnotation(2);
		assertThat(annotation.isMetaPresent()).isTrue();
	}

	@Test
	public void getDepthWhenDirectAnnotationReturnsZero() {
		MergedAnnotation<?> annotation = create(String.class, "test", null);
		assertThat(annotation.getDepth()).isEqualTo(0);
	}

	@Test
	public void getDepthWhenMetaAnnotationReturnsOne() {
		TypeMappedAnnotation<Annotation> annotation = createMetaAnnotation(2);
		assertThat(annotation.getDepth()).isEqualTo(1);
	}

	@Test
	public void getDepthWhenMetaMetaAnnotationReturnsTwo() {
		TypeMappedAnnotation<Annotation> annotation = createMetaAnnotation(3);
		assertThat(annotation.getDepth()).isEqualTo(2);
	}

	@Test
	public void getAggregateIndexReturnsAggregateIndex() {
		this.aggregateIndex = 123;
		MergedAnnotation<?> annotation = create(String.class, "test", null);
		assertThat(annotation.getAggregateIndex()).isEqualTo(123);
	}

	@Test
	public void getSourceReturnsSource() {
		MergedAnnotation<?> annotation = create(String.class, "test", null);
		assertThat(annotation.getSource()).isSameAs(this.source);
	}

	@Test
	public void getParentWhenDirectAnnotationReturnsNull() {
		MergedAnnotation<?> annotation = create(String.class, "test", null);
		assertThat(annotation.getParent()).isNull();
	}

	@Test
	public void getParentWhenMetaAnnotationReturnsParent() {
		TypeMappedAnnotation<Annotation> annotation = createMetaAnnotation(2);
		assertThat(annotation.getParent().getType()).isEqualTo("com.example.Annotation0");
	}

	@Test
	public void getParentWhenMetaMetaAnnotationReturnsMetaParent() {
		TypeMappedAnnotation<Annotation> annotation = createMetaAnnotation(3);
		assertThat(annotation.getParent().getType()).isEqualTo("com.example.Annotation1");
	}

	@Test
	public void hasNonDefaultValueWhenHasDefaultValueReturnsFalse() {
		MergedAnnotation<?> annotation = create(int.class, 123, 123);
		assertThat(annotation.hasNonDefaultValue("value")).isFalse();
	}

	@Test
	public void hasNoneDefaultValueWhenHasNonDefaultValueReturnsTrue() {
		MergedAnnotation<?> annotation = create(int.class, 456, 123);
		assertThat(annotation.hasNonDefaultValue("value")).isTrue();
	}

	@Test
	public void hasDefaultValueWhenHasDefaultValueReturnsTrue() {
		MergedAnnotation<?> annotation = create(int.class, 123, 123);
		assertThat(annotation.hasDefaultValue("value")).isTrue();
	}

	@Test
	public void hasDefaultValueWhenHasNonDefaultValueReturnsFalse() {
		MergedAnnotation<?> annotation = create(int.class, 456, 123);
		assertThat(annotation.hasDefaultValue("value")).isFalse();
	}

	@Test
	public void hasDefaultValueWhenMissingAttributeThrowsNoSuchElementException() {
		MergedAnnotation<?> annotation = create(int.class, 456, 123);
		assertThatNoSuchElementException(() -> annotation.hasDefaultValue("missing"));
	}

	@Test
	public void getByteWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(byte.class, (byte) 123, null);
		assertThat(annotation.getByte("value")).isEqualTo((byte) 123);
	}

	@Test
	public void getByteWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(byte.class, null, (byte) 123);
		assertThat(annotation.getByte("value")).isEqualTo((byte) 123);
	}

	@Test
	public void getByteArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(byte[].class, new byte[] { 123 }, null);
		assertThat(annotation.getByteArray("value")).containsExactly(123);
	}

	@Test
	public void getByteArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(byte[].class, null, new byte[] { 123 });
		assertThat(annotation.getByteArray("value")).containsExactly(123);
	}

	@Test
	public void getBooleanWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(boolean.class, true, null);
		assertThat(annotation.getBoolean("value")).isTrue();
	}

	@Test
	public void getBooleanWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(boolean.class, null, true);
		assertThat(annotation.getBoolean("value")).isTrue();
	}

	@Test
	public void getBooleanArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(boolean[].class, new boolean[] { true },
				null);
		assertThat(annotation.getBooleanArray("value")).containsExactly(true);
	}

	@Test
	public void getBooleanArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(boolean[].class, null,
				new boolean[] { true });
		assertThat(annotation.getBooleanArray("value")).containsExactly(true);
	}

	@Test
	public void getCharWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(char.class, 'c', null);
		assertThat(annotation.getChar("value")).isEqualTo('c');
	}

	@Test
	public void getCharWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(char.class, null, 'c');
		assertThat(annotation.getChar("value")).isEqualTo('c');
	}

	@Test
	public void getCharArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(char[].class, new char[] { 'c' }, null);
		assertThat(annotation.getCharArray("value")).containsExactly('c');
	}

	@Test
	public void getCharArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(char[].class, null, new char[] { 'c' });
		assertThat(annotation.getCharArray("value")).containsExactly('c');
	}

	@Test
	public void getShortWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(short.class, (short) 123, null);
		assertThat(annotation.getShort("value")).isEqualTo((short) 123);
	}

	@Test
	public void getShortWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(short.class, null, (short) 123);
		assertThat(annotation.getShort("value")).isEqualTo((short) 123);
	}

	@Test
	public void getShortArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(short[].class, new short[] { 123 }, null);
		assertThat(annotation.getShortArray("value")).containsExactly((short) 123);
	}

	@Test
	public void getShortArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(short[].class, null, new short[] { 123 });
		assertThat(annotation.getShortArray("value")).containsExactly((short) 123);
	}

	@Test
	public void getIntWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(int.class, 123, null);
		assertThat(annotation.getInt("value")).isEqualTo(123);
	}

	@Test
	public void getIntWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(int.class, null, 123);
		assertThat(annotation.getInt("value")).isEqualTo(123);
	}

	@Test
	public void getIntArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(int[].class, new int[] { 123 }, null);
		assertThat(annotation.getIntArray("value")).containsExactly(123);
	}

	@Test
	public void getIntArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(int[].class, null, new int[] { 123 });
		assertThat(annotation.getIntArray("value")).containsExactly(123);
	}

	@Test
	public void getLongWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(long.class, 123L, null);
		assertThat(annotation.getLong("value")).isEqualTo(123L);
	}

	@Test
	public void getLongWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(long.class, null, 123L);
		assertThat(annotation.getLong("value")).isEqualTo(123L);
	}

	@Test
	public void getLongArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(long[].class, new long[] { 123 }, null);
		assertThat(annotation.getLongArray("value")).containsExactly(123L);
	}

	@Test
	public void getLongArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(long[].class, null, new long[] { 123 });
		assertThat(annotation.getLongArray("value")).containsExactly(123L);
	}

	@Test
	public void getDoubleWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(double.class, 123.0, null);
		assertThat(annotation.getDouble("value")).isEqualTo(123.0);
	}

	@Test
	public void getDoubleWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(double.class, null, 123.0);
		assertThat(annotation.getDouble("value")).isEqualTo(123.0);
	}

	@Test
	public void getDoubleArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(double[].class, new double[] { 123.0 },
				null);
		assertThat(annotation.getDoubleArray("value")).containsExactly(123.0);
	}

	@Test
	public void getDoubleArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(double[].class, null,
				new double[] { 123.0 });
		assertThat(annotation.getDoubleArray("value")).containsExactly(123.0);
	}

	@Test
	public void getFloatWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(float.class, (float) 123.0, null);
		assertThat(annotation.getFloat("value")).isEqualTo((float) 123.0);
	}

	@Test
	public void getFloatWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(float.class, null, (float) 123.0);
		assertThat(annotation.getFloat("value")).isEqualTo((float) 123.0);
	}

	@Test
	public void getFloatArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(float[].class,
				new float[] { (float) 123.0 }, null);
		assertThat(annotation.getFloatArray("value")).containsExactly((float) 123.0);
	}

	@Test
	public void getFloatArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(float[].class, null,
				new float[] { (float) 123.0 });
		assertThat(annotation.getFloatArray("value")).containsExactly((float) 123.0);
	}

	@Test
	public void getStringWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(String.class, "abc", null);
		assertThat(annotation.getString("value")).isEqualTo("abc");
	}

	@Test
	public void getStringWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(String.class, null, "abc");
		assertThat(annotation.getString("value")).isEqualTo("abc");
	}

	@Test
	public void getStringArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(String[].class, new String[] { "abc" },
				null);
		assertThat(annotation.getStringArray("value")).containsExactly("abc");
	}

	@Test
	public void getStringArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(String[].class, null,
				new String[] { "abc" });
		assertThat(annotation.getStringArray("value")).containsExactly("abc");
	}

	@Test
	public void getClassWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(Class.class,
				ClassReference.from(String.class), null);
		assertThat(annotation.getClass("value")).isEqualTo(String.class);
	}

	@Test
	public void getClassWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(String.class, null,
				ClassReference.from(String.class));
		assertThat(annotation.getClass("value")).isEqualTo(String.class);
	}

	@Test
	public void getClassArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(Class[].class,
				new ClassReference[] { ClassReference.from(String.class) }, null);
		assertThat(annotation.getClassArray("value")).containsExactly(String.class);
	}

	@Test
	public void getClassArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(ClassReference[].class, null,
				new ClassReference[] { ClassReference.from(String.class) });
		assertThat(annotation.getClassArray("value")).containsExactly(String.class);
	}

	@Test
	public void getClassAsStringWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(Class.class,
				ClassReference.from(String.class), null);
		assertThat(annotation.getString("value")).isEqualTo(String.class.getName());
	}

	@Test
	public void getClassAsStringWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(String.class, null,
				ClassReference.from(String.class));
		assertThat(annotation.getString("value")).isEqualTo(String.class.getName());
	}

	@Test
	public void getClassAsStringArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(Class[].class,
				new ClassReference[] { ClassReference.from(String.class) }, null);
		assertThat(annotation.getStringArray("value")).containsExactly(
				String.class.getName());
	}

	@Test
	public void getClassAsStringArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(ClassReference[].class, null,
				new ClassReference[] { ClassReference.from(String.class) });
		assertThat(annotation.getStringArray("value")).containsExactly(
				String.class.getName());
	}

	@Test
	public void getEnumWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(EnumValueReference.class,
				EnumValueReference.from(ExampleEnum.ONE), null);
		assertThat(annotation.getEnum("value", ExampleEnum.class)).isEqualTo(
				ExampleEnum.ONE);
	}

	@Test
	public void getEnumWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(EnumValueReference.class, null,
				EnumValueReference.from(ExampleEnum.ONE));
		assertThat(annotation.getEnum("value", ExampleEnum.class)).isEqualTo(
				ExampleEnum.ONE);
	}

	@Test
	public void getEnumArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(EnumValueReference[].class,
				new EnumValueReference[] { EnumValueReference.from(ExampleEnum.ONE) },
				null);
		assertThat(annotation.getEnumArray("value", ExampleEnum.class)).containsExactly(
				ExampleEnum.ONE);
	}

	@Test
	public void getEnumArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(EnumValueReference[].class, null,
				new EnumValueReference[] { EnumValueReference.from(ExampleEnum.ONE) });
		assertThat(annotation.getEnumArray("value", ExampleEnum.class)).containsExactly(
				ExampleEnum.ONE);
	}

	@Test
	public void getAnnotationWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation.class,
				DeclaredAttributes.of("value", "test"), null);
		assertThat(
				annotation.getAnnotation("value", StringValueAnnotation.class).getString(
						"value")).isEqualTo("test");
	}

	@Test
	public void getAnnotationWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation.class, null,
				DeclaredAttributes.of("value", "test"));
		assertThat(
				annotation.getAnnotation("value", StringValueAnnotation.class).getString(
						"value")).isEqualTo("test");
	}

	@Test
	public void getAnnotationWhenWrongTypeThrowsException() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation.class, null,
				DeclaredAttributes.of("value", "test"));
		assertThatIllegalStateException().isThrownBy(() -> annotation.getAnnotation(
				"value", ClassValueAnnotation.class)).withMessage(
						"Attribute 'value' is a " + StringValueAnnotation.class.getName()
								+ " and cannot be cast to "
								+ ClassValueAnnotation.class.getName());
	}

	@Test
	public void getAnnotationWhenArrayTypeThrowsException() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation[].class,
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") },
				null);
		assertThatIllegalStateException().isThrownBy(() -> annotation.getAnnotation(
				"value", StringValueAnnotation.class)).withMessage(
						"Attribute 'value' is an array type");
	}

	@Test
	public void getAnnotationArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation[].class,
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") },
				null);
		MergedAnnotation<StringValueAnnotation>[] array = annotation.getAnnotationArray(
				"value", StringValueAnnotation.class);
		assertThat(array).hasSize(1);
		assertThat(array[0].getString("value")).isEqualTo("test");
	}

	@Test
	public void getAnnotationArrayHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation[].class, null,
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") });
		MergedAnnotation<StringValueAnnotation>[] array = annotation.getAnnotationArray(
				"value", StringValueAnnotation.class);
		assertThat(array).hasSize(1);
		assertThat(array[0].getString("value")).isEqualTo("test");
	}

	@Test
	public void getAnnotationArrayWhenWrongTypeThrowsException() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation[].class, null,
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") });
		assertThatIllegalStateException().isThrownBy(() -> annotation.getAnnotationArray(
				"value", ClassValueAnnotation.class)).withMessage(
						"Attribute 'value' is a " + StringValueAnnotation.class.getName()
								+ " and cannot be cast to "
								+ ClassValueAnnotation.class.getName());
	}

	@Test
	public void getAnnotationArrayWhenNotArrayTypeThrowsException() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation.class,
				DeclaredAttributes.of("value", "test"), null);
		assertThatIllegalStateException().isThrownBy(() -> annotation.getAnnotationArray(
				"value", StringValueAnnotation.class)).withMessage(
						"Attribute 'value' is not an array type");
	}

	@Test
	public void getRequiredAttributeWhenMissingThrowsException() {
		MergedAnnotation<?> annotation = create(byte.class, (byte) 123, null);
		assertThatNoSuchElementException(() -> annotation.getByte("missing"));
		assertThatNoSuchElementException(() -> annotation.getByteArray("missing"));
		assertThatNoSuchElementException(() -> annotation.getBoolean("missing"));
		assertThatNoSuchElementException(() -> annotation.getBooleanArray("missing"));
		assertThatNoSuchElementException(() -> annotation.getChar("missing"));
		assertThatNoSuchElementException(() -> annotation.getCharArray("missing"));
		assertThatNoSuchElementException(() -> annotation.getShort("missing"));
		assertThatNoSuchElementException(() -> annotation.getShortArray("missing"));
		assertThatNoSuchElementException(() -> annotation.getInt("missing"));
		assertThatNoSuchElementException(() -> annotation.getIntArray("missing"));
		assertThatNoSuchElementException(() -> annotation.getLong("missing"));
		assertThatNoSuchElementException(() -> annotation.getLongArray("missing"));
		assertThatNoSuchElementException(() -> annotation.getDouble("missing"));
		assertThatNoSuchElementException(() -> annotation.getDoubleArray("missing"));
		assertThatNoSuchElementException(() -> annotation.getFloat("missing"));
		assertThatNoSuchElementException(() -> annotation.getFloatArray("missing"));
		assertThatNoSuchElementException(() -> annotation.getString("missing"));
		assertThatNoSuchElementException(() -> annotation.getStringArray("missing"));
		assertThatNoSuchElementException(() -> annotation.getClass("missing"));
		assertThatNoSuchElementException(() -> annotation.getClassArray("missing"));
		assertThatNoSuchElementException(
				() -> annotation.getEnum("missing", ExampleEnum.class));
		assertThatNoSuchElementException(
				() -> annotation.getEnumArray("missing", ExampleEnum.class));
		assertThatNoSuchElementException(
				() -> annotation.getAnnotation("missing", StringValueAnnotation.class));
		assertThatNoSuchElementException(() -> annotation.getAnnotationArray("missing",
				StringValueAnnotation.class));
	}

	@Test
	public void getValueWhenAvailableReturnsOptionalOf() {
		MergedAnnotation<?> annotation = create(byte.class, (byte) 123, null);
		assertThat(annotation.getValue("value", Byte.class)).isEqualTo(
				Optional.of((byte) 123));
	}

	@Test
	public void getValueWhenMissingReturnsEmptyOptional() {
		MergedAnnotation<?> annotation = create(byte.class, (byte) 123, null);
		assertThat(annotation.getValue("missing", Byte.class)).isEqualTo(
				Optional.empty());
	}

	@Test
	public void getValueForClassReferenceAsClassAdapts() {
		MergedAnnotation<?> annotation = create(Class.class,
				ClassReference.from(String.class), null);
		assertThat(annotation.getValue("value", Class.class).get()).isEqualTo(
				String.class);
	}

	@Test
	public void getValueForClassReferenceAsStringAdapts() {
		MergedAnnotation<?> annotation = create(Class.class,
				ClassReference.from(String.class), null);
		assertThat(annotation.getValue("value", String.class).get()).isEqualTo(
				"java.lang.String");
	}

	@Test
	public void getValueForClassReferenceAsObjectAdapts() {
		MergedAnnotation<?> annotation = create(Class.class,
				ClassReference.from(String.class), null);
		assertThat(annotation.getValue("value", Object.class).get()).isEqualTo(
				String.class);
	}

	@Test
	public void getValueForClassReferenceArrayAsClassArrayAdapts() {
		MergedAnnotation<?> annotation = create(Class[].class,
				new ClassReference[] { ClassReference.from(String.class) }, null);
		Class<?>[] value = annotation.getValue("value", Class[].class).get();
		assertThat(value).containsExactly(String.class);
	}

	@Test
	public void getValueForClassReferenceArrayAsStringArrayAdapts() {
		MergedAnnotation<?> annotation = create(Class[].class,
				new ClassReference[] { ClassReference.from(String.class) }, null);
		String[] value = annotation.getValue("value", String[].class).get();
		assertThat(value).containsExactly("java.lang.String");
	}

	@Test
	public void getValueForClassReferenceArrayAsObjectAdapts() {
		MergedAnnotation<?> annotation = create(Class[].class,
				new ClassReference[] { ClassReference.from(String.class) }, null);
		Class<?>[] value = (Class<?>[]) annotation.getValue("value", Object.class).get();
		assertThat(value).containsExactly(String.class);
	}

	@Test
	public void getValueForEnumValueReferenceAsEnumAdapts() {
		MergedAnnotation<?> annotation = create(EnumValueReference.class,
				EnumValueReference.from(ExampleEnum.ONE), null);
		assertThat(annotation.getValue("value", ExampleEnum.class).get()).isEqualTo(
				ExampleEnum.ONE);
	}

	@Test
	public void getValueForEnumValueReferenceAsObjectAdapts() {
		MergedAnnotation<?> annotation = create(EnumValueReference.class,
				EnumValueReference.from(ExampleEnum.ONE), null);
		assertThat(annotation.getValue("value", Object.class).get()).isEqualTo(
				ExampleEnum.ONE);
	}

	@Test
	public void getValueForEnumValueReferenceArrayAsEnumArrayAdapts() {
		MergedAnnotation<?> annotation = create(ExampleEnum[].class,
				new EnumValueReference[] { EnumValueReference.from(ExampleEnum.ONE) },
				null);
		ExampleEnum[] value = annotation.getValue("value", ExampleEnum[].class).get();
		assertThat(value).containsExactly(ExampleEnum.ONE);
	}

	@Test
	public void getValueForEnumValueReferenceArrayAsObjectAdapts() {
		MergedAnnotation<?> annotation = create(ExampleEnum[].class,
				new EnumValueReference[] { EnumValueReference.from(ExampleEnum.ONE) },
				null);
		ExampleEnum[] value = (ExampleEnum[]) annotation.getValue("value",
				Object.class).get();
		assertThat(value).containsExactly(ExampleEnum.ONE);
	}

	@Test
	public void getValueForDeclaredAttributesAsAnnotationAdapts() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation.class,
				DeclaredAttributes.of("value", "test"), null);
		StringValueAnnotation nested = annotation.getValue("value",
				StringValueAnnotation.class).get();
		assertThat(nested.value()).isEqualTo("test");
	}

	@Test
	public void getValueForDeclaredAttributesAsObjectAdapts() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation.class,
				DeclaredAttributes.of("value", "test"), null);
		StringValueAnnotation nested = (StringValueAnnotation) annotation.getValue(
				"value", Object.class).get();
		assertThat(nested.value()).isEqualTo("test");
	}

	@Test
	public void getValueForDeclaredAttributesArrayAsAnnotationArrayAdapts() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation[].class,
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") },
				null);
		StringValueAnnotation[] nested = annotation.getValue("value",
				StringValueAnnotation[].class).get();
		assertThat(nested[0].value()).isEqualTo("test");
	}

	@Test
	public void getValueForDeclaredAttributesArrayAsObjectAdapts() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation[].class,
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") },
				null);
		StringValueAnnotation[] nested = (StringValueAnnotation[]) annotation.getValue(
				"value", Object.class).get();
		assertThat(nested[0].value()).isEqualTo("test");
	}

	@Test
	public void getValueForEmptyArrayAsObjectAdapts() {
		MergedAnnotation<?> annotation = create(int[].class, new Object[0], null);
		int[] value = (int[]) annotation.getValue("value", Object.class).get();
		assertThat(value).isEmpty();
	}

	@Test
	public void getValueWhenUnsupportedTypeThrowsException() {
		MergedAnnotation<?> annotation = create(String.class, "test", null);
		assertThatIllegalArgumentException().isThrownBy(
				() -> annotation.getValue("value", InputStream.class)).withMessage(
						"Type " + InputStream.class.getName() + " is not supported");
	}

	@Test
	public void getDefaultValueWhenHasDefaultValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(int.class, 456, 123);
		assertThat(annotation.getDefaultValue("value", Integer.class)).hasValue(123);
	}

	@Test
	public void getDefaultValueWhenHasNoDefaultValueReturnsEmpty() {
		MergedAnnotation<?> annotation = create(int.class, 456, null);
		assertThat(annotation.getDefaultValue("value", Integer.class)).isEmpty();
	}

	@Test
	public void getDefaultValueWhenAttributeIsMissingReturnsEmpty() {
		MergedAnnotation<?> annotation = create(int.class, 456, 123);
		assertThat(annotation.getDefaultValue("missing", Integer.class)).isEmpty();
	}

	@Test
	public void getValueWhenUnderlyingArrayIsEmptyObjectArrayReturnsCorrectType() {
		assertGetValueFromEmptyArray("byte[]", byte[].class, new byte[0]);
		assertGetValueFromEmptyArray("byte[]", Object.class, new byte[0]);
		assertGetValueFromEmptyArray("boolean[]", boolean[].class, new boolean[0]);
		assertGetValueFromEmptyArray("boolean[]", Object.class, new boolean[0]);
		assertGetValueFromEmptyArray("char[]", char[].class, new char[0]);
		assertGetValueFromEmptyArray("char[]", Object.class, new char[0]);
		assertGetValueFromEmptyArray("short[]", short[].class, new short[0]);
		assertGetValueFromEmptyArray("short[]", Object.class, new short[0]);
		assertGetValueFromEmptyArray("int[]", int[].class, new int[0]);
		assertGetValueFromEmptyArray("int[]", Object.class, new int[0]);
		assertGetValueFromEmptyArray("long[]", long[].class, new long[0]);
		assertGetValueFromEmptyArray("long[]", Object.class, new long[0]);
		assertGetValueFromEmptyArray("float[]", float[].class, new float[0]);
		assertGetValueFromEmptyArray("float[]", Object.class, new float[0]);
		assertGetValueFromEmptyArray("double[]", double[].class, new double[0]);
		assertGetValueFromEmptyArray("double[]", Object.class, new double[0]);
		assertGetValueFromEmptyArray("java.lang.String[]", String[].class, new String[0]);
		assertGetValueFromEmptyArray("java.lang.String[]", Object.class, new String[0]);
		assertGetValueFromEmptyArray("java.lang.Class[]", Class[].class, new Class[0]);
		assertGetValueFromEmptyArray("java.lang.Class[]", Object.class, new Class[0]);
		assertGetValueFromEmptyArray(TestEnum.class.getName() + "[]", TestEnum[].class,
				new TestEnum[0]);
		assertGetValueFromEmptyArray(TestEnum.class.getName() + "[]", Object.class,
				new TestEnum[0]);
		assertGetValueFromEmptyArray(StringValueAnnotation.class.getName() + "[]",
				StringValueAnnotation[].class, new StringValueAnnotation[0]);
		assertGetValueFromEmptyArray(StringValueAnnotation.class.getName() + "[]",
				Object.class, new StringValueAnnotation[0]);
		assertGetValueFromEmptyArray(StringValueAnnotation.class.getName() + "[]",
				MergedAnnotation[].class, new MergedAnnotation[0]);
	}

	private void assertGetValueFromEmptyArray(String attributeType, Class<?> requiredType,
			Object expected) {
		DeclaredAttributes rootAttributes = DeclaredAttributes.of("value", new Object[0]);
		AttributeTypes attributeTypes = AttributeTypes.of(
				AttributeType.of("value", attributeType, DeclaredAnnotations.NONE, null));
		AnnotationType annotationType = AnnotationType.of("com.example.Component",
				DeclaredAnnotations.NONE, attributeTypes);
		AnnotationTypeMapping mapping = new AnnotationTypeMapping(
				getClass().getClassLoader(), RepeatableContainers.none(),
				AnnotationFilter.PLAIN, annotationType);
		TypeMappedAnnotation<?> annotation = new TypeMappedAnnotation<>(mapping,
				this.source, this.aggregateIndex, rootAttributes);
		assertThat(annotation.getValue("value", requiredType).get()).isEqualTo(expected);
	}

	@Test
	public void filterDefaultValueFiltersDefaultValues() {
		MergedAnnotation<?> annotation = createTwoAttributeAnnotation();
		MergedAnnotation<?> filtered = annotation.filterDefaultValues();
		assertThat(filtered.getValue("one", Integer.class)).isEmpty();
		assertThat(filtered.getValue("two", Integer.class)).hasValue(2);
	}

	@Test
	public void filterAttributesAppliesFilter() {
		MergedAnnotation<?> annotation = createTwoAttributeAnnotation();
		MergedAnnotation<?> filtered = annotation.filterAttributes("one"::equals);
		assertThat(filtered.getValue("one", Integer.class)).hasValue(1);
		assertThat(filtered.getValue("two", Integer.class)).isEmpty();
	}

	@Test
	public void filterAttributesWhenAlreadyFilteredAppliesFilter() {
		MergedAnnotation<?> annotation = createTwoAttributeAnnotation();
		MergedAnnotation<?> filtered = annotation.filterAttributes(
				"one"::equals).filterAttributes("two"::equals);
		assertThat(filtered.getValue("one", Integer.class)).isEmpty();
		assertThat(filtered.getValue("two", Integer.class)).isEmpty();
	}

	@Test
	public void withNonMergedAttributesReturnsNonMerged() {
		AnnotationTypeMapping mapping = createWithStringAttribute(null,
				"com.example.MyComponent", "myName");
		AnnotationTypeMapping metaMapping = createWithStringAttribute(mapping,
				"com.example.Component", "name");
		metaMapping.addAlias("name", mapping, "myName");
		MergedAnnotation<?> annotation = new TypeMappedAnnotation<>(metaMapping,
				this.source, this.aggregateIndex,
				DeclaredAttributes.of("myName", "test"));
		assertThat(annotation.getString("name")).isEqualTo("test");
		assertThat(annotation.withNonMergedAttributes().getString("name")).isEmpty();
	}

	@Test
	public void withNonMergedWhenMirroredReturnsNonMergedButStillMirrored() {
		AnnotationTypeMapping mapping = createWithStringAttribute(null,
				"com.example.Component", "a", "b");
		mapping.addMirrorSet("a", "b");
		MergedAnnotation<?> annotation = new TypeMappedAnnotation<>(mapping, this.source,
				this.aggregateIndex, DeclaredAttributes.of("a", "test"));
		assertThat(annotation.getString("a")).isEqualTo("test");
		assertThat(annotation.getString("b")).isEqualTo("test");
		MergedAnnotation<?> nonMergedAnnotation = annotation.withNonMergedAttributes();
		assertThat(nonMergedAnnotation.getString("a")).isEqualTo("test");
		assertThat(nonMergedAnnotation.getString("b")).isEqualTo("test");
	}

	@Test
	public void asMapCreatesMap() {
		MergedAnnotation<?> annotation = createTwoAttributeAnnotation();
		Map<String, Object> map = annotation.asMap();
		assertThat(map).containsExactly(entry("one", 1), entry("two", 2));
	}

	@Test
	public void asMapWhenClassAndMapClassToStringOptionContainsString() {
		AnnotationType annotationType = AnnotationType.resolve(
				ClassValueAnnotation.class);
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				ClassReference.from(StringBuilder.class));
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		Map<String, Object> map = annotation.asMap(MapValues.CLASS_TO_STRING);
		assertThat(map).containsOnly(entry("value", StringBuilder.class.getName()));
	}

	@Test
	public void asMapWhenClassArrayAndMapClassToStringOptionContainsStringArray() {
		AnnotationType annotationType = AnnotationType.resolve(
				ClassArrayValueAnnotation.class);
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				new ClassReference[] { ClassReference.from(StringBuffer.class) });
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		Map<String, Object> map = annotation.asMap(MapValues.CLASS_TO_STRING);
		assertThat(map).containsOnly(
				entry("value", new String[] { StringBuffer.class.getName() }));
	}

	@Test
	public void asMapWhenNestedContainsSynthesized() {
		AnnotationType annotationType = AnnotationType.resolve(
				AnnotationValueAnnotation.class);
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				DeclaredAttributes.of("value", "test"));
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		Map<String, Object> map = annotation.asMap();
		assertThat(map).hasSize(1).containsKey("value");
		StringValueAnnotation example = (StringValueAnnotation) map.get("value");
		assertThat(example.value()).isEqualTo("test");
	}

	@Test
	public void asMapWhenNestedArrayContainsSynthesized() {
		AnnotationType annotationType = AnnotationType.resolve(
				AnnotationArrayValueAnnotation.class);
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") });
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		Map<String, Object> map = annotation.asMap();
		assertThat(map).hasSize(1).containsKey("value");
		StringValueAnnotation[] example = (StringValueAnnotation[]) map.get("value");
		assertThat(example[0].value()).isEqualTo("test");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void asMapWhenNestedAndNestedAnnotationsToMapOptionContainsNestedMap() {
		AnnotationType annotationType = AnnotationType.resolve(
				AnnotationValueAnnotation.class);
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
		AnnotationType annotationType = AnnotationType.resolve(
				AnnotationArrayValueAnnotation.class);
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
		AnnotationType annotationType = AnnotationType.resolve(
				AnnotationValueAnnotation.class);
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				DeclaredAttributes.of("value", "test"));
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		AnnotationAttributes map = annotation.asMap(source -> new AnnotationAttributes(),
				MapValues.ANNOTATION_TO_MAP);
		assertThat(map).hasSize(1).containsKey("value");
		AnnotationAttributes example = (AnnotationAttributes) map.get("value");
		assertThat(example).containsOnly(entry("value", "test"));
	}

	@Test
	public void asSuppliedMapWhenFactoryReturnsNullReturnsNull() {
		AnnotationType annotationType = AnnotationType.resolve(
				AnnotationValueAnnotation.class);
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				DeclaredAttributes.of("value", "test"));
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		AnnotationAttributes map = annotation.asMap(source -> null);
		assertThat(map).isNull();
	}

	@Test
	public void synthesizeReturnsAnnotation() {
		MergedAnnotation<StringValueAnnotation> annotation = create(
				AnnotationType.resolve(StringValueAnnotation.class),
				DeclaredAttributes.of("value", "hello"));
		StringValueAnnotation synthesized = annotation.synthesize();
		assertThat(synthesized.value()).isEqualTo("hello");
	}

	@Test
	public void synthesizeWithPredicateWhenPredicateMatchesReturnsOptionalOfAnnotation() {
		MergedAnnotation<StringValueAnnotation> annotation = create(
				AnnotationType.resolve(StringValueAnnotation.class),
				DeclaredAttributes.of("value", "hello"));
		Optional<StringValueAnnotation> synthesized = annotation.synthesize(
				mergedAnnotation -> true);
		assertThat(synthesized.get().value()).isEqualTo("hello");
	}

	@Test
	public void synthesizeWithPredicateWhenPredicateDoesNotMatchReturnsEmpty() {
		MergedAnnotation<StringValueAnnotation> annotation = create(
				AnnotationType.resolve(StringValueAnnotation.class),
				DeclaredAttributes.of("value", "hello"));
		Optional<StringValueAnnotation> synthesized = annotation.synthesize(
				mergedAnnotation -> false);
		assertThat(synthesized).isEmpty();
	}

	@Test
	public void getAttributeWhenAlaisedMappedMapsAttribute() {
		AnnotationTypeMapping mapping = createWithStringAttribute(null,
				"com.example.MyComponent", "myName");
		AnnotationTypeMapping metaMapping = createWithStringAttribute(mapping,
				"com.example.Component", "name");
		metaMapping.addAlias("name", mapping, "myName");
		MergedAnnotation<?> annotation = new TypeMappedAnnotation<>(metaMapping,
				this.source, this.aggregateIndex,
				DeclaredAttributes.of("myName", "test"));
		assertThat(annotation.getString("name")).isEqualTo("test");
	}

	@Test
	public void getAttributeWhenConventionMappedMapsAttribute() {
		AnnotationTypeMapping mapping = createWithStringAttribute(null,
				"com.example.MyComponent", "name");
		AnnotationTypeMapping metaMapping = createWithStringAttribute(mapping,
				"com.example.Component", "name");
		MergedAnnotation<?> annotation = new TypeMappedAnnotation<>(metaMapping,
				this.source, this.aggregateIndex, DeclaredAttributes.of("name", "test"));
		assertThat(annotation.getString("name")).isEqualTo("test");
	}

	@Test
	public void getAttributeWhenConventionRestrictedDoesNotMapAttribute() {
		AnnotationTypeMapping mapping = createWithStringAttribute(null,
				"com.example.MyComponent", "value");
		AnnotationTypeMapping metaMapping = createWithStringAttribute(mapping,
				"com.example.Component", "value");
		MergedAnnotation<?> annotation = new TypeMappedAnnotation<>(metaMapping,
				this.source, this.aggregateIndex, DeclaredAttributes.of("value", "test"));
		assertThat(annotation.getString("value")).isEmpty();
		assertThat(annotation.getParent().getString("value")).isEqualTo("test");
	}

	@Test
	public void getAttributeWhenNoAliasOrConventionMappingMapsToAnnotationAttributes() {
		AnnotationTypeMapping mapping = createWithStringAttribute(null,
				"com.example.MyComponent", "myName");
		AnnotationType annotationType = AnnotationType.of("com.example.Component",
				DeclaredAnnotations.NONE,
				AttributeTypes.of(createStringAttributeType("name")));
		AnnotationTypeMapping metaMapping = new AnnotationTypeMapping(
				getClass().getClassLoader(), RepeatableContainers.standardRepeatables(),
				AnnotationFilter.PLAIN, mapping, annotationType,
				DeclaredAttributes.of("name", "test"));
		MergedAnnotation<?> annotation = new TypeMappedAnnotation<>(metaMapping,
				this.source, this.aggregateIndex, DeclaredAttributes.NONE);
		assertThat(annotation.getString("name")).isEqualTo("test");
		assertThat(annotation.getParent().getString("myName")).isEmpty();
	}

	@Test
	public void getAttributeWhenNoAliasOrConventionOrAnnotationAttributeMapsToAnnotationDefault() {
		AnnotationTypeMapping mapping = createWithStringAttribute(null,
				"com.example.MyComponent", "myName");
		AttributeType metaAttributeType = AttributeType.of("name", String.class.getName(),
				DeclaredAnnotations.NONE, "test");
		AnnotationType metaAnnotationType = createAnnotationType("com.example.Component",
				metaAttributeType);
		AnnotationTypeMapping metaMapping = new AnnotationTypeMapping(
				getClass().getClassLoader(), RepeatableContainers.standardRepeatables(),
				AnnotationFilter.PLAIN, mapping, metaAnnotationType,
				DeclaredAttributes.NONE);
		MergedAnnotation<?> annotation = new TypeMappedAnnotation<>(metaMapping,
				this.source, this.aggregateIndex, DeclaredAttributes.NONE);
		assertThat(annotation.getString("name")).isEqualTo("test");
	}

	@Test
	public void getAttributeWhenMappingFromNonArrayToArrayWrapsResultInArray() {
		AnnotationTypeMapping mapping = createWithStringAttribute(null,
				"com.example.MyComponent", "name");
		AttributeType metaAttributeType = AttributeType.of("name", "java.lang.String[]",
				DeclaredAnnotations.NONE, new String[0]);
		AnnotationType metaAnnotationType = createAnnotationType("com.example.Component",
				metaAttributeType);
		AnnotationTypeMapping metaMapping = new AnnotationTypeMapping(
				getClass().getClassLoader(), RepeatableContainers.standardRepeatables(),
				AnnotationFilter.PLAIN, mapping, metaAnnotationType,
				DeclaredAttributes.NONE);
		MergedAnnotation<?> annotation = new TypeMappedAnnotation<>(metaMapping,
				this.source, this.aggregateIndex, DeclaredAttributes.of("name", "test"));
		assertThat(annotation.getStringArray("name")).containsExactly("test");
		assertThat(annotation.getParent().getString("name")).isEqualTo("test");
	}

	@Test
	public void getAttributeWhenMirroredReturnsMirrorValues() {
		AnnotationTypeMapping mapping = createWithStringAttribute(null,
				"com.example.Component", "a", "b", "c");
		mapping.addMirrorSet("a", "b", "c");
		MergedAnnotation<?> annotation = new TypeMappedAnnotation<>(mapping, this.source,
				this.aggregateIndex, DeclaredAttributes.of("b", "B"));
		assertThat(annotation.getString("a")).isEqualTo("B");
		assertThat(annotation.getString("b")).isEqualTo("B");
		assertThat(annotation.getString("c")).isEqualTo("B");
	}

	@Test
	public void getAttributeWhenMirroredToEmptyArrayReturnsMirrorValues() {
		AttributeType a = AttributeType.of("a", "int[]", DeclaredAnnotations.NONE,
				new Object[0]);
		AttributeType b = AttributeType.of("b", "int[]", DeclaredAnnotations.NONE,
				new Object[0]);
		AnnotationTypeMapping mapping = new AnnotationTypeMapping(
				getClass().getClassLoader(), RepeatableContainers.standardRepeatables(),
				AnnotationFilter.PLAIN, null,
				createAnnotationType("com.example.Component", a, b),
				DeclaredAttributes.NONE);
		mapping.addMirrorSet("a", "b");
		MergedAnnotation<?> annotation = new TypeMappedAnnotation<>(mapping, this.source,
				this.aggregateIndex, DeclaredAttributes.of("b", new int[] { 123 }));
		assertThat(annotation.getIntArray("a")).containsExactly(123);
		assertThat(annotation.getIntArray("b")).containsExactly(123);
	}

	@Test
	public void getAttributeWhenMirroredToShadowReturnsMirroredValues() {
		// See SPR-14069 for background
		// Shadowing is when a meta-annotation is declared with a value that can
		// be ignored because it is also referenced via an AliasFor attribute
		// that must be provided by the user since it has no default.
		// Ideally we'd not support this going forward, but we want to remain
		// back-compatible as much as possible
		AttributeType c = AttributeType.of("c", "java.lang.String",
				DeclaredAnnotations.NONE, null);
		AnnotationTypeMapping mapping = new AnnotationTypeMapping(
				getClass().getClassLoader(), RepeatableContainers.standardRepeatables(),
				AnnotationFilter.PLAIN, null,
				createAnnotationType("com.example.MyComponent", c),
				DeclaredAttributes.NONE);
		AttributeType a = createStringAttributeType("a");
		AttributeType b = createStringAttributeType("b");
		AnnotationType metaAnnotationType = AnnotationType.of("com.example.Component",
				DeclaredAnnotations.NONE, AttributeTypes.of(a, b));
		AnnotationTypeMapping metaMapping = new AnnotationTypeMapping(
				getClass().getClassLoader(), RepeatableContainers.standardRepeatables(),
				AnnotationFilter.PLAIN, mapping, metaAnnotationType,
				DeclaredAttributes.of("a", "duplicateDeclaration"));
		metaMapping.addMirrorSet("a", "b");
		metaMapping.addAlias("b", mapping, "c");
		MergedAnnotation<?> annotation = new TypeMappedAnnotation<>(metaMapping,
				this.source, this.aggregateIndex, DeclaredAttributes.of("c", "C"));
		assertThat(annotation.getString("a")).isEqualTo("C");
		assertThat(annotation.getString("b")).isEqualTo("C");
	}

	@Test
	public void createWhenMirrorAttributesHaveDifferentValuesThrowsException() {
		AnnotationTypeMapping mapping = createWithStringAttribute(null,
				"com.example.Component", "a", "b");
		mapping.addMirrorSet("a", "b");
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> new TypeMappedAnnotation<>(mapping, this.source,
						this.aggregateIndex,
						DeclaredAttributes.of("a", "A", "b", "B"))).withMessageContaining(
								"Different @AliasFor mirror values");
	}

	@Test
	public void fromAnnotationTypeWhenAnnotationTypeIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> TypeMappedAnnotation.from(null, (Class<Annotation>) null,
						null)).withMessage("AnnotationType must not be null");
	}

	@Test
	public void fromAnnotationTypeReturnsAnnotation() {
		TypeMappedAnnotation<?> annotation = TypeMappedAnnotation.from(
				null, AnnotationTypeAnnotation.class, null);
		assertThat(annotation.getType()).isEqualTo(
				AnnotationTypeAnnotation.class.getName());
		assertThat(annotation.getValue("classValue", Class.class)).contains(
				InputStream.class);
		assertThat(annotation.getValue("stringValue", String.class)).isEmpty();
		assertThat(annotation.getDefaultValue("classValue", Class.class)).contains(
				InputStream.class);
		assertThat(annotation.getDefaultValue("stringValue", String.class)).isEmpty();
	}

	@Test
	public void fromAnnotationWhenAnnotationIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> TypeMappedAnnotation.from(null, (Annotation) null)).withMessage(
						"Annotation must not be null");
	}

	@Test
	public void fromAnnotationReturnsAnnotation() {
		TypeMappedAnnotation<?> annotation = TypeMappedAnnotation.from(
				WithAnnotationTypeAnnotation.class,
				WithAnnotationTypeAnnotation.class.getDeclaredAnnotation(
						AnnotationTypeAnnotation.class));
		assertThat(annotation.getType()).isEqualTo(
				AnnotationTypeAnnotation.class.getName());
		assertThat(annotation.getSource()).isEqualTo(WithAnnotationTypeAnnotation.class);
		assertThat(annotation.getValue("classValue", Class.class)).contains(
				OutputStream.class);
		assertThat(annotation.getValue("stringValue", String.class)).contains("test");
		assertThat(annotation.getDefaultValue("classValue", Class.class)).contains(
				InputStream.class);
		assertThat(annotation.getDefaultValue("stringValue", String.class)).isEmpty();
	}

	private <A extends Annotation> TypeMappedAnnotation<A> createTwoAttributeAnnotation() {
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

	private <A extends Annotation> TypeMappedAnnotation<A> create(Class<?> attributeType,
			Object value, Object defaultValue) {
		String attributeClassName = ClassUtils.getQualifiedName(attributeType);
		return create(AttributeType.of("value", attributeClassName,
				DeclaredAnnotations.NONE, defaultValue), value);
	}

	private <A extends Annotation> TypeMappedAnnotation<A> create(
			AttributeType attributeType, Object value) {
		AnnotationType annotationType = AnnotationType.of("com.example.Component",
				DeclaredAnnotations.NONE, AttributeTypes.of(attributeType));
		DeclaredAttributes attributes = DeclaredAttributes.of("value", value);
		return create(annotationType, attributes);
	}

	private <A extends Annotation> TypeMappedAnnotation<A> create(
			AnnotationType annotationType, DeclaredAttributes rootAttributes) {
		AnnotationTypeMapping mapping = new AnnotationTypeMapping(
				getClass().getClassLoader(), RepeatableContainers.standardRepeatables(),
				AnnotationFilter.PLAIN, annotationType);
		return new TypeMappedAnnotation<>(mapping, this.source, this.aggregateIndex,
				rootAttributes);
	}

	private <A extends Annotation> TypeMappedAnnotation<A> createMetaAnnotation(
			int depth) {
		AnnotationTypeMapping mapping = null;
		for (int i = 0; i < depth; i++) {
			AnnotationType annotationType = AnnotationType.of(
					"com.example.Annotation" + i, DeclaredAnnotations.NONE,
					AttributeTypes.NONE);
			mapping = new AnnotationTypeMapping(getClass().getClassLoader(),
					RepeatableContainers.standardRepeatables(), AnnotationFilter.PLAIN,
					mapping, annotationType, DeclaredAttributes.NONE);
		}
		return new TypeMappedAnnotation<>(mapping, this.source, this.aggregateIndex,
				DeclaredAttributes.NONE);
	}

	private AnnotationTypeMapping createWithStringAttribute(AnnotationTypeMapping parent,
			String annotationClassName, String... attributeNames) {
		AttributeType[] attributeTypes = new AttributeType[attributeNames.length];
		for (int i = 0; i < attributeTypes.length; i++) {
			attributeTypes[i] = createStringAttributeType(attributeNames[i]);
		}
		AnnotationType annotationType = createAnnotationType(annotationClassName,
				attributeTypes);
		return new AnnotationTypeMapping(getClass().getClassLoader(),
				RepeatableContainers.standardRepeatables(), AnnotationFilter.PLAIN,
				parent, annotationType, DeclaredAttributes.NONE);
	}

	private AttributeType createStringAttributeType(String attributeName) {
		return AttributeType.of(attributeName, String.class.getName(),
				DeclaredAnnotations.NONE, "");
	}

	private AnnotationType createAnnotationType(String className,
			AttributeType... attributeTypes) {
		return AnnotationType.of(className, DeclaredAnnotations.NONE,
				AttributeTypes.of(attributeTypes));
	}

	private void assertThatNoSuchElementException(ThrowingCallable throwingCallable) {
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
				throwingCallable);
	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface NoAttributesAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface StringValueAnnotation {

		String value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface AnnotationValueAnnotation {

		StringValueAnnotation value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface AnnotationArrayValueAnnotation {

		StringValueAnnotation[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface ClassValueAnnotation {

		Class<?> value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface ClassArrayValueAnnotation {

		Class<?>[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface AnnotationTypeAnnotation {

		Class<?> classValue() default InputStream.class;

		String stringValue();

	}

	@AnnotationTypeAnnotation(classValue = OutputStream.class, stringValue = "test")
	private static class WithAnnotationTypeAnnotation {

	}

	enum ExampleEnum {

		ONE, TWO, THREE

	}

	// FIXME new from/of methods

}
