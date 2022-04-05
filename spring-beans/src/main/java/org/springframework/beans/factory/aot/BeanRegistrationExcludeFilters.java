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
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A managed collection of {@link BeanRegistrationExcludeFilter} instances.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class BeanRegistrationExcludeFilters {

	private static final Log logger = LogFactory.getLog(BeanRegistrationExcludeFilters.class);

	private final List<BeanRegistrationExcludeFilter> filters;

	/**
	 * Create a new {@link DefinedBeanExcludeFilters} instance, obtaining filters using
	 * the default {@link SpringFactoriesLoader} and the given {@link BeanFactory}.
	 * @param beanFactory the bean factory to use
	 */
	BeanRegistrationExcludeFilters(ConfigurableListableBeanFactory beanFactory) {
		this(SpringFactoriesLoader.forDefaultResourceLocation(), beanFactory);
	}

	/**
	 * Create a new {@link DefinedBeanExcludeFilters} instance, obtaining filters using
	 * the given {@link SpringFactoriesLoader} and {@link BeanFactory}.
	 * @param springFactoriesLoader the factories loader to use
	 * @param beanFactory the bean factory to use
	 */
	BeanRegistrationExcludeFilters(SpringFactoriesLoader springFactoriesLoader,
			ConfigurableListableBeanFactory beanFactory) {
		Assert.notNull(springFactoriesLoader, "'springFactoriesLoader' must not be null");
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		List<BeanRegistrationExcludeFilter> filters = new ArrayList<>();
		filters.addAll(springFactoriesLoader.load(BeanRegistrationExcludeFilter.class));
		filters.addAll(BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, BeanRegistrationExcludeFilter.class)
				.values());
		AnnotationAwareOrderComparator.sort(filters);
		this.filters = Collections.unmodifiableList(filters);
	}

	boolean isExcluded(RegisteredBean registeredBean) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		for (BeanRegistrationExcludeFilter filter : this.filters) {
			if (filter.isExcluded(registeredBean)) {
				logger.trace(LogMessage.format("Excluding registered bean '%s' from bean factory %s due to %s",
						registeredBean.getBeanName(), ObjectUtils.identityToString(registeredBean.getBeanFactory()),
						filter.getClass().getName()));
				return true;
			}
		}
		return false;
	}

}
