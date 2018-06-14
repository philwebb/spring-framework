/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jms.support.converter;

import javax.jms.BytesMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.junit.Before;
import org.junit.Test;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Arjen Poutsma
 */
public class MarshallingMessageConverterTests {

	private MarshallingMessageConverter converter;

	private Marshaller marshallerMock;

	private Unmarshaller unmarshallerMock;

	private Session sessionMock;


	@Before
	public void setup() throws Exception {
		this.marshallerMock = mock(Marshaller.class);
		this.unmarshallerMock = mock(Unmarshaller.class);
		this.sessionMock = mock(Session.class);
		this.converter = new MarshallingMessageConverter(this.marshallerMock, this.unmarshallerMock);
	}


	@Test
	public void toBytesMessage() throws Exception {
		BytesMessage bytesMessageMock = mock(BytesMessage.class);
		Object toBeMarshalled = new Object();
		given(this.sessionMock.createBytesMessage()).willReturn(bytesMessageMock);

		this.converter.toMessage(toBeMarshalled, this.sessionMock);

		verify(this.marshallerMock).marshal(eq(toBeMarshalled), isA(Result.class));
		verify(bytesMessageMock).writeBytes(isA(byte[].class));
	}

	@Test
	public void fromBytesMessage() throws Exception {
		BytesMessage bytesMessageMock = mock(BytesMessage.class);
		Object unmarshalled = new Object();

		given(bytesMessageMock.getBodyLength()).willReturn(10L);
		given(bytesMessageMock.readBytes(isA(byte[].class))).willReturn(0);
		given(this.unmarshallerMock.unmarshal(isA(Source.class))).willReturn(unmarshalled);

		Object result = this.converter.fromMessage(bytesMessageMock);
		assertEquals("Invalid result", result, unmarshalled);
	}

	@Test
	public void toTextMessage() throws Exception {
		this.converter.setTargetType(MessageType.TEXT);
		TextMessage textMessageMock = mock(TextMessage.class);
		Object toBeMarshalled = new Object();

		given(this.sessionMock.createTextMessage(isA(String.class))).willReturn(textMessageMock);

		this.converter.toMessage(toBeMarshalled, this.sessionMock);

		verify(this.marshallerMock).marshal(eq(toBeMarshalled), isA(Result.class));
	}

	@Test
	public void fromTextMessage() throws Exception {
		TextMessage textMessageMock = mock(TextMessage.class);
		Object unmarshalled = new Object();

		String text = "foo";
		given(textMessageMock.getText()).willReturn(text);
		given(this.unmarshallerMock.unmarshal(isA(Source.class))).willReturn(unmarshalled);

		Object result = this.converter.fromMessage(textMessageMock);
		assertEquals("Invalid result", result, unmarshalled);
	}

}
