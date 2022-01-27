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

package org.springframework.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.generator.DefaultCodeContribution;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessorTests.ResourceInjectionBean;
import org.springframework.beans.factory.generator.BeanInstanceContributor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.javapoet.support.CodeSnippet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for code contribution of {@link AutowiredAnnotationBeanPostProcessor}.
 *
 * @author Stephane Nicoll
 */
class AutowiredAnnotationBeanInstanceContributorTests {

	@Test
	void testPreProcessorWithInjectionPoints() {
		CodeContribution contribution = contribute(ResourceInjectionBean.class);
		assertThat(CodeSnippet.process(contribution.statements().toCodeBlock())).isEqualTo("""
				instanceContext.field("testBean", TestBean.class)
						.resolve(beanFactory, false).ifResolved((attributes) -> {
							Field testBeanField = ReflectionUtils.findField(AutowiredAnnotationBeanPostProcessorTests.ResourceInjectionBean.class, "testBean", TestBean.class);
							ReflectionUtils.makeAccessible(testBeanField);
							ReflectionUtils.setField(testBeanField, bean, attributes.get(0));
						});
				instanceContext.method("setTestBean2", TestBean.class)
						.invoke(beanFactory, (attributes) -> bean.setTestBean2(attributes.get(0)));""");
		assertThat(contribution.runtimeHints().reflection().typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.fields()).singleElement().satisfies(fieldHint ->
					assertThat(fieldHint.getName()).isEqualTo("testBean"));
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint ->
					assertThat(methodHint.getName()).isEqualTo("setTestBean2"));
		});
		assertThat(contribution.protectedAccess().isAccessible("com.example")).isTrue();
	}

	private CodeContribution contribute(Class<?> beanType) {
		return contribute(new RootBeanDefinition(beanType), beanType);
	}

	private DefaultCodeContribution contribute(RootBeanDefinition beanDefinition, Class<?> type) {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		BeanInstanceContributor contributor = bpp.preProcess(beanDefinition, type, "test");
		assertThat(contributor).isNotNull();
		DefaultCodeContribution contribution = new DefaultCodeContribution(new RuntimeHints());
		contributor.contribute(contribution);
		return contribution;
	}
}
