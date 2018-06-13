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
 * Package providing integration of
 * <a href="http://www.hibernate.org">Hibernate 5.x</a>
 * with Spring concepts.
 *
 * <p>Contains an implementation of Spring's transaction SPI for local Hibernate transactions.
 * This package is intentionally rather minimal, with no template classes or the like,
 * in order to follow Hibernate recommendations as closely as possible. We recommend
 * using Hibernate's native <code>sessionFactory.getCurrentSession()</code> style.
 *
 * <p><b>This package supports Hibernate 5.x only.</b>
 */
@NonNullApi
@NonNullFields
package org.springframework.orm.hibernate5;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
