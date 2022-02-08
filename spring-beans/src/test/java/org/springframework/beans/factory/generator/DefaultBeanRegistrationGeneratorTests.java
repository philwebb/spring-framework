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

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.EnvironmentAwareComponent;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.NoDependencyComponent;
import org.springframework.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import org.springframework.beans.testfixture.beans.factory.generator.factory.SampleFactory;
import org.springframework.beans.testfixture.beans.factory.generator.injection.InjectionComponent;
import org.springframework.beans.testfixture.beans.factory.generator.visibility.ProtectedConstructorComponent;
import org.springframework.beans.testfixture.beans.factory.generator.visibility.ProtectedFactoryMethod;
import org.springframework.core.testfixture.aot.generator.visibility.PublicFactoryBean;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.support.CodeSnippet;
import org.springframework.javapoet.support.MultiStatement;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultBeanRegistrationGenerator}.
 *
 * @author Stephane Nicoll
 */
class DefaultBeanRegistrationGeneratorTests {

	@Test
	void generateUsingConstructor() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(InjectionComponent.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, singleConstructor(InjectionComponent.class), code -> code.add("() -> test"));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", InjectionComponent.class).withConstructor(String.class)
						.instanceSupplier(() -> test).register(beanFactory);
				""");
	}

	@Test
	void generateUsingConstructorWithNoArgument() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(SimpleConfiguration.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, singleConstructor(SimpleConfiguration.class), code -> code.add("() -> test"));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> test).register(beanFactory);
				""");
	}

	@Test
	void generateUsingConstructorOnInnerClass() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(EnvironmentAwareComponent.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, singleConstructor(EnvironmentAwareComponent.class), code -> code.add("() -> test"));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", InnerComponentConfiguration.EnvironmentAwareComponent.class).withConstructor(InnerComponentConfiguration.class, Environment.class)
						.instanceSupplier(() -> test).register(beanFactory);
				""");
	}

	@Test
	void generateUsingConstructorOnInnerClassWithNoExtraArg() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(NoDependencyComponent.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, singleConstructor(NoDependencyComponent.class), code -> code.add("() -> test"));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", InnerComponentConfiguration.NoDependencyComponent.class)
						.instanceSupplier(() -> test).register(beanFactory);
				""");
	}

	@Test
	void generateUsingFactoryMethod() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, method(SampleFactory.class, "create", String.class), code -> code.add("() -> test"));
		assertThat(registration.hasImport(SampleFactory.class)).isTrue();
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", String.class).withFactoryMethod(SampleFactory.class, "create", String.class)
						.instanceSupplier(() -> test).register(beanFactory);
				""");
	}

	@Test
	void generateUsingFactoryMethodWithNoArgument() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(Integer.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, method(SampleFactory.class, "integerBean"), code -> code.add("() -> test"));
		assertThat(registration.hasImport(SampleFactory.class)).isTrue();
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", Integer.class).withFactoryMethod(SampleFactory.class, "integerBean")
						.instanceSupplier(() -> test).register(beanFactory);
				""");
	}

	@Test
	void generateUsingPublicAccessDoesNotAccessAnotherPackage() {
		DefaultGeneratedTypeContext context = createGeneratedTypeContext();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(SimpleConfiguration.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, singleConstructor(SimpleConfiguration.class), context);
		assertThat(context.toJavaFiles()).hasSize(1);
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(SimpleConfiguration::new).register(beanFactory);
				""");
	}

	@Test
	void generateUsingProtectedConstructorWritesToBlessedPackage() {
		DefaultGeneratedTypeContext context = createGeneratedTypeContext();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(ProtectedConstructorComponent.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, singleConstructor(ProtectedConstructorComponent.class), context);
		assertThat(context.hasGeneratedType(ProtectedConstructorComponent.class.getPackageName())).isTrue();
		GeneratedType generatedType = context.getGeneratedType(ProtectedConstructorComponent.class.getPackageName());
		assertThat(removeIndent(codeOf(generatedType), 1)).containsSequence("""
				public static void registerTest(DefaultListableBeanFactory beanFactory) {
					BeanDefinitionRegistrar.of("test", ProtectedConstructorComponent.class)
							.instanceSupplier(ProtectedConstructorComponent::new).register(beanFactory);
				}""");
		assertThat(registration.getSnippet()).isEqualTo(
				ProtectedConstructorComponent.class.getPackageName() + ".Test.registerTest(beanFactory);\n");
	}

	@Test
	void generateUsingProtectedFactoryMethodWritesToBlessedPackage() {
		DefaultGeneratedTypeContext context = createGeneratedTypeContext();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, method(ProtectedFactoryMethod.class, "testBean", Integer.class), context);
		assertThat(context.hasGeneratedType(ProtectedFactoryMethod.class.getPackageName())).isTrue();
		GeneratedType generatedType = context.getGeneratedType(ProtectedConstructorComponent.class.getPackageName());
		assertThat(removeIndent(codeOf(generatedType), 1)).containsSequence("""
				public static void registerProtectedFactoryMethod_test(DefaultListableBeanFactory beanFactory) {
					BeanDefinitionRegistrar.of("test", String.class).withFactoryMethod(ProtectedFactoryMethod.class, "testBean", Integer.class)
							.instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(ProtectedFactoryMethod.class).testBean(attributes.get(0)))).register(beanFactory);
				}""");
		assertThat(registration.getSnippet()).isEqualTo(
				ProtectedConstructorComponent.class.getPackageName() + ".Test.registerProtectedFactoryMethod_test(beanFactory);\n");
	}

	@Test
	void generateUsingProtectedGenericTypeWritesToBlessedPackage() {
		DefaultGeneratedTypeContext context = createGeneratedTypeContext();
		RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder.rootBeanDefinition(
				PublicFactoryBean.class).getBeanDefinition();
		// This resolve the generic parameter to a protected type
		beanDefinition.setTargetType(PublicFactoryBean.resolveToProtectedGenericParameter());
		CodeSnippet registration = beanRegistration(beanDefinition, singleConstructor(PublicFactoryBean.class), context);
		assertThat(context.hasGeneratedType(PublicFactoryBean.class.getPackageName())).isTrue();
		GeneratedType generatedType = context.getGeneratedType(PublicFactoryBean.class.getPackageName());
		assertThat(removeIndent(codeOf(generatedType), 1)).containsSequence("""
				public static void registerTest(DefaultListableBeanFactory beanFactory) {
					BeanDefinitionRegistrar.of("test", ResolvableType.forClassWithGenerics(PublicFactoryBean.class, ProtectedType.class))
							.instanceSupplier(PublicFactoryBean::new).register(beanFactory);
				}""");
		assertThat(registration.getSnippet()).isEqualTo(
				PublicFactoryBean.class.getPackageName() + ".Test.registerTest(beanFactory);\n");
	}

	@Test
	void generateWithBeanDefinitionHavingSyntheticFlag() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setSynthetic(true)).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.setSynthetic(true)).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingDependsOn() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setDependsOn("test")).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.setDependsOn(new String[] { "test" })).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingLazyInit() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setLazyInit(true)).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.setLazyInit(true)).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingRole() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.setRole(2)).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingScope() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.setScope("prototype")).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingAutowiredCandidate() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setAutowireCandidate(false)).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.setAutowireCandidate(false)).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingDefaultAutowiredCandidateDoesNotConfigureIt() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setAutowireCandidate(true)).getSnippet())
				.doesNotContain("bd.setAutowireCandidate(");
	}

	@Test
	void generateWithBeanDefinitionHavingMultipleAttributes() {
		assertThat(simpleConfigurationRegistration(bd -> {
			bd.setSynthetic(true);
			bd.setPrimary(true);
		}).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> {
					bd.setPrimary(true);
					bd.setSynthetic(true);
				}).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingProperty() {
		assertThat(simpleConfigurationRegistration(bd -> bd.getPropertyValues().addPropertyValue("test", "Hello")).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.getPropertyValues().addPropertyValue("test", "Hello")).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingSeveralProperties() {
		CodeSnippet registration = simpleConfigurationRegistration(bd -> {
			bd.getPropertyValues().addPropertyValue("test", "Hello");
			bd.getPropertyValues().addPropertyValue("counter", 42);
		});
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> {
					MutablePropertyValues propertyValues = bd.getPropertyValues();
					propertyValues.addPropertyValue("test", "Hello");
					propertyValues.addPropertyValue("counter", 42);
				}).register(beanFactory);
				""");
		assertThat(registration.hasImport(MutablePropertyValues.class)).isTrue();
	}

	@Test
	void generateWithBeanDefinitionHavingPropertyReference() {
		CodeSnippet registration = simpleConfigurationRegistration(bd -> bd.getPropertyValues()
				.addPropertyValue("myService", new RuntimeBeanReference("test")));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.getPropertyValues().addPropertyValue("myService", new RuntimeBeanReference("test"))).register(beanFactory);
				""");
		assertThat(registration.hasImport(RuntimeBeanReference.class)).isTrue();
	}

	CodeSnippet simpleConfigurationRegistration(Consumer<RootBeanDefinition> bd) {
		RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(SimpleConfiguration.class).getBeanDefinition();
		bd.accept(beanDefinition);
		return beanRegistration(beanDefinition, singleConstructor(SimpleConfiguration.class),
				code -> code.add("() -> SimpleConfiguration::new"));
	}

	@Test
	void generateUsingSingleConstructorArgument() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, "hello");
		CodeSnippet registration = beanRegistration(beanDefinition, method(SampleFactory.class, "create", String.class),
				code -> code.add("() -> test"));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", String.class).withFactoryMethod(SampleFactory.class, "create", String.class)
						.instanceSupplier(() -> test).customize((bd) -> bd.getConstructorArgumentValues().addIndexedArgumentValue(0, "hello")).register(beanFactory);
				""");
	}

	@Test
	void generateUsingSeveralConstructorArguments() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(String.class)
				.addConstructorArgValue(42).addConstructorArgReference("testBean")
				.getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, method(SampleFactory.class, "create", Number.class, String.class),
				code -> code.add("() -> test"));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", String.class).withFactoryMethod(SampleFactory.class, "create", Number.class, String.class)
						.instanceSupplier(() -> test).customize((bd) -> {
					ConstructorArgumentValues argumentValues = bd.getConstructorArgumentValues();
					argumentValues.addIndexedArgumentValue(0, 42);
					argumentValues.addIndexedArgumentValue(1, new RuntimeBeanReference("testBean"));
				}).register(beanFactory);
				""");
		assertThat(registration.hasImport(ConstructorArgumentValues.class)).isTrue();
	}


	private CodeSnippet beanRegistration(BeanDefinition beanDefinition, Executable instanceCreator, GeneratedTypeContext context) {
		DefaultBeanRegistrationGenerator generator = new DefaultBeanRegistrationGenerator("test", beanDefinition, instanceCreator, Collections.emptyList());
		return CodeSnippet.of(generator.generateBeanRegistration(context));
	}

	private CodeSnippet beanRegistration(BeanDefinition beanDefinition, Executable instanceCreator, Consumer<Builder> instanceSupplier) {
		DefaultBeanRegistrationGenerator generator = new DefaultBeanRegistrationGenerator("test", beanDefinition, instanceCreator, Collections.emptyList());
		CodeBlock.Builder code = CodeBlock.builder();
		generator.writeBeanRegistration(toMultiStatements(instanceSupplier), code);
		return CodeSnippet.of(code.build());
	}

	private DefaultGeneratedTypeContext createGeneratedTypeContext() {
		return new DefaultGeneratedTypeContext("com.example", packageName -> GeneratedType.of(ClassName.get(packageName, "Test")));
	}

	private Constructor<?> singleConstructor(Class<?> type) {
		return type.getDeclaredConstructors()[0];
	}

	private Method method(Class<?> type, String name, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(type, name, parameterTypes);
		assertThat(method).isNotNull();
		return method;
	}

	private MultiStatement toMultiStatements(Consumer<Builder> instanceSupplier) {
		Builder code = CodeBlock.builder();
		instanceSupplier.accept(code);
		MultiStatement statements = new MultiStatement();
		statements.add(code.build());
		return statements;
	}

	private String codeOf(GeneratedType type) {
		try {
			StringWriter out = new StringWriter();
			type.toJavaFile().writeTo(out);
			return out.toString();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private String removeIndent(String content, int indent) {
		return content.lines().map(line -> {
			for (int i = 0; i < indent; i++) {
				if (line.startsWith("\t")) {
					line = line.substring(1);
				}
			}
			return line;
		}).collect(Collectors.joining("\n"));
	}

}
