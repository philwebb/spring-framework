/*
 * Copyright 2002-2018 the original author or authors.
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

/**
 * Package containing Spring's basic AOP infrastructure, compliant with the
 * <a href="http://aopalliance.sourceforge.net">AOP Alliance</a> interfaces.
 *
 * <p>Spring AOP supports proxying interfaces or classes, introductions, and offers
 * static and dynamic pointcuts.
 *
 * <p>Any Spring AOP proxy can be cast to the ProxyConfig AOP configuration interface
 * in this package to add or remove interceptors.
 *
 * <p>The ProxyFactoryBean is a convenient way to create AOP proxies in a BeanFactory
 * or ApplicationContext. However, proxies can be created programmatically using the
 * ProxyFactory class.
 */
@NonNullApi
@NonNullFields
package org.springframework.aop.framework;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
