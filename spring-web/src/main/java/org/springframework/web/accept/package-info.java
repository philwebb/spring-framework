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
 * This package contains classes used to determine the requested the media types in a request.
 *
 * <p>{@link org.springframework.web.accept.ContentNegotiationStrategy} is the main
 * abstraction for determining requested {@linkplain org.springframework.http.MediaType media types}
 * with implementations based on
 * {@linkplain org.springframework.web.accept.PathExtensionContentNegotiationStrategy path extensions}, a
 * {@linkplain org.springframework.web.accept.ParameterContentNegotiationStrategy a request parameter}, the
 * {@linkplain org.springframework.web.accept.HeaderContentNegotiationStrategy 'Accept' header}, or a
 * {@linkplain org.springframework.web.accept.FixedContentNegotiationStrategy default content type}.
 *
 * <p>{@link org.springframework.web.accept.ContentNegotiationManager} is used to delegate to one
 * ore more of the above strategies in a specific order.
 */
@NonNullApi
@NonNullFields
package org.springframework.web.accept;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
