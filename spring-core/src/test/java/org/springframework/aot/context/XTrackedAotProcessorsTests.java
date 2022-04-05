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

package org.springframework.aot.context;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aot.context.XTrackedAotProcessors.FileTracker;
import org.springframework.aot.context.XTrackedAotProcessors.Tracker;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.InMemoryGeneratedFiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link XTrackedAotProcessors}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class XTrackedAotProcessorsTests {

	private final List<Object> contributed = new ArrayList<>();

	private Object lastContributed;

	private final XTrackedAotProcessors processors = new XTrackedAotProcessors(this::contribute, new TestTracker());

	@Test
	void createWhenApplyActionIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new XTrackedAotProcessors(null, new TestTracker()))
				.withMessage("'applyAction' must not be null");
	}

	@Test
	void createWhenTrackerIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new XTrackedAotProcessors(this::contribute, null))
				.withMessage("'tracker' must not be null");
	}

	@Test
	void addAddsAotProcessor() {
		TestAotStringStringProcessor processor1 = new ContributingAotStringStringProcessor();
		TestAotStringStringProcessor processor2 = new NonContributingAotStringStringProcessor();
		this.processors.add(processor1);
		assertThat(this.processors.contains(processor1)).isTrue();
		assertThat(this.processors.contains(processor2)).isFalse();
	}

	@Test
	void addWhenAotProcessorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.processors.add(null))
				.withMessage("'aotProcessor' must not be null");
	}

	@Test
	void removeRemovesAotProcessors() {
		TestAotStringStringProcessor processor1 = new ContributingAotStringStringProcessor();
		TestAotStringStringProcessor processor2 = new NonContributingAotStringStringProcessor();
		this.processors.add(processor1);
		this.processors.add(processor2);
		this.processors.remove(processor2);
		assertThat(this.processors.contains(processor1)).isTrue();
		assertThat(this.processors.contains(processor2)).isFalse();
	}

	@Test
	void removeWhenAotProcessorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.processors.remove(null))
				.withMessage("'aotProcessor' must not be null");
	}

	@Test
	void containsReturnsTrueIfContainsProcessor() {
		TestAotStringStringProcessor processor = new ContributingAotStringStringProcessor();
		this.processors.add(processor);
		assertThat(this.processors.contains(processor)).isTrue();
	}

	@Test
	void containsReturnsFalseIfDoesNotContainProcessor() {
		TestAotStringStringProcessor processor = new ContributingAotStringStringProcessor();
		assertThat(this.processors.contains(processor)).isFalse();
	}

	@Test
	void allOfTypeWhenProcessorTypeIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.processors.allOfType(null))
				.withMessage("'processorType' must not be null");
	}

	@Test
	void allOfTypeWhenProcessorTypeIsNotInterfaceThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.processors.allOfType(ContributingAotStringStringProcessor.class))
				.withMessage("'processorType' must be a subinterface of AotProcessor");
	}

	@Test
	@SuppressWarnings("unchecked")
	void allOfTypeWhenProcessorTypeIsNotSubInterfaceThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.processors.allOfType(AotProcessor.class))
				.withMessage("'processorType' must be a subinterface of AotProcessor");
	}

	@Test
	void processAndApplyContributionsWhenMultipleTypesAppliesOnlyToTypeMatched() {
		this.processors.add(new ContributingAotStringStringProcessor());
		this.processors.add(new ContributingAotIntegerIntegerProcessor());
		this.processors.allOfType(TestAotStringStringProcessor.class).processAndApplyContributions("a", "b");
		assertThat(this.contributed).containsExactly("ab");
	}

	@Test
	void processAndApplyContributionsWithAndAppliesToBoth() {
		this.processors.add(new ContributingAotStringStringProcessor("1"));
		Set<ContributingAotStringStringProcessor> additional = Collections
				.singleton(new ContributingAotStringStringProcessor("2"));
		this.processors.allOfType(TestAotStringStringProcessor.class).and(additional).processAndApplyContributions("a",
				"b");
		assertThat(this.contributed).containsExactly("1ab", "2ab");
	}

	@Test
	void processAndApplyContributionsWhenNameIsClassShouldUseClassName() {
		Tracker tracker = mock(Tracker.class);
		XTrackedAotProcessors processors = new XTrackedAotProcessors(this::contribute, tracker);
		processors.add(new NonContributingAotClassStringProcessor());
		processors.allOfType(TestAotClassStringProcessor.class).processAndApplyContributions(InputStream.class, "?");
		verify(tracker).shouldSkip(NonContributingAotClassStringProcessor.class, "java.io.InputStream");
	}

	@Test
	void processAndApplyWhenTrackerSkipsShouldSkip() {
		Tracker tracker = mock(Tracker.class);
		given(tracker.shouldSkip(ContributingAotStringStringProcessor.class, "name")).willReturn(true);
		XTrackedAotProcessors processors = new XTrackedAotProcessors(this::contribute, tracker);
		processors.add(new ContributingAotStringStringProcessor());
		processors.allOfType(TestAotStringStringProcessor.class).processAndApplyContributions("name", "value");
		verify(tracker).shouldSkip(ContributingAotStringStringProcessor.class, "name");
		verifyNoMoreInteractions(tracker);
		assertThat(this.lastContributed).isNull();
	}

	@Test
	void processAndApplyContributionsWhenContributesShouldMarksProcessed() {
		Tracker tracker = mock(Tracker.class);
		XTrackedAotProcessors processors = new XTrackedAotProcessors(this::contribute, tracker);
		processors.add(new ContributingAotStringStringProcessor());
		processors.allOfType(TestAotStringStringProcessor.class).processAndApplyContributions("name", "value");
		verify(tracker).markProcessed(ContributingAotStringStringProcessor.class, "name");
	}

	@Test
	void processAndApplyContributionsWhenNoContributionMarksProcessed() {
		Tracker tracker = mock(Tracker.class);
		XTrackedAotProcessors processors = new XTrackedAotProcessors(this::contribute, tracker);
		processors.add(new NonContributingAotStringStringProcessor());
		processors.allOfType(TestAotStringStringProcessor.class).processAndApplyContributions("name", "value");
		verify(tracker).markProcessed(NonContributingAotStringStringProcessor.class, "name");
	}

	@Test
	void processAndApplyContributionsAppliesContribution() {
		XTrackedAotProcessors processors = new XTrackedAotProcessors(this::contribute, new TestTracker());
		processors.add(new ContributingAotStringStringProcessor());
		processors.allOfType(TestAotStringStringProcessor.class).processAndApplyContributions("spring", "framework");
		assertThat(this.lastContributed).isEqualTo("springframework");
	}

	private void contribute(XAotContribution contribution) {
		if (contribution instanceof TestAotContribution) {
			Object value = ((TestAotContribution) contribution).getValue();
			this.contributed.add(value);
			this.lastContributed = value;
		}
	}

	/**
	 * Tests for {@link FileTracker}.
	 */
	@Nested
	class FileTrackerTests {

		@Test
		void shouldSkipWhenInFileAndNotProcessedReturnsTrue() {
			FileTracker tracker = new FileTracker(null, this::getTestResourceLocation);
			assertThat(tracker.shouldSkip(ContributingAotStringStringProcessor.class, "a")).isTrue();
		}

		@Test
		void shouldSkipWhenNotInFileAndNotProcessedReturnsFalse() {
			FileTracker tracker = new FileTracker(null, this::getTestResourceLocation);
			assertThat(tracker.shouldSkip(ContributingAotStringStringProcessor.class, "b")).isFalse();
		}

		@Test
		void shouldSkipWhenNotInFileAndProcessedReturnsFalse() {
			FileTracker tracker = new FileTracker(null, this::getTestResourceLocation);
			tracker.markProcessed(ContributingAotStringStringProcessor.class, "b");
			assertThat(tracker.shouldSkip(ContributingAotStringStringProcessor.class, "b")).isTrue();
		}

		@Test
		void saveWhenNotFileSavesMarked() throws Exception {
			FileTracker tracker = new FileTracker();
			tracker.markProcessed(ContributingAotStringStringProcessor.class, "b");
			tracker.markProcessed(ContributingAotStringStringProcessor.class, "a");
			tracker.markProcessed(ContributingAotStringStringProcessor.class, "c");
			tracker.markProcessed(ContributingAotIntegerIntegerProcessor.class, "3");
			tracker.markProcessed(ContributingAotIntegerIntegerProcessor.class, "2");
			tracker.markProcessed(ContributingAotIntegerIntegerProcessor.class, "1");
			InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
			tracker.save(generatedFiles);
			assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE,
					"META-INF/spring/org.springframework.aot.context.AotProcessor/"
							+ ContributingAotStringStringProcessor.class.getName() + ".processed"))
									.isEqualTo("a\nb\nc\n");
			assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE,
					"META-INF/spring/org.springframework.aot.context.AotProcessor/"
							+ ContributingAotIntegerIntegerProcessor.class.getName() + ".processed"))
									.isEqualTo("1\n2\n3\n");
		}

		@Test
		void saveWhenSomeInFileSavesOnlyMarked() throws IOException {
			FileTracker tracker = new FileTracker(null, this::getTestResourceLocation);
			tracker.markProcessed(ContributingAotStringStringProcessor.class, "b");
			InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
			tracker.save(generatedFiles);
			assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE,
					getTestResourceLocation(ContributingAotStringStringProcessor.class))).isEqualTo("b\n");
		}

		private String getTestResourceLocation(Class<?> processorImplementationType) {
			return String.format("org/springframework/aot/context/%s.processed", processorImplementationType.getName());
		}

	}

	static class TestTracker implements Tracker {

		@Override
		public boolean shouldSkip(Class<?> processorImplementationType, String name) {
			return false;
		}

		@Override
		public void markProcessed(Class<?> processorImplementationType, String name) {
		}

	}

	static interface TestAotStringStringProcessor extends AotProcessor<String, String> {

	}

	static class ContributingAotStringStringProcessor implements TestAotStringStringProcessor {

		private final String prefix;

		ContributingAotStringStringProcessor() {
			this("");
		}

		ContributingAotStringStringProcessor(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public XAotContribution processAheadOfTime(String name, String instance) {
			return new TestAotContribution(this.prefix + name + instance);
		}

	}

	static class NonContributingAotStringStringProcessor implements TestAotStringStringProcessor {

		@Override
		public XAotContribution processAheadOfTime(String name, String instance) {
			return null;
		}

	}

	static interface TestAotIntegerIntegerProcessor extends AotProcessor<Integer, Integer> {

	}

	static class ContributingAotIntegerIntegerProcessor implements TestAotIntegerIntegerProcessor {

		@Override
		public XAotContribution processAheadOfTime(Integer name, Integer instance) {
			return new TestAotContribution(name + instance);
		}

	}

	static class NonContributingAotIntegerIntegerProcessor implements TestAotIntegerIntegerProcessor {

		@Override
		public XAotContribution processAheadOfTime(Integer name, Integer instance) {
			return null;
		}

	}

	static interface TestAotClassStringProcessor extends AotProcessor<Class<?>, String> {

	}

	static class NonContributingAotClassStringProcessor implements TestAotClassStringProcessor {

		@Override
		public XAotContribution processAheadOfTime(Class<?> name, String instance) {
			return null;
		}

	}

	static class TestAotContribution implements XAotContribution {

		private final Object value;

		TestAotContribution(Object value) {
			this.value = value;
		}

		@Override
		public void applyTo(XAotContext aotContext) {
		}

		public Object getValue() {
			return this.value;
		}

	}
}
