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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.context.AotContribution;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.log.LogMessage;

/**
 * {@link AotBeanFactoryProcessor} that contributes code to register beans.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class BeanRegistrationsAotBeanFactoryProcessor implements AotBeanFactoryProcessor {

	private static final Log logger = LogFactory.getLog(BeanRegistrationsAotBeanFactoryProcessor.class);

	private Map<ConfigurableListableBeanFactory, AotDefinedBeanExcludeFilters> aotDefinedBeanExcludeFilters = new HashMap<>();

	private Map<ConfigurableListableBeanFactory, BeanRegistrationCodeProviders> beanRegistrationCodeProviders = new HashMap<>();

	@Override
	public AotContribution processAheadOfTime(UniqueBeanFactoryName beanFactoryName,
			ConfigurableListableBeanFactory beanFactory) {
		logger.trace(LogMessage.format("Generating bean registrations contribution for '%s'", beanFactoryName));
		AotDefinedBeanExcludeFilters excludeFilters = this.aotDefinedBeanExcludeFilters.computeIfAbsent(beanFactory,
				AotDefinedBeanExcludeFilters::new);
		BeanRegistrationCodeProviders registrationProviders = this.beanRegistrationCodeProviders
				.computeIfAbsent(beanFactory, BeanRegistrationCodeProviders::new);
		Map<DefinedBean, BeanRegistrationCode> registrations = new LinkedHashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			DefinedBean definedBean = new DefaultDefinedBean(beanFactoryName, beanFactory, beanName);
			if (excludeFilters.isExcluded(definedBean)) {
				logger.trace(LogMessage.format("Excluded '%s' from AOT registration and processing", beanName));
				continue;
			}
			BeanRegistrationCode registration = registrationProviders.getBeanRegistrationCode(definedBean);
			logger.trace(
					LogMessage.format("Adding registration %s for '%s'", registration.getClass().getName(), beanName));
			registrations.put(definedBean, registration);
		}
		if (registrations.isEmpty()) {
			logger.trace(LogMessage.format("No registration contribution for '%s'", beanFactoryName));
		}
		logger.trace(
				LogMessage.format("Contributing %s registrations for '%s'", registrations.size(), beanFactoryName));
		return new BeanRegistrationsContribution(beanFactoryName, beanFactory, registrations);
	}

}
