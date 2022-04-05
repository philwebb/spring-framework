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

import java.util.Objects;

import org.springframework.aot.context.XTrackedAotProcessors;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.Assert;

/**
 * A unique name associated with a bean defined in a {@link BeanFactory}. Used to
 * disambiguate when an {@link XAotDefinedBeanProcessor} has already been run.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see UniqueBeanFactoryName
 * @see XAotDefinedBeanProcessor
 * @see XTrackedAotProcessors
 */
public final class XUniqueBeanName {

	private final UniqueBeanFactoryName beanFactoryName;

	private final String beanName;

	public UniqueBeanName(UniqueBeanFactoryName beanFactoryName, String beanName) {
		Assert.notNull(beanFactoryName, "'beanFactoryName' must not be null");
		Assert.hasText(beanName, "'beanName' must not be empty");
		this.beanFactoryName = beanFactoryName;
		this.beanName = beanName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.beanFactoryName, this.beanName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		UniqueBeanName other = (UniqueBeanName) obj;
		return Objects.equals(this.beanFactoryName, other.beanFactoryName)
				&& Objects.equals(this.beanName, other.beanName);
	}

	@Override
	public String toString() {
		return this.beanFactoryName + ":" + this.beanName;
	}

}
