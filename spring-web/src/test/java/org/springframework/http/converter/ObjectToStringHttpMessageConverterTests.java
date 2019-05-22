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

package org.springframework.http.converter;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;

/**
 * Test cases for {@link ObjectToStringHttpMessageConverter} class.
 *
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @author Rossen Stoyanchev
 */
public class ObjectToStringHttpMessageConverterTests {

	private ObjectToStringHttpMessageConverter converter;

	private MockHttpServletResponse servletResponse;

	private ServletServerHttpResponse response;


	@Before
	public void setup() {
		ConversionService conversionService = new DefaultConversionService();
		this.converter = new ObjectToStringHttpMessageConverter(conversionService);

		this.servletResponse = new MockHttpServletResponse();
		this.response = new ServletServerHttpResponse(this.servletResponse);
	}


	@Test
	public void canRead() {
		assertThat(this.converter.canRead(Math.class, null)).isFalse();
		assertThat(this.converter.canRead(Resource.class, null)).isFalse();

		assertThat(this.converter.canRead(Locale.class, null)).isTrue();
		assertThat(this.converter.canRead(BigInteger.class, null)).isTrue();

		assertThat(this.converter.canRead(BigInteger.class, MediaType.TEXT_HTML)).isFalse();
		assertThat(this.converter.canRead(BigInteger.class, MediaType.TEXT_XML)).isFalse();
		assertThat(this.converter.canRead(BigInteger.class, MediaType.APPLICATION_XML)).isFalse();
	}

	@Test
	public void canWrite() {
		assertThat(this.converter.canWrite(Math.class, null)).isFalse();
		assertThat(this.converter.canWrite(Resource.class, null)).isFalse();

		assertThat(this.converter.canWrite(Locale.class, null)).isTrue();
		assertThat(this.converter.canWrite(Double.class, null)).isTrue();

		assertThat(this.converter.canWrite(BigInteger.class, MediaType.TEXT_HTML)).isFalse();
		assertThat(this.converter.canWrite(BigInteger.class, MediaType.TEXT_XML)).isFalse();
		assertThat(this.converter.canWrite(BigInteger.class, MediaType.APPLICATION_XML)).isFalse();

		assertThat(this.converter.canWrite(BigInteger.class, MediaType.valueOf("text/*"))).isTrue();
	}

	@Test
	public void defaultCharset() throws IOException {
		this.converter.write(Integer.valueOf(5), null, response);

		assertThat(servletResponse.getCharacterEncoding()).isEqualTo("ISO-8859-1");
	}

	@Test
	public void defaultCharsetModified() throws IOException {
		ConversionService cs = new DefaultConversionService();
		ObjectToStringHttpMessageConverter converter = new ObjectToStringHttpMessageConverter(cs, StandardCharsets.UTF_16);
		converter.write((byte) 31, null, this.response);

		assertThat(this.servletResponse.getCharacterEncoding()).isEqualTo("UTF-16");
	}

	@Test
	public void writeAcceptCharset() throws IOException {
		this.converter.setWriteAcceptCharset(true);
		this.converter.write(new Date(), null, this.response);

		assertNotNull(this.servletResponse.getHeader("Accept-Charset"));
	}

	@Test
	public void writeAcceptCharsetTurnedOff() throws IOException {
		this.converter.setWriteAcceptCharset(false);
		this.converter.write(new Date(), null, this.response);

		assertNull(this.servletResponse.getHeader("Accept-Charset"));
	}

	@Test
	public void read() throws IOException {
		Short shortValue = Short.valueOf((short) 781);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType(MediaType.TEXT_PLAIN_VALUE);
		request.setContent(shortValue.toString().getBytes(StringHttpMessageConverter.DEFAULT_CHARSET));
		assertThat(this.converter.read(Short.class, new ServletServerHttpRequest(request))).isEqualTo(shortValue);

		Float floatValue = Float.valueOf(123);
		request = new MockHttpServletRequest();
		request.setContentType(MediaType.TEXT_PLAIN_VALUE);
		request.setCharacterEncoding("UTF-16");
		request.setContent(floatValue.toString().getBytes("UTF-16"));
		assertThat(this.converter.read(Float.class, new ServletServerHttpRequest(request))).isEqualTo(floatValue);

		Long longValue = Long.valueOf(55819182821331L);
		request = new MockHttpServletRequest();
		request.setContentType(MediaType.TEXT_PLAIN_VALUE);
		request.setCharacterEncoding("UTF-8");
		request.setContent(longValue.toString().getBytes("UTF-8"));
		assertThat(this.converter.read(Long.class, new ServletServerHttpRequest(request))).isEqualTo(longValue);
	}

	@Test
	public void write() throws IOException {
		this.converter.write((byte) -8, null, this.response);

		assertThat(this.servletResponse.getCharacterEncoding()).isEqualTo("ISO-8859-1");
		assertThat(this.servletResponse.getContentType().startsWith(MediaType.TEXT_PLAIN_VALUE)).isTrue();
		assertEquals(2, this.servletResponse.getContentLength());
		assertThat(this.servletResponse.getContentAsByteArray()).isEqualTo(new byte[] { '-', '8' });
	}

	@Test
	public void writeUtf16() throws IOException {
		MediaType contentType = new MediaType("text", "plain", StandardCharsets.UTF_16);
		this.converter.write(Integer.valueOf(958), contentType, this.response);

		assertThat(this.servletResponse.getCharacterEncoding()).isEqualTo("UTF-16");
		assertThat(this.servletResponse.getContentType().startsWith(MediaType.TEXT_PLAIN_VALUE)).isTrue();
		assertEquals(8, this.servletResponse.getContentLength());
		// First two bytes: byte order mark
		assertThat(this.servletResponse.getContentAsByteArray()).isEqualTo(new byte[] { -2, -1, 0, '9', 0, '5', 0, '8' });
	}

	@Test
	public void testConversionServiceRequired() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ObjectToStringHttpMessageConverter(null));
	}

}
