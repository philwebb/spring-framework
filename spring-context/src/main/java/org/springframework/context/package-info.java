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
 * This package builds on the beans package to add support for
 * message sources and for the Observer design pattern, and the
 * ability for application objects to obtain resources using a
 * consistent API.
 *
 * <p>There is no necessity for Spring applications to depend
 * on ApplicationContext or even BeanFactory functionality
 * explicitly. One of the strengths of the Spring architecture
 * is that application objects can often be configured without
 * any dependency on Spring-specific APIs.
 */
@NonNullApi
@NonNullFields
package org.springframework.context;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
