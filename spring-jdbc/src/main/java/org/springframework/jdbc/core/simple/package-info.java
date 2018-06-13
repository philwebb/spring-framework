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
 * Simplification layer for table inserts and stored procedure calls.
 *
 * <p>{@code SimpleJdbcInsert} and {@code SimpleJdbcCall} take advantage of database
 * meta-data provided by the JDBC driver to simplify the application code. Much of the
 * parameter specification becomes unnecessary since it can be looked up in the meta-data.
 */
@NonNullApi
@NonNullFields
package org.springframework.jdbc.core.simple;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
