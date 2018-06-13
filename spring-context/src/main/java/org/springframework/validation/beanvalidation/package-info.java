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
 * Support classes for integrating a JSR-303 Bean Validation provider
 * (such as Hibernate Validator) into a Spring ApplicationContext
 * and in particular with Spring's data binding and validation APIs.
 *
 * <p>The central class is {@link
 * org.springframework.validation.beanvalidation.LocalValidatorFactoryBean}
 * which defines a shared ValidatorFactory/Validator setup for availability
 * to other Spring components.
 */
@NonNullApi
@NonNullFields
package org.springframework.validation.beanvalidation;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
