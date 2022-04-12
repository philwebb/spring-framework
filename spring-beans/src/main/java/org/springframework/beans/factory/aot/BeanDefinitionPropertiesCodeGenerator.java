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

package org.springframework.beans.factory.aot;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.instance.CollectionInstanceCodeGenerator;
import org.springframework.aot.generate.instance.DefaultInstanceCodeGenerationService;
import org.springframework.aot.generate.instance.DefaultInstanceCodeGenerationService.InstanceCodeGenerators;
import org.springframework.aot.generate.instance.InstanceCodeGenerationService;
import org.springframework.aot.generate.instance.InstanceCodeGenerator;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ObjectUtils;
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

	private static final RootBeanDefinition DEFAULT_BEAN_DEFINITON = new RootBeanDefinition();

	private static final String BEAN_DEFINITION_VARIABLE = BeanRegistrationCodeGenerator.BEAN_DEFINITION_VARIABLE;

	private final BeanDefinition beanDefinition;

	private final Predicate<String> attributeFilter;

	private final Function<PropertyValue, CodeBlock> propertyValueCodeGenerator;

	private final DefaultInstanceCodeGenerationService instanceCodeGenerationService;

	BeanDefinitionPropertiesCodeGenerator(BeanDefinition beanDefinition, Predicate<String> attributeFilter,
			MethodGenerator methodGenerator, Function<PropertyValue, CodeBlock> propertyValueCodeGenerator) {
		this.beanDefinition = beanDefinition;
		this.attributeFilter = attributeFilter;
		this.propertyValueCodeGenerator = propertyValueCodeGenerator;
		this.instanceCodeGenerationService = new DefaultInstanceCodeGenerationService(null, methodGenerator,
				this::addInstanceCodeGenerators);
	}

	private void addInstanceCodeGenerators(InstanceCodeGenerators instanceCodeGenerators) {
		instanceCodeGenerators.add(BeanReferenceInstanceCodeGenerator.INSTANCE);
		instanceCodeGenerators.add(ManagedListInstanceCodeGenerator.INSTANCE);
		instanceCodeGenerators.add(ManagedSetInstanceCodeGenerator.INSTANCE);
		instanceCodeGenerators.add(ManagedMapInstanceCodeGenerator.INSTANCE);
		instanceCodeGenerators.addDefaults();
	}

	CodeBlock generateCode() {
		CodeBlock.Builder builder = CodeBlock.builder();
		addStatementForValue(builder, BeanDefinition::isPrimary, "$L.setPrimary($L)");
		addStatementForValue(builder, BeanDefinition::getScope, this::hasScope, "$L.setScope($S)");
		addStatementForValue(builder, BeanDefinition::getDependsOn, this::hasDependsOn, "$L.setDependsOn($L)",
				this::toStringVarArgs);
		addStatementForValue(builder, BeanDefinition::isAutowireCandidate, "$L.setAutowireCandidate($L)");
		addStatementForValue(builder, BeanDefinition::getRole, this::hasRole, "$L.setRole($L)", this::toRole);
		if (this.beanDefinition instanceof AbstractBeanDefinition) {
			addStatementForValue(builder, AbstractBeanDefinition::getLazyInit, "$L.setLazyInit($L)");
			addStatementForValue(builder, AbstractBeanDefinition::isSynthetic, "$L.setSynthetic($L)");
		}
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
				CodeBlock value = this.instanceCodeGenerationService.generateCode(valueHolder.getValue());
				builder.addStatement("$L.getConstructorArgumentValues().addIndexedArgumentValue($L, $L)",
						BEAN_DEFINITION_VARIABLE, index, value);
			});
		}
	}

	private void addPropertyValues(CodeBlock.Builder builder) {
		MutablePropertyValues propertyValues = this.beanDefinition.getPropertyValues();
		if (!propertyValues.isEmpty()) {
			for (PropertyValue propertyValue : propertyValues) {
				CodeBlock code = this.propertyValueCodeGenerator.apply(propertyValue);
				if (code == null) {
					code = this.instanceCodeGenerationService.generateCode(propertyValue.getValue());
				}
				builder.addStatement("$L.getPropertyValues().addPropertyValue($S, $L)", BEAN_DEFINITION_VARIABLE,
						propertyValue.getName(), code);
			}
		}
	}

	private void addAttributes(CodeBlock.Builder builder) {
		String[] attributeNames = this.beanDefinition.attributeNames();
		if (!ObjectUtils.isEmpty(attributeNames)) {
			for (String attributeName : attributeNames) {
				if (this.attributeFilter.test(attributeName)) {
					CodeBlock value = this.instanceCodeGenerationService
							.generateCode(this.beanDefinition.getAttribute(attributeName));
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

	private <B extends BeanDefinition, T> void addStatementForValue(CodeBlock.Builder builder, Function<B, T> getter,
			String format) {
		addStatementForValue(builder, getter, (defaultValue, actualValue) -> !Objects.equals(defaultValue, actualValue),
				format);
	}

	private <B extends BeanDefinition, T> void addStatementForValue(CodeBlock.Builder builder, Function<B, T> getter,
			BiPredicate<T, T> filter, String format) {
		addStatementForValue(builder, getter, filter, format, actualValue -> actualValue);
	}

	@SuppressWarnings("unchecked")
	private <B extends BeanDefinition, T> void addStatementForValue(CodeBlock.Builder builder, Function<B, T> getter,
			BiPredicate<T, T> filter, String format, Function<T, Object> formatter) {
		T defaultValue = getter.apply((B) DEFAULT_BEAN_DEFINITON);
		T actualValue = getter.apply((B) this.beanDefinition);
		if (filter.test(defaultValue, actualValue)) {
			builder.addStatement(format, BEAN_DEFINITION_VARIABLE, formatter.apply(actualValue));
		}
	}

	/**
	 * {@link InstanceCodeGenerator} for {@link BeanReference} types.
	 */
	static class BeanReferenceInstanceCodeGenerator implements InstanceCodeGenerator {

		public static final BeanReferenceInstanceCodeGenerator INSTANCE = new BeanReferenceInstanceCodeGenerator();

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type, InstanceCodeGenerationService service) {
			if (value instanceof BeanReference beanReference) {
				return CodeBlock.of("new $T($S)", RuntimeBeanReference.class, beanReference.getBeanName());
			}
			return null;
		}

	}

	/**
	 * {@link InstanceCodeGenerator} for {@link ManagedList} types.
	 */
	static class ManagedListInstanceCodeGenerator extends CollectionInstanceCodeGenerator<ManagedList<?>> {

		static final ManagedListInstanceCodeGenerator INSTANCE = new ManagedListInstanceCodeGenerator();

		public ManagedListInstanceCodeGenerator() {
			super(ManagedList.class, CodeBlock.of("new $T()", ManagedList.class));
		}
	}

	/**
	 * {@link InstanceCodeGenerator} for {@link ManagedSet} types.
	 */
	static class ManagedSetInstanceCodeGenerator extends CollectionInstanceCodeGenerator<ManagedSet<?>> {

		static final ManagedSetInstanceCodeGenerator INSTANCE = new ManagedSetInstanceCodeGenerator();

		public ManagedSetInstanceCodeGenerator() {
			super(ManagedSet.class, CodeBlock.of("new $T()", ManagedSet.class));
		}

	}

	/**
	 * {@link InstanceCodeGenerator} for {@link ManagedMap} types.
	 */
	static class ManagedMapInstanceCodeGenerator implements InstanceCodeGenerator {

		static final ManagedMapInstanceCodeGenerator INSTANCE = new ManagedMapInstanceCodeGenerator();

		private static final CodeBlock EMPTY_RESULT = CodeBlock.of("$T.ofEntries()", ManagedMap.class);

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type, InstanceCodeGenerationService service) {
			if (value instanceof ManagedMap<?, ?> managedMap) {
				return generateManagedMapCode(type, service, managedMap);
			}
			return null;
		}

		private <K, V> CodeBlock generateManagedMapCode(ResolvableType type, InstanceCodeGenerationService service,
				ManagedMap<K, V> managedMap) {
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
				CodeBlock keyCode = service.generateCode(entry.getKey(), keyType);
				CodeBlock valueCode = service.generateCode(entry.getValue(), valueType);
				builder.add("$T.entry($L,$L)", Map.class, keyCode, valueCode);
				builder.add((!iterator.hasNext()) ? "" : ", ");
			}
			builder.add(")");
			return builder.build();
		}

	}

}
