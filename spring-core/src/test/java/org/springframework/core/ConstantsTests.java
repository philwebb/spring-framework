/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import java.util.Locale;
import java.util.Set;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 28.04.2003
 */
public class ConstantsTests {

	@Test
	public void constants() {
		Constants c = new Constants(A.class);
		assertThat((Object) c.getClassName()).isEqualTo(A.class.getName());
		assertEquals(9, c.getSize());

		assertEquals(A.DOG, c.asNumber("DOG").intValue());
		assertEquals(A.DOG, c.asNumber("dog").intValue());
		assertEquals(A.CAT, c.asNumber("cat").intValue());

		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.asNumber("bogus"));

		assertThat(c.asString("S1").equals(A.S1)).isTrue();
		assertThatExceptionOfType(Constants.ConstantException.class).as("wrong type").isThrownBy(() ->
				c.asNumber("S1"));
	}

	@Test
	public void getNames() {
		Constants c = new Constants(A.class);

		Set<?> names = c.getNames("");
		assertEquals(c.getSize(), names.size());
		assertThat(names.contains("DOG")).isTrue();
		assertThat(names.contains("CAT")).isTrue();
		assertThat(names.contains("S1")).isTrue();

		names = c.getNames("D");
		assertEquals(1, names.size());
		assertThat(names.contains("DOG")).isTrue();

		names = c.getNames("d");
		assertEquals(1, names.size());
		assertThat(names.contains("DOG")).isTrue();
	}

	@Test
	public void getValues() {
		Constants c = new Constants(A.class);

		Set<?> values = c.getValues("");
		assertEquals(7, values.size());
		assertThat(values.contains(Integer.valueOf(0))).isTrue();
		assertThat(values.contains(Integer.valueOf(66))).isTrue();
		assertThat(values.contains("")).isTrue();

		values = c.getValues("D");
		assertEquals(1, values.size());
		assertThat(values.contains(Integer.valueOf(0))).isTrue();

		values = c.getValues("prefix");
		assertEquals(2, values.size());
		assertThat(values.contains(Integer.valueOf(1))).isTrue();
		assertThat(values.contains(Integer.valueOf(2))).isTrue();

		values = c.getValuesForProperty("myProperty");
		assertEquals(2, values.size());
		assertThat(values.contains(Integer.valueOf(1))).isTrue();
		assertThat(values.contains(Integer.valueOf(2))).isTrue();
	}

	@Test
	public void getValuesInTurkey() {
		Locale oldLocale = Locale.getDefault();
		Locale.setDefault(new Locale("tr", ""));
		try {
			Constants c = new Constants(A.class);

			Set<?> values = c.getValues("");
			assertEquals(7, values.size());
			assertThat(values.contains(Integer.valueOf(0))).isTrue();
			assertThat(values.contains(Integer.valueOf(66))).isTrue();
			assertThat(values.contains("")).isTrue();

			values = c.getValues("D");
			assertEquals(1, values.size());
			assertThat(values.contains(Integer.valueOf(0))).isTrue();

			values = c.getValues("prefix");
			assertEquals(2, values.size());
			assertThat(values.contains(Integer.valueOf(1))).isTrue();
			assertThat(values.contains(Integer.valueOf(2))).isTrue();

			values = c.getValuesForProperty("myProperty");
			assertEquals(2, values.size());
			assertThat(values.contains(Integer.valueOf(1))).isTrue();
			assertThat(values.contains(Integer.valueOf(2))).isTrue();
		}
		finally {
			Locale.setDefault(oldLocale);
		}
	}

	@Test
	public void suffixAccess() {
		Constants c = new Constants(A.class);

		Set<?> names = c.getNamesForSuffix("_PROPERTY");
		assertEquals(2, names.size());
		assertThat(names.contains("NO_PROPERTY")).isTrue();
		assertThat(names.contains("YES_PROPERTY")).isTrue();

		Set<?> values = c.getValuesForSuffix("_PROPERTY");
		assertEquals(2, values.size());
		assertThat(values.contains(Integer.valueOf(3))).isTrue();
		assertThat(values.contains(Integer.valueOf(4))).isTrue();
	}

	@Test
	public void toCode() {
		Constants c = new Constants(A.class);

		assertThat((Object) c.toCode(Integer.valueOf(0), "")).isEqualTo("DOG");
		assertThat((Object) c.toCode(Integer.valueOf(0), "D")).isEqualTo("DOG");
		assertThat((Object) c.toCode(Integer.valueOf(0), "DO")).isEqualTo("DOG");
		assertThat((Object) c.toCode(Integer.valueOf(0), "DoG")).isEqualTo("DOG");
		assertThat((Object) c.toCode(Integer.valueOf(0), null)).isEqualTo("DOG");
		assertThat((Object) c.toCode(Integer.valueOf(66), "")).isEqualTo("CAT");
		assertThat((Object) c.toCode(Integer.valueOf(66), "C")).isEqualTo("CAT");
		assertThat((Object) c.toCode(Integer.valueOf(66), "ca")).isEqualTo("CAT");
		assertThat((Object) c.toCode(Integer.valueOf(66), "cAt")).isEqualTo("CAT");
		assertThat((Object) c.toCode(Integer.valueOf(66), null)).isEqualTo("CAT");
		assertThat((Object) c.toCode("", "")).isEqualTo("S1");
		assertThat((Object) c.toCode("", "s")).isEqualTo("S1");
		assertThat((Object) c.toCode("", "s1")).isEqualTo("S1");
		assertThat((Object) c.toCode("", null)).isEqualTo("S1");
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCode("bogus", "bogus"));
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCode("bogus", null));

		assertThat((Object) c.toCodeForProperty(Integer.valueOf(1), "myProperty")).isEqualTo("MY_PROPERTY_NO");
		assertThat((Object) c.toCodeForProperty(Integer.valueOf(2), "myProperty")).isEqualTo("MY_PROPERTY_YES");
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCodeForProperty("bogus", "bogus"));

		assertThat((Object) c.toCodeForSuffix(Integer.valueOf(0), "")).isEqualTo("DOG");
		assertThat((Object) c.toCodeForSuffix(Integer.valueOf(0), "G")).isEqualTo("DOG");
		assertThat((Object) c.toCodeForSuffix(Integer.valueOf(0), "OG")).isEqualTo("DOG");
		assertThat((Object) c.toCodeForSuffix(Integer.valueOf(0), "DoG")).isEqualTo("DOG");
		assertThat((Object) c.toCodeForSuffix(Integer.valueOf(0), null)).isEqualTo("DOG");
		assertThat((Object) c.toCodeForSuffix(Integer.valueOf(66), "")).isEqualTo("CAT");
		assertThat((Object) c.toCodeForSuffix(Integer.valueOf(66), "T")).isEqualTo("CAT");
		assertThat((Object) c.toCodeForSuffix(Integer.valueOf(66), "at")).isEqualTo("CAT");
		assertThat((Object) c.toCodeForSuffix(Integer.valueOf(66), "cAt")).isEqualTo("CAT");
		assertThat((Object) c.toCodeForSuffix(Integer.valueOf(66), null)).isEqualTo("CAT");
		assertThat((Object) c.toCodeForSuffix("", "")).isEqualTo("S1");
		assertThat((Object) c.toCodeForSuffix("", "1")).isEqualTo("S1");
		assertThat((Object) c.toCodeForSuffix("", "s1")).isEqualTo("S1");
		assertThat((Object) c.toCodeForSuffix("", null)).isEqualTo("S1");
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCodeForSuffix("bogus", "bogus"));
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCodeForSuffix("bogus", null));
	}

	@Test
	public void getValuesWithNullPrefix() throws Exception {
		Constants c = new Constants(A.class);
		Set<?> values = c.getValues(null);
		assertEquals("Must have returned *all* public static final values", 7, values.size());
	}

	@Test
	public void getValuesWithEmptyStringPrefix() throws Exception {
		Constants c = new Constants(A.class);
		Set<Object> values = c.getValues("");
		assertEquals("Must have returned *all* public static final values", 7, values.size());
	}

	@Test
	public void getValuesWithWhitespacedStringPrefix() throws Exception {
		Constants c = new Constants(A.class);
		Set<?> values = c.getValues(" ");
		assertEquals("Must have returned *all* public static final values", 7, values.size());
	}

	@Test
	public void withClassThatExposesNoConstants() throws Exception {
		Constants c = new Constants(NoConstants.class);
		assertEquals(0, c.getSize());
		final Set<?> values = c.getValues("");
		assertNotNull(values);
		assertEquals(0, values.size());
	}

	@Test
	public void ctorWithNullClass() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new Constants(null));
	}


	private static final class NoConstants {
	}


	@SuppressWarnings("unused")
	private static final class A {

		public static final int DOG = 0;
		public static final int CAT = 66;
		public static final String S1 = "";

		public static final int PREFIX_NO = 1;
		public static final int PREFIX_YES = 2;

		public static final int MY_PROPERTY_NO = 1;
		public static final int MY_PROPERTY_YES = 2;

		public static final int NO_PROPERTY = 3;
		public static final int YES_PROPERTY = 4;

		/** ignore these */
		protected static final int P = -1;
		protected boolean f;
		static final Object o = new Object();
	}

}
