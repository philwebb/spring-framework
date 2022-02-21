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

package org.springframework.context.generator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.generator.BeanRegistrationGenerator;
import org.springframework.beans.factory.generator.BeanRegistrationGeneratorProvider;
import org.springframework.beans.factory.generator.DefaultBeanRegistrationGeneratorProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.javapoet.CodeBlock;

/**
 * An {@link ApplicationContextInitializationContributor} that generates the bean definitions
 * of a bean factory, using {@link BeanRegistrationGeneratorProvider} to use
 * appropriate customizations if necessary.
 *
 * <p>{@link BeanRegistrationGeneratorProvider} can be ordered, with the default
 * implementation always coming last.
 *
 * @author Stephane Nicoll
 */
@Order(0)
class BeanDefinitionsContributor implements ApplicationContextInitializationContributor {

	private final DefaultListableBeanFactory beanFactory;

	private final List<BeanRegistrationGeneratorProvider> generatorProviders;

	BeanDefinitionsContributor(DefaultListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.generatorProviders = initializeProviders(beanFactory);
	}

	private static List<BeanRegistrationGeneratorProvider> initializeProviders(DefaultListableBeanFactory beanFactory) {
		List<BeanRegistrationGeneratorProvider> providers = new ArrayList<>(SpringFactoriesLoader.loadFactories(
				BeanRegistrationGeneratorProvider.class, beanFactory.getBeanClassLoader()));
		providers.add(new DefaultBeanRegistrationGeneratorProvider(beanFactory));
		return providers;
	}

	@Override
	public CodeBlock contribute(GeneratedTypeContext generationContext) {
		return writeBeanDefinitions(generationContext);
	}

	private CodeBlock writeBeanDefinitions(GeneratedTypeContext generationContext) {
		CodeBlock.Builder code = CodeBlock.builder();
		for (String beanName : this.beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition(beanName);
			try {
				if (!isExcluded(beanName, beanDefinition)) {
					BeanRegistrationGenerator generator = getBeanRegistrationGenerator(
							beanName, beanDefinition);
					code.add(generator.generateBeanRegistration(generationContext));
				}
			}
			catch (Exception ex) {
				String msg = String.format("Failed to handle bean with name '%s' and type '%s'",
						beanName, beanDefinition.getResolvableType());
				throw new BeanDefinitionGenerationException(beanName, beanDefinition, msg, ex);
			}
		}
		return code.build();
	}

	// TODO: need to figure out how to get this around
	private boolean isExcluded(String beanName, BeanDefinition beanDefinition) {
		return false;
	}

	private BeanRegistrationGenerator getBeanRegistrationGenerator(String beanName, BeanDefinition beanDefinition) {
		for (BeanRegistrationGeneratorProvider provider : this.generatorProviders) {
			BeanRegistrationGenerator generator = provider.getBeanRegistrationGenerator(beanName, beanDefinition);
			if (generator != null) {
				return generator;
			}
		}
		throw new BeanRegistrationGeneratorNotFoundException(beanName, beanDefinition);
	}

}
