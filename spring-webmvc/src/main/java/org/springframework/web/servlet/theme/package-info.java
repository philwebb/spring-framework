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
 * Theme support classes for Spring's web MVC framework.
 * Provides standard ThemeResolver implementations,
 * and a HandlerInterceptor for theme changes.
 *
 * <p>
 * <ul>
 * <li>If you don't provide a bean of one of these classes as {@code themeResolver},
 * a {@code FixedThemeResolver} will be provided with the default theme name 'theme'.</li>
 * <li>If you use a defined {@code FixedThemeResolver}, you will able to use another theme
 * name for default, but the users will stick on this theme.</li>
 * <li>With a {@code CookieThemeResolver} or {@code SessionThemeResolver}, you can allow
 * the user to change his current theme.</li>
 * <li>Generally, you will put in the themes resource bundles the paths of CSS files, images and HTML constructs.</li>
 * <li>For retrieving themes data, you can either use the spring:theme tag in JSP or access via the
 * {@code RequestContext} for other view technologies.</li>
 * <li>The {@code pagedlist} demo application uses themes</li>
 * </ul>
 */
@NonNullApi
@NonNullFields
package org.springframework.web.servlet.theme;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
