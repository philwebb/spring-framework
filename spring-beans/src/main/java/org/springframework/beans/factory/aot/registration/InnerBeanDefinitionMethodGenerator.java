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

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RegisteredBean;

/**
 * Interface that can be used to generate a method to create an inner-bean.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationsCode#getInnerBeanDefinitionMethodGenerator()
 */
@FunctionalInterface
public interface InnerBeanDefinitionMethodGenerator {

	/**
	 * Generate a new method that will create a {@link BeanDefinition} for the inner-bean.
	 * @param generationContext the generation context
	 * @param innerRegisteredBean the registered inner-bean
	 * @param innerBeanPropertyName the name of the property that defined the registered
	 * inner-bean
	 * @return a reference to the newly generated method
	 */
	MethodReference generateInnerBeanDefinitionMethod(GenerationContext generationContext,
			RegisteredBean innerRegisteredBean, String innerBeanPropertyName);

}