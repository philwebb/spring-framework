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
 * The classes in this package represent EIS operations as threadsafe,
 * reusable objects. This higher level of CCI abstraction depends on the
 * lower-level abstraction in the {@code org.springframework.jca.cci.core} package.
 * Exceptions thrown are as in the {@code org.springframework.dao} package,
 * meaning that code using this package does not need to worry about error handling.
 */
@NonNullApi
@NonNullFields
package org.springframework.jca.cci.object;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
