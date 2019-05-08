/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.support;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import temp.ExpectedException;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.context.support.TestPropertySourceUtils.*;

/**
 * Unit tests for {@link TestPropertySourceUtils}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class TestPropertySourceUtilsTests {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final String[] KEY_VALUE_PAIR = new String[] {"key = value"};

	private static final String[] FOO_LOCATIONS = new String[] {"classpath:/foo.properties"};



	@Test
	public void emptyAnnotation() {
		assertThatIllegalStateException().isThrownBy(() ->
				buildMergedTestPropertySources(EmptyPropertySources.class))
			.withMessageStartingWith("Could not detect default properties file for test")
			.withMessageContaining("EmptyPropertySources.properties");
	}

	@Test
	public void extendedEmptyAnnotation() {
		assertThatIllegalStateException().isThrownBy(() ->
				buildMergedTestPropertySources(ExtendedEmptyPropertySources.class))
			.withMessageStartingWith("Could not detect default properties file for test")
			.withMessageContaining("ExtendedEmptyPropertySources.properties");
	}

	@Test
	public void value() {
		assertMergedTestPropertySources(ValuePropertySources.class, asArray("classpath:/value.xml"),
				EMPTY_STRING_ARRAY);
	}

	@Test
	public void locationsAndValueAttributes() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
		buildMergedTestPropertySources(LocationsAndValuePropertySources.class));
	}

	@Test
	public void locationsAndProperties() {
		assertMergedTestPropertySources(LocationsAndPropertiesPropertySources.class,
				asArray("classpath:/foo1.xml", "classpath:/foo2.xml"), asArray("k1a=v1a", "k1b: v1b"));
	}

	@Test
	public void inheritedLocationsAndProperties() {
		assertMergedTestPropertySources(InheritedPropertySources.class,
				asArray("classpath:/foo1.xml", "classpath:/foo2.xml"), asArray("k1a=v1a", "k1b: v1b"));
	}

	@Test
	public void extendedLocationsAndProperties() {
		assertMergedTestPropertySources(ExtendedPropertySources.class,
				asArray("classpath:/foo1.xml", "classpath:/foo2.xml", "classpath:/bar1.xml", "classpath:/bar2.xml"),
				asArray("k1a=v1a", "k1b: v1b", "k2a v2a", "k2b: v2b"));
	}

	@Test
	public void overriddenLocations() {
		assertMergedTestPropertySources(OverriddenLocationsPropertySources.class,
				asArray("classpath:/baz.properties"), asArray("k1a=v1a", "k1b: v1b", "key = value"));
	}

	@Test
	public void overriddenProperties() {
		assertMergedTestPropertySources(OverriddenPropertiesPropertySources.class,
				asArray("classpath:/foo1.xml", "classpath:/foo2.xml", "classpath:/baz.properties"), KEY_VALUE_PAIR);
	}

	@Test
	public void overriddenLocationsAndProperties() {
		assertMergedTestPropertySources(OverriddenLocationsAndPropertiesPropertySources.class,
				asArray("classpath:/baz.properties"), KEY_VALUE_PAIR);
	}


	@Test
	public void addPropertiesFilesToEnvironmentWithNullContext() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
		addPropertiesFilesToEnvironment((ConfigurableApplicationContext) null, FOO_LOCATIONS)).withMessageContaining("must not be null");
	}

	@Test
	public void addPropertiesFilesToEnvironmentWithContextAndNullLocations() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
		addPropertiesFilesToEnvironment(mock(ConfigurableApplicationContext.class), (String[]) null)).withMessageContaining("must not be null");
	}

	@Test
	public void addPropertiesFilesToEnvironmentWithNullEnvironment() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
		addPropertiesFilesToEnvironment((ConfigurableEnvironment) null, mock(ResourceLoader.class), FOO_LOCATIONS)).withMessageContaining("must not be null");
	}

	@Test
	public void addPropertiesFilesToEnvironmentWithEnvironmentAndNullLocations() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
		addPropertiesFilesToEnvironment(new MockEnvironment(), mock(ResourceLoader.class), (String[]) null)).withMessageContaining("must not be null");
	}

	@Test
	public void addPropertiesFilesToEnvironmentWithSinglePropertyFromVirtualFile() {
		ConfigurableEnvironment environment = new MockEnvironment();

		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.remove(MockPropertySource.MOCK_PROPERTIES_PROPERTY_SOURCE_NAME);
		assertEquals(0, propertySources.size());

		String pair = "key = value";
		ByteArrayResource resource = new ByteArrayResource(pair.getBytes(), "from inlined property: " + pair);
		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		when(resourceLoader.getResource(anyString())).thenReturn(resource);

		addPropertiesFilesToEnvironment(environment, resourceLoader, FOO_LOCATIONS);
		assertEquals(1, propertySources.size());
		assertEquals("value", environment.getProperty("key"));
	}

	@Test
	public void addInlinedPropertiesToEnvironmentWithNullContext() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
		addInlinedPropertiesToEnvironment((ConfigurableApplicationContext) null, KEY_VALUE_PAIR)).withMessageContaining("context");
	}

	@Test
	public void addInlinedPropertiesToEnvironmentWithContextAndNullInlinedProperties() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
		addInlinedPropertiesToEnvironment(mock(ConfigurableApplicationContext.class), (String[]) null)).withMessageContaining("inlined");
	}

	@Test
	public void addInlinedPropertiesToEnvironmentWithNullEnvironment() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
		addInlinedPropertiesToEnvironment((ConfigurableEnvironment) null, KEY_VALUE_PAIR)).withMessageContaining("environment");
	}

	@Test
	public void addInlinedPropertiesToEnvironmentWithEnvironmentAndNullInlinedProperties() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
		addInlinedPropertiesToEnvironment(new MockEnvironment(), (String[]) null)).withMessageContaining("inlined");
	}

	@Test
	public void addInlinedPropertiesToEnvironmentWithMalformedUnicodeInValue() {
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() ->
		addInlinedPropertiesToEnvironment(new MockEnvironment(), asArray("key = \\uZZZZ"))).withMessageContaining("Failed to load test environment property");
	}

	@Test
	public void addInlinedPropertiesToEnvironmentWithMultipleKeyValuePairsInSingleInlinedProperty() {
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() ->
		addInlinedPropertiesToEnvironment(new MockEnvironment(), asArray("a=b\nx=y"))).withMessageContaining("Failed to load exactly one test environment property");
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void addInlinedPropertiesToEnvironmentWithEmptyProperty() {
		ConfigurableEnvironment environment = new MockEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.remove(MockPropertySource.MOCK_PROPERTIES_PROPERTY_SOURCE_NAME);
		assertEquals(0, propertySources.size());
		addInlinedPropertiesToEnvironment(environment, asArray("  "));
		assertEquals(1, propertySources.size());
		assertEquals(0, ((Map) propertySources.iterator().next().getSource()).size());
	}

	@Test
	public void convertInlinedPropertiesToMapWithNullInlinedProperties() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
		convertInlinedPropertiesToMap((String[]) null)).withMessageContaining("inlined");
	}


	private static void assertMergedTestPropertySources(Class<?> testClass, String[] expectedLocations,
			String[] expectedProperties) {

		MergedTestPropertySources mergedPropertySources = buildMergedTestPropertySources(testClass);
		assertNotNull(mergedPropertySources);
		assertArrayEquals(expectedLocations, mergedPropertySources.getLocations());
		assertArrayEquals(expectedProperties, mergedPropertySources.getProperties());
	}


	@SafeVarargs
	private static <T> T[] asArray(T... arr) {
		return arr;
	}


	@TestPropertySource
	static class EmptyPropertySources {
	}

	@TestPropertySource
	static class ExtendedEmptyPropertySources extends EmptyPropertySources {
	}

	@TestPropertySource(locations = "/foo", value = "/bar")
	static class LocationsAndValuePropertySources {
	}

	@TestPropertySource("/value.xml")
	static class ValuePropertySources {
	}

	@TestPropertySource(locations = { "/foo1.xml", "/foo2.xml" }, properties = { "k1a=v1a", "k1b: v1b" })
	static class LocationsAndPropertiesPropertySources {
	}

	static class InheritedPropertySources extends LocationsAndPropertiesPropertySources {
	}

	@TestPropertySource(locations = { "/bar1.xml", "/bar2.xml" }, properties = { "k2a v2a", "k2b: v2b" })
	static class ExtendedPropertySources extends LocationsAndPropertiesPropertySources {
	}

	@TestPropertySource(locations = "/baz.properties", properties = "key = value", inheritLocations = false)
	static class OverriddenLocationsPropertySources extends LocationsAndPropertiesPropertySources {
	}

	@TestPropertySource(locations = "/baz.properties", properties = "key = value", inheritProperties = false)
	static class OverriddenPropertiesPropertySources extends LocationsAndPropertiesPropertySources {
	}

	@TestPropertySource(locations = "/baz.properties", properties = "key = value", inheritLocations = false, inheritProperties = false)
	static class OverriddenLocationsAndPropertiesPropertySources extends LocationsAndPropertiesPropertySources {
	}

}
