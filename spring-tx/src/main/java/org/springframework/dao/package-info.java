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
 * Exception hierarchy enabling sophisticated error handling independent
 * of the data access approach in use. For example, when DAOs and data
 * access frameworks use the exceptions in this package (and custom
 * subclasses), calling code can detect and handle common problems such
 * as deadlocks without being tied to a particular data access strategy,
 * such as JDBC.
 *
 * <p>All these exceptions are unchecked, meaning that calling code can
 * leave them uncaught and treat all data access exceptions as fatal.
 *
 * <p>The classes in this package are discussed in Chapter 9 of
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 */
@NonNullApi
@NonNullFields
package org.springframework.dao;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
