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
 * Tests for {@link UniqueBeanName}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class UniqueBeanNameTests {

	@Test
	void createWhenBeanFactoryNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new UniqueBeanName(null, "myBean"))
				.withMessage("'beanFactoryName' must not be null");
	}

	@Test
	void createWhenBeanNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new UniqueBeanName(new UniqueBeanFactoryName("test"), ""))
				.withMessage("'beanName' must not be empty");
	}

	@Test
	void createCreatesName() {
		UniqueBeanFactoryName beanFactoryName = new UniqueBeanFactoryName("test");
		UniqueBeanName name = new UniqueBeanName(beanFactoryName, "myBean");
		assertThat(name).hasToString("test:myBean");
	}

	@Test
	void equalsAndHashCode() {
		UniqueBeanFactoryName beanFactoryName1 = new UniqueBeanFactoryName("bf");
		UniqueBeanFactoryName beanFactoryName2 = new UniqueBeanFactoryName("bf");
		UniqueBeanFactoryName beanFactoryName3 = new UniqueBeanFactoryName("fb");
		UniqueBeanName name1 = new UniqueBeanName(beanFactoryName1, "bn");
		UniqueBeanName name2 = new UniqueBeanName(beanFactoryName1, "bn");
		UniqueBeanName name3 = new UniqueBeanName(beanFactoryName2, "bn");
		UniqueBeanName name4 = new UniqueBeanName(beanFactoryName2, "nb");
		UniqueBeanName name5 = new UniqueBeanName(beanFactoryName3, "bn");
		UniqueBeanName name6 = new UniqueBeanName(beanFactoryName3, "nb");
		assertThat(name1.hashCode()).isEqualTo(name2.hashCode()).isEqualTo(name3.hashCode());
		assertThat(name1).isEqualTo(name2).isEqualTo(name3).isNotEqualTo(name4);
		assertThat(name5).isNotEqualTo(name1).isNotEqualTo(name6);
	}

}
