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

package org.springframework.aot.test.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;

/**
 * Assertion methods for {@code DynamicFile} instances.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class DynamicFileAssert<A extends DynamicFileAssert<A, F>, F extends DynamicFile> extends AbstractAssert<A, F> {

	DynamicFileAssert(F actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public A contains(CharSequence... values) {
		assertThat(this.actual.getContent()).contains(values);
		return this.myself;
	}

	public A isEqualTo(Object expected) {
		assertThat(this.actual.getContent()).isEqualTo(expected != null ? expected.toString() : null);
		return this.myself;
	}

}
