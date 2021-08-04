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

package org.springframework.beans.factory.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Default implementation of a {@link XBeanContainer}.
 *
 * @author Phillip Webb
 * @since 6.0.0
 */
public abstract class XDefaultBeanContainer implements XBeanContainer {

	private final Map<String, FunctionalBean<?>> beans = new ConcurrentHashMap<>();

	@Override
	public <T> void register(FunctionBeanDefinition<T> registration,
			BeanCondition... conditions) {
		if (conditionsMatch(conditions)) {
			this.beans.compute(registration.getName(), (name, existingBean) -> {
				if (existingBean != null) {
					attemptBeanOverride(existingBean, registration);
				}
				return new FunctionalBean<>(this, registration);
			});
		}
	}

	private <T> void attemptBeanOverride(FunctionalBean<?> existingBean,
			FunctionBeanDefinition<T> registration) {
		throw new IllegalStateException("Beans cannot be overriden");
	}

	@Override
	public void registerFrom(Supplier<FunctionalBeanRegistrar> registrar,
			BeanCondition... conditions) {
		if (conditionsMatch(conditions)) {
			registrar.get().apply(this);
		}
	}

	private boolean conditionsMatch(BeanCondition[] conditions) {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> BeanSelection<T> select(BeanSelector<T> selector) {
		List<FunctionalBean<T>> selection = new ArrayList<>();
		for (FunctionalBean<?> bean : this.beans.values()) {
			if (selector.test(bean.getRegistration())) {
				selection.add((FunctionalBean<T>) bean);
			}
		}
		return new XDefaultBeanSelection<>(selection);
	}

}
