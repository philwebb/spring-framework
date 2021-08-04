/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.util.function;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InstanceSupplier}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class InstanceSupplierTests {

	@Test
	void andWhenNextIsNullThrowsException() {
		// FIXME
	}

	@Test
	void andReturnsComposedSupplier() {
		// FIXME
	}

	@Test
	void fromWhenSupplierIsNullThrowsException() {
		// FIXME
	}

	@Test
	void fromAdaptsSupplier() {
		// FIXME
	}

	@Test
	void viaWhenExtractorIsNullThrowsException() {
		// FIXME
	}

	@Test
	void viaWhenTypeIsNullThrowsException() {
		// FIXME
	}

	@Test
	void viaAppliesExtraction() {
		// FIXME
	}

	@Test
	void viaCombinedWithAndProvidesSimpleFactoryMethodPattern() throws Throwable {
		TestContext context = new TestContext();
		context.add(new TestConfiguration());
		InstanceSupplier<TestContext, TestBean> supplier = InstanceSupplier.via(
				TestContext.class, TestContext::get, TestConfiguration.class).and(
						TestConfiguration::testBean);
		TestBean testBean = supplier.get(context);
		assertThat(testBean).isNotNull();
	}

	static class TestContext {

		private final Map<Class<?>, Object> contents = new LinkedHashMap<>();

		@SuppressWarnings("unchecked")
		<T> T get(Class<T> type) {
			return (T) this.contents.get(type);
		}

		void add(Object instance) {
			this.contents.put(instance.getClass(), instance);
		}

	}

	static class TestConfiguration {

		TestBean testBean() {
			return new TestBean();
		}

	}

	static class TestBean {

	}

}
