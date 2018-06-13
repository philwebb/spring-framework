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
 * Abstractions for reactive HTTP server support including a
 * {@link org.springframework.http.server.reactive.ServerHttpRequest} and
 * {@link org.springframework.http.server.reactive.ServerHttpResponse} along with an
 * {@link org.springframework.http.server.reactive.HttpHandler} for processing.
 *
 * <p>Also provides implementations adapting to different runtimes
 * including Servlet 3.1 containers, Netty + Reactor IO, and Undertow.
 */
@NonNullApi
@NonNullFields
package org.springframework.http.server.reactive;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
