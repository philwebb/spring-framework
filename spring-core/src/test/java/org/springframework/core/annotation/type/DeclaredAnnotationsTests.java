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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.Test;

import org.springframework.core.annotation.type.DeclaredAnnotations.IntrospectionFailures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DeclaredAnnotations}.
 *
 * @author Phillip Webb
 */
public class DeclaredAnnotationsTests {

	@Test
	public void noneProvidesEmptyIterator() {
		assertThat(DeclaredAnnotations.NONE).isEmpty();
	}

	@Test
	public void findWhenContainsReturnsDeclaredAnnotation() {
		DeclaredAnnotations declaredAnnotations = DeclaredAnnotations.of(null,
				DeclaredAnnotation.of("com.example.Service", DeclaredAttributes.NONE),
				DeclaredAnnotation.of("com.example.Component", DeclaredAttributes.NONE));
		assertThat(declaredAnnotations.find("com.example.Component").getType()).isEqualTo(
				"com.example.Component");
	}

	@Test
	public void findWhenMissingReturnsNull() {
		DeclaredAnnotations declaredAnnotations = DeclaredAnnotations.of(null,
				DeclaredAnnotation.of("com.example.Service", DeclaredAttributes.NONE));
		assertThat(declaredAnnotations.find("com.example.Component")).isNull();
	}

	@Test
	public void fromReturnsSimpleDeclaredAnnotations() {
		DeclaredAnnotations declaredAnnotations = DeclaredAnnotations.from(
				WithTestAnnotation.class,
				WithTestAnnotation.class.getDeclaredAnnotations());
		assertThat(declaredAnnotations).isInstanceOf(SimpleDeclaredAnnotations.class);
	}

	@Test
	public void ofReturnsSimpleDeclaredAnnotations() {
		DeclaredAnnotations declaredAnnotations = DeclaredAnnotations.of(null,
				DeclaredAnnotation.of("com.example.Service", DeclaredAttributes.NONE));
		assertThat(declaredAnnotations).isInstanceOf(SimpleDeclaredAnnotations.class);
	}

	@Test
	public void fromWhenThrowingFailuresThrows() {
		assertThatIllegalStateException().isThrownBy(
				() -> DeclaredAnnotations.from(IntrospectionFailures.THROW, null,
						createFailingAnnotation())).withStackTraceContaining(
								"FailFailFail");
	}

	@Test
	public void fromWhenIgnoringFailuresDoesNotThrow() {
		DeclaredAnnotations declaredAnnotations = DeclaredAnnotations.from(
				IntrospectionFailures.IGNORE, null, createFailingAnnotation());
		assertThat(declaredAnnotations).isEmpty();
	}

	@Test
	public void fromWhenLoggingFailuresLogsAndDoesNotThrow() throws Exception {
		Logger logger = getLog4JLogger();
		TestAppender appender = new TestAppender();
		appender.start();
		logger.addAppender(appender);
		try {
			DeclaredAnnotations declaredAnnotations = DeclaredAnnotations.from(
					IntrospectionFailures.LOG, null, createFailingAnnotation());
			assertThat(declaredAnnotations).isEmpty();
			assertThat(appender).anyMatch(
					message -> message.contains("Failed to introspect annotations"));
		}
		finally {
			appender.stop();
			logger.removeAppender(appender);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private TestAnnotation createFailingAnnotation() {
		TestAnnotation annotation = mock(TestAnnotation.class);
		given(annotation.annotationType()).willReturn((Class) TestAnnotation.class);
		given(annotation.value()).willThrow(new NoClassDefFoundError("FailFailFail"));
		return annotation;
	}

	private Logger getLog4JLogger() throws NoSuchFieldException, IllegalAccessException {
		Log log = LogFactory.getLog(DeclaredAnnotations.class);
		Field field = log.getClass().getDeclaredField("logger");
		field.setAccessible(true);
		return (Logger) field.get(log);
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface TestAnnotation {

		Class<?> value() default Void.class;

	}

	@TestAnnotation
	private static class WithTestAnnotation {

	}

	private static class TestAppender extends AbstractAppender
			implements Iterable<String> {

		private final List<String> messages = new ArrayList<>();

		public TestAppender() {
			super("test", null, null);
		}

		@Override
		public void append(LogEvent event) {
			this.messages.add(event.toString());
		}

		@Override
		public Iterator<String> iterator() {
			return this.messages.iterator();
		}

	}

}
