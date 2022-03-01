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

package org.springframework.beans.factory.aot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link UniqueBeanFactoryName}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class UniqueBeanFactoryNameTests {

	@Test
	void createWhenNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new UniqueBeanFactoryName(""))
				.withMessage("'value' must not be empty");
	}

	@Test
	void createWithNoParent() {
		UniqueBeanFactoryName name = new UniqueBeanFactoryName("application");
		assertThat(name).hasToString("application");
	}

	@Test
	void createWithParent() {
		UniqueBeanFactoryName parent = new UniqueBeanFactoryName("application");
		UniqueBeanFactoryName child = new UniqueBeanFactoryName(parent, "management");
		assertThat(child).hasToString("application:management");
	}

	@Test
	void equalsAndHashCode() {
		UniqueBeanFactoryName name1 = new UniqueBeanFactoryName("application");
		UniqueBeanFactoryName name2 = new UniqueBeanFactoryName("application");
		UniqueBeanFactoryName name3 = new UniqueBeanFactoryName("other");
		UniqueBeanFactoryName name4 = new UniqueBeanFactoryName(name1, "management");
		UniqueBeanFactoryName name5 = new UniqueBeanFactoryName(name2, "management");
		UniqueBeanFactoryName name6 = new UniqueBeanFactoryName(name2, "xmanagement");
		assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
		assertThat(name4.hashCode()).isEqualByComparingTo(name5.hashCode());
		assertThat(name1).isEqualTo(name2).isNotEqualTo(name3).isNotEqualTo(name4);
		assertThat(name4).isEqualTo(name5).isNotEqualTo(name6).isNotEqualTo(name1);
	}

}
