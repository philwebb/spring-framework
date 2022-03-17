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

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionPropertiesCodeGenerator}.
 */
@Nested
class BeanDefinitionPropertiesCodeGeneratorTests {

	// FIXME inner bean definition on

	private RootBeanDefinition beanDefinition = new RootBeanDefinition();

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
		BeanDefinitionPropertiesCodeGenerator code = new BeanDefinitionPropertiesCodeGenerator(this.beanDefinition,
				"bd", attribute -> "a".equals(attribute));
		testCompiledResult(code, (actual, compiled) -> {
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

	private void testCompiledResult(RootBeanDefinition bd, BiConsumer<RootBeanDefinition, Compiled> result) {
		testCompiledResult(new BeanDefinitionPropertiesCodeGenerator(bd, "bd"), result);
	}

	private void testCompiledResult(BeanDefinitionPropertiesCodeGenerator code,
			BiConsumer<RootBeanDefinition, Compiled> result) {
		JavaFile javaFile = createJavaFile(code);
		TestCompiler.forSystem().compile(javaFile::writeTo, (compiled) -> {
			RootBeanDefinition beanDefinition = (RootBeanDefinition) compiled.getInstance(Supplier.class).get();
			result.accept(beanDefinition, compiled);
		});
	}

	private JavaFile createJavaFile(BeanDefinitionPropertiesCodeGenerator code) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("BeanSupplier");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(ParameterizedTypeName.get(Supplier.class, RootBeanDefinition.class));
		builder.addMethod(
				MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC).returns(RootBeanDefinition.class)
						.addStatement("$T bd = new $T()", RootBeanDefinition.class, RootBeanDefinition.class)
						.addCode(code.getCodeBlock()).addStatement("return bd").build());
		return JavaFile.builder("com.example", builder.build()).build();
	}

}