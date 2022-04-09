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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Internal factory used to create {@link BeanRegistrationMethodGenerator} instances.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationMethodGenerator
 */
class BeanRegistrationMethodGeneratorFactory {

	private static final Log logger = LogFactory.getLog(BeanRegistrationMethodGeneratorFactory.class);

	private final List<BeanRegistrationAotProcessor> aotProcessors;

	private final List<BeanRegistrationExcludeFilter> excludeFilters;

	BeanRegistrationMethodGeneratorFactory(ConfigurableListableBeanFactory beanFactory) {
		this(new AotFactoriesLoader(beanFactory));
	}

	BeanRegistrationMethodGeneratorFactory(AotFactoriesLoader loader) {
		this.aotProcessors = loader.load(BeanRegistrationAotProcessor.class);
		this.excludeFilters = loader.load(BeanRegistrationExcludeFilter.class);
	}

	@Nullable
	BeanRegistrationMethodGenerator getBeanRegistrationMethodGenerator(RegisteredBean registeredBean) {
		if (isExcluded(registeredBean)) {
			return null;
		}
		List<BeanRegistrationAotContribution> contributions = getAotContributions(registeredBean);
		return new BeanRegistrationMethodGenerator(this, registeredBean, contributions);
	}

	private boolean isExcluded(RegisteredBean registeredBean) {
		for (BeanRegistrationExcludeFilter excludeFilter : this.excludeFilters) {
			if (excludeFilter.isExcluded(registeredBean)) {
				logger.trace(LogMessage.format("Excluding registered bean '%s' from bean factory %s due to %s",
						registeredBean.getBeanName(), ObjectUtils.identityToString(registeredBean.getBeanFactory()),
						excludeFilter.getClass().getName()));
				return true;
			}
		}
		return false;
	}

	private List<BeanRegistrationAotContribution> getAotContributions(RegisteredBean registeredBean) {
		String beanName = registeredBean.getBeanName();
		List<BeanRegistrationAotContribution> contributions = new ArrayList<>();
		for (BeanRegistrationAotProcessor aotProcessor : this.aotProcessors) {
			BeanRegistrationAotContribution contribution = aotProcessor.processAheadOfTime(registeredBean);
			if (contribution != null) {
				logger.trace(LogMessage.format("Adding bean registration AOT contribution %S from %S to '%S'",
						contribution.getClass().getName(), aotProcessor.getClass().getName(), beanName));
				contributions.add(contribution);
			}
		}
		return contributions;
	}

}
