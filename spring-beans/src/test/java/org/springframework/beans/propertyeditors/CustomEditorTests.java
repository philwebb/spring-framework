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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.beans.PropertyVetoException;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import org.junit.Test;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.tests.sample.beans.BooleanTestBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.IndexedTestBean;
import org.springframework.tests.sample.beans.NumberTestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;
import static temp.XAssert.assertSame;

/**
 * Unit tests for the various PropertyEditors in Spring.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Rob Harrop
 * @author Arjen Poutsma
 * @author Chris Beams
 * @since 10.06.2003
 */
public class CustomEditorTests {

	@Test
	public void testComplexObject() {
		TestBean tb = new TestBean();
		String newName = "Rod";
		String tbString = "Kerry_34";

		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(ITestBean.class, new TestBeanEditor());
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("age", new Integer(55)));
		pvs.addPropertyValue(new PropertyValue("name", newName));
		pvs.addPropertyValue(new PropertyValue("touchy", "valid"));
		pvs.addPropertyValue(new PropertyValue("spouse", tbString));
		bw.setPropertyValues(pvs);
		assertThat(tb.getSpouse() != null).as("spouse is non-null").isTrue();
		assertThat(tb.getSpouse().getName().equals("Kerry") && tb.getSpouse().getAge() == 34).as("spouse name is Kerry and age is 34").isTrue();
	}

	@Test
	public void testComplexObjectWithOldValueAccess() {
		TestBean tb = new TestBean();
		String newName = "Rod";
		String tbString = "Kerry_34";

		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.setExtractOldValueForEditor(true);
		bw.registerCustomEditor(ITestBean.class, new OldValueAccessingTestBeanEditor());
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("age", new Integer(55)));
		pvs.addPropertyValue(new PropertyValue("name", newName));
		pvs.addPropertyValue(new PropertyValue("touchy", "valid"));
		pvs.addPropertyValue(new PropertyValue("spouse", tbString));

		bw.setPropertyValues(pvs);
		assertThat(tb.getSpouse() != null).as("spouse is non-null").isTrue();
		assertThat(tb.getSpouse().getName().equals("Kerry") && tb.getSpouse().getAge() == 34).as("spouse name is Kerry and age is 34").isTrue();
		ITestBean spouse = tb.getSpouse();

		bw.setPropertyValues(pvs);
		assertSame("Should have remained same object", spouse, tb.getSpouse());
	}

	@Test
	public void testCustomEditorForSingleProperty() {
		TestBean tb = new TestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(String.class, "name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix" + text);
			}
		});
		bw.setPropertyValue("name", "value");
		bw.setPropertyValue("touchy", "value");
		assertEquals("prefixvalue", bw.getPropertyValue("name"));
		assertEquals("prefixvalue", tb.getName());
		assertEquals("value", bw.getPropertyValue("touchy"));
		assertEquals("value", tb.getTouchy());
	}

	@Test
	public void testCustomEditorForAllStringProperties() {
		TestBean tb = new TestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(String.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix" + text);
			}
		});
		bw.setPropertyValue("name", "value");
		bw.setPropertyValue("touchy", "value");
		assertEquals("prefixvalue", bw.getPropertyValue("name"));
		assertEquals("prefixvalue", tb.getName());
		assertEquals("prefixvalue", bw.getPropertyValue("touchy"));
		assertEquals("prefixvalue", tb.getTouchy());
	}

	@Test
	public void testCustomEditorForSingleNestedProperty() {
		TestBean tb = new TestBean();
		tb.setSpouse(new TestBean());
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(String.class, "spouse.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix" + text);
			}
		});
		bw.setPropertyValue("spouse.name", "value");
		bw.setPropertyValue("touchy", "value");
		assertEquals("prefixvalue", bw.getPropertyValue("spouse.name"));
		assertEquals("prefixvalue", tb.getSpouse().getName());
		assertEquals("value", bw.getPropertyValue("touchy"));
		assertEquals("value", tb.getTouchy());
	}

	@Test
	public void testCustomEditorForAllNestedStringProperties() {
		TestBean tb = new TestBean();
		tb.setSpouse(new TestBean());
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(String.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix" + text);
			}
		});
		bw.setPropertyValue("spouse.name", "value");
		bw.setPropertyValue("touchy", "value");
		assertEquals("prefixvalue", bw.getPropertyValue("spouse.name"));
		assertEquals("prefixvalue", tb.getSpouse().getName());
		assertEquals("prefixvalue", bw.getPropertyValue("touchy"));
		assertEquals("prefixvalue", tb.getTouchy());
	}

	@Test
	public void testDefaultBooleanEditorForPrimitiveType() {
		BooleanTestBean tb = new BooleanTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);

		bw.setPropertyValue("bool1", "true");
		assertThat(Boolean.TRUE.equals(bw.getPropertyValue("bool1"))).as("Correct bool1 value").isTrue();
		assertThat(tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "false");
		assertThat(Boolean.FALSE.equals(bw.getPropertyValue("bool1"))).as("Correct bool1 value").isTrue();
		boolean condition4 = !tb.isBool1();
		assertThat(condition4).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "  true  ");
		assertThat(tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "  false  ");
		boolean condition3 = !tb.isBool1();
		assertThat(condition3).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "on");
		assertThat(tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "off");
		boolean condition2 = !tb.isBool1();
		assertThat(condition2).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "yes");
		assertThat(tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "no");
		boolean condition1 = !tb.isBool1();
		assertThat(condition1).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "1");
		assertThat(tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "0");
		boolean condition = !tb.isBool1();
		assertThat(condition).as("Correct bool1 value").isTrue();

		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
				bw.setPropertyValue("bool1", "argh"));
	}

	@Test
	public void testDefaultBooleanEditorForWrapperType() {
		BooleanTestBean tb = new BooleanTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);

		bw.setPropertyValue("bool2", "true");
		assertThat(Boolean.TRUE.equals(bw.getPropertyValue("bool2"))).as("Correct bool2 value").isTrue();
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "false");
		assertThat(Boolean.FALSE.equals(bw.getPropertyValue("bool2"))).as("Correct bool2 value").isTrue();
		boolean condition3 = !tb.getBool2().booleanValue();
		assertThat(condition3).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "on");
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "off");
		boolean condition2 = !tb.getBool2().booleanValue();
		assertThat(condition2).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "yes");
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "no");
		boolean condition1 = !tb.getBool2().booleanValue();
		assertThat(condition1).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "1");
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "0");
		boolean condition = !tb.getBool2().booleanValue();
		assertThat(condition).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "");
		assertNull("Correct bool2 value", tb.getBool2());
	}

	@Test
	public void testCustomBooleanEditorWithAllowEmpty() {
		BooleanTestBean tb = new BooleanTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(Boolean.class, new CustomBooleanEditor(true));

		bw.setPropertyValue("bool2", "true");
		assertThat(Boolean.TRUE.equals(bw.getPropertyValue("bool2"))).as("Correct bool2 value").isTrue();
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "false");
		assertThat(Boolean.FALSE.equals(bw.getPropertyValue("bool2"))).as("Correct bool2 value").isTrue();
		boolean condition3 = !tb.getBool2().booleanValue();
		assertThat(condition3).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "on");
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "off");
		boolean condition2 = !tb.getBool2().booleanValue();
		assertThat(condition2).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "yes");
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "no");
		boolean condition1 = !tb.getBool2().booleanValue();
		assertThat(condition1).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "1");
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "0");
		boolean condition = !tb.getBool2().booleanValue();
		assertThat(condition).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "");
		assertThat(bw.getPropertyValue("bool2") == null).as("Correct bool2 value").isTrue();
		assertThat(tb.getBool2() == null).as("Correct bool2 value").isTrue();
	}

	@Test
	public void testCustomBooleanEditorWithSpecialTrueAndFalseStrings() throws Exception {
		String trueString = "pechorin";
		String falseString = "nash";

		CustomBooleanEditor editor = new CustomBooleanEditor(trueString, falseString, false);

		editor.setAsText(trueString);
		assertThat(((Boolean) editor.getValue()).booleanValue()).isTrue();
		assertEquals(trueString, editor.getAsText());
		editor.setAsText(falseString);
		assertThat(((Boolean) editor.getValue()).booleanValue()).isFalse();
		assertEquals(falseString, editor.getAsText());

		editor.setAsText(trueString.toUpperCase());
		assertThat(((Boolean) editor.getValue()).booleanValue()).isTrue();
		assertEquals(trueString, editor.getAsText());
		editor.setAsText(falseString.toUpperCase());
		assertThat(((Boolean) editor.getValue()).booleanValue()).isFalse();
		assertEquals(falseString, editor.getAsText());
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(null));
	}

	@Test
	public void testDefaultNumberEditor() {
		NumberTestBean tb = new NumberTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);

		bw.setPropertyValue("short1", "1");
		bw.setPropertyValue("short2", "2");
		bw.setPropertyValue("int1", "7");
		bw.setPropertyValue("int2", "8");
		bw.setPropertyValue("long1", "5");
		bw.setPropertyValue("long2", "6");
		bw.setPropertyValue("bigInteger", "3");
		bw.setPropertyValue("float1", "7.1");
		bw.setPropertyValue("float2", "8.1");
		bw.setPropertyValue("double1", "5.1");
		bw.setPropertyValue("double2", "6.1");
		bw.setPropertyValue("bigDecimal", "4.5");

		assertThat(new Short("1").equals(bw.getPropertyValue("short1"))).as("Correct short1 value").isTrue();
		assertThat(tb.getShort1() == 1).as("Correct short1 value").isTrue();
		assertThat(new Short("2").equals(bw.getPropertyValue("short2"))).as("Correct short2 value").isTrue();
		assertThat(new Short("2").equals(tb.getShort2())).as("Correct short2 value").isTrue();
		assertThat(new Integer("7").equals(bw.getPropertyValue("int1"))).as("Correct int1 value").isTrue();
		assertThat(tb.getInt1() == 7).as("Correct int1 value").isTrue();
		assertThat(new Integer("8").equals(bw.getPropertyValue("int2"))).as("Correct int2 value").isTrue();
		assertThat(new Integer("8").equals(tb.getInt2())).as("Correct int2 value").isTrue();
		assertThat(new Long("5").equals(bw.getPropertyValue("long1"))).as("Correct long1 value").isTrue();
		assertThat(tb.getLong1() == 5).as("Correct long1 value").isTrue();
		assertThat(new Long("6").equals(bw.getPropertyValue("long2"))).as("Correct long2 value").isTrue();
		assertThat(new Long("6").equals(tb.getLong2())).as("Correct long2 value").isTrue();
		assertThat(new BigInteger("3").equals(bw.getPropertyValue("bigInteger"))).as("Correct bigInteger value").isTrue();
		assertThat(new BigInteger("3").equals(tb.getBigInteger())).as("Correct bigInteger value").isTrue();
		assertThat(new Float("7.1").equals(bw.getPropertyValue("float1"))).as("Correct float1 value").isTrue();
		assertThat(new Float("7.1").equals(new Float(tb.getFloat1()))).as("Correct float1 value").isTrue();
		assertThat(new Float("8.1").equals(bw.getPropertyValue("float2"))).as("Correct float2 value").isTrue();
		assertThat(new Float("8.1").equals(tb.getFloat2())).as("Correct float2 value").isTrue();
		assertThat(new Double("5.1").equals(bw.getPropertyValue("double1"))).as("Correct double1 value").isTrue();
		assertThat(tb.getDouble1() == 5.1).as("Correct double1 value").isTrue();
		assertThat(new Double("6.1").equals(bw.getPropertyValue("double2"))).as("Correct double2 value").isTrue();
		assertThat(new Double("6.1").equals(tb.getDouble2())).as("Correct double2 value").isTrue();
		assertThat(new BigDecimal("4.5").equals(bw.getPropertyValue("bigDecimal"))).as("Correct bigDecimal value").isTrue();
		assertThat(new BigDecimal("4.5").equals(tb.getBigDecimal())).as("Correct bigDecimal value").isTrue();
	}

	@Test
	public void testCustomNumberEditorWithoutAllowEmpty() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMAN);
		NumberTestBean tb = new NumberTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(short.class, new CustomNumberEditor(Short.class, nf, false));
		bw.registerCustomEditor(Short.class, new CustomNumberEditor(Short.class, nf, false));
		bw.registerCustomEditor(int.class, new CustomNumberEditor(Integer.class, nf, false));
		bw.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, nf, false));
		bw.registerCustomEditor(long.class, new CustomNumberEditor(Long.class, nf, false));
		bw.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, nf, false));
		bw.registerCustomEditor(BigInteger.class, new CustomNumberEditor(BigInteger.class, nf, false));
		bw.registerCustomEditor(float.class, new CustomNumberEditor(Float.class, nf, false));
		bw.registerCustomEditor(Float.class, new CustomNumberEditor(Float.class, nf, false));
		bw.registerCustomEditor(double.class, new CustomNumberEditor(Double.class, nf, false));
		bw.registerCustomEditor(Double.class, new CustomNumberEditor(Double.class, nf, false));
		bw.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, nf, false));

		bw.setPropertyValue("short1", "1");
		bw.setPropertyValue("short2", "2");
		bw.setPropertyValue("int1", "7");
		bw.setPropertyValue("int2", "8");
		bw.setPropertyValue("long1", "5");
		bw.setPropertyValue("long2", "6");
		bw.setPropertyValue("bigInteger", "3");
		bw.setPropertyValue("float1", "7,1");
		bw.setPropertyValue("float2", "8,1");
		bw.setPropertyValue("double1", "5,1");
		bw.setPropertyValue("double2", "6,1");
		bw.setPropertyValue("bigDecimal", "4,5");

		assertThat(new Short("1").equals(bw.getPropertyValue("short1"))).as("Correct short1 value").isTrue();
		assertThat(tb.getShort1() == 1).as("Correct short1 value").isTrue();
		assertThat(new Short("2").equals(bw.getPropertyValue("short2"))).as("Correct short2 value").isTrue();
		assertThat(new Short("2").equals(tb.getShort2())).as("Correct short2 value").isTrue();
		assertThat(new Integer("7").equals(bw.getPropertyValue("int1"))).as("Correct int1 value").isTrue();
		assertThat(tb.getInt1() == 7).as("Correct int1 value").isTrue();
		assertThat(new Integer("8").equals(bw.getPropertyValue("int2"))).as("Correct int2 value").isTrue();
		assertThat(new Integer("8").equals(tb.getInt2())).as("Correct int2 value").isTrue();
		assertThat(new Long("5").equals(bw.getPropertyValue("long1"))).as("Correct long1 value").isTrue();
		assertThat(tb.getLong1() == 5).as("Correct long1 value").isTrue();
		assertThat(new Long("6").equals(bw.getPropertyValue("long2"))).as("Correct long2 value").isTrue();
		assertThat(new Long("6").equals(tb.getLong2())).as("Correct long2 value").isTrue();
		assertThat(new BigInteger("3").equals(bw.getPropertyValue("bigInteger"))).as("Correct bigInteger value").isTrue();
		assertThat(new BigInteger("3").equals(tb.getBigInteger())).as("Correct bigInteger value").isTrue();
		assertThat(new Float("7.1").equals(bw.getPropertyValue("float1"))).as("Correct float1 value").isTrue();
		assertThat(new Float("7.1").equals(new Float(tb.getFloat1()))).as("Correct float1 value").isTrue();
		assertThat(new Float("8.1").equals(bw.getPropertyValue("float2"))).as("Correct float2 value").isTrue();
		assertThat(new Float("8.1").equals(tb.getFloat2())).as("Correct float2 value").isTrue();
		assertThat(new Double("5.1").equals(bw.getPropertyValue("double1"))).as("Correct double1 value").isTrue();
		assertThat(tb.getDouble1() == 5.1).as("Correct double1 value").isTrue();
		assertThat(new Double("6.1").equals(bw.getPropertyValue("double2"))).as("Correct double2 value").isTrue();
		assertThat(new Double("6.1").equals(tb.getDouble2())).as("Correct double2 value").isTrue();
		assertThat(new BigDecimal("4.5").equals(bw.getPropertyValue("bigDecimal"))).as("Correct bigDecimal value").isTrue();
		assertThat(new BigDecimal("4.5").equals(tb.getBigDecimal())).as("Correct bigDecimal value").isTrue();
	}

	@Test
	public void testCustomNumberEditorWithAllowEmpty() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMAN);
		NumberTestBean tb = new NumberTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(long.class, new CustomNumberEditor(Long.class, nf, true));
		bw.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, nf, true));

		bw.setPropertyValue("long1", "5");
		bw.setPropertyValue("long2", "6");
		assertThat(new Long("5").equals(bw.getPropertyValue("long1"))).as("Correct long1 value").isTrue();
		assertThat(tb.getLong1() == 5).as("Correct long1 value").isTrue();
		assertThat(new Long("6").equals(bw.getPropertyValue("long2"))).as("Correct long2 value").isTrue();
		assertThat(new Long("6").equals(tb.getLong2())).as("Correct long2 value").isTrue();

		bw.setPropertyValue("long2", "");
		assertThat(bw.getPropertyValue("long2") == null).as("Correct long2 value").isTrue();
		assertThat(tb.getLong2() == null).as("Correct long2 value").isTrue();
		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
				bw.setPropertyValue("long1", ""));
		assertThat(bw.getPropertyValue("long1")).isEqualTo(5L);
		assertThat(tb.getLong1()).isEqualTo(5);
	}

	@Test
	public void testCustomNumberEditorWithFrenchBigDecimal() throws Exception {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRENCH);
		NumberTestBean tb = new NumberTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, nf, true));
		bw.setPropertyValue("bigDecimal", "1000");
		assertEquals(1000.0f, tb.getBigDecimal().floatValue(), 0f);
		bw.setPropertyValue("bigDecimal", "1000,5");
		assertEquals(1000.5f, tb.getBigDecimal().floatValue(), 0f);
		bw.setPropertyValue("bigDecimal", "1 000,5");
		assertEquals(1000.5f, tb.getBigDecimal().floatValue(), 0f);
	}

	@Test
	public void testParseShortGreaterThanMaxValueWithoutNumberFormat() {
		CustomNumberEditor editor = new CustomNumberEditor(Short.class, true);
		assertThatExceptionOfType(NumberFormatException.class).as("greater than Short.MAX_VALUE + 1").isThrownBy(() ->
			editor.setAsText(String.valueOf(Short.MAX_VALUE + 1)));
	}

	@Test
	public void testByteArrayPropertyEditor() {
		PrimitiveArrayBean bean = new PrimitiveArrayBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.setPropertyValue("byteArray", "myvalue");
		assertEquals("myvalue", new String(bean.getByteArray()));
	}

	@Test
	public void testCharArrayPropertyEditor() {
		PrimitiveArrayBean bean = new PrimitiveArrayBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.setPropertyValue("charArray", "myvalue");
		assertEquals("myvalue", new String(bean.getCharArray()));
	}

	@Test
	public void testCharacterEditor() {
		CharBean cb = new CharBean();
		BeanWrapper bw = new BeanWrapperImpl(cb);

		bw.setPropertyValue("myChar", new Character('c'));
		assertEquals('c', cb.getMyChar());

		bw.setPropertyValue("myChar", "c");
		assertEquals('c', cb.getMyChar());

		bw.setPropertyValue("myChar", "\u0041");
		assertEquals('A', cb.getMyChar());

		bw.setPropertyValue("myChar", "\\u0022");
		assertEquals('"', cb.getMyChar());

		CharacterEditor editor = new CharacterEditor(false);
		editor.setAsText("M");
		assertEquals("M", editor.getAsText());
	}

	@Test
	public void testCharacterEditorWithAllowEmpty() {
		CharBean cb = new CharBean();
		BeanWrapper bw = new BeanWrapperImpl(cb);
		bw.registerCustomEditor(Character.class, new CharacterEditor(true));

		bw.setPropertyValue("myCharacter", new Character('c'));
		assertEquals(new Character('c'), cb.getMyCharacter());

		bw.setPropertyValue("myCharacter", "c");
		assertEquals(new Character('c'), cb.getMyCharacter());

		bw.setPropertyValue("myCharacter", "\u0041");
		assertEquals(new Character('A'), cb.getMyCharacter());

		bw.setPropertyValue("myCharacter", " ");
		assertEquals(new Character(' '), cb.getMyCharacter());

		bw.setPropertyValue("myCharacter", "");
		assertNull(cb.getMyCharacter());
	}

	@Test
	public void testCharacterEditorSetAsTextWithStringLongerThanOneCharacter() throws Exception {
		PropertyEditor charEditor = new CharacterEditor(false);
		assertThatIllegalArgumentException().isThrownBy(() ->
				charEditor.setAsText("ColdWaterCanyon"));
	}

	@Test
	public void testCharacterEditorGetAsTextReturnsEmptyStringIfValueIsNull() throws Exception {
		PropertyEditor charEditor = new CharacterEditor(false);
		assertEquals("", charEditor.getAsText());
		charEditor = new CharacterEditor(true);
		charEditor.setAsText(null);
		assertEquals("", charEditor.getAsText());
		charEditor.setAsText("");
		assertEquals("", charEditor.getAsText());
		charEditor.setAsText(" ");
		assertEquals(" ", charEditor.getAsText());
	}

	@Test
	public void testCharacterEditorSetAsTextWithNullNotAllowingEmptyAsNull() throws Exception {
		PropertyEditor charEditor = new CharacterEditor(false);
		assertThatIllegalArgumentException().isThrownBy(() ->
				charEditor.setAsText(null));
	}

	@Test
	public void testClassEditor() {
		PropertyEditor classEditor = new ClassEditor();
		classEditor.setAsText(TestBean.class.getName());
		assertEquals(TestBean.class, classEditor.getValue());
		assertEquals(TestBean.class.getName(), classEditor.getAsText());

		classEditor.setAsText(null);
		assertEquals("", classEditor.getAsText());
		classEditor.setAsText("");
		assertEquals("", classEditor.getAsText());
		classEditor.setAsText("\t  ");
		assertEquals("", classEditor.getAsText());
	}

	@Test
	public void testClassEditorWithNonExistentClass() throws Exception {
		PropertyEditor classEditor = new ClassEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				classEditor.setAsText("hairdresser.on.Fire"));
	}

	@Test
	public void testClassEditorWithArray() {
		PropertyEditor classEditor = new ClassEditor();
		classEditor.setAsText("org.springframework.tests.sample.beans.TestBean[]");
		assertEquals(TestBean[].class, classEditor.getValue());
		assertEquals("org.springframework.tests.sample.beans.TestBean[]", classEditor.getAsText());
	}

	/*
	* SPR_2165 - ClassEditor is inconsistent with multidimensional arrays
	*/
	@Test
	public void testGetAsTextWithTwoDimensionalArray() throws Exception {
		String[][] chessboard = new String[8][8];
		ClassEditor editor = new ClassEditor();
		editor.setValue(chessboard.getClass());
		assertEquals("java.lang.String[][]", editor.getAsText());
	}

	/*
	 * SPR_2165 - ClassEditor is inconsistent with multidimensional arrays
	 */
	@Test
	public void testGetAsTextWithRidiculousMultiDimensionalArray() throws Exception {
		String[][][][][] ridiculousChessboard = new String[8][4][0][1][3];
		ClassEditor editor = new ClassEditor();
		editor.setValue(ridiculousChessboard.getClass());
		assertEquals("java.lang.String[][][][][]", editor.getAsText());
	}

	@Test
	public void testFileEditor() {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("file:myfile.txt");
		assertEquals(new File("myfile.txt"), fileEditor.getValue());
		assertEquals((new File("myfile.txt")).getPath(), fileEditor.getAsText());
	}

	@Test
	public void testFileEditorWithRelativePath() {
		PropertyEditor fileEditor = new FileEditor();
		try {
			fileEditor.setAsText("myfile.txt");
		}
		catch (IllegalArgumentException ex) {
			// expected: should get resolved as class path resource,
			// and there is no such resource in the class path...
		}
	}

	@Test
	public void testFileEditorWithAbsolutePath() {
		PropertyEditor fileEditor = new FileEditor();
		// testing on Windows
		if (new File("C:/myfile.txt").isAbsolute()) {
			fileEditor.setAsText("C:/myfile.txt");
			assertEquals(new File("C:/myfile.txt"), fileEditor.getValue());
		}
		// testing on Unix
		if (new File("/myfile.txt").isAbsolute()) {
			fileEditor.setAsText("/myfile.txt");
			assertEquals(new File("/myfile.txt"), fileEditor.getValue());
		}
	}

	@Test
	public void testLocaleEditor() {
		PropertyEditor localeEditor = new LocaleEditor();
		localeEditor.setAsText("en_CA");
		assertEquals(Locale.CANADA, localeEditor.getValue());
		assertEquals("en_CA", localeEditor.getAsText());

		localeEditor = new LocaleEditor();
		assertEquals("", localeEditor.getAsText());
	}

	@Test
	public void testPatternEditor() {
		final String REGEX = "a.*";

		PropertyEditor patternEditor = new PatternEditor();
		patternEditor.setAsText(REGEX);
		assertEquals(Pattern.compile(REGEX).pattern(), ((Pattern) patternEditor.getValue()).pattern());
		assertEquals(REGEX, patternEditor.getAsText());

		patternEditor = new PatternEditor();
		assertEquals("", patternEditor.getAsText());

		patternEditor = new PatternEditor();
		patternEditor.setAsText(null);
		assertEquals("", patternEditor.getAsText());
	}

	@Test
	public void testCustomBooleanEditor() {
		CustomBooleanEditor editor = new CustomBooleanEditor(false);

		editor.setAsText("true");
		assertEquals(Boolean.TRUE, editor.getValue());
		assertEquals("true", editor.getAsText());

		editor.setAsText("false");
		assertEquals(Boolean.FALSE, editor.getValue());
		assertEquals("false", editor.getAsText());

		editor.setValue(null);
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());

		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(null));
	}

	@Test
	public void testCustomBooleanEditorWithEmptyAsNull() {
		CustomBooleanEditor editor = new CustomBooleanEditor(true);

		editor.setAsText("true");
		assertEquals(Boolean.TRUE, editor.getValue());
		assertEquals("true", editor.getAsText());

		editor.setAsText("false");
		assertEquals(Boolean.FALSE, editor.getValue());
		assertEquals("false", editor.getAsText());

		editor.setValue(null);
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testCustomDateEditor() {
		CustomDateEditor editor = new CustomDateEditor(null, false);
		editor.setValue(null);
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testCustomDateEditorWithEmptyAsNull() {
		CustomDateEditor editor = new CustomDateEditor(null, true);
		editor.setValue(null);
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testCustomDateEditorWithExactDateLength() {
		int maxLength = 10;
		String validDate = "01/01/2005";
		String invalidDate = "01/01/05";

		assertThat(validDate.length() == maxLength).isTrue();
		assertThat(invalidDate.length() == maxLength).isFalse();

		CustomDateEditor editor = new CustomDateEditor(new SimpleDateFormat("MM/dd/yyyy"), true, maxLength);
		editor.setAsText(validDate);
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(invalidDate))
			.withMessageContaining("10");
	}

	@Test
	public void testCustomNumberEditor() {
		CustomNumberEditor editor = new CustomNumberEditor(Integer.class, false);
		editor.setAsText("5");
		assertEquals(new Integer(5), editor.getValue());
		assertEquals("5", editor.getAsText());
		editor.setValue(null);
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testCustomNumberEditorWithHex() {
		CustomNumberEditor editor = new CustomNumberEditor(Integer.class, false);
		editor.setAsText("0x" + Integer.toHexString(64));
		assertEquals(new Integer(64), editor.getValue());
	}

	@Test
	public void testCustomNumberEditorWithEmptyAsNull() {
		CustomNumberEditor editor = new CustomNumberEditor(Integer.class, true);
		editor.setAsText("5");
		assertEquals(new Integer(5), editor.getValue());
		assertEquals("5", editor.getAsText());
		editor.setAsText("");
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
		editor.setValue(null);
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testStringTrimmerEditor() {
		StringTrimmerEditor editor = new StringTrimmerEditor(false);
		editor.setAsText("test");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText(" test ");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText("");
		assertEquals("", editor.getValue());
		assertEquals("", editor.getAsText());
		editor.setValue(null);
		assertEquals("", editor.getAsText());
		editor.setAsText(null);
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testStringTrimmerEditorWithEmptyAsNull() {
		StringTrimmerEditor editor = new StringTrimmerEditor(true);
		editor.setAsText("test");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText(" test ");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText("  ");
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
		editor.setValue(null);
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testStringTrimmerEditorWithCharsToDelete() {
		StringTrimmerEditor editor = new StringTrimmerEditor("\r\n\f", false);
		editor.setAsText("te\ns\ft");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText(" test ");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText("");
		assertEquals("", editor.getValue());
		assertEquals("", editor.getAsText());
		editor.setValue(null);
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testStringTrimmerEditorWithCharsToDeleteAndEmptyAsNull() {
		StringTrimmerEditor editor = new StringTrimmerEditor("\r\n\f", true);
		editor.setAsText("te\ns\ft");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText(" test ");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText(" \n\f ");
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
		editor.setValue(null);
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testIndexedPropertiesWithCustomEditorForType() {
		IndexedTestBean bean = new IndexedTestBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(String.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix" + text);
			}
		});
		TestBean tb0 = bean.getArray()[0];
		TestBean tb1 = bean.getArray()[1];
		TestBean tb2 = ((TestBean) bean.getList().get(0));
		TestBean tb3 = ((TestBean) bean.getList().get(1));
		TestBean tb4 = ((TestBean) bean.getMap().get("key1"));
		TestBean tb5 = ((TestBean) bean.getMap().get("key2"));
		assertEquals("name0", tb0.getName());
		assertEquals("name1", tb1.getName());
		assertEquals("name2", tb2.getName());
		assertEquals("name3", tb3.getName());
		assertEquals("name4", tb4.getName());
		assertEquals("name5", tb5.getName());
		assertEquals("name0", bw.getPropertyValue("array[0].name"));
		assertEquals("name1", bw.getPropertyValue("array[1].name"));
		assertEquals("name2", bw.getPropertyValue("list[0].name"));
		assertEquals("name3", bw.getPropertyValue("list[1].name"));
		assertEquals("name4", bw.getPropertyValue("map[key1].name"));
		assertEquals("name5", bw.getPropertyValue("map[key2].name"));
		assertEquals("name4", bw.getPropertyValue("map['key1'].name"));
		assertEquals("name5", bw.getPropertyValue("map[\"key2\"].name"));

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].name", "name5");
		pvs.add("array[1].name", "name4");
		pvs.add("list[0].name", "name3");
		pvs.add("list[1].name", "name2");
		pvs.add("map[key1].name", "name1");
		pvs.add("map['key2'].name", "name0");
		bw.setPropertyValues(pvs);
		assertEquals("prefixname5", tb0.getName());
		assertEquals("prefixname4", tb1.getName());
		assertEquals("prefixname3", tb2.getName());
		assertEquals("prefixname2", tb3.getName());
		assertEquals("prefixname1", tb4.getName());
		assertEquals("prefixname0", tb5.getName());
		assertEquals("prefixname5", bw.getPropertyValue("array[0].name"));
		assertEquals("prefixname4", bw.getPropertyValue("array[1].name"));
		assertEquals("prefixname3", bw.getPropertyValue("list[0].name"));
		assertEquals("prefixname2", bw.getPropertyValue("list[1].name"));
		assertEquals("prefixname1", bw.getPropertyValue("map[\"key1\"].name"));
		assertEquals("prefixname0", bw.getPropertyValue("map['key2'].name"));
	}

	@Test
	public void testIndexedPropertiesWithCustomEditorForProperty() {
		IndexedTestBean bean = new IndexedTestBean(false);
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(String.class, "array.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("array" + text);
			}
		});
		bw.registerCustomEditor(String.class, "list.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list" + text);
			}
		});
		bw.registerCustomEditor(String.class, "map.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("map" + text);
			}
		});
		bean.populate();

		TestBean tb0 = bean.getArray()[0];
		TestBean tb1 = bean.getArray()[1];
		TestBean tb2 = ((TestBean) bean.getList().get(0));
		TestBean tb3 = ((TestBean) bean.getList().get(1));
		TestBean tb4 = ((TestBean) bean.getMap().get("key1"));
		TestBean tb5 = ((TestBean) bean.getMap().get("key2"));
		assertEquals("name0", tb0.getName());
		assertEquals("name1", tb1.getName());
		assertEquals("name2", tb2.getName());
		assertEquals("name3", tb3.getName());
		assertEquals("name4", tb4.getName());
		assertEquals("name5", tb5.getName());
		assertEquals("name0", bw.getPropertyValue("array[0].name"));
		assertEquals("name1", bw.getPropertyValue("array[1].name"));
		assertEquals("name2", bw.getPropertyValue("list[0].name"));
		assertEquals("name3", bw.getPropertyValue("list[1].name"));
		assertEquals("name4", bw.getPropertyValue("map[key1].name"));
		assertEquals("name5", bw.getPropertyValue("map[key2].name"));
		assertEquals("name4", bw.getPropertyValue("map['key1'].name"));
		assertEquals("name5", bw.getPropertyValue("map[\"key2\"].name"));

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].name", "name5");
		pvs.add("array[1].name", "name4");
		pvs.add("list[0].name", "name3");
		pvs.add("list[1].name", "name2");
		pvs.add("map[key1].name", "name1");
		pvs.add("map['key2'].name", "name0");
		bw.setPropertyValues(pvs);
		assertEquals("arrayname5", tb0.getName());
		assertEquals("arrayname4", tb1.getName());
		assertEquals("listname3", tb2.getName());
		assertEquals("listname2", tb3.getName());
		assertEquals("mapname1", tb4.getName());
		assertEquals("mapname0", tb5.getName());
		assertEquals("arrayname5", bw.getPropertyValue("array[0].name"));
		assertEquals("arrayname4", bw.getPropertyValue("array[1].name"));
		assertEquals("listname3", bw.getPropertyValue("list[0].name"));
		assertEquals("listname2", bw.getPropertyValue("list[1].name"));
		assertEquals("mapname1", bw.getPropertyValue("map[\"key1\"].name"));
		assertEquals("mapname0", bw.getPropertyValue("map['key2'].name"));
	}

	@Test
	public void testIndexedPropertiesWithIndividualCustomEditorForProperty() {
		IndexedTestBean bean = new IndexedTestBean(false);
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(String.class, "array[0].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("array0" + text);
			}
		});
		bw.registerCustomEditor(String.class, "array[1].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("array1" + text);
			}
		});
		bw.registerCustomEditor(String.class, "list[0].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list0" + text);
			}
		});
		bw.registerCustomEditor(String.class, "list[1].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list1" + text);
			}
		});
		bw.registerCustomEditor(String.class, "map[key1].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("mapkey1" + text);
			}
		});
		bw.registerCustomEditor(String.class, "map[key2].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("mapkey2" + text);
			}
		});
		bean.populate();

		TestBean tb0 = bean.getArray()[0];
		TestBean tb1 = bean.getArray()[1];
		TestBean tb2 = ((TestBean) bean.getList().get(0));
		TestBean tb3 = ((TestBean) bean.getList().get(1));
		TestBean tb4 = ((TestBean) bean.getMap().get("key1"));
		TestBean tb5 = ((TestBean) bean.getMap().get("key2"));
		assertEquals("name0", tb0.getName());
		assertEquals("name1", tb1.getName());
		assertEquals("name2", tb2.getName());
		assertEquals("name3", tb3.getName());
		assertEquals("name4", tb4.getName());
		assertEquals("name5", tb5.getName());
		assertEquals("name0", bw.getPropertyValue("array[0].name"));
		assertEquals("name1", bw.getPropertyValue("array[1].name"));
		assertEquals("name2", bw.getPropertyValue("list[0].name"));
		assertEquals("name3", bw.getPropertyValue("list[1].name"));
		assertEquals("name4", bw.getPropertyValue("map[key1].name"));
		assertEquals("name5", bw.getPropertyValue("map[key2].name"));
		assertEquals("name4", bw.getPropertyValue("map['key1'].name"));
		assertEquals("name5", bw.getPropertyValue("map[\"key2\"].name"));

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].name", "name5");
		pvs.add("array[1].name", "name4");
		pvs.add("list[0].name", "name3");
		pvs.add("list[1].name", "name2");
		pvs.add("map[key1].name", "name1");
		pvs.add("map['key2'].name", "name0");
		bw.setPropertyValues(pvs);
		assertEquals("array0name5", tb0.getName());
		assertEquals("array1name4", tb1.getName());
		assertEquals("list0name3", tb2.getName());
		assertEquals("list1name2", tb3.getName());
		assertEquals("mapkey1name1", tb4.getName());
		assertEquals("mapkey2name0", tb5.getName());
		assertEquals("array0name5", bw.getPropertyValue("array[0].name"));
		assertEquals("array1name4", bw.getPropertyValue("array[1].name"));
		assertEquals("list0name3", bw.getPropertyValue("list[0].name"));
		assertEquals("list1name2", bw.getPropertyValue("list[1].name"));
		assertEquals("mapkey1name1", bw.getPropertyValue("map[\"key1\"].name"));
		assertEquals("mapkey2name0", bw.getPropertyValue("map['key2'].name"));
	}

	@Test
	public void testNestedIndexedPropertiesWithCustomEditorForProperty() {
		IndexedTestBean bean = new IndexedTestBean();
		TestBean tb0 = bean.getArray()[0];
		TestBean tb1 = bean.getArray()[1];
		TestBean tb2 = ((TestBean) bean.getList().get(0));
		TestBean tb3 = ((TestBean) bean.getList().get(1));
		TestBean tb4 = ((TestBean) bean.getMap().get("key1"));
		TestBean tb5 = ((TestBean) bean.getMap().get("key2"));
		tb0.setNestedIndexedBean(new IndexedTestBean());
		tb1.setNestedIndexedBean(new IndexedTestBean());
		tb2.setNestedIndexedBean(new IndexedTestBean());
		tb3.setNestedIndexedBean(new IndexedTestBean());
		tb4.setNestedIndexedBean(new IndexedTestBean());
		tb5.setNestedIndexedBean(new IndexedTestBean());
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(String.class, "array.nestedIndexedBean.array.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("array" + text);
			}

			@Override
			public String getAsText() {
				return ((String) getValue()).substring(5);
			}
		});
		bw.registerCustomEditor(String.class, "list.nestedIndexedBean.list.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list" + text);
			}

			@Override
			public String getAsText() {
				return ((String) getValue()).substring(4);
			}
		});
		bw.registerCustomEditor(String.class, "map.nestedIndexedBean.map.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("map" + text);
			}

			@Override
			public String getAsText() {
				return ((String) getValue()).substring(4);
			}
		});
		assertEquals("name0", tb0.getName());
		assertEquals("name1", tb1.getName());
		assertEquals("name2", tb2.getName());
		assertEquals("name3", tb3.getName());
		assertEquals("name4", tb4.getName());
		assertEquals("name5", tb5.getName());
		assertEquals("name0", bw.getPropertyValue("array[0].nestedIndexedBean.array[0].name"));
		assertEquals("name1", bw.getPropertyValue("array[1].nestedIndexedBean.array[1].name"));
		assertEquals("name2", bw.getPropertyValue("list[0].nestedIndexedBean.list[0].name"));
		assertEquals("name3", bw.getPropertyValue("list[1].nestedIndexedBean.list[1].name"));
		assertEquals("name4", bw.getPropertyValue("map[key1].nestedIndexedBean.map[key1].name"));
		assertEquals("name5", bw.getPropertyValue("map['key2'].nestedIndexedBean.map[\"key2\"].name"));

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].nestedIndexedBean.array[0].name", "name5");
		pvs.add("array[1].nestedIndexedBean.array[1].name", "name4");
		pvs.add("list[0].nestedIndexedBean.list[0].name", "name3");
		pvs.add("list[1].nestedIndexedBean.list[1].name", "name2");
		pvs.add("map[key1].nestedIndexedBean.map[\"key1\"].name", "name1");
		pvs.add("map['key2'].nestedIndexedBean.map[key2].name", "name0");
		bw.setPropertyValues(pvs);
		assertEquals("arrayname5", tb0.getNestedIndexedBean().getArray()[0].getName());
		assertEquals("arrayname4", tb1.getNestedIndexedBean().getArray()[1].getName());
		assertEquals("listname3", ((TestBean) tb2.getNestedIndexedBean().getList().get(0)).getName());
		assertEquals("listname2", ((TestBean) tb3.getNestedIndexedBean().getList().get(1)).getName());
		assertEquals("mapname1", ((TestBean) tb4.getNestedIndexedBean().getMap().get("key1")).getName());
		assertEquals("mapname0", ((TestBean) tb5.getNestedIndexedBean().getMap().get("key2")).getName());
		assertEquals("arrayname5", bw.getPropertyValue("array[0].nestedIndexedBean.array[0].name"));
		assertEquals("arrayname4", bw.getPropertyValue("array[1].nestedIndexedBean.array[1].name"));
		assertEquals("listname3", bw.getPropertyValue("list[0].nestedIndexedBean.list[0].name"));
		assertEquals("listname2", bw.getPropertyValue("list[1].nestedIndexedBean.list[1].name"));
		assertEquals("mapname1", bw.getPropertyValue("map['key1'].nestedIndexedBean.map[key1].name"));
		assertEquals("mapname0", bw.getPropertyValue("map[key2].nestedIndexedBean.map[\"key2\"].name"));
	}

	@Test
	public void testNestedIndexedPropertiesWithIndexedCustomEditorForProperty() {
		IndexedTestBean bean = new IndexedTestBean();
		TestBean tb0 = bean.getArray()[0];
		TestBean tb1 = bean.getArray()[1];
		TestBean tb2 = ((TestBean) bean.getList().get(0));
		TestBean tb3 = ((TestBean) bean.getList().get(1));
		TestBean tb4 = ((TestBean) bean.getMap().get("key1"));
		TestBean tb5 = ((TestBean) bean.getMap().get("key2"));
		tb0.setNestedIndexedBean(new IndexedTestBean());
		tb1.setNestedIndexedBean(new IndexedTestBean());
		tb2.setNestedIndexedBean(new IndexedTestBean());
		tb3.setNestedIndexedBean(new IndexedTestBean());
		tb4.setNestedIndexedBean(new IndexedTestBean());
		tb5.setNestedIndexedBean(new IndexedTestBean());
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(String.class, "array[0].nestedIndexedBean.array[0].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("array" + text);
			}
		});
		bw.registerCustomEditor(String.class, "list.nestedIndexedBean.list[1].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list" + text);
			}
		});
		bw.registerCustomEditor(String.class, "map[key1].nestedIndexedBean.map.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("map" + text);
			}
		});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].nestedIndexedBean.array[0].name", "name5");
		pvs.add("array[1].nestedIndexedBean.array[1].name", "name4");
		pvs.add("list[0].nestedIndexedBean.list[0].name", "name3");
		pvs.add("list[1].nestedIndexedBean.list[1].name", "name2");
		pvs.add("map[key1].nestedIndexedBean.map[\"key1\"].name", "name1");
		pvs.add("map['key2'].nestedIndexedBean.map[key2].name", "name0");
		bw.setPropertyValues(pvs);
		assertEquals("arrayname5", tb0.getNestedIndexedBean().getArray()[0].getName());
		assertEquals("name4", tb1.getNestedIndexedBean().getArray()[1].getName());
		assertEquals("name3", ((TestBean) tb2.getNestedIndexedBean().getList().get(0)).getName());
		assertEquals("listname2", ((TestBean) tb3.getNestedIndexedBean().getList().get(1)).getName());
		assertEquals("mapname1", ((TestBean) tb4.getNestedIndexedBean().getMap().get("key1")).getName());
		assertEquals("name0", ((TestBean) tb5.getNestedIndexedBean().getMap().get("key2")).getName());
	}

	@Test
	public void testIndexedPropertiesWithDirectAccessAndPropertyEditors() {
		IndexedTestBean bean = new IndexedTestBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(TestBean.class, "array", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("array" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "list", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("list" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "map", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("map" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0]", "a");
		pvs.add("array[1]", "b");
		pvs.add("list[0]", "c");
		pvs.add("list[1]", "d");
		pvs.add("map[key1]", "e");
		pvs.add("map['key2']", "f");
		bw.setPropertyValues(pvs);
		assertEquals("arraya", bean.getArray()[0].getName());
		assertEquals("arrayb", bean.getArray()[1].getName());
		assertEquals("listc", ((TestBean) bean.getList().get(0)).getName());
		assertEquals("listd", ((TestBean) bean.getList().get(1)).getName());
		assertEquals("mape", ((TestBean) bean.getMap().get("key1")).getName());
		assertEquals("mapf", ((TestBean) bean.getMap().get("key2")).getName());
	}

	@Test
	public void testIndexedPropertiesWithDirectAccessAndSpecificPropertyEditors() {
		IndexedTestBean bean = new IndexedTestBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(TestBean.class, "array[0]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("array0" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "array[1]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("array1" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "list[0]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("list0" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "list[1]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("list1" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "map[key1]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("mapkey1" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "map[key2]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("mapkey2" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0]", "a");
		pvs.add("array[1]", "b");
		pvs.add("list[0]", "c");
		pvs.add("list[1]", "d");
		pvs.add("map[key1]", "e");
		pvs.add("map['key2']", "f");
		bw.setPropertyValues(pvs);
		assertEquals("array0a", bean.getArray()[0].getName());
		assertEquals("array1b", bean.getArray()[1].getName());
		assertEquals("list0c", ((TestBean) bean.getList().get(0)).getName());
		assertEquals("list1d", ((TestBean) bean.getList().get(1)).getName());
		assertEquals("mapkey1e", ((TestBean) bean.getMap().get("key1")).getName());
		assertEquals("mapkey2f", ((TestBean) bean.getMap().get("key2")).getName());
	}

	@Test
	public void testIndexedPropertiesWithListPropertyEditor() {
		IndexedTestBean bean = new IndexedTestBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(List.class, "list", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				List<TestBean> result = new ArrayList<>();
				result.add(new TestBean("list" + text, 99));
				setValue(result);
			}
		});
		bw.setPropertyValue("list", "1");
		assertEquals("list1", ((TestBean) bean.getList().get(0)).getName());
		bw.setPropertyValue("list[0]", "test");
		assertEquals("test", bean.getList().get(0));
	}

	@Test
	public void testConversionToOldCollections() throws PropertyVetoException {
		OldCollectionsBean tb = new OldCollectionsBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(Vector.class, new CustomCollectionEditor(Vector.class));
		bw.registerCustomEditor(Hashtable.class, new CustomMapEditor(Hashtable.class));

		bw.setPropertyValue("vector", new String[] {"a", "b"});
		assertEquals(2, tb.getVector().size());
		assertEquals("a", tb.getVector().get(0));
		assertEquals("b", tb.getVector().get(1));

		bw.setPropertyValue("hashtable", Collections.singletonMap("foo", "bar"));
		assertEquals(1, tb.getHashtable().size());
		assertEquals("bar", tb.getHashtable().get("foo"));
	}

	@Test
	public void testUninitializedArrayPropertyWithCustomEditor() {
		IndexedTestBean bean = new IndexedTestBean(false);
		BeanWrapper bw = new BeanWrapperImpl(bean);
		PropertyEditor pe = new CustomNumberEditor(Integer.class, true);
		bw.registerCustomEditor(null, "list.age", pe);
		TestBean tb = new TestBean();
		bw.setPropertyValue("list", new ArrayList<>());
		bw.setPropertyValue("list[0]", tb);
		assertEquals(tb, bean.getList().get(0));
		assertEquals(pe, bw.findCustomEditor(int.class, "list.age"));
		assertEquals(pe, bw.findCustomEditor(null, "list.age"));
		assertEquals(pe, bw.findCustomEditor(int.class, "list[0].age"));
		assertEquals(pe, bw.findCustomEditor(null, "list[0].age"));
	}

	@Test
	public void testArrayToArrayConversion() throws PropertyVetoException {
		IndexedTestBean tb = new IndexedTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(TestBean.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean(text, 99));
			}
		});
		bw.setPropertyValue("array", new String[] {"a", "b"});
		assertEquals(2, tb.getArray().length);
		assertEquals("a", tb.getArray()[0].getName());
		assertEquals("b", tb.getArray()[1].getName());
	}

	@Test
	public void testArrayToStringConversion() throws PropertyVetoException {
		TestBean tb = new TestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(String.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("-" + text + "-");
			}
		});
		bw.setPropertyValue("name", new String[] {"a", "b"});
		assertEquals("-a,b-", tb.getName());
	}

	@Test
	public void testClassArrayEditorSunnyDay() throws Exception {
		ClassArrayEditor classArrayEditor = new ClassArrayEditor();
		classArrayEditor.setAsText("java.lang.String,java.util.HashMap");
		Class<?>[] classes = (Class<?>[]) classArrayEditor.getValue();
		assertEquals(2, classes.length);
		assertEquals(String.class, classes[0]);
		assertEquals(HashMap.class, classes[1]);
		assertEquals("java.lang.String,java.util.HashMap", classArrayEditor.getAsText());
		// ensure setAsText can consume the return value of getAsText
		classArrayEditor.setAsText(classArrayEditor.getAsText());
	}

	@Test
	public void testClassArrayEditorSunnyDayWithArrayTypes() throws Exception {
		ClassArrayEditor classArrayEditor = new ClassArrayEditor();
		classArrayEditor.setAsText("java.lang.String[],java.util.Map[],int[],float[][][]");
		Class<?>[] classes = (Class<?>[]) classArrayEditor.getValue();
		assertEquals(4, classes.length);
		assertEquals(String[].class, classes[0]);
		assertEquals(Map[].class, classes[1]);
		assertEquals(int[].class, classes[2]);
		assertEquals(float[][][].class, classes[3]);
		assertEquals("java.lang.String[],java.util.Map[],int[],float[][][]", classArrayEditor.getAsText());
		// ensure setAsText can consume the return value of getAsText
		classArrayEditor.setAsText(classArrayEditor.getAsText());
	}

	@Test
	public void testClassArrayEditorSetAsTextWithNull() throws Exception {
		ClassArrayEditor editor = new ClassArrayEditor();
		editor.setAsText(null);
		assertNull(editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testClassArrayEditorSetAsTextWithEmptyString() throws Exception {
		ClassArrayEditor editor = new ClassArrayEditor();
		editor.setAsText("");
		assertNull(editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testClassArrayEditorSetAsTextWithWhitespaceString() throws Exception {
		ClassArrayEditor editor = new ClassArrayEditor();
		editor.setAsText("\n");
		assertNull(editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testCharsetEditor() throws Exception {
		CharsetEditor editor = new CharsetEditor();
		String name = "UTF-8";
		editor.setAsText(name);
		Charset charset = Charset.forName(name);
		assertEquals("Invalid Charset conversion", charset, editor.getValue());
		editor.setValue(charset);
		assertEquals("Invalid Charset conversion", name, editor.getAsText());
	}


	private static class TestBeanEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) {
			TestBean tb = new TestBean();
			StringTokenizer st = new StringTokenizer(text, "_");
			tb.setName(st.nextToken());
			tb.setAge(Integer.parseInt(st.nextToken()));
			setValue(tb);
		}
	}


	private static class OldValueAccessingTestBeanEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) {
			TestBean tb = new TestBean();
			StringTokenizer st = new StringTokenizer(text, "_");
			tb.setName(st.nextToken());
			tb.setAge(Integer.parseInt(st.nextToken()));
			if (!tb.equals(getValue())) {
				setValue(tb);
			}
		}
	}


	@SuppressWarnings("unused")
	private static class PrimitiveArrayBean {

		private byte[] byteArray;

		private char[] charArray;

		public byte[] getByteArray() {
			return byteArray;
		}

		public void setByteArray(byte[] byteArray) {
			this.byteArray = byteArray;
		}

		public char[] getCharArray() {
			return charArray;
		}

		public void setCharArray(char[] charArray) {
			this.charArray = charArray;
		}
	}


	@SuppressWarnings("unused")
	private static class CharBean {

		private char myChar;

		private Character myCharacter;

		public char getMyChar() {
			return myChar;
		}

		public void setMyChar(char myChar) {
			this.myChar = myChar;
		}

		public Character getMyCharacter() {
			return myCharacter;
		}

		public void setMyCharacter(Character myCharacter) {
			this.myCharacter = myCharacter;
		}
	}


	@SuppressWarnings("unused")
	private static class OldCollectionsBean {

		private Vector<?> vector;

		private Hashtable<?, ?> hashtable;

		public Vector<?> getVector() {
			return vector;
		}

		public void setVector(Vector<?> vector) {
			this.vector = vector;
		}

		public Hashtable<?, ?> getHashtable() {
			return hashtable;
		}

		public void setHashtable(Hashtable<?, ?> hashtable) {
			this.hashtable = hashtable;
		}
	}

}
