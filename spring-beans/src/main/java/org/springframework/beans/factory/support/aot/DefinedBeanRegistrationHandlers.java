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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.aot.DefinedBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.generate.BeanRegistrationMethodCodeGenerator;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;

/**
 * A managed collection of {@link DefinedBeanRegistrationHandler} instances.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class DefinedBeanRegistrationHandlers {

	private static final Log logger = LogFactory.getLog(DefinedBeanRegistrationHandlers.class);

	private final List<DefinedBeanRegistrationHandler> handlers;

	/**
	 * Create a new {@link DefinedBeanRegistrationHandlers} instance, obtaining handlers
	 * using the default {@link SpringFactoriesLoader} and the given {@link BeanFactory}.
	 * @param beanFactory the bean factory to use
	 */
	DefinedBeanRegistrationHandlers(ConfigurableListableBeanFactory beanFactory) {
		this(SpringFactoriesLoader.forDefaultResourceLocation(), beanFactory);
	}

	/**
	 * Create a new {@link DefinedBeanRegistrationHandlers} instance, obtaining handlers
	 * using the given {@link SpringFactoriesLoader} and {@link BeanFactory}.
	 * @param springFactoriesLoader the factories loader to use
	 * @param beanFactory the bean factory to use
	 */
	DefinedBeanRegistrationHandlers(SpringFactoriesLoader springFactoriesLoader,
			ConfigurableListableBeanFactory beanFactory) {
		List<DefinedBeanRegistrationHandler> providers = new ArrayList<>();
		providers.addAll(springFactoriesLoader.load(DefinedBeanRegistrationHandler.class));
		providers.addAll(BeanFactoryUtils
				.beansOfTypeIncludingAncestors(beanFactory, DefinedBeanRegistrationHandler.class).values());
		AnnotationAwareOrderComparator.sort(providers);
		this.handlers = Collections.unmodifiableList(providers);
	}

	/**
	 * Return the {@link BeanRegistrationMethodCodeGenerator} that should be used for the
	 * defined bean.
	 * @param definedBean the bean to check
	 * @return a {@link BeanRegistrationMethodCodeGenerator} instance
	 */
	public DefinedBeanRegistrationHandler getHandler(DefinedBean definedBean) {
		for (DefinedBeanRegistrationHandler handler : this.handlers) {
			if (handler.canHandle(definedBean)) {
				logger.trace(LogMessage.format("Using DefinedBeanRegistrationHandler %s for '%s'",
						handler.getClass().getName(), definedBean.getUniqueBeanName()));
				return handler;
			}
		}
		logger.trace(LogMessage.format("Returning default DefinedBeanRegistrationHandler for '%s'",
				definedBean.getUniqueBeanName()));
		return DefaultDefinedBeanRegistrationHandler.INSTANCE;
	}

	static final class DefaultDefinedBeanRegistrationHandler implements DefinedBeanRegistrationHandler {

		static final DefinedBeanRegistrationHandler INSTANCE = new DefaultDefinedBeanRegistrationHandler();

		@Override
		public boolean canHandle(DefinedBean definedBean) {
			return true;
		}

		@Override
		@Nullable
		public BeanRegistrationMethodCodeGenerator getBeanRegistrationMethodCodeGenerator(DefinedBean definedBean) {
			return null;
		}

	}

}
