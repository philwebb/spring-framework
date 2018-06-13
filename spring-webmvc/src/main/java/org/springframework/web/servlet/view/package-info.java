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
 * Provides standard View and ViewResolver implementations,
 * including abstract base classes for custom implementations.
 *
 * <p>Application developers don't usually need to implement views,
 * as the framework provides standard views for JSPs, FreeMarker,
 * XSLT, etc. However, the ability to implement custom views easily
 * by subclassing the AbstractView class in this package can be
 * very helpful if an application has unusual view requirements.
 */
@NonNullApi
@NonNullFields
package org.springframework.web.servlet.view;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
