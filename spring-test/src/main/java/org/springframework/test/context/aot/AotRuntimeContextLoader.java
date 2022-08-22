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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.log.LogMessage;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Loads an {@link ApplicationContext} for AOT run-time execution using the
 * {@link AotContextLoader} API.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see AotContextLoader
 */
public class AotRuntimeContextLoader {

	private static final Log logger = LogFactory.getLog(AotRuntimeContextLoader.class);


	public GenericApplicationContext loadContext(MergedContextConfiguration mergedConfig,
			ApplicationContextInitializer<GenericApplicationContext> contextInitializer)
			throws TestContextAotException {

		logger.info(LogMessage.format("Loading ApplicationContext in AOT mode for %s", mergedConfig));

		ContextLoader contextLoader = mergedConfig.getContextLoader();
		if (!((contextLoader instanceof AotContextLoader aotContextLoader) &&
				(aotContextLoader.createContextForAotRuntime(mergedConfig)
						instanceof GenericApplicationContext gac))) {
			throw new TestContextAotException("""
					Cannot load ApplicationContext for AOT runtime for %s. The configured \
					ContextLoader [%s] must be an AotContextLoader and must create a \
					GenericApplicationContext."""
						.formatted(mergedConfig, contextLoader.getClass().getName()));
		}
		aotContextLoader.prepareContextForAotRuntime(gac, mergedConfig);
		contextInitializer.initialize(gac);
		aotContextLoader.customizeContextForAotRuntime(gac, mergedConfig);
		gac.refresh();
		gac.registerShutdownHook();
		return gac;
	}

}
