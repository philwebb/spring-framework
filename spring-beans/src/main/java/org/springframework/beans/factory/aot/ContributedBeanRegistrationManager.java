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

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Internal helper class used to create {@link ContributedBeanRegistration} instances.
 * Takes care of {@link BeanRegistrationAotProcessor processing},
 * {@link BeanRegistrationExcludeFilter filtering} and
 * {@link BeanRegistrationCodeGeneratorFactory code generation} concerns.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see ContributedBeanRegistration
 */
class ContributedBeanRegistrationManager {

	private static final Log logger = LogFactory.getLog(ContributedBeanRegistrationManager.class);

	private final List<BeanRegistrationAotProcessor> aotProcessors;

	private final List<BeanRegistrationExcludeFilter> excludeFilters;

	private final List<BeanRegistrationCodeGeneratorFactory> codeGeneratorFactories;

	ContributedBeanRegistrationManager(ConfigurableListableBeanFactory beanFactory) {
		this(beanFactory, SpringFactoriesLoader.forResourceLocation("META-INF/spring/aot.factories"));
	}

	ContributedBeanRegistrationManager(ListableBeanFactory beanFactory, SpringFactoriesLoader factoriesLoader) {
		this.aotProcessors = load(beanFactory, factoriesLoader, BeanRegistrationAotProcessor.class);
		this.excludeFilters = load(beanFactory, factoriesLoader, BeanRegistrationExcludeFilter.class);
		this.codeGeneratorFactories = load(beanFactory, factoriesLoader, BeanRegistrationCodeGeneratorFactory.class);
	}

	static <T> List<T> load(ListableBeanFactory beanFactory, SpringFactoriesLoader factoriesLoader, Class<T> type) {
		List<T> result = new ArrayList<>();
		result.addAll(BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, type).values());
		result.addAll(factoriesLoader.load(type));
		AnnotationAwareOrderComparator.sort(result);
		return Collections.unmodifiableList(result);
	}

	@Nullable
	ContributedBeanRegistration getContributedBeanRegistration(RegisteredBean registeredBean) {
		if (isExcluded(registeredBean)) {
			return null;
		}
		List<BeanRegistrationAotContribution> contributions = getAotContributions(registeredBean);
		BeanRegistrationCodeGenerator codeGenerator = getCodeGenerator(registeredBean);
		return new ContributedBeanRegistration(registeredBean, contributions, codeGenerator);
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
		for (BeanRegistrationAotProcessor aotProcessor : aotProcessors) {
			BeanRegistrationAotContribution contribution = aotProcessor.processAheadOfTime(registeredBean);
			if (contribution != null) {
				logger.trace(LogMessage.format("Adding bean registration AOT contribution %S from %S to '%S'",
						contribution.getClass().getName(), aotProcessor.getClass().getName(), beanName));
				contributions.add(contribution);
			}
		}
		return contributions;
	}

	private BeanRegistrationCodeGenerator getCodeGenerator(RegisteredBean registeredBean) {
		InnerBeanRegistrationMethodGenerator innerBeanRegistrationMethodGenerator = this::generateInnerBeanRegistrationMethod;
		for (BeanRegistrationCodeGeneratorFactory codeGeneratorFactory : codeGeneratorFactories) {
			BeanRegistrationCodeGenerator codeGenerator = codeGeneratorFactory
					.getBeanRegistrationCodeGenerator(registeredBean, innerBeanRegistrationMethodGenerator);
			if (codeGenerator != null) {
				logger.trace(LogMessage.format("Using bean registration code generator %S for '%S'",
						codeGenerator.getClass().getName(), registeredBean.getBeanName()));
				return codeGenerator;
			}
		}
		return new DefaultBeanRegistrationCodeGenerator(registeredBean);
	}

	private MethodReference generateInnerBeanRegistrationMethod(GenerationContext generationContext,
			RegisteredBean innerRegisteredBean) {
		return getContributedBeanRegistration(innerRegisteredBean).generateRegistrationMethod(generationContext);
	}

}
