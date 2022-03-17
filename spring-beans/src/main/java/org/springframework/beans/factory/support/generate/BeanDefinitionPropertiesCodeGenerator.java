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
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.generator.BeanParameterGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
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

	private final RootBeanDefinition beanDefinition;

	private final String variable;

	private final Predicate<String> attributeFilter;

	protected BeanDefinitionPropertiesCodeGenerator(RootBeanDefinition beanDefinition, String variable) {
		this(beanDefinition, variable, (attribute) -> false);
	}

	protected BeanDefinitionPropertiesCodeGenerator(RootBeanDefinition beanDefinition, String variable,
			Predicate<String> attributeFilter) {
		this.beanDefinition = beanDefinition;
		this.variable = variable;
		this.attributeFilter = attributeFilter;
	}

	protected CodeBlock getCodeBlock() {
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

	private void addConstructorArgumentValues(Builder builder) {
		Map<Integer, ValueHolder> argumentValues = this.beanDefinition.getConstructorArgumentValues()
				.getIndexedArgumentValues();
		if (!argumentValues.isEmpty()) {
			argumentValues.forEach((index, valueHolder) -> {
				CodeBlock value = BeanParameterGenerator.INSTANCE.generateParameterValue(valueHolder.getValue());
				builder.addStatement("$L.getConstructorArgumentValues().addIndexedArgumentValue($L,$L)", this.variable,
						index, value);
			});
		}
	}

	private void addPropertyValues(Builder builder) {
		MutablePropertyValues propertyValues = this.beanDefinition.getPropertyValues();
		if (!propertyValues.isEmpty()) {
			for (PropertyValue propertyValue : propertyValues) {
				CodeBlock value = BeanParameterGenerator.INSTANCE.generateParameterValue(propertyValue.getValue());
				builder.addStatement("$L.getPropertyValues().addPropertyValue($S, $L)", this.variable,
						propertyValue.getName(), value);
			}
		}
	}

	private void addAttributes(Builder builder) {
		String[] attributeNames = this.beanDefinition.attributeNames();
		if (!ObjectUtils.isEmpty(attributeNames)) {
			for (String attributeName : attributeNames) {
				if (this.attributeFilter.test(attributeName)) {
					CodeBlock value = BeanParameterGenerator.INSTANCE
							.generateParameterValue(this.beanDefinition.getAttribute(attributeName));
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
		Builder builder = CodeBlock.builder();
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

	private <T> void addStatementForValue(Builder builder, Function<RootBeanDefinition, T> getter, String format) {
		addStatementForValue(builder, getter, (defaultValue, actualValue) -> !Objects.equals(defaultValue, actualValue),
				format);
	}

	private <T> void addStatementForValue(Builder builder, Function<RootBeanDefinition, T> getter,
			BiPredicate<T, T> filter, String format) {
		addStatementForValue(builder, getter, filter, format, actualValue -> actualValue);
	}

	private <T> void addStatementForValue(Builder builder, Function<RootBeanDefinition, T> getter,
			BiPredicate<T, T> filter, String format, Function<T, Object> formatter) {
		T defaultValue = getter.apply(DEFAULT_BEAN_DEFINITON);
		T actualValue = getter.apply(this.beanDefinition);
		if (filter.test(defaultValue, actualValue)) {
			builder.addStatement(format, this.variable, formatter.apply(actualValue));
		}
	}

}