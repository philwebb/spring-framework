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
 * Top-level package for the {@code spring-webflux} module that contains
 * {@link org.springframework.web.reactive.DispatcherHandler}, the main entry
 * point for WebFlux server endpoint processing including key contracts used to
 * map requests to handlers, invoke them, and process the result.
 *
 * <p>The module provides two programming models for reactive server endpoints.
 * One based on annotated {@code @Controller}'s and another based on functional
 * routing and handling. The module also contains a functional, reactive
 * {@code WebClient} as well as client and server, reactive WebSocket support.
 */
@NonNullApi
@NonNullFields
package org.springframework.web.reactive;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
