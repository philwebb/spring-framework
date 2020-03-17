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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynamicPropertiesContextCustomizerFactory}.
 *
 * @author Phillip Webb
 */
class DynamicPropertiesContextCustomizerFactoryTests {

	private DynamicPropertiesContextCustomizerFactory factory = new DynamicPropertiesContextCustomizerFactory();

	private List<ContextConfigurationAttributes> configAttributes = Collections.emptyList();

	@Test
	void createContextCustomizerWhenNoAnnoatedMethodsReturnsNull() {
		DynamicPropertiesContextCustomizer customizer = this.factory.createContextCustomizer(
				NoDynamicProperties.class, this.configAttributes);
		assertThat(customizer).isNull();
	}

	@Test
	void createContextCustomizerWhenSingleAnnoatedMethodReturnsCustomizer() {
		DynamicPropertiesContextCustomizer customizer = this.factory.createContextCustomizer(
				SingleDynamicProperty.class, this.configAttributes);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getMethods()).flatExtracting(Method::getName).containsOnly(
				"p1");
	}

	@Test
	void createContextCustomizerWhenMultipleAnnoatedMethodsReturnsCustomizer() {
		DynamicPropertiesContextCustomizer customizer = this.factory.createContextCustomizer(
				MultipleDynamicProperty.class, this.configAttributes);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getMethods()).flatExtracting(Method::getName).containsOnly(
				"p1", "p2", "p3");
	}

	@Test
	void createContextCustomizerWhenAnnoatedMethodsInBaseClassReturnsCustomizer() {
		DynamicPropertiesContextCustomizer customizer = this.factory.createContextCustomizer(
				SubClassDynamicProperty.class, this.configAttributes);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getMethods()).flatExtracting(Method::getName).containsOnly(
				"p1", "p2");
	}



	static class NoDynamicProperties {

		void empty() {
		}

	}

	static class SingleDynamicProperty {

		@DynamicPropertySource
		static void p1(DynamicPropertyValues values) {
		}

	}

	static class MultipleDynamicProperty {

		@DynamicPropertySource
		static void p1(DynamicPropertyValues values) {
		}

		@DynamicPropertySource
		static void p2(DynamicPropertyValues values) {
		}

		@DynamicPropertySource
		static void p3(DynamicPropertyValues values) {
		}

	}

	static class BaseDynamicProperty {

		@DynamicPropertySource
		static void p1(DynamicPropertyValues values) {
		}

	}

	static class SubClassDynamicProperty extends BaseDynamicProperty {

		@DynamicPropertySource
		static void p2(DynamicPropertyValues values) {
		}

	}

}
