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

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.EnvironmentAwareComponent;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.NoDependencyComponent;
import org.springframework.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolderFactoryBean;
import org.springframework.beans.testfixture.beans.factory.generator.factory.SampleFactory;
import org.springframework.beans.testfixture.beans.factory.generator.injection.InjectionComponent;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.support.CodeSnippet;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultBeanInstanceGenerator}.
 *
 * @author Stephane Nicoll
 */
class DefaultBeanInstanceGeneratorTests {

	@Test
	void generateUsingDefaultConstructorUsesMethodReference() {
		CodeContribution contribution = generate(SimpleConfiguration.class.getDeclaredConstructors()[0]);
		assertThat(code(contribution)).isEqualTo("SimpleConfiguration::new");
	}

	@Test
	void generateUsingConstructorWithoutParameterAndMultipleCandidatesDoesNotUseMethodReference() throws NoSuchMethodException {
		CodeContribution contribution = generate(TestBean.class.getConstructor());
		assertThat(code(contribution)).isEqualTo("() -> new TestBean()");
	}

	@Test
	void generateUsingConstructorWithParameter() {
		CodeContribution contribution = generate(InjectionComponent.class.getDeclaredConstructors()[0]);
		assertThat(code(contribution).lines()).containsOnly(
				"(instanceContext) -> instanceContext.create(beanFactory, (attributes) -> "
						+ "new InjectionComponent(attributes.get(0)))");
	}

	@Test
	void generateUsingConstructorWithInnerClassAndNoExtraArg() {
		CodeContribution contribution = generate(NoDependencyComponent.class.getDeclaredConstructors()[0]);
		assertThat(code(contribution).lines()).containsOnly(
				"() -> beanFactory.getBean(InnerComponentConfiguration.class).new NoDependencyComponent()");
	}

	@Test
	void generateUsingConstructorWithInnerClassAndExtraArg() {
		CodeContribution contribution = generate(EnvironmentAwareComponent.class.getDeclaredConstructors()[0]);
		assertThat(code(contribution).lines()).containsOnly(
				"(instanceContext) -> instanceContext.create(beanFactory, (attributes) -> "
						+ "beanFactory.getBean(InnerComponentConfiguration.class).new EnvironmentAwareComponent(attributes.get(1)))");
	}

	@Test
	void generateUsingConstructorOfTypeWithGeneric() {
		CodeContribution contribution = generate(NumberHolderFactoryBean.class.getDeclaredConstructors()[0]);
		assertThat(code(contribution)).isEqualTo("NumberHolderFactoryBean::new");
	}

	@Test
	void generateUsingNoArgConstructorAndContributorsDoesNotUseMethodReference() {
		CodeContribution contribution = generate(SimpleConfiguration.class.getDeclaredConstructors()[0],
				contrib -> contrib.statements().add(CodeBlock.of("// hello\n")),
				contrib -> contrib.statements().add(CodeBlock.of("// world\n")));
		assertThat(code(contribution)).isEqualTo("""
				(instanceContext) -> {
					SimpleConfiguration bean = new SimpleConfiguration();
					// hello
					// world
					return bean;
				}""");
	}

	@Test
	void generateUsingMethodWithNoArg() {
		CodeContribution contribution = generate(method(SimpleConfiguration.class, "stringBean"));
		assertThat(code(contribution)).isEqualTo("() -> beanFactory.getBean(SimpleConfiguration.class).stringBean()");
	}

	@Test
	void generateUsingStaticMethodWithNoArg() {
		CodeContribution contribution = generate(method(SampleFactory.class, "integerBean"));
		assertThat(code(contribution)).isEqualTo("() -> SampleFactory.integerBean()");
	}

	@Test
	void generateUsingMethodWithArg() {
		CodeContribution contribution = generate(method(SampleFactory.class, "create",
				Number.class, String.class));
		assertThat(code(contribution)).isEqualTo("(instanceContext) -> instanceContext.create(beanFactory, (attributes) -> "
				+ "SampleFactory.create(attributes.get(0), attributes.get(1)))");
	}

	@Test
	void generateUsingMethodAndContributors() {
		CodeContribution contribution = generate(method(SimpleConfiguration.class, "stringBean"),
				contrib -> contrib.statements().add(CodeBlock.of("// hello\n")),
				contrib -> contrib.statements().add(CodeBlock.of("// world\n")));
		assertThat(code(contribution)).isEqualTo("""
				(instanceContext) -> {
					String bean = beanFactory.getBean(SimpleConfiguration.class).stringBean();
					// hello
					// world
					return bean;
				}""");
	}

	private String code(CodeContribution contribution) {
		return CodeSnippet.process(contribution.statements().toCodeBlock());
	}

	private CodeContribution generate(Executable executable,
			BeanInstanceContributor... beanInstanceContributors) {
		DefaultBeanInstanceGenerator generator = new DefaultBeanInstanceGenerator(executable,
				Arrays.asList(beanInstanceContributors));
		return generator.generateBeanInstance(new RuntimeHints());
	}

	private static Method method(Class<?> type, String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(type, methodName, parameterTypes);
		assertThat(method).isNotNull();
		return method;
	}

}
