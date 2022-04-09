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

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;

/**
 * AOT specific factory loading mechanism for internal use within the framework.
 * <p>
 * Loads and instantiates factories of a given type from
 * {@value #FACTORIES_RESOURCE_LOCATION} and merges them with matching beans from a
 * {@link ListableBeanFactory}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see SpringFactoriesLoader
 */
public class AotFactoriesLoader {

	public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring/aot.factories";

	private final ListableBeanFactory beanFactory;

	private final SpringFactoriesLoader factoriesLoader;

	public AotFactoriesLoader(ListableBeanFactory beanFactory) {
		this(beanFactory, SpringFactoriesLoader.forResourceLocation(FACTORIES_RESOURCE_LOCATION));
	}

	public AotFactoriesLoader(ListableBeanFactory beanFactory, SpringFactoriesLoader factoriesLoader) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		Assert.notNull(beanFactory, "'factoriesLoader' must not be null");
		this.beanFactory = beanFactory;
		this.factoriesLoader = factoriesLoader;
	}

	public <T> List<T> load(Class<T> type) {
		List<T> result = new ArrayList<>();
		result.addAll(BeanFactoryUtils.beansOfTypeIncludingAncestors(this.beanFactory, type).values());
		result.addAll(this.factoriesLoader.load(type));
		AnnotationAwareOrderComparator.sort(result);
		return Collections.unmodifiableList(result);
	}

}
