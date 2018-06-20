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
 * SPI package allowing Spring AOP framework to handle arbitrary advice types.
 *
 * <p>Users who want merely to <i>use</i> the Spring AOP framework, rather than extend
 * its capabilities, don't need to concern themselves with this package.
 *
 * <p>You may wish to use these adapters to wrap Spring-specific advices, such as MethodBeforeAdvice,
 * in MethodInterceptor, to allow their use in another AOP framework supporting the AOP Alliance interfaces.
 *
 * <p>These adapters do not depend on any other Spring framework classes to allow such usage.
 */
@NonNullApi
@NonNullFields
package org.springframework.aop.framework.adapter;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
