/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.util.HashSet;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * {@link AnnotationFilter} implementation used for
 * {@link AnnotationFilter#packages(String...)}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class PackagesAnnotationFilter implements AnnotationFilter {

	private final Set<String> prefixes;

	PackagesAnnotationFilter(String... packages) {
		Assert.notNull(packages, "Packages must not be null");
		this.prefixes = new HashSet<>(packages.length);
		for (int i = 0; i < packages.length; i++) {
			Assert.hasText(packages[i], "Package must not have empty elements");
			this.prefixes.add(packages[i] + ".");
		}
	}

	@Override
	public boolean matches(String annotationType) {
		if (annotationType != null) {
			for (String prefix : this.prefixes) {
				if (annotationType.startsWith(prefix)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		PackagesAnnotationFilter other = (PackagesAnnotationFilter) obj;
		return this.prefixes.equals(other.prefixes);
	}

	@Override
	public int hashCode() {
		return this.prefixes.hashCode();
	}

}
