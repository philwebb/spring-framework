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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;

/**
 * A managed collection of {@link BeanRegistrationCodeProvider} instances.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class BeanRegistrationCodeProviders {

	private static final Log logger = LogFactory.getLog(BeanRegistrationCodeProviders.class);

	private final List<BeanRegistrationCodeProvider> providers;

	/**
	 * Create a new {@link BeanRegistrationCodeProviders} instance, obtaining providers
	 * using the default {@link SpringFactoriesLoader} and the given {@link BeanFactory}.
	 * @param beanFactory the bean factory to use
	 */
	BeanRegistrationCodeProviders(ConfigurableListableBeanFactory beanFactory) {
		this(SpringFactoriesLoader.forDefaultResourceLocation(), beanFactory);
	}

	/**
	 * Create a new {@link BeanRegistrationCodeProviders} instance, obtaining providers
	 * using the given {@link SpringFactoriesLoader} and {@link BeanFactory}.
	 * @param springFactoriesLoader the factories loader to use
	 * @param beanFactory the bean factory to use
	 */
	BeanRegistrationCodeProviders(SpringFactoriesLoader springFactoriesLoader,
			ConfigurableListableBeanFactory beanFactory) {
		List<BeanRegistrationCodeProvider> providers = new ArrayList<>();
		providers.addAll(springFactoriesLoader.load(BeanRegistrationCodeProvider.class));
		providers.addAll(BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, BeanRegistrationCodeProvider.class)
				.values());
		AnnotationAwareOrderComparator.sort(providers);
		this.providers = Collections.unmodifiableList(providers);
	}

	/**
	 * Return the {@link BeanRegistrationCode} that should be used for the defined bean.
	 * @param definedBean the bean to check
	 * @return a {@link BeanRegistrationCode} instance
	 */
	BeanRegistrationCode getBeanRegistrationCode(DefinedBean definedBean) {
		for (BeanRegistrationCodeProvider provider : providers) {
			BeanRegistrationCode provided = provider.getBeanRegistrationCode(definedBean);
			if (provided != null) {
				logger.trace(LogMessage.format("Returning BeanRegistrationCode provided by %s for '%s'",
						provider.getClass().getName(), definedBean.getUniqueBeanName()));
				return provided;
			}
		}
		logger.trace(
				LogMessage.format("Returning default BeanRegistrationCode for '%s'", definedBean.getUniqueBeanName()));
		return new DefaultBeanRegistrationCode(definedBean);
	}

}
