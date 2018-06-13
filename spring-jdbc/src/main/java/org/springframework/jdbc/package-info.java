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
 * The classes in this package make JDBC easier to use and
 * reduce the likelihood of common errors. In particular, they:
 * <ul>
 * <li>Simplify error handling, avoiding the need for try/catch/finally
 * blocks in application code.
 * <li>Present exceptions to application code in a generic hierarchy of
 * unchecked exceptions, enabling applications to catch data access
 * exceptions without being dependent on JDBC, and to ignore fatal
 * exceptions there is no value in catching.
 * <li>Allow the implementation of error handling to be modified
 * to target different RDBMSes without introducing proprietary
 * dependencies into application code.
 * </ul>
 *
 * <p>This package and related packages are discussed in Chapter 9 of
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 */
@NonNullApi
@NonNullFields
package org.springframework.jdbc;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
