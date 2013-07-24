/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.weaving.DefaultContextLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * {@code @Configuration} class that registers a {@link LoadTimeWeaver} bean.
 *
 * <p>This configuration class is automatically imported when using the @{@link
 * EnableLoadTimeWeaving} annotation.  See {@code @EnableLoadTimeWeaving} Javadoc for
 * complete usage details.
 *
 * @author Chris Beams
 * @since 3.1
 * @see LoadTimeWeavingConfigurer
 * @see ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 */
@Configuration
public class LoadTimeWeavingConfiguration implements BeanClassLoaderAware {

	@Autowired(required=false)
	private LoadTimeWeavingConfigurer loadTimeWeavingConfigurer;

	private ClassLoader beanClassLoader;


	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	@Bean(name=ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public LoadTimeWeaver loadTimeWeaver() {
		LoadTimeWeaver loadTimeWeaver = null;

		if (this.loadTimeWeavingConfigurer != null) {
			// the user has provided a custom LTW instance
			loadTimeWeaver = this.loadTimeWeavingConfigurer.getLoadTimeWeaver();
		}

		if (loadTimeWeaver == null) {
			// no custom LTW provided -> fall back to the default
			loadTimeWeaver = new DefaultContextLoadTimeWeaver(this.beanClassLoader);
		}

		return loadTimeWeaver;
	}

}
