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
 * Properties editors used to convert from String values to object
 * types such as java.util.Properties.
 *
 * <p>Some of these editors are registered automatically by BeanWrapperImpl.
 * "CustomXxxEditor" classes are intended for manual registration in
 * specific binding processes, as they are localized or the like.
 */
@NonNullApi
@NonNullFields
package org.springframework.beans.propertyeditors;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
