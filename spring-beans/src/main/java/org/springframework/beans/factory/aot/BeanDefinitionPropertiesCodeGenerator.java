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

import java.lang.reflect.Executable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.MethodNameGenerator;
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
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Code generator to set {@link RootBeanDefinition} properties.
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
 * <li>{@code beanFactory} - The {@link DefaultListableBeanFactory} used for
 * injection.</li>
 * </ul>
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class BeanDefinitionPropertiesCodeGenerator {

	static final String BEAN_DEFINITION_VARIABLE = "beanDefinition";

	static final String BEAN_FACTORY_VARIABLE = InstanceSupplierCodeGenerator.BEAN_FACTORY_VARIABLE;

	private static final RootBeanDefinition DEFAULT_BEAN_DEFINITON = new RootBeanDefinition();

	private final DefaultInstanceCodeGenerationService instanceCodeGenerationService;

	private final InstanceSupplierCodeGenerator suppliedInstanceBeanDefinitionCodeGenerator;

	private final GeneratedMethods generatedMethods;

	/**
	 * Create a new {@link BeanDefinitionPropertiesCodeGenerator} instance.
	 * @param generatedMethods the generated methods
	 * @param constructorOrFactoryMethodResolver resolver used to find the constructor or
	 * factory method for a bean definition
	 */
	BeanDefinitionPropertiesCodeGenerator(GeneratedMethods generatedMethods,
			Function<BeanDefinition, Executable> constructorOrFactoryMethodResolver) {
		this.instanceCodeGenerationService = createInstanceCodeGenerationService(generatedMethods);
		this.suppliedInstanceBeanDefinitionCodeGenerator = new InstanceSupplierCodeGenerator(
				generatedMethods, constructorOrFactoryMethodResolver);
		this.generatedMethods = generatedMethods;
	}

	private DefaultInstanceCodeGenerationService createInstanceCodeGenerationService(
			GeneratedMethods generatedMethods) {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(generatedMethods);
		service.add(BeanReferenceInstanceCodeGenerator.INSTANCE);
		service.add(ManagedListInstanceCodeGenerator.INSTANCE);
		service.add(ManagedSetInstanceCodeGenerator.INSTANCE);
		service.add(ManagedMapInstanceCodeGenerator.INSTANCE);
		return service;
	}

	/**
	 * Generate code to set the {@link RootBeanDefinition} properties.
	 * @param beanDefinition the source bean definition
	 * @param name a name that can be used when generating new methods. The bean name
	 * should be used unless generating source for an inner-beans.
	 * @return the generated code
	 */
	CodeBlock generateCode(BeanDefinition beanDefinition, String name) {
		return generateCode(beanDefinition, name, attribute -> false);
	}

	/**
	 * Generate code to set the {@link RootBeanDefinition} properties.
	 * @param beanDefinition the source bean definition
	 * @param name a name that can be used when generating new methods. The bean name
	 * should be used unless generating source for an inner-beans
	 * @param attributeFilter a predicate that can be used to filter attributes by their
	 * name
	 * @return the generated code
	 */
	CodeBlock generateCode(BeanDefinition beanDefinition, String name, Predicate<String> attributeFilter) {
		return new CodeBuilder(beanDefinition, name, attributeFilter).build();
	}

	/**
	 * Builder used to create the {@link CodeBlock}.
	 */
	private class CodeBuilder {

		private final BeanDefinition beanDefinition;

		private final String name;

		private final Predicate<String> attributeFilter;

		CodeBuilder(BeanDefinition beanDefinition, String name, Predicate<String> attributeFilter) {
			this.beanDefinition = beanDefinition;
			this.name = name;
			this.attributeFilter = attributeFilter;
		}

		CodeBlock build() {
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
					CodeBlock value = BeanDefinitionPropertiesCodeGenerator.this.instanceCodeGenerationService
							.generateCode(valueHolder.getValue());
					builder.addStatement("$L.getConstructorArgumentValues().addIndexedArgumentValue($L, $L)",
							BEAN_DEFINITION_VARIABLE, index, value);
				});
			}
		}

		private void addPropertyValues(CodeBlock.Builder builder) {
			DefaultInstanceCodeGenerationService instanceCodeGenerationService = new DefaultInstanceCodeGenerationService(
					BeanDefinitionPropertiesCodeGenerator.this.instanceCodeGenerationService);
			instanceCodeGenerationService.add(new BeanDefinitionInstanceCodeGenerator(this.name));
			MutablePropertyValues propertyValues = this.beanDefinition.getPropertyValues();
			if (!propertyValues.isEmpty()) {
				for (PropertyValue propertyValue : propertyValues) {
					Object value = propertyValue.getValue();
					CodeBlock code = instanceCodeGenerationService.generateCode(propertyValue.getName(),
							value);
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
						CodeBlock value = BeanDefinitionPropertiesCodeGenerator.this.instanceCodeGenerationService
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
				case BeanDefinition.ROLE_INFRASTRUCTURE -> CodeBlock.builder().add("$T.ROLE_INFRASTRUCTURE", BeanDefinition.class).build();
				case BeanDefinition.ROLE_SUPPORT -> CodeBlock.builder().add("$T.ROLE_SUPPORT", BeanDefinition.class).build();
				default -> value;
			};
		}

		private <B extends BeanDefinition, T> void addStatementForValue(CodeBlock.Builder builder,
				Function<B, T> getter, String format) {
			addStatementForValue(builder, getter,
					(defaultValue, actualValue) -> !Objects.equals(defaultValue, actualValue), format);
		}

		private <B extends BeanDefinition, T> void addStatementForValue(CodeBlock.Builder builder,
				Function<B, T> getter, BiPredicate<T, T> filter, String format) {
			addStatementForValue(builder, getter, filter, format, actualValue -> actualValue);
		}

		@SuppressWarnings("unchecked")
		private <B extends BeanDefinition, T> void addStatementForValue(CodeBlock.Builder builder,
				Function<B, T> getter, BiPredicate<T, T> filter, String format, Function<T, Object> formatter) {
			T defaultValue = getter.apply((B) DEFAULT_BEAN_DEFINITON);
			T actualValue = getter.apply((B) this.beanDefinition);
			if (filter.test(defaultValue, actualValue)) {
				builder.addStatement(format, BEAN_DEFINITION_VARIABLE, formatter.apply(actualValue));
			}
		}

	}

	/**
	 * {@link InstanceCodeGenerator} for {@link BeanReference} types.
	 */
	static class BeanReferenceInstanceCodeGenerator implements InstanceCodeGenerator {

		public static final BeanReferenceInstanceCodeGenerator INSTANCE = new BeanReferenceInstanceCodeGenerator();

		@Override
		public CodeBlock generateCode(String name, Object value, ResolvableType type,
				InstanceCodeGenerationService service) {
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
		public CodeBlock generateCode(String name, Object value, ResolvableType type,
				InstanceCodeGenerationService service) {
			if (value instanceof ManagedMap<?, ?> managedMap) {
				return generateManagedMapCode(name, type, service, managedMap);
			}
			return null;
		}

		private <K, V> CodeBlock generateManagedMapCode(String name, ResolvableType type,
				InstanceCodeGenerationService service, ManagedMap<K, V> managedMap) {
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
				CodeBlock keyCode = service.generateCode(name, entry.getKey(), keyType);
				CodeBlock valueCode = service.generateCode(name, entry.getValue(), valueType);
				builder.add("$T.entry($L,$L)", Map.class, keyCode, valueCode);
				builder.add((!iterator.hasNext()) ? "" : ", ");
			}
			builder.add(")");
			return builder.build();
		}

	}

	/**
	 * {@link InstanceCodeGenerator} for inner {@link BeanDefinition} types.
	 */
	class BeanDefinitionInstanceCodeGenerator implements InstanceCodeGenerator {

		private final String name;

		public BeanDefinitionInstanceCodeGenerator(String name) {
			this.name = name;
		}

		@Override
		public CodeBlock generateCode(String name, Object value, ResolvableType type,
				InstanceCodeGenerationService service) {
			if (value instanceof BeanDefinition beanDefinition) {
				GeneratedMethod generatedMethod = BeanDefinitionPropertiesCodeGenerator.this.generatedMethods
						.add("get", this.name, name).generateBy(builder -> {
							builder.addJavadoc("Get the bean instance for '$L' ('$L').", this.name, name);
							builder.addModifiers(Modifier.PRIVATE);
							builder.addParameter(DefaultListableBeanFactory.class, BEAN_FACTORY_VARIABLE);
							builder.returns(BeanDefinition.class);
							builder.addCode(getMethodCode(name, beanDefinition));
						});
				return CodeBlock.of("$L($L)", generatedMethod.getName(), BEAN_FACTORY_VARIABLE);
			}
			return null;
		}

		private CodeBlock getMethodCode(String name, BeanDefinition beanDefinition) {
			CodeBlock.Builder builder = CodeBlock.builder();
			String compoundName = MethodNameGenerator.join(this.name, name);
			builder.add("$T beanDefinition = ", RootBeanDefinition.class);
			builder.addStatement(BeanDefinitionPropertiesCodeGenerator.this.suppliedInstanceBeanDefinitionCodeGenerator
					.generateCode(beanDefinition, compoundName));
			builder.add(BeanDefinitionPropertiesCodeGenerator.this.generateCode(beanDefinition, compoundName));
			builder.addStatement("return beanDefinition");
			return builder.build();
		}

	}

}
