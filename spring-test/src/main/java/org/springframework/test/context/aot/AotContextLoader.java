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

package org.springframework.test.context.aot;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;

/**
 * Strategy interface for loading an {@link ApplicationContext} for AOT processing
 * as well as AOT runtime execution for integration tests managed by the Spring
 * TestContext Framework.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public interface AotContextLoader extends SmartContextLoader {

	/**
	 * Load a new {@link ApplicationContext} for AOT build-time processing based
	 * on the supplied {@link MergedContextConfiguration}, configure the context,
	 * and return the context.
	 * <p>In contrast to {@link #loadContext(MergedContextConfiguration)}, this
	 * method must <strong>not</strong>
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#refresh()
	 * refresh} the {@code ApplicationContext} or
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#registerShutdownHook()
	 * register a JVM shutdown hook} for it. Otherwise, this method should implement
	 * behavior identical to {@link #loadContext(MergedContextConfiguration)}.
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @return a new application context
	 * @throws Exception if context loading failed
	 * @see #createContextForAotRuntime(MergedContextConfiguration)
	 * @see #prepareContextForAotRuntime(ApplicationContext, MergedContextConfiguration)
	 * @see #customizeContextForAotRuntime(ApplicationContext, MergedContextConfiguration)
	 */
	ApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig) throws Exception;

	/**
	 * Load a new {@link ApplicationContext} for AOT run-time execution based on
	 * the supplied {@link MergedContextConfiguration} and
	 * {@link ApplicationContextInitializer}.
	 * <p>This method must instantiate, initialize and
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#refresh()
	 * refresh} the {@code ApplicationContext}.
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @param initializer the {@link ApplicationContextInitializer} that should
	 * be applied in order to re-create bean definitions
	 * @return a new generic application context
	 * @throws Exception if context loading failed
	 * @see #createContextForAotRuntime(MergedContextConfiguration)
	 * @see #prepareContextForAotRuntime(ApplicationContext,
	 * MergedContextConfiguration)
	 * @see #customizeContextForAotRuntime(ApplicationContext,
	 * MergedContextConfiguration)
	 */
	GenericApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig,
			ApplicationContextInitializer<GenericApplicationContext> initializer) throws Exception;
}
