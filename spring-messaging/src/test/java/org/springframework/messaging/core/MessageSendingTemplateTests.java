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

package org.springframework.messaging.core;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.*;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertSame;

/**
 * Unit tests for {@link AbstractMessageSendingTemplate}.
 *
 * @author Rossen Stoyanchev
 */
public class MessageSendingTemplateTests {

	private TestMessageSendingTemplate template;

	private TestMessagePostProcessor postProcessor;

	private Map<String, Object> headers;


	@Before
	public void setup() {
		this.template = new TestMessageSendingTemplate();
		this.postProcessor = new TestMessagePostProcessor();
		this.headers = new HashMap<>();
		this.headers.put("key", "value");
	}

	@Test
	public void send() {
		Message<?> message = new GenericMessage<Object>("payload");
		this.template.setDefaultDestination("home");
		this.template.send(message);

		assertThat((Object) this.template.destination).isEqualTo("home");
		assertSame(message, this.template.message);
	}

	@Test
	public void sendToDestination() {
		Message<?> message = new GenericMessage<Object>("payload");
		this.template.send("somewhere", message);

		assertThat((Object) this.template.destination).isEqualTo("somewhere");
		assertSame(message, this.template.message);
	}

	@Test
	public void sendMissingDestination() {
		Message<?> message = new GenericMessage<Object>("payload");
		assertThatIllegalStateException().isThrownBy(() ->
				this.template.send(message));
	}

	@Test
	public void convertAndSend() {
		this.template.convertAndSend("somewhere", "payload", headers, this.postProcessor);

		assertThat((Object) this.template.destination).isEqualTo("somewhere");
		assertNotNull(this.template.message);
		assertThat(this.template.message.getHeaders().get("key")).isEqualTo("value");
		assertThat(this.template.message.getPayload()).isEqualTo("payload");

		assertNotNull(this.postProcessor.getMessage());
		assertSame(this.template.message, this.postProcessor.getMessage());
	}

	@Test
	public void convertAndSendPayload() {
		this.template.setDefaultDestination("home");
		this.template.convertAndSend("payload");

		assertThat((Object) this.template.destination).isEqualTo("home");
		assertNotNull(this.template.message);
		assertEquals("expected 'id' and 'timestamp' headers only", 2, this.template.message.getHeaders().size());
		assertThat(this.template.message.getPayload()).isEqualTo("payload");
	}

	@Test
	public void convertAndSendPayloadToDestination() {
		this.template.convertAndSend("somewhere", "payload");

		assertThat((Object) this.template.destination).isEqualTo("somewhere");
		assertNotNull(this.template.message);
		assertEquals("expected 'id' and 'timestamp' headers only", 2, this.template.message.getHeaders().size());
		assertThat(this.template.message.getPayload()).isEqualTo("payload");
	}

	@Test
	public void convertAndSendPayloadAndHeadersToDestination() {
		this.template.convertAndSend("somewhere", "payload", headers);

		assertThat((Object) this.template.destination).isEqualTo("somewhere");
		assertNotNull(this.template.message);
		assertThat(this.template.message.getHeaders().get("key")).isEqualTo("value");
		assertThat(this.template.message.getPayload()).isEqualTo("payload");
	}

	@Test
	public void convertAndSendPayloadAndMutableHeadersToDestination() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setHeader("foo", "bar");
		accessor.setLeaveMutable(true);
		MessageHeaders messageHeaders = accessor.getMessageHeaders();

		this.template.setMessageConverter(new StringMessageConverter());
		this.template.convertAndSend("somewhere", "payload", messageHeaders);

		MessageHeaders actual = this.template.message.getHeaders();
		assertSame(messageHeaders, actual);
		assertThat(actual.get(MessageHeaders.CONTENT_TYPE)).isEqualTo(new MimeType("text", "plain", StandardCharsets.UTF_8));
		assertThat(actual.get("foo")).isEqualTo("bar");
	}

	@Test
	public void convertAndSendPayloadWithPostProcessor() {
		this.template.setDefaultDestination("home");
		this.template.convertAndSend((Object) "payload", this.postProcessor);

		assertThat((Object) this.template.destination).isEqualTo("home");
		assertNotNull(this.template.message);
		assertEquals("expected 'id' and 'timestamp' headers only", 2, this.template.message.getHeaders().size());
		assertThat(this.template.message.getPayload()).isEqualTo("payload");

		assertNotNull(this.postProcessor.getMessage());
		assertSame(this.template.message, this.postProcessor.getMessage());
	}

	@Test
	public void convertAndSendPayloadWithPostProcessorToDestination() {
		this.template.convertAndSend("somewhere", "payload", this.postProcessor);

		assertThat((Object) this.template.destination).isEqualTo("somewhere");
		assertNotNull(this.template.message);
		assertEquals("expected 'id' and 'timestamp' headers only", 2, this.template.message.getHeaders().size());
		assertThat(this.template.message.getPayload()).isEqualTo("payload");

		assertNotNull(this.postProcessor.getMessage());
		assertSame(this.template.message, this.postProcessor.getMessage());
	}

	@Test
	public void convertAndSendNoMatchingConverter() {

		MessageConverter converter = new CompositeMessageConverter(
				Arrays.<MessageConverter>asList(new MappingJackson2MessageConverter()));
		this.template.setMessageConverter(converter);

		this.headers.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_XML);
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				this.template.convertAndSend("home", "payload", new MessageHeaders(this.headers)));
	}


	private static class TestMessageSendingTemplate extends AbstractMessageSendingTemplate<String> {

		private String destination;

		private Message<?> message;

		@Override
		protected void doSend(String destination, Message<?> message) {
			this.destination = destination;
			this.message = message;
		}
	}

}

class TestMessagePostProcessor implements MessagePostProcessor {

	private Message<?> message;


	Message<?> getMessage() {
		return this.message;
	}

	@Override
	public Message<?> postProcessMessage(Message<?> message) {
		this.message = message;
		return message;
	}
}
