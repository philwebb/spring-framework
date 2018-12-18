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
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.core.annotation.AnnotationTypeMapping.Reference;
import org.springframework.core.annotation.MergedAnnotation.MapValues;
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
import static org.junit.Assert.fail;

/**
 * Tests for {@link TypeMappedAnnotation}.
 *
 * @author Phillip Webb
 * @since 5.0
 */
@Ignore
public class TypeMappedAnnotationTests {

	private final Object source = new Object();

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
				ClassReference.of(String.class), null);
		assertThat(annotation.getClass("value")).isEqualTo(String.class);
	}

	@Test
	public void getClassWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(String.class, null,
				ClassReference.of(String.class));
		assertThat(annotation.getClass("value")).isEqualTo(String.class);
	}

	@Test
	public void getClassArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(Class[].class,
				new ClassReference[] { ClassReference.of(String.class) }, null);
		assertThat(annotation.getClassArray("value")).containsExactly(String.class);
	}

	@Test
	public void getClassArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(ClassReference[].class, null,
				new ClassReference[] { ClassReference.of(String.class) });
		assertThat(annotation.getClassArray("value")).containsExactly(String.class);
	}

	@Test
	public void getClassAsStringWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(Class.class,
				ClassReference.of(String.class), null);
		assertThat(annotation.getString("value")).isEqualTo(String.class.getName());
	}

	@Test
	public void getClassAsStringWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(String.class, null,
				ClassReference.of(String.class));
		assertThat(annotation.getString("value")).isEqualTo(String.class.getName());
	}

	@Test
	public void getClassAsStringArrayWhenHasAttributeValueReturnsValue() {
		MergedAnnotation<?> annotation = create(Class[].class,
				new ClassReference[] { ClassReference.of(String.class) }, null);
		assertThat(annotation.getStringArray("value")).containsExactly(
				String.class.getName());
	}

	@Test
	public void getClassAsStringArrayWhenHasNoAttributeValueReturnsDefaultValue() {
		MergedAnnotation<?> annotation = create(ClassReference[].class, null,
				new ClassReference[] { ClassReference.of(String.class) });
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
	public void getAttributeWhenAvailableReturnsOptionalOf() {
		MergedAnnotation<?> annotation = create(byte.class, (byte) 123, null);
		assertThat(annotation.getAttribute("value", Byte.class)).isEqualTo(
				Optional.of((byte) 123));
	}

	@Test
	public void getAttributeWhenMissingReturnsEmptyOptional() {
		MergedAnnotation<?> annotation = create(byte.class, (byte) 123, null);
		assertThat(annotation.getAttribute("missing", Byte.class)).isEqualTo(
				Optional.empty());
	}

	@Test
	public void getClassReferenceAttributeAsClassAdapts() {
		MergedAnnotation<?> annotation = create(Class.class,
				ClassReference.of(String.class), null);
		assertThat(annotation.getAttribute("value", Class.class).get()).isEqualTo(
				String.class);
	}

	@Test
	public void getAttributeForClassReferenceAsStringAdapts() {
		MergedAnnotation<?> annotation = create(Class.class,
				ClassReference.of(String.class), null);
		assertThat(annotation.getAttribute("value", String.class).get()).isEqualTo(
				"java.lang.String");
	}

	@Test
	public void getAttributeForArrayAsClassArrayAdapts() {
		MergedAnnotation<?> annotation = create(Class[].class,
				new ClassReference[] { ClassReference.of(String.class) }, null);
		assertThat(annotation.getAttribute("value", Class[].class).get()).containsExactly(
				String.class);
	}

	@Test
	public void getAttributeForClassReferenceAsStringArrayAdapts() {
		MergedAnnotation<?> annotation = create(Class[].class,
				new ClassReference[] { ClassReference.of(String.class) }, null);
		assertThat(
				annotation.getAttribute("value", String[].class).get()).containsExactly(
						"java.lang.String");
	}

	@Test
	public void getAttributeForEnumValueReferenceAsEnumAdapts() {
		MergedAnnotation<?> annotation = create(EnumValueReference.class,
				EnumValueReference.from(ExampleEnum.ONE), null);
		assertThat(annotation.getAttribute("value", ExampleEnum.class).get()).isEqualTo(
				ExampleEnum.ONE);
	}

	@Test
	public void getAttributeForEnumValueReferenceArrayAsEnumArrayAdapts() {
		MergedAnnotation<?> annotation = create(EnumValueReference[].class,
				new EnumValueReference[] { EnumValueReference.from(ExampleEnum.ONE) },
				null);
		assertThat(annotation.getAttribute("value",
				ExampleEnum[].class).get()).containsExactly(ExampleEnum.ONE);
	}

	@Test
	public void getAttributeForDeclaredAttributesAsAnnotationAdapts() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation.class,
				DeclaredAttributes.of("value", "test"), null);
		StringValueAnnotation nested = annotation.getAttribute("value",
				StringValueAnnotation.class).get();
		assertThat(nested.value()).isEqualTo("test");
	}

	@Test
	public void getAttributeForDeclaredAttributesArrayAsAnnotationArrayAdapts() {
		MergedAnnotation<?> annotation = create(StringValueAnnotation[].class,
				new DeclaredAttributes[] { DeclaredAttributes.of("value", "test") },
				null);
		StringValueAnnotation nested = annotation.getAttribute("value",
				StringValueAnnotation[].class).get()[0];
		assertThat(nested.value()).isEqualTo("test");
	}

	@Test
	public void getAttributeWhenUnsupportedTypeThrowsException() {
		MergedAnnotation<?> annotation = create(String.class, "test", null);
		assertThatIllegalArgumentException().isThrownBy(
				() -> annotation.getAttribute("value", InputStream.class)).withMessage(
						"Type " + InputStream.class.getName() + " is not supported");
	}

	@Test
	public void extractFromEmptyObjectArraySupportsEveryArrayType() {
		TypeMappedAnnotation<?> annotation = createTwoAttributeAnnotation();
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
	public void withNonMergedAttributesReturnsNonMerged() {
		AttributeType parentAttributeType = AttributeType.of("name",
				String.class.getName(), DeclaredAnnotations.NONE, "");
		AnnotationType parentType = AnnotationType.of("com.example.Component",
				DeclaredAnnotations.NONE, AttributeTypes.of(parentAttributeType));
		AnnotationTypeMapping parentMapping = new AnnotationTypeMapping(
				getClass().getClassLoader(), RepeatableContainers.standardRepeatables(),
				null, parentType, DeclaredAttributes.NONE);
		AttributeType attributeType = AttributeType.of("componentName",
				String.class.getName(), DeclaredAnnotations.NONE, "");
		AnnotationType type = AnnotationType.of("com.example.Service",
				DeclaredAnnotations.NONE, AttributeTypes.of(attributeType));
		AnnotationTypeMapping mapping = new AnnotationTypeMapping(
				getClass().getClassLoader(), RepeatableContainers.standardRepeatables(),
				parentMapping, type, DeclaredAttributes.NONE);
		mapping.addAlias("componentName", new Reference(parentMapping, parentAttributeType));
		fail();
	}

	@Test
	public void withNonMergedWhenMirroredReturnsNonMergedButStillMirrored() {
		fail();
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
				ClassReference.of(StringBuilder.class));
		MergedAnnotation<?> annotation = create(annotationType, attributes);
		Map<String, Object> map = annotation.asMap(MapValues.CLASS_TO_STRING);
		assertThat(map).containsOnly(entry("value", StringBuilder.class.getName()));
	}

	@Test
	public void asMapWhenClassArrayAndMapClassToStringOptionContainsStringArray() {
		AnnotationType annotationType = AnnotationType.resolve(
				ClassArrayValueAnnotation.class);
		DeclaredAttributes attributes = DeclaredAttributes.of("value",
				new ClassReference[] { ClassReference.of(StringBuffer.class) });
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
		fail();
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
		fail();
	}

	@Test
	public void synthesizeWithPredicateWhenPredicateDoesNotMatchReturnsEmpty() {
		fail();
	}

	// FIXME mirror and alias tests

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
				annotationType);
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
					RepeatableContainers.standardRepeatables(), mapping, annotationType,
					DeclaredAttributes.NONE);
		}
		return new TypeMappedAnnotation<>(mapping, this.source, this.aggregateIndex,
				DeclaredAttributes.NONE);
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

	enum ExampleEnum {

		ONE, TWO, THREE

	}

}
