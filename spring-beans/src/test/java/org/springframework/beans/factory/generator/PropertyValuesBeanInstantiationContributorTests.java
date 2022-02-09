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

package org.springframework.beans.factory.generator;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.generator.DefaultCodeContribution;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolderFactoryBean;
import org.springframework.beans.testfixture.beans.factory.generator.property.ConfigurableBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link PropertyValuesBeanInstantiationContributor}.
 *
 * @author Stephane Nicoll
 */
class PropertyValuesBeanInstantiationContributorTests {

	@Test
	void contributeWithNoPropertyValuesDoesNotAccessRuntimeHints() {
		RootBeanDefinition bd = new RootBeanDefinition(String.class);
		CodeContribution contribution = mock(CodeContribution.class);
		new PropertyValuesBeanInstantiationContributor(bd).contribute(contribution);
		verifyNoInteractions(contribution);
	}

	@Test
	void contributeWithProperty() {
		BeanDefinition bd = BeanDefinitionBuilder.rootBeanDefinition(NumberHolderFactoryBean.class)
				.addPropertyValue("number", 42).getBeanDefinition();
		CodeContribution contribute = contribute(bd);
		assertThat(contribute.runtimeHints().reflection().getTypeHint(NumberHolderFactoryBean.class)).satisfies(hint -> {
			assertThat(hint.fields()).isEmpty();
			assertThat(hint.constructors()).isEmpty();
			assertThat(hint.methods()).singleElement().satisfies(methodHint -> {
				assertThat(methodHint.getName()).isEqualTo("setNumber");
				assertThat(methodHint.getParameterTypes()).containsExactly(TypeReference.of(Number.class));
				assertThat(methodHint.getModes()).containsOnly(ExecutableMode.INVOKE);
			});
			assertThat(hint.getMemberCategories()).isEmpty();
		});
	}

	@Test
	void contributeWithMultipleProperties() {
		BeanDefinition bd = BeanDefinitionBuilder.rootBeanDefinition(ConfigurableBean.class)
				.addPropertyValue("name", "hello").addPropertyValue("counter", 42).getBeanDefinition();
		CodeContribution contribute = contribute(bd);
		assertThat(contribute.runtimeHints().reflection().getTypeHint(ConfigurableBean.class)).satisfies(hint -> {
			assertThat(hint.fields()).isEmpty();
			assertThat(hint.constructors()).isEmpty();
			assertThat(hint.methods()).anySatisfy(methodHint -> {
				assertThat(methodHint.getName()).isEqualTo("setName");
				assertThat(methodHint.getParameterTypes()).containsExactly(TypeReference.of(String.class));
				assertThat(methodHint.getModes()).containsOnly(ExecutableMode.INVOKE);
			});
			assertThat(hint.methods()).anySatisfy(methodHint -> {
				assertThat(methodHint.getName()).isEqualTo("setCounter");
				assertThat(methodHint.getParameterTypes()).containsExactly(TypeReference.of(Integer.class));
				assertThat(methodHint.getModes()).containsOnly(ExecutableMode.INVOKE);
			});
			assertThat(hint.methods()).hasSize(2);
			assertThat(hint.getMemberCategories()).isEmpty();
		});
	}

	@Test
	void contributeWithInvalidProperty() {
		BeanDefinition bd = BeanDefinitionBuilder.rootBeanDefinition(ConfigurableBean.class)
				.addPropertyValue("notAProperty", "invalid").addPropertyValue("name", "hello")
				.getBeanDefinition();
		CodeContribution contribute = contribute(bd);
		assertThat(contribute.runtimeHints().reflection().getTypeHint(ConfigurableBean.class)).satisfies(hint -> {
			assertThat(hint.fields()).isEmpty();
			assertThat(hint.constructors()).isEmpty();
			assertThat(hint.methods()).singleElement().satisfies(methodHint -> {
				assertThat(methodHint.getName()).isEqualTo("setName");
				assertThat(methodHint.getParameterTypes()).containsExactly(TypeReference.of(String.class));
				assertThat(methodHint.getModes()).containsOnly(ExecutableMode.INVOKE);
			});
			assertThat(hint.getMemberCategories()).isEmpty();
		});
	}

	private CodeContribution contribute(BeanDefinition beanDefinition) {
		CodeContribution contribution = new DefaultCodeContribution(new RuntimeHints());
		new PropertyValuesBeanInstantiationContributor(beanDefinition).contribute(contribution);
		return contribution;
	}

}
