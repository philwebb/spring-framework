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
 * Implementation package for {@code java.util.concurrent} based caches.
 * Provides a {@link org.springframework.cache.CacheManager CacheManager}
 * and {@link org.springframework.cache.Cache Cache} implementation for
 * use in a Spring context, using a JDK based thread pool at runtime.
 */
@NonNullApi
@NonNullFields
package org.springframework.cache.concurrent;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
