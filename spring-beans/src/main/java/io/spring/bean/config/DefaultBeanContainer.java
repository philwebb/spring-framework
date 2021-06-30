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

package io.spring.bean.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Default implementation of a {@link BeanContainer}.
 *
 * @author Phillip Webb
 * @since 6.0.0
 */
public class DefaultBeanContainer implements BeanContainer {

	private final Map<String, Bean<?>> beans = new ConcurrentHashMap<>();

	@Override
	public <T> void register(BeanRegistration<T> registration,
			BeanCondition... conditions) {
		if (conditionsMatch(conditions)) {
			this.beans.compute(registration.getName(), (name, existingBean) -> {
				if (existingBean != null) {
					attemptBeanOverride(existingBean, registration);
				}
				return new Bean<>(this, registration);
			});
		}
	}

	private <T> void attemptBeanOverride(Bean<?> existingBean,
			BeanRegistration<T> registration) {
		throw new IllegalStateException("Beans cannot be overriden");
	}

	@Override
	public void registerFrom(Supplier<BeanRegistrar> registrar,
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
		List<Bean<T>> selection = new ArrayList<>();
		for (Bean<?> bean : this.beans.values()) {
			if (selector.test(bean.getRegistration())) {
				selection.add((Bean<T>) bean);
			}
		}
		return new DefaultBeanSelection<>(selection);
	}

}
