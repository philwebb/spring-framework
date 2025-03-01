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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.service.invoker.HttpServiceProxyFactoryProvider;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * FIXME.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 7.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ImportHttpServiceClients.Container.class)
@Import(ImportHttpServiceClientsRegistrar.class)
public @interface ImportHttpServiceClients {

	/**
	 * An attribute name that can be used to {@link AttributeAccessor access} the service
	 * proxy factory ID from scanned bean definitions.
	 */
	static final String ID_ATTRIBUTE_NAME = HttpServiceProxyFactoryProvider.class.getName() + ".ID";


	/**
	 * Alias for {@link #clients}.
	 * <p>
	 * Allows for more concise annotation declarations if no other attributes are needed
	 * &mdash; for example, {@code @ImportHttpServiceClients(org.my.MyClient)} instead of
	 * {@code @ImportHttpServiceClients(clients = "org.my.pkg")}.
	 */
	@AliasFor("clients")
	Class<?>[] value() default {};

	/**
	 * Interface client types to import.
	 * <p>
	 * {@link #value} is an alias for (and mutually exclusive with) this attribute.
	 */
	@AliasFor("value")
	Class<?>[] clients() default {};

	/**
	 * An optional ID that indicates a unique {@link HttpServiceProxyFactoryProvider} bean
	 * should be used to lookup the {@link HttpServiceProxyFactory}. If not specified, a
	 * unique {@link HttpServiceProxyFactory} bean will.
	 */
	String using() default "";

	/**
	 * Base packages to scan for interface clients.
	 * <p>
	 * Use {@link #basePackageClasses} for a type-safe alternative to String-based package
	 * names.
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages} for specifying the packages to scan
	 * for interface clients. The package of each class specified will be scanned.
	 * <p>
	 * Consider creating a special no-op marker class or interface in each package that
	 * serves no purpose other than being referenced by this attribute.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * The {@link BeanNameGenerator} class to be used for naming detected components
	 * within the Spring container.
	 * <p>
	 * By default, the names will be generated based on the client interface type and the
	 * {@link #using()} attribute.
	 */
	Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

	/**
	 * Controls the class files eligible for interface client detection.
	 * <p>
	 * Consider use of {@link #includeFilters} and {@link #excludeFilters} for a more
	 * flexible approach.
	 */
	String resourcePattern() default "**/*.class";

	/**
	 * Specifies which types are eligible for client interface scanning.
	 * <p>
	 * Further narrows the set of candidates from everything in {@link #basePackages} to
	 * everything in the base packages that matches the given filter or filters.
	 * @see #resourcePattern()
	 */
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for client interface scanning.
	 * @see #resourcePattern
	 */
	Filter[] excludeFilters() default {};

	/**
	 * Specify whether scanned beans should be registered for lazy initialization.
	 * <p>
	 * Default is {@code false}; switch this to {@code true} when desired.
	 */
	boolean lazyInit() default false;

	/**
	 * Container for repeated {@link ImportHttpServiceClients @ImportHttpServiceClients}
	 * annotations.
	 */
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import(ImportHttpServiceClientsRegistrar.class)
	@interface Container {

		ImportHttpServiceClients[] value();

	}

}
