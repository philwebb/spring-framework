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

package org.springframework.beans.factory.aot.registration;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Internal code generator to set {@link RootBeanDefinition} properties.
 * <p>
 * Generates code in the following form:<blockquote><pre class="code">
 * beanDefinition.setPrimary(true);
 * beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
 * ...
 * </pre></blockquote>
 * <p>
 * The generated code expects the following variables to be available:
 * <p>
 * <ul>
 * <li>{@code beanDefinition} - The {@link RootBeanDefinition} to configure.</li>
 * </ul>
 * <p>
 * Note that this generator does <b>not</b> set the {@link InstanceSupplier}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class BeanDefinitionPropertiesCodeGenerator {

	private static final RootBeanDefinition DEFAULT_BEAN_DEFINITION = new RootBeanDefinition();

	private static final String BEAN_DEFINITION_VARIABLE = BeanRegistrationCodeFragments.BEAN_DEFINITION_VARIABLE;

	private final RuntimeHints hints;

	private final Predicate<String> attributeFilter;

	private final BiFunction<String, Object, CodeBlock> customValueCodeGenerator;

	private final BeanDefinitionPropertyValueCodeGenerator valueCodeGenerator;

	BeanDefinitionPropertiesCodeGenerator(RuntimeHints hints, Predicate<String> attributeFilter,
			MethodGenerator methodGenerator, BiFunction<String, Object, CodeBlock> customValueCodeGenerator) {
		this.hints = hints;
		this.attributeFilter = attributeFilter;
		this.customValueCodeGenerator = customValueCodeGenerator;
		this.valueCodeGenerator = new BeanDefinitionPropertyValueCodeGenerator(methodGenerator);
	}

	CodeBlock generateCode(BeanDefinition beanDefinition) {
		CodeBlock.Builder builder = CodeBlock.builder();
		addStatementForValue(builder, beanDefinition, BeanDefinition::isPrimary, "$L.setPrimary($L)");
		addStatementForValue(builder, beanDefinition, BeanDefinition::getScope, this::hasScope, "$L.setScope($S)");
		addStatementForValue(builder, beanDefinition, BeanDefinition::getDependsOn, this::hasDependsOn,
				"$L.setDependsOn($L)", this::toStringVarArgs);
		addStatementForValue(builder, beanDefinition, BeanDefinition::isAutowireCandidate,
				"$L.setAutowireCandidate($L)");
		addStatementForValue(builder, beanDefinition, BeanDefinition::getRole, this::hasRole, "$L.setRole($L)",
				this::toRole);
		if (beanDefinition instanceof AbstractBeanDefinition abstractBeanDefinition) {
			addStatementForValue(builder, beanDefinition, AbstractBeanDefinition::getLazyInit, "$L.setLazyInit($L)");
			addStatementForValue(builder, beanDefinition, AbstractBeanDefinition::isSynthetic, "$L.setSynthetic($L)");
			addInitDestroyMethods(builder, abstractBeanDefinition, abstractBeanDefinition.getInitMethodNames(),
					"$L.setInitMethodNames($L)");
			addInitDestroyMethods(builder, abstractBeanDefinition, abstractBeanDefinition.getDestroyMethodNames(),
					"$L.setDestroyMethodNames($L)");
		}
		addConstructorArgumentValues(builder, beanDefinition);
		addPropertyValues(builder, beanDefinition);
		addAttributes(builder, beanDefinition);
		return builder.build();
	}

	private void addInitDestroyMethods(Builder builder, AbstractBeanDefinition beanDefinition, String[] methodNames,
			String format) {
		if (!ObjectUtils.isEmpty(methodNames)) {
			Class<?> beanUserClass = ClassUtils.getUserClass(beanDefinition.getResolvableType().toClass());
			Builder arguments = CodeBlock.builder();
			for (int i = 0; i < methodNames.length; i++) {
				String methodName = methodNames[i];
				if (!AbstractBeanDefinition.INFER_METHOD.equals(methodName)) {
					arguments.add((i != 0) ? ", $S" : "$S", methodName);
					addInitDestroyHint(beanUserClass, methodName);
				}
			}
			builder.addStatement(format, BEAN_DEFINITION_VARIABLE, arguments.build());
		}
	}

	private void addInitDestroyHint(Class<?> beanUserClass, String methodName) {
		Method method = ReflectionUtils.findMethod(beanUserClass, methodName);
		if (method != null) {
			this.hints.reflection().registerMethod(method);
		}
	}

	private void addConstructorArgumentValues(CodeBlock.Builder builder, BeanDefinition beanDefinition) {
		Map<Integer, ValueHolder> argumentValues = beanDefinition.getConstructorArgumentValues()
				.getIndexedArgumentValues();
		if (!argumentValues.isEmpty()) {
			argumentValues.forEach((index, valueHolder) -> {
				String name = valueHolder.getName();
				Object value = valueHolder.getValue();
				CodeBlock code = this.customValueCodeGenerator.apply(name, value);
				if (code == null) {
					code = this.valueCodeGenerator.generateCode(value);
				}
				builder.addStatement("$L.getConstructorArgumentValues().addIndexedArgumentValue($L, $L)",
						BEAN_DEFINITION_VARIABLE, index, code);
			});
		}
	}

	private void addPropertyValues(CodeBlock.Builder builder, BeanDefinition beanDefinition) {
		MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
		if (!propertyValues.isEmpty()) {
			for (PropertyValue propertyValue : propertyValues) {
				String name = propertyValue.getName();
				Object value = propertyValue.getValue();
				CodeBlock code = this.customValueCodeGenerator.apply(name, value);
				if (code == null) {
					code = this.valueCodeGenerator.generateCode(value);
				}
				builder.addStatement("$L.getPropertyValues().addPropertyValue($S, $L)", BEAN_DEFINITION_VARIABLE,
						propertyValue.getName(), code);
			}
		}
	}

	private void addAttributes(CodeBlock.Builder builder, BeanDefinition beanDefinition) {
		String[] attributeNames = beanDefinition.attributeNames();
		if (!ObjectUtils.isEmpty(attributeNames)) {
			for (String attributeName : attributeNames) {
				if (this.attributeFilter.test(attributeName)) {
					CodeBlock value = this.valueCodeGenerator
							.generateCode(beanDefinition.getAttribute(attributeName));
					builder.addStatement("$L.setAttribute($S, $L)", BEAN_DEFINITION_VARIABLE, attributeName, value);
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
		return switch (value) {
			case BeanDefinition.ROLE_INFRASTRUCTURE -> CodeBlock.builder()
					.add("$T.ROLE_INFRASTRUCTURE", BeanDefinition.class).build();
			case BeanDefinition.ROLE_SUPPORT -> CodeBlock.builder().add("$T.ROLE_SUPPORT", BeanDefinition.class).build();
			default -> value;
		};
	}

	private <B extends BeanDefinition, T> void addStatementForValue(CodeBlock.Builder builder,
			BeanDefinition beanDefinition, Function<B, T> getter, String format) {
		addStatementForValue(builder, beanDefinition, getter,
				(defaultValue, actualValue) -> !Objects.equals(defaultValue, actualValue), format);
	}

	private <B extends BeanDefinition, T> void addStatementForValue(CodeBlock.Builder builder,
			BeanDefinition beanDefinition, Function<B, T> getter, BiPredicate<T, T> filter, String format) {
		addStatementForValue(builder, beanDefinition, getter, filter, format, actualValue -> actualValue);
	}

	@SuppressWarnings("unchecked")
	private <B extends BeanDefinition, T> void addStatementForValue(CodeBlock.Builder builder,
			BeanDefinition beanDefinition, Function<B, T> getter, BiPredicate<T, T> filter, String format,
			Function<T, Object> formatter) {
		T defaultValue = getter.apply((B) DEFAULT_BEAN_DEFINITION);
		T actualValue = getter.apply((B) beanDefinition);
		if (filter.test(defaultValue, actualValue)) {
			builder.addStatement(format, BEAN_DEFINITION_VARIABLE, formatter.apply(actualValue));
		}
	}




}