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
 * Core Spring AOP interfaces, built on AOP Alliance AOP interoperability interfaces.
 *
 * <p>Any AOP Alliance MethodInterceptor is usable in Spring.
 *
 * <br>Spring AOP also offers:
 * <ul>
 * <li>Introduction support
 * <li>A Pointcut abstraction, supporting "static" pointcuts
 * (class and method-based) and "dynamic" pointcuts (also considering method arguments).
 * There are currently no AOP Alliance interfaces for pointcuts.
 * <li>A full range of advice types, including around, before, after returning and throws advice.
 * <li>Extensibility allowing arbitrary custom advice types to
 * be plugged in without modifying the core framework.
 * </ul>
 *
 * <p>Spring AOP can be used programmatically or (preferably)
 * integrated with the Spring IoC container.
 */
@NonNullApi
@NonNullFields
package org.springframework.aop;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
