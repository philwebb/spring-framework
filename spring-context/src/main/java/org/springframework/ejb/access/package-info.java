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
 * This package contains classes that allow easy access to EJBs.
 * The basis are AOP interceptors run before and after the EJB invocation.
 * In particular, the classes in this package allow transparent access
 * to stateless session beans (SLSBs) with local interfaces, avoiding
 * the need for application code using them to use EJB-specific APIs
 * and JNDI lookups, and work with business interfaces that could be
 * implemented without using EJB. This provides a valuable decoupling
 * of client (such as web components) and business objects (which may
 * or may not be EJBs). This gives us the choice of introducing EJB
 * into an application (or removing EJB from an application) without
 * affecting code using business objects.
 *
 * <p>The motivation for the classes in this package are discussed in Chapter 11 of
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 *
 * <p>However, the implementation and naming of classes in this package has changed.
 * It now uses FactoryBeans and AOP, rather than the custom bean definitions described in
 * <i>Expert One-on-One J2EE</i>.
 */
@NonNullApi
@NonNullFields
package org.springframework.ejb.access;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
