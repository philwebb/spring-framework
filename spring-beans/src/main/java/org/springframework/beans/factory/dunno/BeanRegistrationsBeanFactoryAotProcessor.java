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

package org.springframework.beans.factory.dunno;

import org.springframework.aot.context.TrackedAotProcessors;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.util.MultiValueMap;

/**
 * {@link BeanFactoryAotProcessor} that generates bean registration code.
 *
 * @author pwebb
 * @since 6.0
 */
public class BeanRegistrationsBeanFactoryAotProcessor implements BeanFactoryAotProcessor {

	// FIXME see BeanRegistrationsAotBeanFactoryProcessor

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {

		TrackedAotProcessors aotProcessors = new TrackedAotProcessors(null, null);


		MultiValueMap<RegisteredBean, BeanRegistrationAotContribution> dunno;
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			RegisteredBean registeredBean = new RegisteredBean(beanName, beanFactory);
			// for all RegisteredBeanAotProcessor dunno.add(registeredBean, null);
			// for add BeanClassAotProcessor
			RegisteredBeanAotProcessor processor = null;
			BeanRegistrationAotContribution contribution = processor.processAheadOfTime(registeredBean);
			// add
		}
		return new BeanRegistrationsBeanFactoryInitializationAotContribution();
	}

}
