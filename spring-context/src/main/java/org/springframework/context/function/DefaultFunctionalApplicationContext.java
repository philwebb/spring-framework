/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.function;

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.function.DefaultFunctionalBeanFactory;
import org.springframework.beans.factory.function.FunctionalBeanFactory;
import org.springframework.beans.factory.function.FunctionalBeanRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Default {@link FunctionalApplicationContext} implementation.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class DefaultFunctionalApplicationContext
		extends AbstractFunctionalApplicationContext {

	private final String id = ObjectUtils.identityToString(this);

	private final String displayName = ObjectUtils.identityToString(this);

	private long startupDate;

	private final FunctionalApplicationContext parent = null;

	private final FunctionalBeanFactory beanFactory;

	private final StandardEnvironment environment = new StandardEnvironment();

	private final MessageSource messageSource = new StaticMessageSource();

	private final ApplicationEventPublisher eventPublisher = null;

	private final PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();

	public DefaultFunctionalApplicationContext(FunctionalBeanRegistrar... registrars) {
		this.beanFactory = new DefaultFunctionalBeanFactory(registrars);
	}

	public DefaultFunctionalApplicationContext(FunctionalBeanFactory beanFactory) {
		Assert.isInstanceOf(HierarchicalBeanFactory.class, beanFactory);
		this.beanFactory = beanFactory;
	}

	@Override
	public FunctionalBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getApplicationName() {
		return "";
	}

	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	@Override
	public long getStartupDate() {
		return this.startupDate;
	}

	@Override
	public ApplicationContext getParent() {
		return this.parent;
	}

	@Override
	public Environment getEnvironment() {
		return this.environment;
	}

	@Override
	protected MessageSource getMessageSource() {
		return this.messageSource;
	}

	@Override
	protected ApplicationEventPublisher getApplicationEventPublisher() {
		return this.eventPublisher;
	}

	@Override
	protected ResourcePatternResolver getResourcePatternResolver() {
		return this.pathMatchingResourcePatternResolver;
	}

}
