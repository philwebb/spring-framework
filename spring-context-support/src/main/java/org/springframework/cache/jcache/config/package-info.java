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
 * Support package for declarative JSR-107 caching configuration. Used
 * by the regular Spring's caching configuration when it detects the
 * JSR-107 API and Spring's JCache implementation.
 *
 * <p>Provide an extension of the {@code CachingConfigurer} that exposes
 * the exception cache resolver to use, see {@code JCacheConfigurer}.
 */
package org.springframework.cache.jcache.config;
