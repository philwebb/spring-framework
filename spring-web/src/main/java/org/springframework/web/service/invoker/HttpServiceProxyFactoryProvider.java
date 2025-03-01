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

package org.springframework.web.service.invoker;

import org.jspecify.annotations.Nullable;

/**
 * Strategy used to provide {@link HttpServiceProxyFactory} instances for a given group
 * ID.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 7.0
 */
@FunctionalInterface
public interface HttpServiceProxyFactoryProvider {

	/**
	 * Return the {@link HttpServiceProxyFactory} that should be used to create the
	 * proxies or {@code null} if the group ID is not known.
	 * @param groupId the ID of the group
	 * @return a {@link HttpServiceProxyFactory} instance or {@code null}
	 */
	@Nullable
	HttpServiceProxyFactory getHttpServiceProxyFactory(String groupId);

}
