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

import org.springframework.lang.Nullable;

/**
 * Interface that can be used to supply a {@link FunctionalBeanRegistrar} for a given
 * {@link FunctionalBeanDefinitionIdentifier}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
@FunctionalInterface
public interface FunctionalBeanRegistrarSupplier {

	/**
	 * Return the registrar that should be applied for the given identifier.
	 * @param identifier the bean definition identifier
	 * @return the {@link FunctionalBeanRegistrar} instance to apply or {@code null}
	 */
	@Nullable
	FunctionalBeanRegistrar getRegistrar(FunctionalBeanDefinitionIdentifier identifier);

}
