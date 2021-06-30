/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.function;

import java.lang.reflect.Method;

/**
 * A unique identity associated with a {@link FunctionalBeanDefinition}. Unlike bean
 * names, these identifiers are not generally used directly by application developers.
 * Instead, these identifiers provide a globally unique way to identify a bean definition
 * so that any {@link FunctionalBeanDefinitionPreProcessor pre-processed} code can
 * contribute functionality to a running application.
 * <p>
 * Identifiers are usually created for a {@link Class}, or a {@link Class} and
 * {@link Method} combination.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public final class FunctionalBeanDefinitionIdentifier {

	public FunctionalBeanDefinitionIdentifier and(String name) {
		return null;
	}

	public static FunctionalBeanDefinitionIdentifier forClass(Class<?> clazz) {
		return null;
	}

	public static FunctionalBeanDefinitionIdentifier of(String value) {
		return null;
	}

}
