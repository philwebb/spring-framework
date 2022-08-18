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

package org.springframework.aot.hint;

import java.util.Comparator;
import java.util.Objects;
import java.util.ResourceBundle;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An immutable hint that describes the need to access a {@link ResourceBundle}.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 6.0
 * @see ResourceHints
 */
public final class ResourceBundleHint implements ConditionalHint, Comparable<ResourceBundleHint> {

	private static final Comparator<ResourceBundleHint> COMPARATOR = Comparator
			.comparing(ResourceBundleHint::getBaseName)
			.thenComparing(CONDITIONAL_HINT_COMPARATOR);


	private final String baseName;

	@Nullable
	private final TypeReference reachableType;


	ResourceBundleHint(String baseName, @Nullable TypeReference reachableType) {
		Assert.notNull(baseName, "'baseName' must not be null");
		this.baseName = baseName;
		this.reachableType = reachableType;
	}


	/**
	 * Return the {@code baseName} of the resource bundle.
	 * @return the base name
	 */
	public String getBaseName() {
		return this.baseName;
	}

	@Override
	public TypeReference getReachableType() {
		return this.reachableType;
	}

	@Override
	public int compareTo(ResourceBundleHint other) {
		return COMPARATOR.compare(this, other);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ResourceBundleHint other = (ResourceBundleHint) obj;
		return this.baseName.equals(other.baseName) && Objects.equals(this.reachableType, other.reachableType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.baseName, this.reachableType);
	}

}
