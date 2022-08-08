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

package org.springframework.aot.hint2;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.Nullable;

/**
 * An immutable hint that describes the need for a JDK interface-based
 * {@link Proxy}.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 6.0
 * @see ProxyHints
 */
public final class JdkProxyHint implements ConditionalHint {

	private final List<TypeReference> proxiedInterfaces;

	@Nullable
	private final TypeReference reachableType;

	JdkProxyHint(TypeReference[] proxiedInterfaces, @Nullable TypeReference reachableType) {
		this.proxiedInterfaces = List.of(proxiedInterfaces);
		this.reachableType = reachableType;
	}

	/**
	 * Return the interfaces to be proxied.
	 * @return the interfaces that the proxy should implement
	 */
	public List<TypeReference> getProxiedInterfaces() {
		return this.proxiedInterfaces;
	}

	@Override
	@Nullable
	public TypeReference getReachableType() {
		return this.reachableType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.proxiedInterfaces, this.reachableType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		JdkProxyHint other = (JdkProxyHint) obj;
		return Objects.equals(proxiedInterfaces, other.proxiedInterfaces)
				&& Objects.equals(reachableType, other.reachableType);
	}

}
