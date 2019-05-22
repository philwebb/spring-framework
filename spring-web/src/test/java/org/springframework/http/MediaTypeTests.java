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

package org.springframework.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.*;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertFalse;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertSame;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
public class MediaTypeTests {

	@Test
	public void testToString() throws Exception {
		MediaType mediaType = new MediaType("text", "plain", 0.7);
		String result = mediaType.toString();
		assertEquals("Invalid toString() returned", "text/plain;q=0.7", result);
	}

	@Test
	public void slashInType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MediaType("text/plain"));
	}

	@Test
	public void slashInSubtype() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MediaType("text", "/"));
	}

	@Test
	public void getDefaultQualityValue() {
		MediaType mediaType = new MediaType("text", "plain");
		assertEquals("Invalid quality value", 1, mediaType.getQualityValue(), 0D);
	}

	@Test
	public void parseMediaType() throws Exception {
		String s = "audio/*; q=0.2";
		MediaType mediaType = MediaType.parseMediaType(s);
		assertEquals("Invalid type", "audio", mediaType.getType());
		assertEquals("Invalid subtype", "*", mediaType.getSubtype());
		assertEquals("Invalid quality factor", 0.2D, mediaType.getQualityValue(), 0D);
	}

	@Test
	public void parseMediaTypeNoSubtype() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio"));
	}

	@Test
	public void parseMediaTypeNoSubtypeSlash() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/"));
	}

	@Test
	public void parseMediaTypeTypeRange() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("*/json"));
	}

	@Test
	public void parseMediaTypeIllegalType() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio(/basic"));
	}

	@Test
	public void parseMediaTypeIllegalSubtype() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/basic)"));
	}

	@Test
	public void parseMediaTypeEmptyParameterAttribute() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/*;=value"));
	}

	@Test
	public void parseMediaTypeEmptyParameterValue() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/*;attr="));
	}

	@Test
	public void parseMediaTypeIllegalParameterAttribute() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/*;attr<=value"));
	}

	@Test
	public void parseMediaTypeIllegalParameterValue() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/*;attr=v>alue"));
	}

	@Test
	public void parseMediaTypeIllegalQualityFactor() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/basic;q=1.1"));
	}

	@Test
	public void parseMediaTypeIllegalCharset() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("text/html; charset=foo-bar"));
	}

	@Test
	public void parseURLConnectionMediaType() throws Exception {
		String s = "*; q=.2";
		MediaType mediaType = MediaType.parseMediaType(s);
		assertEquals("Invalid type", "*", mediaType.getType());
		assertEquals("Invalid subtype", "*", mediaType.getSubtype());
		assertEquals("Invalid quality factor", 0.2D, mediaType.getQualityValue(), 0D);
	}

	@Test
	public void parseMediaTypes() throws Exception {
		String s = "text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c";
		List<MediaType> mediaTypes = MediaType.parseMediaTypes(s);
		assertNotNull("No media types returned", mediaTypes);
		assertEquals("Invalid amount of media types", 4, mediaTypes.size());

		mediaTypes = MediaType.parseMediaTypes("");
		assertNotNull("No media types returned", mediaTypes);
		assertEquals("Invalid amount of media types", 0, mediaTypes.size());
	}

	@Test
	public void compareTo() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audio = new MediaType("audio");
		MediaType audioWave = new MediaType("audio", "wave");
		MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
		MediaType audioBasic07 = new MediaType("audio", "basic", 0.7);

		// equal
		assertEquals("Invalid comparison result", 0, audioBasic.compareTo(audioBasic));
		assertEquals("Invalid comparison result", 0, audio.compareTo(audio));
		assertEquals("Invalid comparison result", 0, audioBasicLevel.compareTo(audioBasicLevel));

		assertThat(audioBasicLevel.compareTo(audio) > 0).as("Invalid comparison result").isTrue();

		List<MediaType> expected = new ArrayList<>();
		expected.add(audio);
		expected.add(audioBasic);
		expected.add(audioBasicLevel);
		expected.add(audioBasic07);
		expected.add(audioWave);

		List<MediaType> result = new ArrayList<>(expected);
		Random rnd = new Random();
		// shuffle & sort 10 times
		for (int i = 0; i < 10; i++) {
			Collections.shuffle(result, rnd);
			Collections.sort(result);

			for (int j = 0; j < result.size(); j++) {
				assertSame("Invalid media type at " + j + ", run " + i, expected.get(j), result.get(j));
			}
		}
	}

	@Test
	public void compareToConsistentWithEquals() {
		MediaType m1 = MediaType.parseMediaType("text/html; q=0.7; charset=iso-8859-1");
		MediaType m2 = MediaType.parseMediaType("text/html; charset=iso-8859-1; q=0.7");

		assertEquals("Media types not equal", m1, m2);
		assertEquals("compareTo() not consistent with equals", 0, m1.compareTo(m2));
		assertEquals("compareTo() not consistent with equals", 0, m2.compareTo(m1));

		m1 = MediaType.parseMediaType("text/html; q=0.7; charset=iso-8859-1");
		m2 = MediaType.parseMediaType("text/html; Q=0.7; charset=iso-8859-1");
		assertEquals("Media types not equal", m1, m2);
		assertEquals("compareTo() not consistent with equals", 0, m1.compareTo(m2));
		assertEquals("compareTo() not consistent with equals", 0, m2.compareTo(m1));
	}

	@Test
	public void compareToCaseSensitivity() {
		MediaType m1 = new MediaType("audio", "basic");
		MediaType m2 = new MediaType("Audio", "Basic");
		assertEquals("Invalid comparison result", 0, m1.compareTo(m2));
		assertEquals("Invalid comparison result", 0, m2.compareTo(m1));

		m1 = new MediaType("audio", "basic", Collections.singletonMap("foo", "bar"));
		m2 = new MediaType("audio", "basic", Collections.singletonMap("Foo", "bar"));
		assertEquals("Invalid comparison result", 0, m1.compareTo(m2));
		assertEquals("Invalid comparison result", 0, m2.compareTo(m1));

		m1 = new MediaType("audio", "basic", Collections.singletonMap("foo", "bar"));
		m2 = new MediaType("audio", "basic", Collections.singletonMap("foo", "Bar"));
		assertThat(m1.compareTo(m2) != 0).as("Invalid comparison result").isTrue();
		assertThat(m2.compareTo(m1) != 0).as("Invalid comparison result").isTrue();


	}

	@Test
	public void specificityComparator() throws Exception {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audioWave = new MediaType("audio", "wave");
		MediaType audio = new MediaType("audio");
		MediaType audio03 = new MediaType("audio", "*", 0.3);
		MediaType audio07 = new MediaType("audio", "*", 0.7);
		MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
		MediaType textHtml = new MediaType("text", "html");
		MediaType allXml = new MediaType("application", "*+xml");
		MediaType all = MediaType.ALL;

		Comparator<MediaType> comp = MediaType.SPECIFICITY_COMPARATOR;

		// equal
		assertEquals("Invalid comparison result", 0, comp.compare(audioBasic,audioBasic));
		assertEquals("Invalid comparison result", 0, comp.compare(audio, audio));
		assertEquals("Invalid comparison result", 0, comp.compare(audio07, audio07));
		assertEquals("Invalid comparison result", 0, comp.compare(audio03, audio03));
		assertEquals("Invalid comparison result", 0, comp.compare(audioBasicLevel, audioBasicLevel));

		// specific to unspecific
		assertThat(comp.compare(audioBasic, audio) < 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audioBasic, all) < 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audio, all) < 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(MediaType.APPLICATION_XHTML_XML, allXml) < 0).as("Invalid comparison result").isTrue();

		// unspecific to specific
		assertThat(comp.compare(audio, audioBasic) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(allXml, MediaType.APPLICATION_XHTML_XML) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(all, audioBasic) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(all, audio) > 0).as("Invalid comparison result").isTrue();

		// qualifiers
		assertThat(comp.compare(audio, audio07) < 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audio07, audio) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audio07, audio03) < 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audio03, audio07) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audio03, all) < 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(all, audio03) > 0).as("Invalid comparison result").isTrue();

		// other parameters
		assertThat(comp.compare(audioBasic, audioBasicLevel) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audioBasicLevel, audioBasic) < 0).as("Invalid comparison result").isTrue();

		// different types
		assertEquals("Invalid comparison result", 0, comp.compare(audioBasic, textHtml));
		assertEquals("Invalid comparison result", 0, comp.compare(textHtml, audioBasic));

		// different subtypes
		assertEquals("Invalid comparison result", 0, comp.compare(audioBasic, audioWave));
		assertEquals("Invalid comparison result", 0, comp.compare(audioWave, audioBasic));
	}

	@Test
	public void sortBySpecificityRelated() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audio = new MediaType("audio");
		MediaType audio03 = new MediaType("audio", "*", 0.3);
		MediaType audio07 = new MediaType("audio", "*", 0.7);
		MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
		MediaType all = MediaType.ALL;

		List<MediaType> expected = new ArrayList<>();
		expected.add(audioBasicLevel);
		expected.add(audioBasic);
		expected.add(audio);
		expected.add(audio07);
		expected.add(audio03);
		expected.add(all);

		List<MediaType> result = new ArrayList<>(expected);
		Random rnd = new Random();
		// shuffle & sort 10 times
		for (int i = 0; i < 10; i++) {
			Collections.shuffle(result, rnd);
			MediaType.sortBySpecificity(result);

			for (int j = 0; j < result.size(); j++) {
				assertSame("Invalid media type at " + j, expected.get(j), result.get(j));
			}
		}
	}

	@Test
	public void sortBySpecificityUnrelated() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audioWave = new MediaType("audio", "wave");
		MediaType textHtml = new MediaType("text", "html");

		List<MediaType> expected = new ArrayList<>();
		expected.add(textHtml);
		expected.add(audioBasic);
		expected.add(audioWave);

		List<MediaType> result = new ArrayList<>(expected);
		MediaType.sortBySpecificity(result);

		for (int i = 0; i < result.size(); i++) {
			assertSame("Invalid media type at " + i, expected.get(i), result.get(i));
		}

	}

	@Test
	public void qualityComparator() throws Exception {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audioWave = new MediaType("audio", "wave");
		MediaType audio = new MediaType("audio");
		MediaType audio03 = new MediaType("audio", "*", 0.3);
		MediaType audio07 = new MediaType("audio", "*", 0.7);
		MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
		MediaType textHtml = new MediaType("text", "html");
		MediaType allXml = new MediaType("application", "*+xml");
		MediaType all = MediaType.ALL;

		Comparator<MediaType> comp = MediaType.QUALITY_VALUE_COMPARATOR;

		// equal
		assertEquals("Invalid comparison result", 0, comp.compare(audioBasic,audioBasic));
		assertEquals("Invalid comparison result", 0, comp.compare(audio, audio));
		assertEquals("Invalid comparison result", 0, comp.compare(audio07, audio07));
		assertEquals("Invalid comparison result", 0, comp.compare(audio03, audio03));
		assertEquals("Invalid comparison result", 0, comp.compare(audioBasicLevel, audioBasicLevel));

		// specific to unspecific
		assertThat(comp.compare(audioBasic, audio) < 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audioBasic, all) < 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audio, all) < 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(MediaType.APPLICATION_XHTML_XML, allXml) < 0).as("Invalid comparison result").isTrue();

		// unspecific to specific
		assertThat(comp.compare(audio, audioBasic) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(all, audioBasic) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(all, audio) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(allXml, MediaType.APPLICATION_XHTML_XML) > 0).as("Invalid comparison result").isTrue();

		// qualifiers
		assertThat(comp.compare(audio, audio07) < 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audio07, audio) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audio07, audio03) < 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audio03, audio07) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audio03, all) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(all, audio03) < 0).as("Invalid comparison result").isTrue();

		// other parameters
		assertThat(comp.compare(audioBasic, audioBasicLevel) > 0).as("Invalid comparison result").isTrue();
		assertThat(comp.compare(audioBasicLevel, audioBasic) < 0).as("Invalid comparison result").isTrue();

		// different types
		assertEquals("Invalid comparison result", 0, comp.compare(audioBasic, textHtml));
		assertEquals("Invalid comparison result", 0, comp.compare(textHtml, audioBasic));

		// different subtypes
		assertEquals("Invalid comparison result", 0, comp.compare(audioBasic, audioWave));
		assertEquals("Invalid comparison result", 0, comp.compare(audioWave, audioBasic));
	}

	@Test
	public void sortByQualityRelated() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audio = new MediaType("audio");
		MediaType audio03 = new MediaType("audio", "*", 0.3);
		MediaType audio07 = new MediaType("audio", "*", 0.7);
		MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
		MediaType all = MediaType.ALL;

		List<MediaType> expected = new ArrayList<>();
		expected.add(audioBasicLevel);
		expected.add(audioBasic);
		expected.add(audio);
		expected.add(all);
		expected.add(audio07);
		expected.add(audio03);

		List<MediaType> result = new ArrayList<>(expected);
		Random rnd = new Random();
		// shuffle & sort 10 times
		for (int i = 0; i < 10; i++) {
			Collections.shuffle(result, rnd);
			MediaType.sortByQualityValue(result);

			for (int j = 0; j < result.size(); j++) {
				assertSame("Invalid media type at " + j, expected.get(j), result.get(j));
			}
		}
	}

	@Test
	public void sortByQualityUnrelated() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audioWave = new MediaType("audio", "wave");
		MediaType textHtml = new MediaType("text", "html");

		List<MediaType> expected = new ArrayList<>();
		expected.add(textHtml);
		expected.add(audioBasic);
		expected.add(audioWave);

		List<MediaType> result = new ArrayList<>(expected);
		MediaType.sortBySpecificity(result);

		for (int i = 0; i < result.size(); i++) {
			assertSame("Invalid media type at " + i, expected.get(i), result.get(i));
		}
	}

	@Test
	public void testWithConversionService() {
		ConversionService conversionService = new DefaultConversionService();
		assertThat(conversionService.canConvert(String.class, MediaType.class)).isTrue();
		MediaType mediaType = MediaType.parseMediaType("application/xml");
		assertEquals(mediaType, conversionService.convert("application/xml", MediaType.class));
	}

	@Test
	public void isConcrete() {
		assertThat(MediaType.TEXT_PLAIN.isConcrete()).as("text/plain not concrete").isTrue();
		assertFalse("*/* concrete", MediaType.ALL.isConcrete());
		assertFalse("text/* concrete", new MediaType("text", "*").isConcrete());
	}

}
