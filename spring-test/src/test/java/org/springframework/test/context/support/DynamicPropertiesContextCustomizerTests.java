/*
 * Copyright 2002-2020 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.DynamicPropertyValues;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;


/**
 * Tests for {@link DynamicPropertiesContextCustomizer}.
 *
 * @author Phillip Webb
 */
class DynamicPropertiesContextCustomizerTests {

	@Test
	void createWhenNonStaticDynamicPropertiesMethodThrowsException() {
		Set<Method> methods = findMethodsNamed(NonStaticDynamicProperty.class, "properties");
		assertThatIllegalStateException().isThrownBy(() ->
				new DynamicPropertiesContextCustomizer(methods))
			.withMessage("@DynamicPropertySource method 'properties' must be static");
	}

	@Test
	void createWhenBadDynamicPropertiesSignatureThrowsException() {
		Set<Method> methods = findMethodsNamed(BadArgsDynamicProperty.class, "properties");
		assertThatIllegalStateException().isThrownBy(() ->
				new DynamicPropertiesContextCustomizer(methods))
			.withMessage("@DynamicPropertySource method 'properties' must accept a single DynamicPropertyValues argument");
	}

	@Test
	void customizeContextAddsPropertySource() throws Exception {
		Set<Method> methods = findMethodsNamed(TestProperties.class, "p1", "p2");
		DynamicPropertiesContextCustomizer customizer = new DynamicPropertiesContextCustomizer(methods);
		ConfigurableApplicationContext context = new StaticApplicationContext();
		customizer.customizeContext(context, mock(MergedContextConfiguration.class));
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getRequiredProperty("p1a")).isEqualTo("v1a");
		assertThat(environment.getRequiredProperty("p1b")).isEqualTo("v1b");
		assertThat(environment.getRequiredProperty("p2a")).isEqualTo("v2a");
		assertThat(environment.getRequiredProperty("p2b")).isEqualTo("v2b");
	}

	@Test
	void equalsAndHashCode() throws Exception {
		DynamicPropertiesContextCustomizer c1 = new DynamicPropertiesContextCustomizer(
				findMethodsNamed(TestProperties.class, "p1", "p2"));
		DynamicPropertiesContextCustomizer c2 = new DynamicPropertiesContextCustomizer(
				findMethodsNamed(TestProperties.class, "p1", "p2"));
		DynamicPropertiesContextCustomizer c3 = new DynamicPropertiesContextCustomizer(
				findMethodsNamed(TestProperties.class, "p1"));
		assertThat(c1.hashCode()).isEqualTo(c1.hashCode()).isEqualTo(c2.hashCode());
		assertThat(c1).isEqualTo(c1).isEqualTo(c2).isNotEqualTo(c3);
	}

	private Set<Method> findMethodsNamed(Class<?> clazz, String... names) {
		Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(clazz,
				method -> ObjectUtils.containsElement(names, method.getName()));
		return new LinkedHashSet<>(Arrays.<Method> asList(methods));
	}

	static class NonStaticDynamicProperty {

		void properties(DynamicPropertyValues values) {
		}

	}

	static class BadArgsDynamicProperty {

		static void properties(String bad) {
		}

	}

	static class TestProperties {

		static void p1(DynamicPropertyValues values) {
			values.add("p1a", () -> "v1a");
			values.add("p1b", () -> "v1b");
		}

		static void p2(DynamicPropertyValues values) {
			values.add("p2a", () -> "v2a");
			values.add("p2b", () -> "v2b");
		}

	}

}
