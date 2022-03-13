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
 * A managed collection of {@link DefinedBeanAotExcludeFilter} instances.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class DefinedBeanAotExcludeFilters {

	private static final Log logger = LogFactory.getLog(DefinedBeanAotExcludeFilters.class);

	private final List<DefinedBeanAotExcludeFilter> filters;

	/**
	 * Create a new {@link DefinedBeanAotExcludeFilters} instance, obtaining filters using
	 * the default {@link SpringFactoriesLoader} and the given {@link BeanFactory}.
	 * @param beanFactory the bean factory to use
	 */
	public DefinedBeanAotExcludeFilters(ConfigurableListableBeanFactory beanFactory) {
		this(SpringFactoriesLoader.forDefaultResourceLocation(), beanFactory);
	}

	/**
	 * Create a new {@link DefinedBeanAotExcludeFilters} instance, obtaining filters using
	 * the given {@link SpringFactoriesLoader} and {@link BeanFactory}.
	 * @param springFactoriesLoader the factories loader to use
	 * @param beanFactory the bean factory to use
	 * @param
	 */
	public DefinedBeanAotExcludeFilters(SpringFactoriesLoader springFactoriesLoader,
			ConfigurableListableBeanFactory beanFactory) {
		List<DefinedBeanAotExcludeFilter> filters = new ArrayList<>();
		filters.addAll(springFactoriesLoader.load(DefinedBeanAotExcludeFilter.class));
		filters.addAll(BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, DefinedBeanAotExcludeFilter.class)
				.values());
		AnnotationAwareOrderComparator.sort(filters);
		this.filters = Collections.unmodifiableList(filters);
	}

	public boolean isExcluded(DefinedBean definedBean) {
		for (DefinedBeanAotExcludeFilter filter : this.filters) {
			if (filter.isExcluded(definedBean)) {
				logger.trace(LogMessage.format("Excluding DefinedBean '%s' from AOT due to %s",
						definedBean.getUniqueBeanName(), filter.getClass().getName()));
				return true;
			}
		}
		return false;
	}

}
