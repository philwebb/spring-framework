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

package org.springframework.beans.factory.support.generate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.aot.TestConstructorOrFactoryMethodResolver;
import org.springframework.beans.testfixture.beans.AnnotatedBean;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionPropertiesCodeGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class BeanDefinitionPropertiesCodeGeneratorTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final RootBeanDefinition beanDefinition = new RootBeanDefinition();

	private final GeneratedMethods generatedMethods = new GeneratedMethods();

	private BeanDefinitionPropertiesCodeGenerator generator = new BeanDefinitionPropertiesCodeGenerator(
			this.generatedMethods, new TestConstructorOrFactoryMethodResolver(this.beanFactory));

	@Test
	void setPrimaryWhenFalse() {
		this.beanDefinition.setPrimary(false);
		testCompiledResult((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setPrimary");
			assertThat(actual.isPrimary()).isFalse();
		});
	}

	@Test
	void setPrimaryWhenTrue() {
		this.beanDefinition.setPrimary(true);
		testCompiledResult((actual, compiled) -> {
			assertThat(actual.isPrimary()).isTrue();
		});
	}

	@Test
	void setScopeWhenEmptyString() {
		this.beanDefinition.setScope("");
		testCompiledResult((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setScope");
			assertThat(actual.getScope()).isEmpty();
		});
	}

	@Test
	void setScopeWhenSingleton() {
		this.beanDefinition.setScope("singleton");
		testCompiledResult((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setScope");
			assertThat(actual.getScope()).isEmpty();
		});
	}

	@Test
	void setScopeWhenOther() {
		this.beanDefinition.setScope("prototype");
		testCompiledResult((actual, compiled) -> {
			assertThat(actual.getScope()).isEqualTo("prototype");
		});
	}

	@Test
	void setDependsOnWhenEmpty() {
		this.beanDefinition.setDependsOn();
		testCompiledResult((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setDependsOn");
			assertThat(actual.getDependsOn()).isNull();
		});
	}

	@Test
	void setDependsOnWhenNotEmpty() {
		this.beanDefinition.setDependsOn("a", "b", "c");
		testCompiledResult((actual, compiled) -> {
			assertThat(actual.getDependsOn()).containsExactly("a", "b", "c");
		});
	}

	@Test
	void setLazyInitWhenNoSet() {
		testCompiledResult((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setLazyInit");
			assertThat(actual.isLazyInit()).isFalse();
			assertThat(actual.getLazyInit()).isNull();
		});
	}

	@Test
	void setLazyInitWhenFalse() {
		this.beanDefinition.setLazyInit(false);
		testCompiledResult((actual, compiled) -> {
			assertThat(actual.isLazyInit()).isFalse();
			assertThat(actual.getLazyInit()).isFalse();
		});
	}

	@Test
	void setLazyInitWhenTrue() {
		this.beanDefinition.setLazyInit(true);
		testCompiledResult((actual, compiled) -> {
			assertThat(actual.isLazyInit()).isTrue();
			assertThat(actual.getLazyInit()).isTrue();
		});
	}

	@Test
	void setAutowireCandidateWhenFalse() {
		this.beanDefinition.setAutowireCandidate(false);
		testCompiledResult((actual, compiled) -> {
			assertThat(actual.isAutowireCandidate()).isFalse();
		});
	}

	@Test
	void setAutowireCandidateWhenTrue() {
		this.beanDefinition.setAutowireCandidate(true);
		testCompiledResult((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setAutowireCandidate");
			assertThat(actual.isAutowireCandidate()).isTrue();
		});
	}

	@Test
	void setSyntheticWhenFalse() {
		this.beanDefinition.setSynthetic(false);
		testCompiledResult((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setSynthetic");
			assertThat(actual.isSynthetic()).isFalse();
		});
	}

	@Test
	void setSyntheticWhenTrue() {
		this.beanDefinition.setSynthetic(true);
		testCompiledResult((actual, compiled) -> {
			assertThat(actual.isSynthetic()).isTrue();
		});
	}

	@Test
	void setRoleWhenApplication() {
		this.beanDefinition.setRole(BeanDefinition.ROLE_APPLICATION);
		testCompiledResult((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setRole");
			assertThat(actual.getRole()).isEqualTo(BeanDefinition.ROLE_APPLICATION);
		});
	}

	@Test
	void setRoleWhenInfrastructure() {
		this.beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		testCompiledResult((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).contains("setRole(BeanDefinition.ROLE_INFRASTRUCTURE);");
			assertThat(actual.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
		});
	}

	@Test
	void setRoleWhenSupport() {
		this.beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
		testCompiledResult((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).contains("setRole(BeanDefinition.ROLE_SUPPORT);");
			assertThat(actual.getRole()).isEqualTo(BeanDefinition.ROLE_SUPPORT);
		});
	}

	@Test
	void setRoleWhenOther() {
		this.beanDefinition.setRole(999);
		testCompiledResult((actual, compiled) -> {
			assertThat(actual.getRole()).isEqualTo(999);
		});
	}

	@Test
	void constructorArgumentValuesWhenValues() {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, String.class);
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, "test");
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(2, 123);
		testCompiledResult((actual, compiled) -> {
			Map<Integer, ValueHolder> values = actual.getConstructorArgumentValues().getIndexedArgumentValues();
			assertThat(values.get(0).getValue()).isEqualTo(String.class);
			assertThat(values.get(1).getValue()).isEqualTo("test");
			assertThat(values.get(2).getValue()).isEqualTo(123);
		});
	}

	@Test
	void propertyValuesWhenValues() {
		this.beanDefinition.getPropertyValues().add("test", String.class);
		this.beanDefinition.getPropertyValues().add("spring", "framework");
		testCompiledResult((actual, compiled) -> {
			assertThat(actual.getPropertyValues().get("test")).isEqualTo(String.class);
			assertThat(actual.getPropertyValues().get("spring")).isEqualTo("framework");
		});
	}

	@Test
	void propertyValuesWhenContainsBeanReference() {
		this.beanDefinition.getPropertyValues().add("myService", new RuntimeBeanNameReference("test"));
		testCompiledResult((actual, compiled) -> {
			assertThat(actual.getPropertyValues().contains("myService")).isTrue();
			assertThat(actual.getPropertyValues().get("myService")).isInstanceOfSatisfying(RuntimeBeanReference.class,
					beanReference -> assertThat(beanReference.getBeanName()).isEqualTo("test"));
		});
	}

	@Test
	void propertyValuesWhenContainsManagedList() {
		ManagedList<Object> managedList = new ManagedList<>();
		managedList.add(new RuntimeBeanNameReference("test"));
		this.beanDefinition.getPropertyValues().add("value", managedList);
		testCompiledResult((actual, compiled) -> {
			Object value = actual.getPropertyValues().get("value");
			assertThat(value).isInstanceOf(ManagedList.class);
			assertThat(((List<?>) value).get(0)).isInstanceOf(BeanReference.class);
		});
	}

	@Test
	void propertyValuesWhenContainsManagedSet() {
		ManagedSet<Object> managedSet = new ManagedSet<>();
		managedSet.add(new RuntimeBeanNameReference("test"));
		this.beanDefinition.getPropertyValues().add("value", managedSet);
		testCompiledResult((actual, compiled) -> {
			Object value = actual.getPropertyValues().get("value");
			assertThat(value).isInstanceOf(ManagedSet.class);
			assertThat(((Set<?>) value).iterator().next()).isInstanceOf(BeanReference.class);
		});
	}

	@Test
	void propertyValuesWhenContainsManagedMap() {
		ManagedMap<String, Object> managedMap = new ManagedMap<>();
		managedMap.put("test", new RuntimeBeanNameReference("test"));
		this.beanDefinition.getPropertyValues().add("value", managedMap);
		testCompiledResult((actual, compiled) -> {
			Object value = actual.getPropertyValues().get("value");
			assertThat(value).isInstanceOf(ManagedMap.class);
			assertThat(((Map<?, ?>) value).get("test")).isInstanceOf(BeanReference.class);
		});
	}

	@Test
	void propertyValuesWhenContainsBeanDefinition() {
		RootBeanDefinition innerBeanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(AnnotatedBean.class).setRole(BeanDefinition.ROLE_INFRASTRUCTURE).setPrimary(true)
				.getBeanDefinition();
		this.beanDefinition.getPropertyValues().add("name", innerBeanDefinition);
		testCompiledResult((actual, compiled) -> {
			RootBeanDefinition actualInnerBeanDefinition = (RootBeanDefinition) actual.getPropertyValues().get("name");
			assertThat(actualInnerBeanDefinition.isPrimary()).isTrue();
			assertThat(actualInnerBeanDefinition.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
			assertThat(actualInnerBeanDefinition.getInstanceSupplier().get()).isInstanceOf(AnnotatedBean.class);
		});
	}

	@Test
	void attributesWhenAllFiltered() {
		this.beanDefinition.setAttribute("a", "A");
		this.beanDefinition.setAttribute("b", "B");
		testCompiledResult((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setAttribute");
			assertThat(actual.getAttribute("a")).isNull();
			assertThat(actual.getAttribute("b")).isNull();
		});
	}

	@Test
	void attributesWhenSomeFiltered() {
		this.beanDefinition.setAttribute("a", "A");
		this.beanDefinition.setAttribute("b", "B");
		Predicate<String> attributeFilter = attribute -> "a".equals(attribute);
		testCompiledResult(this.beanDefinition, attributeFilter, (actual, compiled) -> {
			assertThat(actual.getAttribute("a")).isEqualTo("A");
			assertThat(actual.getAttribute("b")).isNull();
		});
	}

	@Test
	void multipleItems() {
		this.beanDefinition.setPrimary(true);
		this.beanDefinition.setScope("test");
		this.beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
		testCompiledResult((actual, compiled) -> {
			assertThat(actual.isPrimary()).isTrue();
			assertThat(actual.getScope()).isEqualTo("test");
			assertThat(actual.getRole()).isEqualTo(BeanDefinition.ROLE_SUPPORT);
		});
	}

	private void testCompiledResult(BiConsumer<RootBeanDefinition, Compiled> result) {
		testCompiledResult(this.beanDefinition, result);
	}

	private void testCompiledResult(RootBeanDefinition beanDefinition,
			BiConsumer<RootBeanDefinition, Compiled> result) {
		testCompiledResult(() -> this.generator.generateCode(beanDefinition, "test"), result);
	}

	private void testCompiledResult(RootBeanDefinition beanDefinition, Predicate<String> attributeFilter,
			BiConsumer<RootBeanDefinition, Compiled> result) {
		testCompiledResult(() -> this.generator.generateCode(beanDefinition, "test", attributeFilter), result);
	}

	private void testCompiledResult(Supplier<CodeBlock> codeBlock, BiConsumer<RootBeanDefinition, Compiled> result) {
		JavaFile javaFile = createJavaFile(codeBlock);
		TestCompiler.forSystem().compile(javaFile::writeTo, (compiled) -> {
			RootBeanDefinition beanDefinition = (RootBeanDefinition) compiled.getInstance(Supplier.class).get();
			result.accept(beanDefinition, compiled);
		});
	}

	private JavaFile createJavaFile(Supplier<CodeBlock> codeBlock) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("BeanSupplier");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(ParameterizedTypeName.get(Supplier.class, RootBeanDefinition.class));
		builder.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC)
				.returns(RootBeanDefinition.class)
				.addStatement("$T beanDefinition = new $T()", RootBeanDefinition.class, RootBeanDefinition.class)
				.addStatement("$T beanFactory = new $T()", DefaultListableBeanFactory.class,
						DefaultListableBeanFactory.class)
				.addCode(codeBlock.get()).addStatement("return beanDefinition").build());
		this.generatedMethods.doWithMethodSpecs(builder::addMethod);
		return JavaFile.builder("com.example", builder.build()).build();
	}

}