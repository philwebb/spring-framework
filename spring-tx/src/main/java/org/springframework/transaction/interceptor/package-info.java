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
 * AOP-based solution for declarative transaction demarcation.
 * Builds on the AOP infrastructure in org.springframework.aop.framework.
 * Any POJO can be transactionally advised with Spring.
 *
 * <p>The TransactionFactoryProxyBean can be used to create transactional
 * AOP proxies transparently to code that uses them.
 *
 * <p>The TransactionInterceptor is the AOP Alliance MethodInterceptor that
 * delivers transactional advice, based on the Spring transaction abstraction.
 * This allows declarative transaction management in any environment,
 * even without JTA if an application uses only a single database.
 */
@NonNullApi
@NonNullFields
package org.springframework.transaction.interceptor;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
