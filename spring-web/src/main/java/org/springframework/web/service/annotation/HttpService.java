/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.service.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ScannedComponentProxyFactory;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import org.springframework.web.service.invoker.HttpServiceProxyCreator;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Indicates that an annotated interface is a HTTP Service component supported by a
 * registered {@link ScannedComponentProxyFactory}.
 *
 * @author Phillip Webb
 * @since 7.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface HttpService {

	// HTTP service is def of contract
	// Client is the client side of the contract

	/**
	 * Alias for {@link Component#name()}
	 */
	@AliasFor(annotation = Component.class, attribute = "value")
	String value() default "";

	/**
	 * Alias for {@link Component#name()}
	 */
	@AliasFor(annotation = Component.class, attribute = "value")
	String name() default "";

	/**
	 * A reference to the {@link HttpServiceProxyFactory} or
	 * {@link HttpServiceProxyCreator} bean that will establish the HTTP client
	 * connection. If not specified, a single {@link HttpServiceProxyFactory} bean must be
	 * registered in the application context.
	 */
	String createdBy() default "";

}
