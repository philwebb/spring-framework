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
 * Support classes for the open source cache
 * <a href="http://ehcache.sourceforge.net">EhCache 2.x</a>,
 * allowing to set up an EhCache CacheManager and Caches
 * as beans in a Spring context.
 *
 * <p>Note: EhCache 3.x lives in a different package namespace
 * and is not covered by the traditional support classes here.
 * Instead, consider using it through JCache (JSR-107), with
 * Spring's support in {@code org.springframework.cache.jcache}.
 */
@NonNullApi
@NonNullFields
package org.springframework.cache.ehcache;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
