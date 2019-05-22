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

package org.springframework.beans;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.tests.sample.beans.CustomEnum;
import org.springframework.tests.sample.beans.GenericBean;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class BeanWrapperEnumTests {

	@Test
	public void testCustomEnum() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", "VALUE_1");
		assertThat(gb.getCustomEnum()).isEqualTo(CustomEnum.VALUE_1);
	}

	@Test
	public void testCustomEnumWithNull() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", null);
		assertThat(gb.getCustomEnum()).isEqualTo(null);
	}

	@Test
	public void testCustomEnumWithEmptyString() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", "");
		assertThat(gb.getCustomEnum()).isEqualTo(null);
	}

	@Test
	public void testCustomEnumArrayWithSingleValue() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumArray", "VALUE_1");
		assertEquals(1, gb.getCustomEnumArray().length);
		assertThat(gb.getCustomEnumArray()[0]).isEqualTo(CustomEnum.VALUE_1);
	}

	@Test
	public void testCustomEnumArrayWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumArray", new String[] {"VALUE_1", "VALUE_2"});
		assertEquals(2, gb.getCustomEnumArray().length);
		assertThat(gb.getCustomEnumArray()[0]).isEqualTo(CustomEnum.VALUE_1);
		assertThat(gb.getCustomEnumArray()[1]).isEqualTo(CustomEnum.VALUE_2);
	}

	@Test
	public void testCustomEnumArrayWithMultipleValuesAsCsv() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumArray", "VALUE_1,VALUE_2");
		assertEquals(2, gb.getCustomEnumArray().length);
		assertThat(gb.getCustomEnumArray()[0]).isEqualTo(CustomEnum.VALUE_1);
		assertThat(gb.getCustomEnumArray()[1]).isEqualTo(CustomEnum.VALUE_2);
	}

	@Test
	public void testCustomEnumSetWithSingleValue() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSet", "VALUE_1");
		assertEquals(1, gb.getCustomEnumSet().size());
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
	}

	@Test
	public void testCustomEnumSetWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSet", new String[] {"VALUE_1", "VALUE_2"});
		assertEquals(2, gb.getCustomEnumSet().size());
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_2)).isTrue();
	}

	@Test
	public void testCustomEnumSetWithMultipleValuesAsCsv() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSet", "VALUE_1,VALUE_2");
		assertEquals(2, gb.getCustomEnumSet().size());
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_2)).isTrue();
	}

	@Test
	public void testCustomEnumSetWithGetterSetterMismatch() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSetMismatch", new String[] {"VALUE_1", "VALUE_2"});
		assertEquals(2, gb.getCustomEnumSet().size());
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_2)).isTrue();
	}

	@Test
	public void testStandardEnumSetWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setConversionService(new DefaultConversionService());
		assertNull(gb.getStandardEnumSet());
		bw.setPropertyValue("standardEnumSet", new String[] {"VALUE_1", "VALUE_2"});
		assertEquals(2, gb.getStandardEnumSet().size());
		assertThat(gb.getStandardEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
		assertThat(gb.getStandardEnumSet().contains(CustomEnum.VALUE_2)).isTrue();
	}

	@Test
	public void testStandardEnumSetWithAutoGrowing() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setAutoGrowNestedPaths(true);
		assertNull(gb.getStandardEnumSet());
		bw.getPropertyValue("standardEnumSet.class");
		assertEquals(0, gb.getStandardEnumSet().size());
	}

	@Test
	public void testStandardEnumMapWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setConversionService(new DefaultConversionService());
		assertNull(gb.getStandardEnumMap());
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("VALUE_1", 1);
		map.put("VALUE_2", 2);
		bw.setPropertyValue("standardEnumMap", map);
		assertEquals(2, gb.getStandardEnumMap().size());
		assertThat(gb.getStandardEnumMap().get(CustomEnum.VALUE_1)).isEqualTo(new Integer(1));
		assertThat(gb.getStandardEnumMap().get(CustomEnum.VALUE_2)).isEqualTo(new Integer(2));
	}

	@Test
	public void testStandardEnumMapWithAutoGrowing() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setAutoGrowNestedPaths(true);
		assertNull(gb.getStandardEnumMap());
		bw.setPropertyValue("standardEnumMap[VALUE_1]", 1);
		assertEquals(1, gb.getStandardEnumMap().size());
		assertThat(gb.getStandardEnumMap().get(CustomEnum.VALUE_1)).isEqualTo(new Integer(1));
	}

	@Test
	public void testNonPublicEnum() {
		NonPublicEnumHolder holder = new NonPublicEnumHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("nonPublicEnum", "VALUE_1");
		assertThat(holder.getNonPublicEnum()).isEqualTo(NonPublicEnum.VALUE_1);
	}


	enum NonPublicEnum {

		VALUE_1, VALUE_2;
	}


	static class NonPublicEnumHolder {

		private NonPublicEnum nonPublicEnum;

		public NonPublicEnum getNonPublicEnum() {
			return nonPublicEnum;
		}

		public void setNonPublicEnum(NonPublicEnum nonPublicEnum) {
			this.nonPublicEnum = nonPublicEnum;
		}
	}

}
