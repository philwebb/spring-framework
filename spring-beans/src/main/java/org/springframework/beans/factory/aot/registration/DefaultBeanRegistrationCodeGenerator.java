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

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;

/**
 * Default implementation of {@link BeanRegistrationCode} that should work for most beans.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationCodeGeneratorFactory
 */
public class DefaultBeanRegistrationCodeGenerator extends AbstractBeanRegistrationCodeGenerator {

	public DefaultBeanRegistrationCodeGenerator(RegisteredBean registeredBean, String innerBeanPropertyName,
			BeanRegistrationsCode beanRegistrationsCode) {
		super(registeredBean, innerBeanPropertyName, beanRegistrationsCode);
	}

	@Override
	public CodeBlock generateCode(GenerationContext generationContext) {
		List<MethodReference> postProcessors = getInstancePostProcessorMethodReferences();
		MethodGenerator x = getBeanRegistrationsCode().getMethodGenerator();

		// BeanDefintion = ...
		// setup
		// return

		return CodeBlock.of("// FIXME");
		// FIXME hide other code generators in protected methods
	}

	protected CodeBlock generateBeanDefinitionPropertiesCode(GenerationContext generationContext) {
		RegisteredBean registeredBean = getRegisteredBean();
		BeanDefinition beanDefintion = registeredBean.getMergedBeanDefinition();
		Predicate<String> attributeFilter = this::isAttributeIncluded;
		MethodGenerator methodGenerator = getBeanRegistrationsCode().getMethodGenerator()
				.withName(registeredBean.getBeanName());
		Function<PropertyValue, CodeBlock> propertyValueCodeGenerator = (propertyValue) -> generatePropertyValueCode(
				generationContext, propertyValue);
		return generateBeanDefinitionPropertiesCode(beanDefintion, attributeFilter, methodGenerator,
				propertyValueCodeGenerator);
	}

	protected CodeBlock generatePropertyValueCode(GenerationContext generationContext, PropertyValue propertyValue) {
		RegisteredBean innerRegisteredBean = getInnerRegisteredBean(propertyValue.getValue());
		if (innerRegisteredBean != null) {
			MethodReference generatedMethod = getBeanRegistrationsCode().getInnerBeanDefinitionMethodGenerator()
					.generateInnerBeanDefinitionMethod(generationContext, innerRegisteredBean, propertyValue.getName());
			return generatedMethod.toInvokeCodeBlock();
		}
		return null;
	}

	@Nullable
	private RegisteredBean getInnerRegisteredBean(Object value) {
		if (value instanceof BeanDefinitionHolder beanDefinitionHolder) {
			return RegisteredBean.ofInnerBean(getRegisteredBean(), beanDefinitionHolder);
		}
		if (value instanceof BeanDefinition beanDefinition) {
			return RegisteredBean.ofInnerBean(getRegisteredBean(), beanDefinition);
		}
		return null;
	}

	protected boolean isAttributeIncluded(String attributeName) {
		return true;
	}

	protected final CodeBlock generateBeanDefinitionPropertiesCode(BeanDefinition beanDefinition,
			Predicate<String> attributeFilter, MethodGenerator methodGenerator,
			Function<PropertyValue, CodeBlock> propertyValueCodeGenerator) {
		return new BeanDefinitionPropertiesCodeGenerator(beanDefinition, attributeFilter, methodGenerator,
				propertyValueCodeGenerator).generateCode();
	}

}
