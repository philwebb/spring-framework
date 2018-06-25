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
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.annotation.AnnotationUtilsTests.ImplicitAliasesContextConfig;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AnnotationAttributes}.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 3.1.1
 */
public class AnnotationAttributesTests {

	private AnnotationAttributes attributes = new AnnotationAttributes();

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void typeSafeAttributeAccess() {
		AnnotationAttributes nestedAttributes = new AnnotationAttributes();
		nestedAttributes.put("value", 10);
		nestedAttributes.put("name", "algernon");

		this.attributes.put("name", "dave");
		this.attributes.put("names", new String[] {"dave", "frank", "hal"});
		this.attributes.put("bool1", true);
		this.attributes.put("bool2", false);
		this.attributes.put("color", Color.RED);
		this.attributes.put("class", Integer.class);
		this.attributes.put("classes", new Class<?>[] {Number.class, Short.class, Integer.class});
		this.attributes.put("number", 42);
		this.attributes.put("anno", nestedAttributes);
		this.attributes.put("annoArray", new AnnotationAttributes[] {nestedAttributes});

		assertThat(this.attributes.getString("name"), equalTo("dave"));
		assertThat(this.attributes.getStringArray("names"), equalTo(new String[] {"dave", "frank", "hal"}));
		assertThat(this.attributes.getBoolean("bool1"), equalTo(true));
		assertThat(this.attributes.getBoolean("bool2"), equalTo(false));
		assertThat(this.attributes.<Color>getEnum("color"), equalTo(Color.RED));
		assertTrue(this.attributes.getClass("class").equals(Integer.class));
		assertThat(this.attributes.getClassArray("classes"), equalTo(new Class<?>[] {Number.class, Short.class, Integer.class}));
		assertThat(this.attributes.<Integer>getNumber("number"), equalTo(42));
		assertThat(this.attributes.getAnnotation("anno").<Integer>getNumber("value"), equalTo(10));
		assertThat(this.attributes.getAnnotationArray("annoArray")[0].getString("name"), equalTo("algernon"));

	}

	@Test
	public void unresolvableClass() throws Exception {
		this.attributes.put("unresolvableClass", new ClassNotFoundException("myclass"));
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage(containsString("myclass"));
		this.attributes.getClass("unresolvableClass");
	}

	@Test
	public void singleElementToSingleElementArrayConversionSupport() throws Exception {
		Filter filter = FilteredClass.class.getAnnotation(Filter.class);

		AnnotationAttributes nestedAttributes = new AnnotationAttributes();
		nestedAttributes.put("name", "Dilbert");

		// Store single elements
		this.attributes.put("names", "Dogbert");
		this.attributes.put("classes", Number.class);
		this.attributes.put("nestedAttributes", nestedAttributes);
		this.attributes.put("filters", filter);

		// Get back arrays of single elements
		assertThat(this.attributes.getStringArray("names"), equalTo(new String[] {"Dogbert"}));
		assertThat(this.attributes.getClassArray("classes"), equalTo(new Class<?>[] {Number.class}));

		AnnotationAttributes[] array = this.attributes.getAnnotationArray("nestedAttributes");
		assertNotNull(array);
		assertThat(array.length, is(1));
		assertThat(array[0].getString("name"), equalTo("Dilbert"));

		Filter[] filters = this.attributes.getAnnotationArray("filters", Filter.class);
		assertNotNull(filters);
		assertThat(filters.length, is(1));
		assertThat(filters[0].pattern(), equalTo("foo"));
	}

	@Test
	public void nestedAnnotations() throws Exception {
		Filter filter = FilteredClass.class.getAnnotation(Filter.class);

		this.attributes.put("filter", filter);
		this.attributes.put("filters", new Filter[] {filter, filter});

		Filter retrievedFilter = this.attributes.getAnnotation("filter", Filter.class);
		assertThat(retrievedFilter, equalTo(filter));
		assertThat(retrievedFilter.pattern(), equalTo("foo"));

		Filter[] retrievedFilters = this.attributes.getAnnotationArray("filters", Filter.class);
		assertNotNull(retrievedFilters);
		assertEquals(2, retrievedFilters.length);
		assertThat(retrievedFilters[1].pattern(), equalTo("foo"));
	}

	@Test
	public void getEnumWithNullAttributeName() {
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage("must not be null or empty");
		this.attributes.getEnum(null);
	}

	@Test
	public void getEnumWithEmptyAttributeName() {
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage("must not be null or empty");
		this.attributes.getEnum("");
	}

	@Test
	public void getEnumWithUnknownAttributeName() {
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage("Attribute 'bogus' not found");
		this.attributes.getEnum("bogus");
	}

	@Test
	public void getEnumWithTypeMismatch() {
		this.attributes.put("color", "RED");
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage(containsString("Attribute 'color' is of type [String], but [Enum] was expected"));
		this.attributes.getEnum("color");
	}

	@Test
	public void getAliasedStringWithImplicitAliases() {
		String value = "metaverse";
		List<String> aliases = Arrays.asList("value", "location1", "location2", "location3", "xmlFile", "groovyScript");

		this.attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);
		this.attributes.put("value", value);
		AnnotationUtils.postProcessAnnotationAttributes(null, this.attributes, false);
		aliases.stream().forEach(alias -> assertEquals(value, this.attributes.getString(alias)));

		this.attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);
		this.attributes.put("location1", value);
		AnnotationUtils.postProcessAnnotationAttributes(null, this.attributes, false);
		aliases.stream().forEach(alias -> assertEquals(value, this.attributes.getString(alias)));

		this.attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);
		this.attributes.put("value", value);
		this.attributes.put("location1", value);
		this.attributes.put("xmlFile", value);
		this.attributes.put("groovyScript", value);
		AnnotationUtils.postProcessAnnotationAttributes(null, this.attributes, false);
		aliases.stream().forEach(alias -> assertEquals(value, this.attributes.getString(alias)));
	}

	@Test
	public void getAliasedStringArrayWithImplicitAliases() {
		String[] value = new String[] {"test.xml"};
		List<String> aliases = Arrays.asList("value", "location1", "location2", "location3", "xmlFile", "groovyScript");

		this.attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);
		this.attributes.put("location1", value);
		AnnotationUtils.postProcessAnnotationAttributes(null, this.attributes, false);
		aliases.stream().forEach(alias -> assertArrayEquals(value, this.attributes.getStringArray(alias)));

		this.attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);
		this.attributes.put("value", value);
		AnnotationUtils.postProcessAnnotationAttributes(null, this.attributes, false);
		aliases.stream().forEach(alias -> assertArrayEquals(value, this.attributes.getStringArray(alias)));

		this.attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);
		this.attributes.put("location1", value);
		this.attributes.put("value", value);
		AnnotationUtils.postProcessAnnotationAttributes(null, this.attributes, false);
		aliases.stream().forEach(alias -> assertArrayEquals(value, this.attributes.getStringArray(alias)));

		this.attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);
		this.attributes.put("location1", value);
		AnnotationUtils.registerDefaultValues(this.attributes);
		AnnotationUtils.postProcessAnnotationAttributes(null, this.attributes, false);
		aliases.stream().forEach(alias -> assertArrayEquals(value, this.attributes.getStringArray(alias)));

		this.attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);
		this.attributes.put("value", value);
		AnnotationUtils.registerDefaultValues(this.attributes);
		AnnotationUtils.postProcessAnnotationAttributes(null, this.attributes, false);
		aliases.stream().forEach(alias -> assertArrayEquals(value, this.attributes.getStringArray(alias)));

		this.attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);
		AnnotationUtils.registerDefaultValues(this.attributes);
		AnnotationUtils.postProcessAnnotationAttributes(null, this.attributes, false);
		aliases.stream().forEach(alias -> assertArrayEquals(new String[] {""}, this.attributes.getStringArray(alias)));
	}


	enum Color {

		RED, WHITE, BLUE
	}


	@Retention(RetentionPolicy.RUNTIME)
	@interface Filter {

		@AliasFor(attribute = "classes")
		Class<?>[] value() default {};

		@AliasFor(attribute = "value")
		Class<?>[] classes() default {};

		String pattern();
	}


	@Filter(pattern = "foo")
	static class FilteredClass {
	}

}
