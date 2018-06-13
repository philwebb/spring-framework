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
 * JdbcTemplate variant with named parameter support.
 *
 * <p>NamedParameterJdbcTemplate is a wrapper around JdbcTemplate that adds
 * support for named parameter parsing. It does not implement the JdbcOperations
 * interface or extend JdbcTemplate, but implements the dedicated
 * NamedParameterJdbcOperations interface.
 *
 * <P>If you need the full power of Spring JDBC for less common operations, use
 * the {@code getJdbcOperations()} method of NamedParameterJdbcTemplate and
 * work with the returned classic template, or use a JdbcTemplate instance directly.
 */
@NonNullApi
@NonNullFields
package org.springframework.jdbc.core.namedparam;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
