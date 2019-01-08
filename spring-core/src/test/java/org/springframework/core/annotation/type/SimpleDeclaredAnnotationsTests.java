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

package org.springframework.core.annotation.type;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SimpleDeclaredAnnotations}.
 *
 * @author Phillip Webb
 */
public class SimpleDeclaredAnnotationsTests {

	private Object source;

	private SimpleDeclaredAnnotation two;

	private SimpleDeclaredAnnotation one;

	private SimpleDeclaredAnnotations annotations;

	@Before
	public void setup() {
		this.source = new Object();
		this.one = new SimpleDeclaredAnnotation("one", DeclaredAttributes.NONE);
		this.two = new SimpleDeclaredAnnotation("two", DeclaredAttributes.NONE);
		this.annotations = new SimpleDeclaredAnnotations(this.source,
				Arrays.asList(this.one, this.two));
	}

	@Test
	public void createFromArrayWhenDeclaredAnnotationIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleDeclaredAnnotations(null,
						(DeclaredAnnotation[]) null)).withMessage(
								"Annotations must not be null");
	}

	@Test
	public void createFromCollectionWhenDeclaredAnnotationsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleDeclaredAnnotations(null,
						(Collection<DeclaredAnnotation>) null)).withMessage(
								"Annotations must not be null");
	}

	@Test
	public void getSourceReturnsSource() {
		assertThat(this.annotations.getSource()).isSameAs(this.source);
	}

	@Test
	public void getSourceWhenSourceIsNullReturnsNull() {
		DeclaredAnnotations annotations = new SimpleDeclaredAnnotations(null,
				new SimpleDeclaredAnnotation[] { this.one, this.two });
		assertThat(annotations.getSource()).isNull();
	}

	@Test
	public void sizeReturnsSize() {
		assertThat(this.annotations.size()).isEqualTo(2);
	}

	@Test
	public void iteratorReturnsAnnotations() {
		assertThat(this.annotations.iterator()).containsExactly(this.one, this.two);
	}

	@Test
	public void findFindsAnnotation() {
		assertThat(this.annotations.find("two")).isEqualTo(this.two);
	}

	@Test
	public void findWhenMissingReturnsNull() {
		assertThat(this.annotations.find("missing")).isNull();
	}

	@Test
	public void toStringReturnsString() {
		assertThat(this.annotations.toString()).isEqualTo("@one\n@two");
	}

}
