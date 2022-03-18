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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.instance.CollectionInstanceCodeGenerator;
import org.springframework.aot.generate.instance.DefaultInstanceCodeGenerationService;
import org.springframework.aot.generate.instance.InstanceCodeGenerationService;
import org.springframework.aot.generate.instance.InstanceCodeGenerator;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Code generator set {@link RootBeanDefinition} properties.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class BeanDefinitionPropertiesCodeGenerator {

	private static final RootBeanDefinition DEFAULT_BEAN_DEFINITON = new RootBeanDefinition();

	private static final String DEFAULT_VARIABLE = "beanDefinition";

	private final GeneratedMethods generatedMethods;

	private final InstanceCodeGenerationService instanceCodeGenerationService;

	BeanDefinitionPropertiesCodeGenerator(GeneratedMethods generatedMethods) {
		this.generatedMethods=generatedMethods;
		this.instanceCodeGenerationService = createInstanceCodeGenerationService(generatedMethods);
	}

	private InstanceCodeGenerationService createInstanceCodeGenerationService(GeneratedMethods generatedMethods) {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(generatedMethods);
		service.add(new BeanReferenceInstanceCodeGenerator());
		service.add(new CollectionInstanceCodeGenerator<>(service, ManagedList.class,
				CodeBlock.of("new $T()", ManagedList.class)));
		service.add(new CollectionInstanceCodeGenerator<>(service, ManagedSet.class,
				CodeBlock.of("new $T()", ManagedSet.class)));
		service.add(new ManagedMapInstanceCodeGenerator(service));
		return service;
	}

	CodeBlock getCodeBlock(@Nullable String beanName, RootBeanDefinition beanDefinition) {
		return getCodeBlock(beanName, beanDefinition, (attribute) -> false, null);
	}

	CodeBlock getCodeBlock(@Nullable String beanName, RootBeanDefinition beanDefinition,
			Predicate<String> attributeFilter) {
		return getCodeBlock(beanName, beanDefinition, attributeFilter, null);
	}

	CodeBlock getCodeBlock(@Nullable String beanName, RootBeanDefinition beanDefinition,
			Predicate<String> attributeFilter, @Nullable String variable) {
		return new PropertiesCodeBlockBuilder(beanDefinition, variable, attributeFilter).build();
	}

	private class PropertiesCodeBlockBuilder {

		private final RootBeanDefinition beanDefinition;

		private final String variable;

		private final Predicate<String> attributeFilter;

		PropertiesCodeBlockBuilder(RootBeanDefinition beanDefinition, String variable,
				Predicate<String> attributeFilter) {
			this.beanDefinition = beanDefinition;
			this.variable = (variable != null) ? variable : DEFAULT_VARIABLE;
			this.attributeFilter = attributeFilter;
		}

		CodeBlock build() {
			CodeBlock.Builder builder = CodeBlock.builder();
			addStatementForValue(builder, RootBeanDefinition::isPrimary, "$L.setPrimary($L)");
			addStatementForValue(builder, RootBeanDefinition::getScope, this::hasScope, "$L.setScope($S)");
			addStatementForValue(builder, RootBeanDefinition::getDependsOn, this::hasDependsOn, "$L.setDependsOn($L)",
					this::toStringVarArgs);
			addStatementForValue(builder, RootBeanDefinition::getLazyInit, "$L.setLazyInit($L)");
			addStatementForValue(builder, RootBeanDefinition::isAutowireCandidate, "$L.setAutowireCandidate($L)");
			addStatementForValue(builder, RootBeanDefinition::isSynthetic, "$L.setSynthetic($L)");
			addStatementForValue(builder, RootBeanDefinition::getRole, this::hasRole, "$L.setRole($L)", this::toRole);
			addConstructorArgumentValues(builder);
			addPropertyValues(builder);
			addAttributes(builder);
			return builder.build();
		}

		private void addConstructorArgumentValues(CodeBlock.Builder builder) {
			Map<Integer, ValueHolder> argumentValues = this.beanDefinition.getConstructorArgumentValues()
					.getIndexedArgumentValues();
			if (!argumentValues.isEmpty()) {
				argumentValues.forEach((index, valueHolder) -> {
					CodeBlock value = BeanDefinitionPropertiesCodeGenerator.this.instanceCodeGenerationService
							.generateCode(valueHolder.getValue());
					builder.addStatement("$L.getConstructorArgumentValues().addIndexedArgumentValue($L, $L)",
							this.variable, index, value);
				});
			}
		}

		private void addPropertyValues(CodeBlock.Builder builder) {
			MutablePropertyValues propertyValues = this.beanDefinition.getPropertyValues();
			if (!propertyValues.isEmpty()) {
				for (PropertyValue propertyValue : propertyValues) {
					CodeBlock value = BeanDefinitionPropertiesCodeGenerator.this.instanceCodeGenerationService
							.generateCode(propertyValue.getValue());
					builder.addStatement("$L.getPropertyValues().addPropertyValue($S, $L)", this.variable,
							propertyValue.getName(), value);
				}
			}
		}

		private void addAttributes(CodeBlock.Builder builder) {
			String[] attributeNames = this.beanDefinition.attributeNames();
			if (!ObjectUtils.isEmpty(attributeNames)) {
				for (String attributeName : attributeNames) {
					if (this.attributeFilter.test(attributeName)) {
						CodeBlock value = BeanDefinitionPropertiesCodeGenerator.this.instanceCodeGenerationService
								.generateCode(this.beanDefinition.getAttribute(attributeName));
						builder.addStatement("$L.setAttribute($S, $L)", this.variable, attributeName, value);
					}
				}
			}
		}

		private boolean hasScope(String defaultValue, String actualValue) {
			return StringUtils.hasText(actualValue) && !ConfigurableBeanFactory.SCOPE_SINGLETON.equals(actualValue);
		}

		private boolean hasDependsOn(String[] defaultValue, String[] actualValue) {
			return !ObjectUtils.isEmpty(actualValue);
		}

		private boolean hasRole(int defaultValue, int actualValue) {
			return actualValue != BeanDefinition.ROLE_APPLICATION;
		}

		private CodeBlock toStringVarArgs(String[] strings) {
			CodeBlock.Builder builder = CodeBlock.builder();
			for (int i = 0; i < strings.length; i++) {
				builder.add((i != 0) ? ", " : "");
				builder.add("$S", strings[i]);
			}
			return builder.build();
		}

		private Object toRole(int value) {
			switch (value) {
			case BeanDefinition.ROLE_INFRASTRUCTURE:
				return CodeBlock.builder().add("$T.ROLE_INFRASTRUCTURE", BeanDefinition.class).build();
			case BeanDefinition.ROLE_SUPPORT:
				return CodeBlock.builder().add("$T.ROLE_SUPPORT", BeanDefinition.class).build();
			}
			return value;
		}

		private <T> void addStatementForValue(CodeBlock.Builder builder, Function<RootBeanDefinition, T> getter,
				String format) {
			addStatementForValue(builder, getter,
					(defaultValue, actualValue) -> !Objects.equals(defaultValue, actualValue), format);
		}

		private <T> void addStatementForValue(CodeBlock.Builder builder, Function<RootBeanDefinition, T> getter,
				BiPredicate<T, T> filter, String format) {
			addStatementForValue(builder, getter, filter, format, actualValue -> actualValue);
		}

		private <T> void addStatementForValue(CodeBlock.Builder builder, Function<RootBeanDefinition, T> getter,
				BiPredicate<T, T> filter, String format, Function<T, Object> formatter) {
			T defaultValue = getter.apply(DEFAULT_BEAN_DEFINITON);
			T actualValue = getter.apply(this.beanDefinition);
			if (filter.test(defaultValue, actualValue)) {
				builder.addStatement(format, this.variable, formatter.apply(actualValue));
			}
		}

	}

	/**
	 * {@link InstanceCodeGenerator} for {@link BeanReference} types.
	 */
	static class BeanReferenceInstanceCodeGenerator implements InstanceCodeGenerator {

		@Override
		public CodeBlock generateCode(String name, Object value, ResolvableType type) {
			if (value instanceof BeanReference beanReference) {
				return CodeBlock.of("new $T($S)", RuntimeBeanReference.class, beanReference.getBeanName());
			}
			return null;
		}

	}

	/**
	 * {@link InstanceCodeGenerator} for {@link ManagedMap} types.
	 */
	static class ManagedMapInstanceCodeGenerator implements InstanceCodeGenerator {

		private static final CodeBlock EMPTY_RESULT = CodeBlock.of("$T.ofEntries()", ManagedMap.class);

		private final InstanceCodeGenerationService codeGenerationService;

		ManagedMapInstanceCodeGenerator(InstanceCodeGenerationService codeGenerationService) {
			this.codeGenerationService = codeGenerationService;
		}

		@Override
		public CodeBlock generateCode(String name, Object value, ResolvableType type) {
			if (value instanceof ManagedMap<?, ?> managedMap) {
				return generateMapCode(name, type, managedMap);
			}
			return null;
		}

		private <K, V> CodeBlock generateMapCode(String name, ResolvableType type, ManagedMap<K, V> managedMap) {
			if (managedMap.isEmpty()) {
				return EMPTY_RESULT;
			}
			ResolvableType keyType = type.as(Map.class).getGeneric(0);
			ResolvableType valueType = type.as(Map.class).getGeneric(1);
			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("$T.ofEntries(", ManagedMap.class);
			Iterator<Map.Entry<K, V>> iterator = managedMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<?, ?> entry = iterator.next();
				CodeBlock keyCode = this.codeGenerationService.generateCode(name, entry.getKey(), keyType);
				CodeBlock valueCode = this.codeGenerationService.generateCode(name, entry.getValue(), valueType);
				builder.add("$T.entry($L,$L)", Map.class, keyCode, valueCode);
				builder.add((!iterator.hasNext()) ? "" : ", ");
			}
			builder.add(")");
			return builder.build();
		}

	}

	class BeanDefinitionInstanceCodeGenerator implements InstanceCodeGenerator {

		public BeanDefinitionInstanceCodeGenerator(@Nullable String beanName) {
		}

		@Override
		public CodeBlock generateCode(String name, Object value, ResolvableType type) {
			if (type instanceof BeanDefinition beanDefinition) {

			}
			return null;
		}

	}

}