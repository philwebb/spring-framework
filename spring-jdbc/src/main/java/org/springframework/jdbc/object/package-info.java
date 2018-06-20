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
 * The classes in this package represent RDBMS queries, updates,
 * and stored procedures as threadsafe, reusable objects. This approach
 * is modelled by JDO, although of course objects returned by queries
 * are "disconnected" from the database.
 *
 * <p>This higher level of JDBC abstraction depends on the lower-level
 * abstraction in the {@code org.springframework.jdbc.core} package.
 * Exceptions thrown are as in the {@code org.springframework.dao} package,
 * meaning that code using this package does not need to implement JDBC or
 * RDBMS-specific error handling.
 *
 * <p>This package and related packages are discussed in Chapter 9 of
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 */
@NonNullApi
@NonNullFields
package org.springframework.jdbc.object;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
