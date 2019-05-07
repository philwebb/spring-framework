/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.annotation;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link PackagesAnnotationFilter}.
 *
 * @author Phillip Webb
 */
public class PackagesAnnotationFilterTests {

	@Test
	public void createWhenPackagesIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new PackagesAnnotationFilter((String[]) null)).withMessage(
						"Packages array must not be null");
	}

	@Test
	public void createWhenPackagesContainsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new PackagesAnnotationFilter((String) null)).withMessage(
						"Packages array must not have empty elements");
	}

	@Test
	public void createWhenPackagesContainsEmptyTextThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new PackagesAnnotationFilter("")).withMessage(
						"Packages array must not have empty elements");
	}

	@Test
	public void matchesWhenInPackageReturnsTrue() {
		PackagesAnnotationFilter filter = new PackagesAnnotationFilter("com.example");
		assertThat(filter.matches("com.example.Component")).isTrue();
	}

	@Test
	public void matchesWhenNotInPackageReturnsFalse() {
		PackagesAnnotationFilter filter = new PackagesAnnotationFilter("com.example");
		assertThat(filter.matches("org.springframework.sterotype.Component")).isFalse();
	}

	@Test
	public void matchesWhenInSimilarPackageReturnsFalse() {
		PackagesAnnotationFilter filter = new PackagesAnnotationFilter("com.example");
		assertThat(filter.matches("com.examples.Component")).isFalse();
	}

	@Test
	public void equalsAndHashCode() {
		PackagesAnnotationFilter filter1 = new PackagesAnnotationFilter("com.example",
				"org.springframework");
		PackagesAnnotationFilter filter2 = new PackagesAnnotationFilter(
				"org.springframework", "com.example");
		PackagesAnnotationFilter filter3 = new PackagesAnnotationFilter("com.examples");
		assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
		assertThat(filter1).isEqualTo(filter1).isEqualTo(filter2).isNotEqualTo(filter3);
	}

}
