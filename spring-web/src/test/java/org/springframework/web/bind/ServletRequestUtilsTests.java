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

package org.springframework.web.bind;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 06.08.2003
 */
public class ServletRequestUtilsTests {

	@Test
	public void testIntParameter() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param1", "5");
		request.addParameter("param2", "e");
		request.addParameter("paramEmpty", "");

		assertEquals(ServletRequestUtils.getIntParameter(request, "param1"), new Integer(5));
		assertEquals(ServletRequestUtils.getIntParameter(request, "param1", 6), 5);
		assertEquals(ServletRequestUtils.getRequiredIntParameter(request, "param1"), 5);

		assertEquals(ServletRequestUtils.getIntParameter(request, "param2", 6), 6);
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredIntParameter(request, "param2"));

		assertEquals(ServletRequestUtils.getIntParameter(request, "param3"), null);
		assertEquals(ServletRequestUtils.getIntParameter(request, "param3", 6), 6);
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredIntParameter(request, "param3"));

		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredIntParameter(request, "paramEmpty"));
	}

	@Test
	public void testIntParameters() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param", new String[] {"1", "2", "3"});

		request.addParameter("param2", "1");
		request.addParameter("param2", "2");
		request.addParameter("param2", "bogus");

		int[] array = new int[] {1, 2, 3};
		int[] values = ServletRequestUtils.getRequiredIntParameters(request, "param");
		assertEquals(3, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i]);
		}

		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredIntParameters(request, "param2"));
	}

	@Test
	public void testLongParameter() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param1", "5");
		request.addParameter("param2", "e");
		request.addParameter("paramEmpty", "");

		assertEquals(ServletRequestUtils.getLongParameter(request, "param1"), new Long(5L));
		assertEquals(ServletRequestUtils.getLongParameter(request, "param1", 6L), 5L);
		assertEquals(ServletRequestUtils.getRequiredIntParameter(request, "param1"), 5L);

		assertEquals(ServletRequestUtils.getLongParameter(request, "param2", 6L), 6L);
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredLongParameter(request, "param2"));

		assertEquals(ServletRequestUtils.getLongParameter(request, "param3"), null);
		assertEquals(ServletRequestUtils.getLongParameter(request, "param3", 6L), 6L);
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredLongParameter(request, "param3"));

		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredLongParameter(request, "paramEmpty"));
	}

	@Test
	public void testLongParameters() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("param", new String[] {"1", "2", "3"});

		request.setParameter("param2", "0");
		request.setParameter("param2", "1");
		request.addParameter("param2", "2");
		request.addParameter("param2", "bogus");

		long[] array = new long[] {1L, 2L, 3L};
		long[] values = ServletRequestUtils.getRequiredLongParameters(request, "param");
		assertEquals(3, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i]);
		}

		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredLongParameters(request, "param2"));

		request.setParameter("param2", new String[] {"1", "2"});
		values = ServletRequestUtils.getRequiredLongParameters(request, "param2");
		assertEquals(2, values.length);
		assertEquals(1, values[0]);
		assertEquals(2, values[1]);

		request.removeParameter("param2");
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredLongParameters(request, "param2"));
	}

	@Test
	public void testFloatParameter() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param1", "5.5");
		request.addParameter("param2", "e");
		request.addParameter("paramEmpty", "");

		assertThat(ServletRequestUtils.getFloatParameter(request, "param1").equals(new Float(5.5f))).isTrue();
		assertThat(ServletRequestUtils.getFloatParameter(request, "param1", 6.5f) == 5.5f).isTrue();
		assertThat(ServletRequestUtils.getRequiredFloatParameter(request, "param1") == 5.5f).isTrue();

		assertThat(ServletRequestUtils.getFloatParameter(request, "param2", 6.5f) == 6.5f).isTrue();
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredFloatParameter(request, "param2"));

		assertThat(ServletRequestUtils.getFloatParameter(request, "param3") == null).isTrue();
		assertThat(ServletRequestUtils.getFloatParameter(request, "param3", 6.5f) == 6.5f).isTrue();
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredFloatParameter(request, "param3"));

		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredFloatParameter(request, "paramEmpty"));
	}

	@Test
	public void testFloatParameters() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param", new String[] {"1.5", "2.5", "3"});

		request.addParameter("param2", "1.5");
		request.addParameter("param2", "2");
		request.addParameter("param2", "bogus");

		float[] array = new float[] {1.5F, 2.5F, 3};
		float[] values = ServletRequestUtils.getRequiredFloatParameters(request, "param");
		assertEquals(3, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i], 0);
		}

		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredFloatParameters(request, "param2"));
	}

	@Test
	public void testDoubleParameter() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param1", "5.5");
		request.addParameter("param2", "e");
		request.addParameter("paramEmpty", "");

		assertThat(ServletRequestUtils.getDoubleParameter(request, "param1").equals(new Double(5.5))).isTrue();
		assertThat(ServletRequestUtils.getDoubleParameter(request, "param1", 6.5) == 5.5).isTrue();
		assertThat(ServletRequestUtils.getRequiredDoubleParameter(request, "param1") == 5.5).isTrue();

		assertThat(ServletRequestUtils.getDoubleParameter(request, "param2", 6.5) == 6.5).isTrue();
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredDoubleParameter(request, "param2"));

		assertThat(ServletRequestUtils.getDoubleParameter(request, "param3") == null).isTrue();
		assertThat(ServletRequestUtils.getDoubleParameter(request, "param3", 6.5) == 6.5).isTrue();
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredDoubleParameter(request, "param3"));

		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredDoubleParameter(request, "paramEmpty"));
	}

	@Test
	public void testDoubleParameters() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param", new String[] {"1.5", "2.5", "3"});

		request.addParameter("param2", "1.5");
		request.addParameter("param2", "2");
		request.addParameter("param2", "bogus");

		double[] array = new double[] {1.5, 2.5, 3};
		double[] values = ServletRequestUtils.getRequiredDoubleParameters(request, "param");
		assertEquals(3, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i], 0);
		}

		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredDoubleParameters(request, "param2"));
	}

	@Test
	public void testBooleanParameter() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param1", "true");
		request.addParameter("param2", "e");
		request.addParameter("param4", "yes");
		request.addParameter("param5", "1");
		request.addParameter("paramEmpty", "");

		assertThat(ServletRequestUtils.getBooleanParameter(request, "param1").equals(Boolean.TRUE)).isTrue();
		assertThat(ServletRequestUtils.getBooleanParameter(request, "param1", false)).isTrue();
		assertThat(ServletRequestUtils.getRequiredBooleanParameter(request, "param1")).isTrue();

		assertThat(ServletRequestUtils.getBooleanParameter(request, "param2", true)).isFalse();
		assertThat(ServletRequestUtils.getRequiredBooleanParameter(request, "param2")).isFalse();

		assertThat(ServletRequestUtils.getBooleanParameter(request, "param3") == null).isTrue();
		assertThat(ServletRequestUtils.getBooleanParameter(request, "param3", true)).isTrue();
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredBooleanParameter(request, "param3"));

		assertThat(ServletRequestUtils.getBooleanParameter(request, "param4", false)).isTrue();
		assertThat(ServletRequestUtils.getRequiredBooleanParameter(request, "param4")).isTrue();

		assertThat(ServletRequestUtils.getBooleanParameter(request, "param5", false)).isTrue();
		assertThat(ServletRequestUtils.getRequiredBooleanParameter(request, "param5")).isTrue();
		assertThat(ServletRequestUtils.getRequiredBooleanParameter(request, "paramEmpty")).isFalse();
	}

	@Test
	public void testBooleanParameters() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param", new String[] {"true", "yes", "off", "1", "bogus"});

		request.addParameter("param2", "false");
		request.addParameter("param2", "true");
		request.addParameter("param2", "");

		boolean[] array = new boolean[] {true, true, false, true, false};
		boolean[] values = ServletRequestUtils.getRequiredBooleanParameters(request, "param");
		assertEquals(array.length, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i]);
		}

		array = new boolean[] {false, true, false};
		values = ServletRequestUtils.getRequiredBooleanParameters(request, "param2");
		assertEquals(array.length, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i]);
		}
	}

	@Test
	public void testStringParameter() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param1", "str");
		request.addParameter("paramEmpty", "");

		assertEquals("str", ServletRequestUtils.getStringParameter(request, "param1"));
		assertEquals("str", ServletRequestUtils.getStringParameter(request, "param1", "string"));
		assertEquals("str", ServletRequestUtils.getRequiredStringParameter(request, "param1"));

		assertEquals(null, ServletRequestUtils.getStringParameter(request, "param3"));
		assertEquals("string", ServletRequestUtils.getStringParameter(request, "param3", "string"));
		assertNull(ServletRequestUtils.getStringParameter(request, "param3", null));
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				ServletRequestUtils.getRequiredStringParameter(request, "param3"));

		assertEquals("", ServletRequestUtils.getStringParameter(request, "paramEmpty"));
		assertEquals("", ServletRequestUtils.getRequiredStringParameter(request, "paramEmpty"));
	}

	@Test
	public void testGetIntParameterWithDefaultValueHandlingIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		StopWatch sw = new StopWatch();
		sw.start();
		for (int i = 0; i < 1000000; i++) {
			ServletRequestUtils.getIntParameter(request, "nonExistingParam", 0);
		}
		sw.stop();
		System.out.println(sw.getTotalTimeMillis());
		assertThat(sw.getTotalTimeMillis() < 250).as("getStringParameter took too long: " + sw.getTotalTimeMillis()).isTrue();
	}

	@Test
	public void testGetLongParameterWithDefaultValueHandlingIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		StopWatch sw = new StopWatch();
		sw.start();
		for (int i = 0; i < 1000000; i++) {
			ServletRequestUtils.getLongParameter(request, "nonExistingParam", 0);
		}
		sw.stop();
		System.out.println(sw.getTotalTimeMillis());
		assertThat(sw.getTotalTimeMillis() < 250).as("getStringParameter took too long: " + sw.getTotalTimeMillis()).isTrue();
	}

	@Test
	public void testGetFloatParameterWithDefaultValueHandlingIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		StopWatch sw = new StopWatch();
		sw.start();
		for (int i = 0; i < 1000000; i++) {
			ServletRequestUtils.getFloatParameter(request, "nonExistingParam", 0f);
		}
		sw.stop();
		System.out.println(sw.getTotalTimeMillis());
		assertThat(sw.getTotalTimeMillis() < 250).as("getStringParameter took too long: " + sw.getTotalTimeMillis()).isTrue();
	}

	@Test
	public void testGetDoubleParameterWithDefaultValueHandlingIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		StopWatch sw = new StopWatch();
		sw.start();
		for (int i = 0; i < 1000000; i++) {
			ServletRequestUtils.getDoubleParameter(request, "nonExistingParam", 0d);
		}
		sw.stop();
		System.out.println(sw.getTotalTimeMillis());
		assertThat(sw.getTotalTimeMillis() < 250).as("getStringParameter took too long: " + sw.getTotalTimeMillis()).isTrue();
	}

	@Test
	public void testGetBooleanParameterWithDefaultValueHandlingIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		StopWatch sw = new StopWatch();
		sw.start();
		for (int i = 0; i < 1000000; i++) {
			ServletRequestUtils.getBooleanParameter(request, "nonExistingParam", false);
		}
		sw.stop();
		System.out.println(sw.getTotalTimeMillis());
		assertThat(sw.getTotalTimeMillis() < 250).as("getStringParameter took too long: " + sw.getTotalTimeMillis()).isTrue();
	}

	@Test
	public void testGetStringParameterWithDefaultValueHandlingIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		StopWatch sw = new StopWatch();
		sw.start();
		for (int i = 0; i < 1000000; i++) {
			ServletRequestUtils.getStringParameter(request, "nonExistingParam", "defaultValue");
		}
		sw.stop();
		System.out.println(sw.getTotalTimeMillis());
		assertThat(sw.getTotalTimeMillis() < 250).as("getStringParameter took too long: " + sw.getTotalTimeMillis()).isTrue();
	}

}
