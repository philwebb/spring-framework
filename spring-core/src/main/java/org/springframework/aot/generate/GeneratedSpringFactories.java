/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.generate;

import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ObjectUtils;

/**
 * Interface that can be used to add entries to generated Spring
 * {@code .factories} files.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see DefaultGeneratedSpringFactories
 */
public interface GeneratedSpringFactories {

	/**
	 * Return {@link Declarations} for the default {@code spring.factories}
	 * resource location.
	 * @return declarations for the default resource location
	 * @see SpringFactoriesLoader#forDefaultResourceLocation()
	 */
	default Declarations forDefaultResourceLocation() {
		return forResourceLocation(SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION);
	}

	/**
	 * Return {@link Declarations} for the named item.
	 * @param classification the classification used to group related items
	 * @param name the name of the item
	 * @return declarations for the named item
	 * @see SpringFactoriesLoader#forNamedItem(Class, String)
	 */
	default <N> Declarations forNamedItem(Class<?> classification, N name) {
		return forResourceLocation(SpringFactoriesLoader.resolveNamedItemLocation(
				classification, toString(name)));
	}

	/**
	 * Return {@link Declarations} for the named item.
	 * @param classification the classification used to group related items
	 * (usually a fully-qualified class name)
	 * @param name the name of the item
	 * @return declarations for the named item
	 * @see SpringFactoriesLoader#forNamedItem(String, String)
	 */
	default <N> Declarations forNamedItem(String classification, N name) {
		return forResourceLocation(SpringFactoriesLoader.resolveNamedItemLocation(
				classification, toString(name)));
	}

	/**
	 * Return {@link Declarations} for the given resource location
	 * @param resourceLocation the resource location
	 * @return declarations for the resource location
	 */
	Declarations forResourceLocation(String resourceLocation);

	private static <N> String toString(N name) {
		return (name instanceof Class<?>) ? ((Class<?>) name).getName()
				: ObjectUtils.nullSafeToString(name);
	}

	/**
	 * Spring factory declarations for a specific file.
	 */
	interface Declarations {

		/**
		 * Add a new spring factory declaration to the file when it is written.
		 * @param factoryType the factory type
		 * @param implementation the implementation class
		 */
		default void add(Class<?> factoryType, Class<?> implementation) {
			add(factoryType, implementation.getName());
		}

		/**
		 * Add a new spring factory declaration to the file when it is written.
		 * @param factoryType the factory type
		 * @param implementationName the generated class name of the
		 * implementation class
		 */
		default void add(Class<?> factoryType, GeneratedClassName implementationName) {
			add(factoryType, implementationName.toString());
		}

		/**
		 * Add a new spring factory declaration to the file when it is written.
		 * @param factoryType the factory type
		 * @param implementationName the class name of the implementation class
		 */
		default void add(Class<?> factoryType, String implementationName) {
			add(factoryType.getName(), implementationName);
		}

		/**
		 * Add a new spring factory declaration to the file when it is written.
		 * @param factoryTypeName the factory type name
		 * @param implementationName the class name of the implementation class
		 */
		void add(String factoryTypeName, String implementationName);

	}

}
