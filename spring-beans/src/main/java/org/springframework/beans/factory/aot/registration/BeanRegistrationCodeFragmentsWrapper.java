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

import java.lang.reflect.Executable;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link BeanRegistrationCodeFragments} implementations that
 * decorate another code fragments generator.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public abstract class BeanRegistrationCodeFragmentsWrapper implements BeanRegistrationCodeFragments {

	private final BeanRegistrationCodeFragments codeFragments;

	protected BeanRegistrationCodeFragmentsWrapper(BeanRegistrationCodeFragments codeFragments) {
		Assert.notNull(codeFragments, "'codeFragments' must not be null");
		this.codeFragments = codeFragments;
	}

	@Override
	public Class<?> getTarget(RegisteredBean registeredBean, Executable constructorOrFactoryMethod) {
		return this.codeFragments.getTarget(registeredBean, constructorOrFactoryMethod);
	}

	@Override
	public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext, ResolvableType beanType,
			BeanRegistrationCode beanRegistrationCode) {
		return this.codeFragments.generateNewBeanDefinitionCode(generationContext, beanType, beanRegistrationCode);
	}

	@Override
	public CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext,
			BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
			Predicate<String> attributeFilter) {
		return this.codeFragments.generateSetBeanDefinitionPropertiesCode(generationContext, beanRegistrationCode,
				beanDefinition, attributeFilter);
	}

	@Override
	public CodeBlock generateSetBeanInstanceSupplierCode(GenerationContext generationContext,
			BeanRegistrationCode beanRegistrationCode, CodeBlock instanceSupplierCode,
			List<MethodReference> postProcessors) {
		return this.codeFragments.generateSetBeanInstanceSupplierCode(generationContext, beanRegistrationCode,
				instanceSupplierCode, postProcessors);
	}

	@Override
	public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
			BeanRegistrationCode beanRegistrationCode, Executable constructorOrFactoryMethod,
			boolean allowDirectSupplierShortcut) {
		return this.codeFragments.generateInstanceSupplierCode(generationContext, beanRegistrationCode,
				constructorOrFactoryMethod, allowDirectSupplierShortcut);
	}

	@Override
	public CodeBlock generateReturnCode(GenerationContext generationContext,
			BeanRegistrationCode beanRegistrationCode) {
		return this.codeFragments.generateReturnCode(generationContext, beanRegistrationCode);
	}

}
