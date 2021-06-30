/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.function;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.function.ConcurrentHashFilter.Candidates;
import org.springframework.beans.factory.function.ConcurrentHashFilter.HashCodeConsumer;
import org.springframework.beans.factory.function.ConcurrentHashFilter.HashCodesExtractor;
import org.springframework.beans.factory.function.ConcurrentHashFilter.Segment;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ConcurrentHashFilter}.
 *
 * @author Phillip Webb
 */
class ConcurrentHashFilterTests {

	private ConcurrentHashFilter<String, Character> filter = new ConcurrentHashFilter<>(
			new LettersHashCodesExtractor());

	@Test
	void createWhenHashCodesExtractorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new ConcurrentHashFilter<>(null)).withMessage(
						"HashCodesExtractor must not be null");
	}

	@Test
	void createWhenCollisionThresholdIsNegativeThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConcurrentHashFilter<>(
				new LettersHashCodesExtractor(), -128, 32, 0.7f, 8)).withMessage(
						"Collision threshold must not be negative");
	}

	@Test
	void createWhenInitialCapacityIsNegativeThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConcurrentHashFilter<>(
				new LettersHashCodesExtractor(), 128, -32, 0.7f, 8)).withMessage(
						"Initial capacity must not be negative");
	}

	@Test
	void createWhenLoadFactorIsNegativeThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new ConcurrentHashFilter<>(new LettersHashCodesExtractor(), 128, 32,
						-0.7f, 8)).withMessage("Load factor must be positive");
	}

	@Test
	void createWhenConcurrencyLevelIsNegativeThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new ConcurrentHashFilter<>(new LettersHashCodesExtractor(), 128, 32,
						0.7f, -8)).withMessage("Concurrency level must be positive");
	}

	@Test
	void addWhenElementIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.filter.add(null));
	}

	@Test
	void addExtractsHashCodes() {
		LettersHashCodesExtractor extractor = spy(LettersHashCodesExtractor.class);
		ConcurrentHashFilter<String, Character> filter = new ConcurrentHashFilter<>(
				extractor);
		filter.add("test");
		verify(extractor).extract(eq("test"), any());
	}

	@Test
	void addWhenSingleHashToElemet() {
		this.filter.add("test");
		Candidates<String> candidates = this.filter.findCandidates('t');
		assertThat(candidates).containsExactly("test");
		assertThat(candidates.getClass().getName()).contains("Single");
	}

	@Test
	void addWhenMultipleHashToElemet() {
		this.filter.add("test");
		this.filter.add("tset");
		Candidates<String> candidates = this.filter.findCandidates('t');
		assertThat(candidates).containsExactly("test", "tset");
		assertThat(candidates.getClass().getName()).contains("Elements");
	}

	@Test
	void removeWhenElementIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.filter.remove(null));
	}

	@Test
	void removeExtractsHashCodes() {
		LettersHashCodesExtractor extractor = spy(LettersHashCodesExtractor.class);
		ConcurrentHashFilter<String, Character> filter = new ConcurrentHashFilter<>(
				extractor);
		filter.remove("test");
		verify(extractor).extract(eq("test"), any());
	}

	@Test
	void removeWhenSingleHashToElement() {
		this.filter.add("test");
		this.filter.remove("test");
		Candidates<String> candidates = this.filter.findCandidates('t');
		assertThat(candidates.isEmpty()).isTrue();
	}

	@Test
	void removeWhenMultipleHashToElement() {
		this.filter.add("test");
		this.filter.add("tset");
		this.filter.remove("test");
		assertThat(this.filter.findCandidates('t')).containsExactly("tset");
		this.filter.remove("tset");
		assertThat(this.filter.findCandidates('t').isEmpty()).isTrue();
	}

	@Test
	void removeWhenSingleHashToElementAndNotValueMatch() {
		this.filter.add("test");
		this.filter.remove("tset");
		Candidates<String> candidates = this.filter.findCandidates('t');
		assertThat(candidates.isEmpty()).isFalse();
	}

	@Test
	void findCandidateWhenAttributeIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.filter.findCandidates(null)).withMessage(
						"Attribute must not be null");
	}

	@Test
	void findCandidateReturnsMatchingCandidates() {
		this.filter.add("spring");
		this.filter.add("summer");
		this.filter.add("autum");
		this.filter.add("winter");
		assertThat(this.filter.findCandidates('s')).containsExactly("spring", "summer");
		assertThat(this.filter.findCandidates('p')).containsExactly("spring");
		assertThat(this.filter.findCandidates('r')).containsExactly("spring", "summer",
				"winter");
	}

	@Test
	void findCandidateWhenMissingReturnsEmpty() {
		this.filter.add("spring");
		this.filter.add("summer");
		this.filter.add("autum");
		this.filter.add("winter");
		assertThat(this.filter.findCandidates('x')).isEmpty();
		assertThat(this.filter.findCandidates('y').isEmpty()).isTrue();
	}

	@Test
	void findCandidateWhenOverCapacityReturnsNull() {
		ConcurrentHashFilter<String, Character> filter = new ConcurrentHashFilter<>(
				new LettersHashCodesExtractor(), 4);
		filter.add("around");
		filter.add("across");
		filter.add("always");
		filter.add("access");
		assertThat(filter.findCandidates('a')).isNotNull();
		filter.add("almost");
		assertThat(filter.findCandidates('a')).isNull();
	}

	@Test
	@SuppressWarnings("rawtypes")
	void addWhenTableOverLoadFactorResizes() throws Exception {
		ConcurrentHashFilter<String, Character> filter = new ConcurrentHashFilter<>(
				new LettersHashCodesExtractor(),
				ConcurrentHashFilter.DEFAULT_COLLISION_THRESHOLD,
				ConcurrentHashFilter.DEFAULT_INITIAL_CAPACITY,
				ConcurrentHashFilter.DEFAULT_LOAD_FACTOR,
				1);
		filter.add("a");
		Field segmentsField = ConcurrentHashFilter.class.getDeclaredField("segments");
		segmentsField.setAccessible(true);
		Segment[] segments = (Segment[]) ReflectionUtils.getField(segmentsField, filter);
		Segment segment = segments[0];
		Field tableField = Segment.class.getDeclaredField("table");
		tableField.setAccessible(true);
		Object[] initialTable = (Object[]) ReflectionUtils.getField(tableField, segment);
		for (char ch = 'a'; ch < 'z'; ch++) {
			filter.add(String.valueOf(ch));
		}
		Object[] resizedTable = (Object[]) ReflectionUtils.getField(tableField, segment);
		assertThat(resizedTable).isNotSameAs(initialTable);
		assertThat(resizedTable.length).isGreaterThan(initialTable.length);
	}

	static class LettersHashCodesExtractor
			implements HashCodesExtractor<String, Character> {

		@Override
		public void extract(String element, HashCodeConsumer<Character> consumer) {
			for (char ch : element.toCharArray()) {
				consumer.accept(ch);
			}
		}

	}

}
