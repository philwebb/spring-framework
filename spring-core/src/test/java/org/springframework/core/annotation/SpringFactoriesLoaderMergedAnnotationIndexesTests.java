/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import org.springframework.core.annotation.MergedAnnotationIndex.QueryResult;
import org.springframework.core.annotation.MergedAnnotationIndex.Scope;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link SpringFactoriesLoaderMergedAnnotationIndexes}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public class SpringFactoriesLoaderMergedAnnotationIndexesTests {

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private final MergedAnnotationIndexes emptyIndexes = new SpringFactoriesLoaderMergedAnnotationIndexes(
			resourceLoader, Collections.emptyList());

	@Test
	public void getWhenCalledTwiceUsesCache() {
		ClassLoader classLoader = getClass().getClassLoader();
		Object index1 = SpringFactoriesLoaderMergedAnnotationIndexes.get(classLoader);
		Object index2 = SpringFactoriesLoaderMergedAnnotationIndexes.get(classLoader);
		assertThat(index1).isNotNull().isSameAs(index2);
	}

	@Test
	public void queryWhenSourceClassIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.emptyIndexes.query(
				null, "com.example.Component", Scope.CLASS_HIERARCHY)).withMessage(
						"SourceClass must not be null");
	}

	@Test
	public void queryWhenAnnotationTypeIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.emptyIndexes.query(
				"com.example.MyBean", null, Scope.CLASS_HIERARCHY)).withMessage(
						"AnnotationType must not be null");
	}

	@Test
	public void queryWhenScopeIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.emptyIndexes.query(
				"com.example.MyBean", "com.example.Component", null)).withMessage(
						"Scope must not be null");
	}

	@Test
	public void queryWhenNoIndexesReturnsUnknown() {
		assertThat(this.emptyIndexes.query("com.example.MyBean", "com.example.Component",
				Scope.METHOD_HIERARCHY)).isEqualTo(QueryResult.UNKNOWN);
	}

	@Test
	public void queryReturnsFirstKnownResult() {
		MergedAnnotationIndex index1 = mock(MergedAnnotationIndex.class);
		MergedAnnotationIndex index2 = mock(MergedAnnotationIndex.class);
		MergedAnnotationIndex index3 = mock(MergedAnnotationIndex.class);
		String sourceClass = "com.example.MyBean";
		String annotationType = "com.example.Component";
		Scope scope = Scope.METHOD_HIERARCHY;
		given(index1.query(this.resourceLoader, sourceClass, annotationType,
				scope)).willReturn(QueryResult.UNKNOWN);
		given(index2.query(this.resourceLoader, sourceClass, annotationType,
				scope)).willReturn(QueryResult.KNOWN_PRESENT);
		given(index3.query(this.resourceLoader, sourceClass, annotationType,
				scope)).willReturn(QueryResult.UNKNOWN);
		MergedAnnotationIndexes indexes = new SpringFactoriesLoaderMergedAnnotationIndexes(
				this.resourceLoader, Arrays.asList(index1, index2, index3));
		QueryResult result = indexes.query(sourceClass, annotationType, scope);
		assertThat(result).isEqualTo(QueryResult.KNOWN_PRESENT);
		verifyZeroInteractions(index3);
	}

}
