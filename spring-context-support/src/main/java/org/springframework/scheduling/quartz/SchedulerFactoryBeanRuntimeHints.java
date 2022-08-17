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

package org.springframework.scheduling.quartz;

import org.springframework.aot.hint.ReflectionHints.MethodRegistration;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that makes sure {@link SchedulerFactoryBean}
 * reflection entries are registered.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
public class SchedulerFactoryBeanRuntimeHints implements RuntimeHintsRegistrar {

	private static String SCHEDULER_FACTORY_CLASS_NAME = "org.quartz.impl.StdSchedulerFactory";

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if (ClassUtils.isPresent(SCHEDULER_FACTORY_CLASS_NAME, classLoader)) {
			MethodRegistration registerInvoke = hints.reflection().registerInvoke()
					.whenReachable(SchedulerFactoryBean.class);
			MethodRegistration registerIntrospect = hints.reflection().registerIntrospect()
					.whenReachable(SchedulerFactoryBean.class);
			registerInvoke.forDeclaredConstructorsIn(SCHEDULER_FACTORY_CLASS_NAME);
			registerInvoke.forDeclaredConstructorsIn(ResourceLoaderClassLoadHelper.class);
			registerInvoke.forDeclaredConstructorsIn(LocalTaskExecutorThreadPool.class);
			registerIntrospect.forMethod(LocalTaskExecutorThreadPool.class, "setInstanceId", String.class);
			registerIntrospect.forMethod(LocalTaskExecutorThreadPool.class, "setInstanceName", String.class);
			registerInvoke.whenReachable(SchedulerFactoryBean.class).forDeclaredConstructorsIn(LocalDataSourceJobStore.class);
		}
	}

}
