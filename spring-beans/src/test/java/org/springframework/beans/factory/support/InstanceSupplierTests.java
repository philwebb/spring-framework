/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.StringUtils;
import org.springframework.util.function.ThrowableSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link InstanceSupplier}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class InstanceSupplierTests {

	private RegisteredBean registeredBean = RegisteredBean.of(new DefaultListableBeanFactory(), "test");

	@Test
	void getWithoutRegisteredBeanThrowsException() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThatIllegalStateException().isThrownBy(() -> supplier.get())
				.withMessage("No RegisteredBean parameter provided");
	}

	@Test
	void getWithExceptionWithoutRegisteredBeanThrowsException() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThatIllegalStateException().isThrownBy(() -> supplier.getWithException())
				.withMessage("No RegisteredBean parameter provided");
	}

	@Test
	void getReturnsResult() throws Exception {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThat(supplier.get(this.registeredBean)).isEqualTo("test");
	}

	@Test
	void withPostProcessorWhenPostProcessorIsNullThrowsException() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThatIllegalArgumentException().isThrownBy(() -> supplier.withPostProcessor(null))
				.withMessage("'instancePostProcessor' must not be null");
	}

	@Test
	void withPostProcessorAddsPostProcessor() throws Exception {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		supplier = supplier.withPostProcessor(InstancePostProcessor.of(String::toUpperCase));
		assertThat(supplier.get(this.registeredBean)).isEqualTo("TEST");
	}

	@Test
	void withMultiplePostProcessorCallsAddsPostProcessors() throws Exception {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		supplier = supplier.withPostProcessor(InstancePostProcessor.of(String::toUpperCase));
		InstanceSupplier<String> supplier2 = supplier
				.withPostProcessor(InstancePostProcessor.of(StringUtils::uncapitalize));
		assertThat(supplier.get(this.registeredBean)).isEqualTo("TEST");
		assertThat(supplier2.get(this.registeredBean)).isEqualTo("tEST");
	}

	@Test
	void ofSupplierWhenInstanceSupplierReturnsSameInstance() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThat(InstanceSupplier.of(supplier)).isSameAs(supplier);
	}

	@Test
	void usingSupplierAdaptsToInstanceSupplier() throws Exception {
		InstanceSupplier<String> instanceSupplier = InstanceSupplier.using(() -> "test");
		assertThat(instanceSupplier.get(this.registeredBean)).isEqualTo("test");
	}

	@Test
	void ofInstanceSupplierAdaptsToInstanceSupplier() throws Exception {
		InstanceSupplier<String> instanceSupplier = InstanceSupplier.of(registeredBean -> "test");
		assertThat(instanceSupplier.get(this.registeredBean)).isEqualTo("test");
	}

	@Test
	void getSuppliedInstanceWhenInstanceSupplierReturnsResult() throws Exception {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThat(InstanceSupplier.getSuppliedInstance(this.registeredBean, supplier)).isEqualTo("test");
	}

	@Test
	void getSuppliedInstanceThrowableSupplierReturnsResult() throws Exception {
		ThrowableSupplier<String> supplier = () -> "test";
		assertThat(InstanceSupplier.getSuppliedInstance(this.registeredBean, supplier)).isEqualTo("test");
	}

	@Test
	void getSuppliedInstanceThrowableSupplierWhenExceptionThrownDoesNotDoubleWrapException() {
		RuntimeException ex = new RuntimeException();
		ThrowableSupplier<String> supplier = () -> {
			throw ex;
		};
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> InstanceSupplier.getSuppliedInstance(this.registeredBean, supplier)).havingCause()
				.isSameAs(ex);
	}

	@Test
	void getSuppliedInstanceReturnsResult() throws Exception {
		Supplier<String> supplier = () -> "test";
		assertThat(InstanceSupplier.getSuppliedInstance(this.registeredBean, supplier)).isEqualTo("test");
	}

	@Test
	void getSuppliedInstanceWhenThrowsExceptionThrowsBeanCreationException() {
		RuntimeException ex = new RuntimeException();
		InstanceSupplier<String> supplier = registeredBean -> {
			throw ex;
		};
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> InstanceSupplier.getSuppliedInstance(this.registeredBean, supplier)).havingCause()
				.isSameAs(ex);

	}

	@Test
	void callFromBeanFactoryProvidesRegisteredBean() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setInstanceSupplier(
				InstanceSupplier.of(registeredBean -> "I am bean " + registeredBean.getBeanName()));
		beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThat(beanFactory.getBean("test")).isEqualTo("I am bean test");
	}

}
