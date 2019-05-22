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

package org.springframework.http.codec.support;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ExtensionRegistry;
import org.junit.Test;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.DataBufferDecoder;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageReader;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.json.Jackson2SmileDecoder;
import org.springframework.http.codec.json.Jackson2SmileEncoder;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link BaseDefaultCodecs}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class CodecConfigurerTests {

	private final CodecConfigurer configurer = new TestCodecConfigurer();

	private final AtomicInteger index = new AtomicInteger(0);


	@Test
	public void defaultReaders() {
		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertEquals(11, readers.size());
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(ByteArrayDecoder.class);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(ByteBufferDecoder.class);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(DataBufferDecoder.class);
		assertThat((Object) readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageReader.class);
		assertStringDecoder(getNextDecoder(readers), true);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(ProtobufDecoder.class);
		assertThat((Object) readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(FormHttpMessageReader.class);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(Jackson2JsonDecoder.class);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(Jackson2SmileDecoder.class);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(Jaxb2XmlDecoder.class);
		assertStringDecoder(getNextDecoder(readers), false);
	}

	@Test
	public void defaultWriters() {
		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		assertEquals(10, writers.size());
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(ByteArrayEncoder.class);
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(ByteBufferEncoder.class);
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(DataBufferEncoder.class);
		assertThat((Object) writers.get(index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageWriter.class);
		assertStringEncoder(getNextEncoder(writers), true);
		assertThat((Object) writers.get(index.getAndIncrement()).getClass()).isEqualTo(ProtobufHttpMessageWriter.class);
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(Jackson2JsonEncoder.class);
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(Jackson2SmileEncoder.class);
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(Jaxb2XmlEncoder.class);
		assertStringEncoder(getNextEncoder(writers), false);
	}

	@Test
	public void defaultAndCustomReaders() {
		Decoder<?> customDecoder1 = mock(Decoder.class);
		Decoder<?> customDecoder2 = mock(Decoder.class);

		given(customDecoder1.canDecode(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customDecoder2.canDecode(ResolvableType.forClass(Object.class), null)).willReturn(true);

		HttpMessageReader<?> customReader1 = mock(HttpMessageReader.class);
		HttpMessageReader<?> customReader2 = mock(HttpMessageReader.class);

		given(customReader1.canRead(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customReader2.canRead(ResolvableType.forClass(Object.class), null)).willReturn(true);

		this.configurer.customCodecs().decoder(customDecoder1);
		this.configurer.customCodecs().decoder(customDecoder2);

		this.configurer.customCodecs().reader(customReader1);
		this.configurer.customCodecs().reader(customReader2);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();

		assertEquals(15, readers.size());
		assertSame(customDecoder1, getNextDecoder(readers));
		assertSame(customReader1, readers.get(this.index.getAndIncrement()));
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(ByteArrayDecoder.class);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(ByteBufferDecoder.class);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(DataBufferDecoder.class);
		assertThat((Object) readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageReader.class);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(StringDecoder.class);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(ProtobufDecoder.class);
		assertThat((Object) readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(FormHttpMessageReader.class);
		assertSame(customDecoder2, getNextDecoder(readers));
		assertSame(customReader2, readers.get(this.index.getAndIncrement()));
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(Jackson2JsonDecoder.class);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(Jackson2SmileDecoder.class);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(Jaxb2XmlDecoder.class);
		assertThat((Object) getNextDecoder(readers).getClass()).isEqualTo(StringDecoder.class);
	}

	@Test
	public void defaultAndCustomWriters() {
		Encoder<?> customEncoder1 = mock(Encoder.class);
		Encoder<?> customEncoder2 = mock(Encoder.class);

		given(customEncoder1.canEncode(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customEncoder2.canEncode(ResolvableType.forClass(Object.class), null)).willReturn(true);

		HttpMessageWriter<?> customWriter1 = mock(HttpMessageWriter.class);
		HttpMessageWriter<?> customWriter2 = mock(HttpMessageWriter.class);

		given(customWriter1.canWrite(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customWriter2.canWrite(ResolvableType.forClass(Object.class), null)).willReturn(true);

		this.configurer.customCodecs().encoder(customEncoder1);
		this.configurer.customCodecs().encoder(customEncoder2);

		this.configurer.customCodecs().writer(customWriter1);
		this.configurer.customCodecs().writer(customWriter2);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();

		assertEquals(14, writers.size());
		assertSame(customEncoder1, getNextEncoder(writers));
		assertSame(customWriter1, writers.get(this.index.getAndIncrement()));
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(ByteArrayEncoder.class);
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(ByteBufferEncoder.class);
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(DataBufferEncoder.class);
		assertThat((Object) writers.get(index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageWriter.class);
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(CharSequenceEncoder.class);
		assertThat((Object) writers.get(index.getAndIncrement()).getClass()).isEqualTo(ProtobufHttpMessageWriter.class);
		assertSame(customEncoder2, getNextEncoder(writers));
		assertSame(customWriter2, writers.get(this.index.getAndIncrement()));
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(Jackson2JsonEncoder.class);
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(Jackson2SmileEncoder.class);
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(Jaxb2XmlEncoder.class);
		assertThat((Object) getNextEncoder(writers).getClass()).isEqualTo(CharSequenceEncoder.class);
	}

	@Test
	public void defaultsOffCustomReaders() {
		Decoder<?> customDecoder1 = mock(Decoder.class);
		Decoder<?> customDecoder2 = mock(Decoder.class);

		given(customDecoder1.canDecode(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customDecoder2.canDecode(ResolvableType.forClass(Object.class), null)).willReturn(true);

		HttpMessageReader<?> customReader1 = mock(HttpMessageReader.class);
		HttpMessageReader<?> customReader2 = mock(HttpMessageReader.class);

		given(customReader1.canRead(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customReader2.canRead(ResolvableType.forClass(Object.class), null)).willReturn(true);

		this.configurer.customCodecs().decoder(customDecoder1);
		this.configurer.customCodecs().decoder(customDecoder2);

		this.configurer.customCodecs().reader(customReader1);
		this.configurer.customCodecs().reader(customReader2);

		this.configurer.registerDefaults(false);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();

		assertEquals(4, readers.size());
		assertSame(customDecoder1, getNextDecoder(readers));
		assertSame(customReader1, readers.get(this.index.getAndIncrement()));
		assertSame(customDecoder2, getNextDecoder(readers));
		assertSame(customReader2, readers.get(this.index.getAndIncrement()));
	}

	@Test
	public void defaultsOffWithCustomWriters() {
		Encoder<?> customEncoder1 = mock(Encoder.class);
		Encoder<?> customEncoder2 = mock(Encoder.class);

		given(customEncoder1.canEncode(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customEncoder2.canEncode(ResolvableType.forClass(Object.class), null)).willReturn(true);

		HttpMessageWriter<?> customWriter1 = mock(HttpMessageWriter.class);
		HttpMessageWriter<?> customWriter2 = mock(HttpMessageWriter.class);

		given(customWriter1.canWrite(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customWriter2.canWrite(ResolvableType.forClass(Object.class), null)).willReturn(true);

		this.configurer.customCodecs().encoder(customEncoder1);
		this.configurer.customCodecs().encoder(customEncoder2);

		this.configurer.customCodecs().writer(customWriter1);
		this.configurer.customCodecs().writer(customWriter2);

		this.configurer.registerDefaults(false);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();

		assertEquals(4, writers.size());
		assertSame(customEncoder1, getNextEncoder(writers));
		assertSame(customWriter1, writers.get(this.index.getAndIncrement()));
		assertSame(customEncoder2, getNextEncoder(writers));
		assertSame(customWriter2, writers.get(this.index.getAndIncrement()));
	}

	@Test
	public void encoderDecoderOverrides() {
		Jackson2JsonDecoder jacksonDecoder = new Jackson2JsonDecoder();
		Jackson2JsonEncoder jacksonEncoder = new Jackson2JsonEncoder();
		ProtobufDecoder protobufDecoder = new ProtobufDecoder(ExtensionRegistry.newInstance());
		ProtobufEncoder protobufEncoder = new ProtobufEncoder();
		Jaxb2XmlEncoder jaxb2Encoder = new Jaxb2XmlEncoder();
		Jaxb2XmlDecoder jaxb2Decoder = new Jaxb2XmlDecoder();

		this.configurer.defaultCodecs().jackson2JsonDecoder(jacksonDecoder);
		this.configurer.defaultCodecs().jackson2JsonEncoder(jacksonEncoder);
		this.configurer.defaultCodecs().protobufDecoder(protobufDecoder);
		this.configurer.defaultCodecs().protobufEncoder(protobufEncoder);
		this.configurer.defaultCodecs().jaxb2Decoder(jaxb2Decoder);
		this.configurer.defaultCodecs().jaxb2Encoder(jaxb2Encoder);

		assertDecoderInstance(jacksonDecoder);
		assertDecoderInstance(protobufDecoder);
		assertDecoderInstance(jaxb2Decoder);
		assertEncoderInstance(jacksonEncoder);
		assertEncoderInstance(protobufEncoder);
		assertEncoderInstance(jaxb2Encoder);
	}

	private Decoder<?> getNextDecoder(List<HttpMessageReader<?>> readers) {
		HttpMessageReader<?> reader = readers.get(this.index.getAndIncrement());
		assertThat((Object) reader.getClass()).isEqualTo(DecoderHttpMessageReader.class);
		return ((DecoderHttpMessageReader<?>) reader).getDecoder();
	}

	private Encoder<?> getNextEncoder(List<HttpMessageWriter<?>> writers) {
		HttpMessageWriter<?> writer = writers.get(this.index.getAndIncrement());
		assertThat((Object) writer.getClass()).isEqualTo(EncoderHttpMessageWriter.class);
		return ((EncoderHttpMessageWriter<?>) writer).getEncoder();
	}

	private void assertStringDecoder(Decoder<?> decoder, boolean textOnly) {
		assertThat((Object) decoder.getClass()).isEqualTo(StringDecoder.class);
		assertThat(decoder.canDecode(ResolvableType.forClass(String.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
		Object expected = !textOnly;
		assertThat((Object) decoder.canDecode(ResolvableType.forClass(String.class), MediaType.TEXT_EVENT_STREAM)).isEqualTo(expected);
	}

	private void assertStringEncoder(Encoder<?> encoder, boolean textOnly) {
		assertThat((Object) encoder.getClass()).isEqualTo(CharSequenceEncoder.class);
		assertThat(encoder.canEncode(ResolvableType.forClass(String.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
		Object expected = !textOnly;
		assertThat((Object) encoder.canEncode(ResolvableType.forClass(String.class), MediaType.TEXT_EVENT_STREAM)).isEqualTo(expected);
	}

	private void assertDecoderInstance(Decoder<?> decoder) {
		assertSame(decoder, this.configurer.getReaders().stream()
				.filter(writer -> writer instanceof DecoderHttpMessageReader)
				.map(writer -> ((DecoderHttpMessageReader<?>) writer).getDecoder())
				.filter(e -> decoder.getClass().equals(e.getClass()))
				.findFirst()
				.filter(e -> e == decoder).orElse(null));
	}

	private void assertEncoderInstance(Encoder<?> encoder) {
		assertSame(encoder, this.configurer.getWriters().stream()
				.filter(writer -> writer instanceof EncoderHttpMessageWriter)
				.map(writer -> ((EncoderHttpMessageWriter<?>) writer).getEncoder())
				.filter(e -> encoder.getClass().equals(e.getClass()))
				.findFirst()
				.filter(e -> e == encoder).orElse(null));
	}


	private static class TestCodecConfigurer extends BaseCodecConfigurer {

		TestCodecConfigurer() {
			super(new TestDefaultCodecs());
		}

		private static class TestDefaultCodecs extends BaseDefaultCodecs {
		}
	}

}
