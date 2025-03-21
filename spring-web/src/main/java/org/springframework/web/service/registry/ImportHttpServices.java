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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(ImportHttpServices.Container.class)
@Documented
@Import(AnnotationHttpServiceRegistrar.class)
public @interface ImportHttpServices {

	@AliasFor("group")
	String value() default HttpServiceGroup.DEFAULT_GROUP_NAME;

	@AliasFor("value")
	String group() default HttpServiceGroup.DEFAULT_GROUP_NAME;

	Class<?>[] httpServiceTypes() default {};

	String[] basePackages() default {};

	Class<?>[] basePackageClasses() default {};

	/**
	 * Specify the type of client to use for the group.
	 * <p>By default, this is {@link HttpServiceGroup.ClientType#UNSPECIFIED}
	 * in which case {@code RestClient} is used, but this default can be reset
	 * via {@link AbstractHttpServiceRegistrar#setDefaultClientType}.
	 */
	HttpServiceGroup.ClientType clientType() default HttpServiceGroup.ClientType.UNSPECIFIED;


	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import(AnnotationHttpServiceRegistrar.class)
	@interface Container {

		ImportHttpServices[] value() default {};

		HttpServiceGroup.ClientType clientType() default HttpServiceGroup.ClientType.UNSPECIFIED;

	}

}
