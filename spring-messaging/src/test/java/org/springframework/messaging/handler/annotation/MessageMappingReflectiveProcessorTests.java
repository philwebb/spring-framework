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

package org.springframework.messaging.handler.annotation;

import java.lang.reflect.Method;
import java.security.Principal;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.JavaReflectionHint.Category;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MessageMappingReflectiveProcessor}.
 *
 * @author Sebastien Deleuze
 */
public class MessageMappingReflectiveProcessorTests {

	private final MessageMappingReflectiveProcessor processor = new MessageMappingReflectiveProcessor();

	private final ReflectionHints hints = new ReflectionHints();

	@Test
	void registerReflectiveHintsForMethodWithReturnValue() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("returnValue");
		processor.registerReflectionHints(hints, method);
		assertThat(hints.javaReflection()).satisfiesExactlyInAnyOrder(
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(OutgoingMessage.class));
					assertThat(javaReflectionHint.getCategories()).containsExactlyInAnyOrder(
							Category.INVOKE_DECLARED_CONSTRUCTORS,
							Category.DECLARED_FIELDS);
					assertThat(javaReflectionHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
							hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
				},
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithExplicitPayload() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("explicitPayload", IncomingMessage.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.javaReflection()).satisfiesExactlyInAnyOrder(
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(IncomingMessage.class));
					assertThat(javaReflectionHint.getCategories()).containsExactlyInAnyOrder(
							Category.INVOKE_DECLARED_CONSTRUCTORS,
							Category.DECLARED_FIELDS);
					assertThat(javaReflectionHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
							hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
				},
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithImplicitPayload() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("implicitPayload", IncomingMessage.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.javaReflection()).satisfiesExactlyInAnyOrder(
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(IncomingMessage.class)),
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithMessage() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("message", Message.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.javaReflection()).satisfiesExactlyInAnyOrder(
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(IncomingMessage.class)),
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithImplicitPayloadAndIgnoredAnnotations() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("implicitPayloadWithIgnoredAnnotations",
				IncomingMessage.class, Ignored.class, Ignored.class, Ignored.class, MessageHeaders.class,
				MessageHeaderAccessor.class, Principal.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.javaReflection()).satisfiesExactlyInAnyOrder(
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(IncomingMessage.class)),
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForClass() {
		processor.registerReflectionHints(hints, SampleAnnotatedController.class);
		assertThat(hints.javaReflection()).singleElement().satisfies(
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleAnnotatedController.class)));
	}


	static class SampleController {

		@MessageMapping
		OutgoingMessage returnValue() {
			return new OutgoingMessage("message");
		}

		@MessageMapping
		void explicitPayload(@Payload IncomingMessage incomingMessage) {
		}

		@MessageMapping
		void implicitPayload(IncomingMessage incomingMessage) {
		}

		@MessageMapping
		void message(Message<IncomingMessage> message) {
		}

		@MessageMapping
		void implicitPayloadWithIgnoredAnnotations(IncomingMessage incomingMessage,
				@DestinationVariable Ignored destinationVariable,
				@Header Ignored header,
				@Headers Ignored headers,
				MessageHeaders messageHeaders,
				MessageHeaderAccessor messageHeaderAccessor,
				Principal principal) {
		}
	}

	@MessageMapping
	static class SampleAnnotatedController {
	}

	static class IncomingMessage {

		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	static class OutgoingMessage {

		private String message;

		public OutgoingMessage(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	static class Ignored {}
}
