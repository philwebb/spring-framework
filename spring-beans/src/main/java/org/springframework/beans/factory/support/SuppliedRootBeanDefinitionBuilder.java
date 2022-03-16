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

package org.springframework.beans.factory.support;

import java.lang.reflect.Executable;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.generator.config.BeanDefinitionRegistrar.ThrowableFunction;
import org.springframework.beans.factory.generator.config.BeanDefinitionRegistrar.ThrowableSupplier;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 *
 * @author pwebb
 * @since 6.0
 */
public class SuppliedRootBeanDefinitionBuilder {

	SuppliedRootBeanDefinitionBuilder(@Nullable String name, Class<?> type) {
	}

	SuppliedRootBeanDefinitionBuilder(@Nullable String name, ResolvableType type) {
	}

	public Using usingConstructor(Class<?> declaringClass, Class<?>... parameterTypes) {
		return null;
	}

	public Using usingFactoryMethod(Class<?> declaringClass, String name, Class<?>... parameterTypes) {
		return null;
	}

	public static class Using {

		private final Executable executable;

		Using(Executable executable) {
			this.executable = executable;
		}

		RootBeanDefinition suppliedBy(ThrowableSupplier<Object> instantiator) {
			return null;
		}
		RootBeanDefinition resolvedBy(ListableBeanFactory beanFactory,
				ThrowableFunction<Object[], Object> instantiator) {
			return null;
		}

	}

}
