/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.service.registry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Indicates that an annotated class is an HTTP service to be registered with an
 * {@link HttpServiceGroup}. Typically used in combination with
 * {@link ImportHttpServices#include() @ImportHttpServices(include=Include.SERVICE_CLIENT_ANNOTATED)}.
 * <p><strong>NOTE:</strong> The {@link #group()} and {@link #clientType()} attributes can
 * only be used when the corresponding {@link ImportHttpServices @ImportHttpServices} have
 * default values.
 *
 * @author Phillip Webb
 * @since 7.0
 * @see ImportHttpServices
 * @see ImportHttpServices#include()
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpServiceClient {

	/**
	 * The name of the HTTP Service group.
	 * <p>If not specified, the {@link ImportHttpServices @ImportHttpServices} {@code group} is used.
	 */
	@AliasFor("value")
	String group() default "";

	/**
	 * The name of the HTTP Service group.
	 * <p>If not specified, the {@link ImportHttpServices @ImportHttpServices} {@code group} is used.
	 */
	@AliasFor("group")
	String value() default "";

	/**
	 * Specify the type of client to use for the group.
	 * <p>If not specified, the {@link ImportHttpServices @ImportHttpServices} {@code clientType} is used.
	 */
	HttpServiceGroup.ClientType clientType() default HttpServiceGroup.ClientType.UNSPECIFIED;
}
