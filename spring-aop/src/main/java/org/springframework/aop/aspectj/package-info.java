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
 * AspectJ integration package. Includes Spring AOP advice implementations for AspectJ 5
 * annotation-style methods, and an AspectJExpressionPointcut: a Spring AOP Pointcut
 * implementation that allows use of the AspectJ pointcut expression language with the Spring AOP
 * runtime framework.
 *
 * <p>Note that use of this package does <i>not</i> require the use of the {@code ajc} compiler
 * or AspectJ load-time weaver. It is intended to enable the use of a valuable subset of AspectJ
 * functionality, with consistent semantics, with the proxy-based Spring AOP framework.
 */
@NonNullApi
@NonNullFields
package org.springframework.aop.aspectj;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
