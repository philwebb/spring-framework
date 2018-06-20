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
 * This package contains mock implementations of the
 * {@link org.springframework.core.env.Environment Environment} and
 * {@link org.springframework.core.env.PropertySource PropertySource}
 * abstractions introduced in Spring 3.1.
 *
 * <p>These <em>mocks</em> are useful for developing <em>out-of-container</em>
 * unit tests for code that depends on environment-specific properties.
 */
@NonNullApi
@NonNullFields
package org.springframework.mock.env;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
