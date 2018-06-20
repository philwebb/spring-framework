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
 * AOP-based solution for declarative caching demarcation using JSR-107 annotations.
 *
 * <p>Strongly based on the infrastructure in org.springframework.cache.interceptor
 * that deals with Spring's caching annotations.
 *
 * <p>Builds on the AOP infrastructure in org.springframework.aop.framework.
 * Any POJO can be cache-advised with Spring.
 */
package org.springframework.cache.jcache.interceptor;
