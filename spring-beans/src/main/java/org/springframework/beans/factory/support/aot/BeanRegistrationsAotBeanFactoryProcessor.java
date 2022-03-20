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

package org.springframework.beans.factory.support.aot;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.context.AotContribution;
import org.springframework.beans.factory.aot.AotBeanFactoryProcessor;
import org.springframework.beans.factory.aot.DefinedBean;
import org.springframework.beans.factory.aot.DefinedBeanExcludeFilters;
import org.springframework.beans.factory.aot.UniqueBeanFactoryName;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.generate.BeanRegistrationMethodCodeGenerator;
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

	private final Map<ConfigurableListableBeanFactory, DefinedBeanExcludeFilters> excludeFilters = new HashMap<>();

	private final Map<ConfigurableListableBeanFactory, DefinedBeanRegistrationHandlers> registrationHandlers = new HashMap<>();

	@Override
	public AotContribution processAheadOfTime(UniqueBeanFactoryName beanFactoryName,
			ConfigurableListableBeanFactory beanFactory) {
		logger.trace(LogMessage.format("Generating bean registrations for bean factory'%s'", beanFactoryName));
		Map<DefinedBean, DefinedBeanRegistrationHandler> handlerMap = getHandlerMap(beanFactoryName, beanFactory);
		if (handlerMap.isEmpty()) {
			logger.trace(LogMessage.format("No registration contribution for '%s'", beanFactoryName));
			return null;
		}
		logger.trace(LogMessage.format("Contributing %s %s for '%s'", handlerMap.size(),
				((handlerMap.size() != 1) ? "registrations" : "registration"), beanFactoryName));
		return new BeanRegistrationsContribution(beanFactoryName, beanFactory, handlerMap);
	}

	private Map<DefinedBean, DefinedBeanRegistrationHandler> getHandlerMap(UniqueBeanFactoryName beanFactoryName,
			ConfigurableListableBeanFactory beanFactory) {
		DefinedBeanExcludeFilters excludeFilters = this.excludeFilters.computeIfAbsent(beanFactory,
				DefinedBeanExcludeFilters::new);
		DefinedBeanRegistrationHandlers registrationHandlers = this.registrationHandlers.computeIfAbsent(beanFactory,
				DefinedBeanRegistrationHandlers::new);
		Map<DefinedBean, DefinedBeanRegistrationHandler> handlerMap = new LinkedHashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			DefinedBean definedBean = new DefinedBean(beanFactory, beanFactoryName, beanName);
			if (excludeFilters.isExcluded(definedBean)) {
				logger.trace(LogMessage.format("Excluded '%s' from AOT registration and processing", beanName));
				continue;
			}
			DefinedBeanRegistrationHandler handler = registrationHandlers.getHandler(definedBean);
			logger.trace(LogMessage.format("Registering bean named '%s' using handler '%s'", beanName,
					handler.getClass().getName()));
			handlerMap.put(definedBean, handler);
		}
		return handlerMap;
	}

}
