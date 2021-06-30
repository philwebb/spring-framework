/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.function;

import org.springframework.aot.GeneratedFile;

/**
 * Factory hook that allows pre-processing of {@link FunctionalBeanDefinition functional
 * bean definitions} ahead-of-time so that useful classes and resources may be added once
 * and used many times.
 * <p>
 * Only bean definitions that have a {@link FunctionalBeanDefinitionIdentifier} can be
 * pre-processed.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public interface FunctionalBeanDefinitionPreProcessor {

	/**
	 * Pre-process the bean definition and add any {@link GeneratedFile generated files}
	 * to the {@link FunctionalBeanDefinitionPreProcessorContext}.
	 * <p>
	 * Pre-processors will often generate a {@link FunctionalBeanRegistrarSupplier} then
	 * {@link FunctionalBeanDefinitionPreProcessorContext#addServiceLoader(Class, String)
	 * register} it as a service loader to be applied later.
	 * <p>
	 * Pre-processors can also register further bean definitions for pre-processing via
	 * the
	 * {@link FunctionalBeanDefinitionPreProcessorContext#registerForPreProcessing(FunctionalBeanDefinition)
	 * context.registerForPreProcessing} method.
	 * @param beanDefinition the bean definition to pre-process
	 * @param context the preprocessor context
	 */
	void preProcessBeanDefinition(FunctionalBeanDefinition<?> beanDefinition,
			FunctionalBeanDefinitionPreProcessorContext context);

}
