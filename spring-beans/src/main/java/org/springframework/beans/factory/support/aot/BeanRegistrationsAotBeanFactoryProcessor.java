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

import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.context.XAotContribution;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.aot.AotBeanClassProcessor;
import org.springframework.beans.factory.aot.XAotBeanFactoryProcessor;
import org.springframework.beans.factory.aot.XAotDefinedBeanProcessor;
import org.springframework.beans.factory.aot.XDefinedBean;
import org.springframework.beans.factory.aot.DefinedBeanExcludeFilters;
import org.springframework.beans.factory.aot.UniqueBeanFactoryName;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.generate.BeanRegistrationMethodCodeGenerator;
import org.springframework.beans.factory.support.generate.BeanRegistrationsJavaFileGenerator;
import org.springframework.beans.factory.support.generate.DefaultBeanRegistrationMethodCodeGenerator;
import org.springframework.core.log.LogMessage;

/**
 * {@link XAotBeanFactoryProcessor} that contributes code to register beans.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class BeanRegistrationsAotBeanFactoryProcessor implements XAotBeanFactoryProcessor {

	private static final Log logger = LogFactory.getLog(BeanRegistrationsAotBeanFactoryProcessor.class);

	private final Function<ConfigurableListableBeanFactory, DefinedBeanExcludeFilters> excludeFiltersFactory;

	private final Function<ConfigurableListableBeanFactory, DefinedBeanRegistrationHandlers> registrationHandlersFactory;

	private final Map<ConfigurableListableBeanFactory, DefinedBeanExcludeFilters> excludeFilters = new HashMap<>();

	private final Map<ConfigurableListableBeanFactory, DefinedBeanRegistrationHandlers> registrationHandlers = new HashMap<>();

	private final Map<ConfigurableListableBeanFactory, Collection<XAotDefinedBeanProcessor>> aotDefinedBeanProcessors = new HashMap<>();

	private final Map<ConfigurableListableBeanFactory, Collection<AotBeanClassProcessor>> aotBeanClassProcessors = new HashMap<>();

	public BeanRegistrationsAotBeanFactoryProcessor() {
		this(DefinedBeanExcludeFilters::new, DefinedBeanRegistrationHandlers::new);
	}

	BeanRegistrationsAotBeanFactoryProcessor(
			Function<ConfigurableListableBeanFactory, DefinedBeanExcludeFilters> excludeFiltersFactory,
			Function<ConfigurableListableBeanFactory, DefinedBeanRegistrationHandlers> registrationHandlersFactory) {
		this.excludeFiltersFactory = excludeFiltersFactory;
		this.registrationHandlersFactory = registrationHandlersFactory;
	}

	@Override
	public XAotContribution processAheadOfTime(UniqueBeanFactoryName beanFactoryName,
			ConfigurableListableBeanFactory beanFactory) {
		logger.trace(LogMessage.format("Generating bean registrations for bean factory'%s'", beanFactoryName));
		Map<XDefinedBean, DefinedBeanRegistrationHandler> handlerMap = getHandlerMap(beanFactoryName, beanFactory);
		if (handlerMap.isEmpty()) {
			logger.trace(LogMessage.format("No registration contribution for '%s'", beanFactoryName));
			return null;
		}
		logger.trace(LogMessage.format("Contributing %s %s for '%s'", handlerMap.size(),
				((handlerMap.size() != 1) ? "registrations" : "registration"), beanFactoryName));
		BeanRegistrationsJavaFileGenerator javaFileGenerator = getJavaFileGenerator(beanFactory, handlerMap);
		Collection<XAotDefinedBeanProcessor> aotDefinedBeanProcessors = this.aotDefinedBeanProcessors
				.computeIfAbsent(beanFactory, this::getAotDefinedBeanProcessors);
		Collection<AotBeanClassProcessor> aotBeanClassProcessors = this.aotBeanClassProcessors
				.computeIfAbsent(beanFactory, this::getAotBeanClassProcessors);
		return new BeanRegistrationsContribution(beanFactoryName, handlerMap.keySet(), javaFileGenerator,
				aotDefinedBeanProcessors, aotBeanClassProcessors);
	}

	private Map<XDefinedBean, DefinedBeanRegistrationHandler> getHandlerMap(UniqueBeanFactoryName beanFactoryName,
			ConfigurableListableBeanFactory beanFactory) {
		DefinedBeanExcludeFilters excludeFilters = getExcludeFilters(beanFactory);
		DefinedBeanRegistrationHandlers registrationHandlers = getRegistrationHandlers(beanFactory);
		Map<XDefinedBean, DefinedBeanRegistrationHandler> handlerMap = new LinkedHashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			XDefinedBean definedBean = new XDefinedBean(beanFactory, beanFactoryName, beanName);
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

	private DefinedBeanExcludeFilters getExcludeFilters(ConfigurableListableBeanFactory beanFactory) {
		return this.excludeFilters.computeIfAbsent(beanFactory,
				this.excludeFiltersFactory);
	}

	private DefinedBeanRegistrationHandlers getRegistrationHandlers(ConfigurableListableBeanFactory beanFactory) {
		return this.registrationHandlers.computeIfAbsent(beanFactory, this.registrationHandlersFactory);
	}


	private BeanRegistrationsJavaFileGenerator getJavaFileGenerator(ConfigurableBeanFactory beanFactory,
			Map<XDefinedBean, DefinedBeanRegistrationHandler> handlerMap) {
		Function<BeanDefinition, Executable> constructorOrFactoryMethodResolver = new ConstructorOrFactoryMethodResolver(
				beanFactory);
		Map<String, BeanRegistrationMethodCodeGenerator> methodCodeGenerators = new LinkedHashMap<>();
		handlerMap.forEach((definedBean, handler) -> {
			String beanName = definedBean.getBeanName();
			BeanRegistrationMethodCodeGenerator methodCodeGenerator = handler
					.getBeanRegistrationMethodCodeGenerator(definedBean);
			BeanDefinition beanDefinition = definedBean.getMergedBeanDefinition();
			if (methodCodeGenerator == null) {
				methodCodeGenerator = new DefaultBeanRegistrationMethodCodeGenerator(beanName, beanDefinition,
						constructorOrFactoryMethodResolver);
			}
			methodCodeGenerators.put(beanName, methodCodeGenerator);
		});
		return new BeanRegistrationsJavaFileGenerator(methodCodeGenerators);
	}

	private Collection<XAotDefinedBeanProcessor> getAotDefinedBeanProcessors(
			ConfigurableListableBeanFactory beanFactory) {
		Class<XAotDefinedBeanProcessor> type = XAotDefinedBeanProcessor.class;
		return beansOfTypeIncludingAncestors(beanFactory, type);
	}

	private Collection<AotBeanClassProcessor> getAotBeanClassProcessors(ConfigurableListableBeanFactory beanFactory) {
		return beansOfTypeIncludingAncestors(beanFactory, AotBeanClassProcessor.class);
	}

	private <T> Collection<T> beansOfTypeIncludingAncestors(ConfigurableListableBeanFactory beanFactory,
			Class<T> type) {
		Map<String, T> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, type);
		return Collections.unmodifiableCollection(beans.values());
	}

}