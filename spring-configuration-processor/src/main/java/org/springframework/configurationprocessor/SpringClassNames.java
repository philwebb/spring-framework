/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.configurationprocessor;

import com.squareup.javapoet.ClassName;

/**
 *
 * @author pwebb
 * @since 5.0
 */
public class SpringClassNames {

	public static final ClassName CONFIGURATION = ClassName.get(
			"org.springframework.context.annotation", "Configuration");

	public static final ClassName BEAN = ClassName.get(
			"org.springframework.context.annotation", "Bean");

	public static final ClassName PRE_PROCESSED_CONFIGURATION = ClassName.get(
			"org.springframework.context.annotation", "PreProcessedConfiguration");

	public static final ClassName BEANS_EXCEPTION = ClassName.get(
			"org.springframework.beans", "BeansException");

	public static final ClassName BEAN_FACTORY = ClassName.get(
			"org.springframework.beans.factory", "BeanFactory");

	public static final ClassName BEAN_FACTORY_AWARE = ClassName.get(
			"org.springframework.beans.factory", "BeanFactoryAware");

}
