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

package org.springframework.core.env;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.mock.env.MockPropertySource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Chris Beams
 * @since 3.1
 */
public class PropertySourcesPropertyResolverTests {

	private Properties testProperties;

	private MutablePropertySources propertySources;

	private ConfigurablePropertyResolver propertyResolver;


	@Before
	public void setUp() {
		this.propertySources = new MutablePropertySources();
		this.propertyResolver = new PropertySourcesPropertyResolver(this.propertySources);
		this.testProperties = new Properties();
		this.propertySources.addFirst(new PropertiesPropertySource("testProperties", this.testProperties));
	}


	@Test
	public void containsProperty() {
		assertThat(this.propertyResolver.containsProperty("foo"), is(false));
		this.testProperties.put("foo", "bar");
		assertThat(this.propertyResolver.containsProperty("foo"), is(true));
	}

	@Test
	public void getProperty() {
		assertThat(this.propertyResolver.getProperty("foo"), nullValue());
		this.testProperties.put("foo", "bar");
		assertThat(this.propertyResolver.getProperty("foo"), is("bar"));
	}

	@Test
	public void getProperty_withDefaultValue() {
		assertThat(this.propertyResolver.getProperty("foo", "myDefault"), is("myDefault"));
		this.testProperties.put("foo", "bar");
		assertThat(this.propertyResolver.getProperty("foo"), is("bar"));
	}

	@Test
	public void getProperty_propertySourceSearchOrderIsFIFO() {
		MutablePropertySources sources = new MutablePropertySources();
		PropertyResolver resolver = new PropertySourcesPropertyResolver(sources);
		sources.addFirst(new MockPropertySource("ps1").withProperty("pName", "ps1Value"));
		assertThat(resolver.getProperty("pName"), equalTo("ps1Value"));
		sources.addFirst(new MockPropertySource("ps2").withProperty("pName", "ps2Value"));
		assertThat(resolver.getProperty("pName"), equalTo("ps2Value"));
		sources.addFirst(new MockPropertySource("ps3").withProperty("pName", "ps3Value"));
		assertThat(resolver.getProperty("pName"), equalTo("ps3Value"));
	}

	@Test
	public void getProperty_withExplicitNullValue() {
		// java.util.Properties does not allow null values (because Hashtable does not)
		Map<String, Object> nullableProperties = new HashMap<>();
		this.propertySources.addLast(new MapPropertySource("nullableProperties", nullableProperties));
		nullableProperties.put("foo", null);
		assertThat(this.propertyResolver.getProperty("foo"), nullValue());
	}

	@Test
	public void getProperty_withTargetType_andDefaultValue() {
		assertThat(this.propertyResolver.getProperty("foo", Integer.class, 42), equalTo(42));
		this.testProperties.put("foo", 13);
		assertThat(this.propertyResolver.getProperty("foo", Integer.class, 42), equalTo(13));
	}

	@Test
	public void getProperty_withStringArrayConversion() {
		this.testProperties.put("foo", "bar,baz");
		assertThat(this.propertyResolver.getProperty("foo", String[].class), equalTo(new String[] { "bar", "baz" }));
	}

	@Test
	public void getProperty_withNonConvertibleTargetType() {
		this.testProperties.put("foo", "bar");

		class TestType { }

		try {
			this.propertyResolver.getProperty("foo", TestType.class);
			fail("Expected ConverterNotFoundException due to non-convertible types");
		}
		catch (ConverterNotFoundException ex) {
			// expected
		}
	}

	@Test
	public void getProperty_doesNotCache_replaceExistingKeyPostConstruction() {
		String key = "foo";
		String value1 = "bar";
		String value2 = "biz";

		HashMap<String, Object> map = new HashMap<>();
		map.put(key, value1); // before construction
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MapPropertySource("testProperties", map));
		PropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(propertyResolver.getProperty(key), equalTo(value1));
		map.put(key, value2); // after construction and first resolution
		assertThat(propertyResolver.getProperty(key), equalTo(value2));
	}

	@Test
	public void getProperty_doesNotCache_addNewKeyPostConstruction() {
		HashMap<String, Object> map = new HashMap<>();
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MapPropertySource("testProperties", map));
		PropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(propertyResolver.getProperty("foo"), equalTo(null));
		map.put("foo", "42");
		assertThat(propertyResolver.getProperty("foo"), equalTo("42"));
	}

	@Test
	public void getPropertySources_replacePropertySource() {
		this.propertySources = new MutablePropertySources();
		this.propertyResolver = new PropertySourcesPropertyResolver(this.propertySources);
		this.propertySources.addLast(new MockPropertySource("local").withProperty("foo", "localValue"));
		this.propertySources.addLast(new MockPropertySource("system").withProperty("foo", "systemValue"));

		// 'local' was added first so has precedence
		assertThat(this.propertyResolver.getProperty("foo"), equalTo("localValue"));

		// replace 'local' with new property source
		this.propertySources.replace("local", new MockPropertySource("new").withProperty("foo", "newValue"));

		// 'system' now has precedence
		assertThat(this.propertyResolver.getProperty("foo"), equalTo("newValue"));

		assertThat(this.propertySources.size(), is(2));
	}

	@Test
	public void getRequiredProperty() {
		this.testProperties.put("exists", "xyz");
		assertThat(this.propertyResolver.getRequiredProperty("exists"), is("xyz"));

		try {
			this.propertyResolver.getRequiredProperty("bogus");
			fail("expected IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void getRequiredProperty_withStringArrayConversion() {
		this.testProperties.put("exists", "abc,123");
		assertThat(this.propertyResolver.getRequiredProperty("exists", String[].class), equalTo(new String[] { "abc", "123" }));

		try {
			this.propertyResolver.getRequiredProperty("bogus", String[].class);
			fail("expected IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void resolvePlaceholders() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolvePlaceholders("Replace this ${key}"), equalTo("Replace this value"));
	}

	@Test
	public void resolvePlaceholders_withUnresolvable() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolvePlaceholders("Replace this ${key} plus ${unknown}"),
				equalTo("Replace this value plus ${unknown}"));
	}

	@Test
	public void resolvePlaceholders_withDefaultValue() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolvePlaceholders("Replace this ${key} plus ${unknown:defaultValue}"),
				equalTo("Replace this value plus defaultValue"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void resolvePlaceholders_withNullInput() {
		new PropertySourcesPropertyResolver(new MutablePropertySources()).resolvePlaceholders(null);
	}

	@Test
	public void resolveRequiredPlaceholders() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolveRequiredPlaceholders("Replace this ${key}"), equalTo("Replace this value"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void resolveRequiredPlaceholders_withUnresolvable() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		resolver.resolveRequiredPlaceholders("Replace this ${key} plus ${unknown}");
	}

	@Test
	public void resolveRequiredPlaceholders_withDefaultValue() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolveRequiredPlaceholders("Replace this ${key} plus ${unknown:defaultValue}"),
				equalTo("Replace this value plus defaultValue"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void resolveRequiredPlaceholders_withNullInput() {
		new PropertySourcesPropertyResolver(new MutablePropertySources()).resolveRequiredPlaceholders(null);
	}

	@Test
	public void setRequiredProperties_andValidateRequiredProperties() {
		// no properties have been marked as required -> validation should pass
		this.propertyResolver.validateRequiredProperties();

		// mark which properties are required
		this.propertyResolver.setRequiredProperties("foo", "bar");

		// neither foo nor bar properties are present -> validating should throw
		try {
			this.propertyResolver.validateRequiredProperties();
			fail("expected validation exception");
		}
		catch (MissingRequiredPropertiesException ex) {
			assertThat(ex.getMessage(), equalTo(
					"The following properties were declared as required " +
					"but could not be resolved: [foo, bar]"));
		}

		// add foo property -> validation should fail only on missing 'bar' property
		this.testProperties.put("foo", "fooValue");
		try {
			this.propertyResolver.validateRequiredProperties();
			fail("expected validation exception");
		}
		catch (MissingRequiredPropertiesException ex) {
			assertThat(ex.getMessage(), equalTo(
					"The following properties were declared as required " +
					"but could not be resolved: [bar]"));
		}

		// add bar property -> validation should pass, even with an empty string value
		this.testProperties.put("bar", "");
		this.propertyResolver.validateRequiredProperties();
	}

	@Test
	public void resolveNestedPropertyPlaceholders() {
		MutablePropertySources ps = new MutablePropertySources();
		ps.addFirst(new MockPropertySource()
			.withProperty("p1", "v1")
			.withProperty("p2", "v2")
			.withProperty("p3", "${p1}:${p2}")              // nested placeholders
			.withProperty("p4", "${p3}")                    // deeply nested placeholders
			.withProperty("p5", "${p1}:${p2}:${bogus}")     // unresolvable placeholder
			.withProperty("p6", "${p1}:${p2}:${bogus:def}") // unresolvable w/ default
			.withProperty("pL", "${pR}")                    // cyclic reference left
			.withProperty("pR", "${pL}")                    // cyclic reference right
		);
		ConfigurablePropertyResolver pr = new PropertySourcesPropertyResolver(ps);
		assertThat(pr.getProperty("p1"), equalTo("v1"));
		assertThat(pr.getProperty("p2"), equalTo("v2"));
		assertThat(pr.getProperty("p3"), equalTo("v1:v2"));
		assertThat(pr.getProperty("p4"), equalTo("v1:v2"));
		try {
			pr.getProperty("p5");
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), containsString(
					"Could not resolve placeholder 'bogus' in value \"${p1}:${p2}:${bogus}\""));
		}
		assertThat(pr.getProperty("p6"), equalTo("v1:v2:def"));
		try {
			pr.getProperty("pL");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().toLowerCase().contains("circular"));
		}
	}

	@Test
	public void ignoreUnresolvableNestedPlaceholdersIsConfigurable() {
		MutablePropertySources ps = new MutablePropertySources();
		ps.addFirst(new MockPropertySource()
			.withProperty("p1", "v1")
			.withProperty("p2", "v2")
			.withProperty("p3", "${p1}:${p2}:${bogus:def}") // unresolvable w/ default
			.withProperty("p4", "${p1}:${p2}:${bogus}")     // unresolvable placeholder
		);
		ConfigurablePropertyResolver pr = new PropertySourcesPropertyResolver(ps);
		assertThat(pr.getProperty("p1"), equalTo("v1"));
		assertThat(pr.getProperty("p2"), equalTo("v2"));
		assertThat(pr.getProperty("p3"), equalTo("v1:v2:def"));

		// placeholders nested within the value of "p4" are unresolvable and cause an
		// exception by default
		try {
			pr.getProperty("p4");
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), containsString(
					"Could not resolve placeholder 'bogus' in value \"${p1}:${p2}:${bogus}\""));
		}

		// relax the treatment of unresolvable nested placeholders
		pr.setIgnoreUnresolvableNestedPlaceholders(true);
		// and observe they now pass through unresolved
		assertThat(pr.getProperty("p4"), equalTo("v1:v2:${bogus}"));

		// resolve[Nested]Placeholders methods behave as usual regardless the value of
		// ignoreUnresolvableNestedPlaceholders
		assertThat(pr.resolvePlaceholders("${p1}:${p2}:${bogus}"), equalTo("v1:v2:${bogus}"));
		try {
			pr.resolveRequiredPlaceholders("${p1}:${p2}:${bogus}");
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), containsString(
					"Could not resolve placeholder 'bogus' in value \"${p1}:${p2}:${bogus}\""));
		}
	}

}
