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

package org.springframework.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link AntPathMatcher}.
 *
 * @author Alef Arendsen
 * @author Seth Ladd
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class AntPathMatcherTests {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void match() {
		// test exact matching
		assertTrue(this.pathMatcher.match("test", "test"));
		assertTrue(this.pathMatcher.match("/test", "/test"));
		assertTrue(this.pathMatcher.match("http://example.org", "http://example.org")); // SPR-14141
		assertFalse(this.pathMatcher.match("/test.jpg", "test.jpg"));
		assertFalse(this.pathMatcher.match("test", "/test"));
		assertFalse(this.pathMatcher.match("/test", "test"));

		// test matching with ?'s
		assertTrue(this.pathMatcher.match("t?st", "test"));
		assertTrue(this.pathMatcher.match("??st", "test"));
		assertTrue(this.pathMatcher.match("tes?", "test"));
		assertTrue(this.pathMatcher.match("te??", "test"));
		assertTrue(this.pathMatcher.match("?es?", "test"));
		assertFalse(this.pathMatcher.match("tes?", "tes"));
		assertFalse(this.pathMatcher.match("tes?", "testt"));
		assertFalse(this.pathMatcher.match("tes?", "tsst"));

		// test matching with *'s
		assertTrue(this.pathMatcher.match("*", "test"));
		assertTrue(this.pathMatcher.match("test*", "test"));
		assertTrue(this.pathMatcher.match("test*", "testTest"));
		assertTrue(this.pathMatcher.match("test/*", "test/Test"));
		assertTrue(this.pathMatcher.match("test/*", "test/t"));
		assertTrue(this.pathMatcher.match("test/*", "test/"));
		assertTrue(this.pathMatcher.match("*test*", "AnothertestTest"));
		assertTrue(this.pathMatcher.match("*test", "Anothertest"));
		assertTrue(this.pathMatcher.match("*.*", "test."));
		assertTrue(this.pathMatcher.match("*.*", "test.test"));
		assertTrue(this.pathMatcher.match("*.*", "test.test.test"));
		assertTrue(this.pathMatcher.match("test*aaa", "testblaaaa"));
		assertFalse(this.pathMatcher.match("test*", "tst"));
		assertFalse(this.pathMatcher.match("test*", "tsttest"));
		assertFalse(this.pathMatcher.match("test*", "test/"));
		assertFalse(this.pathMatcher.match("test*", "test/t"));
		assertFalse(this.pathMatcher.match("test/*", "test"));
		assertFalse(this.pathMatcher.match("*test*", "tsttst"));
		assertFalse(this.pathMatcher.match("*test", "tsttst"));
		assertFalse(this.pathMatcher.match("*.*", "tsttst"));
		assertFalse(this.pathMatcher.match("test*aaa", "test"));
		assertFalse(this.pathMatcher.match("test*aaa", "testblaaab"));

		// test matching with ?'s and /'s
		assertTrue(this.pathMatcher.match("/?", "/a"));
		assertTrue(this.pathMatcher.match("/?/a", "/a/a"));
		assertTrue(this.pathMatcher.match("/a/?", "/a/b"));
		assertTrue(this.pathMatcher.match("/??/a", "/aa/a"));
		assertTrue(this.pathMatcher.match("/a/??", "/a/bb"));
		assertTrue(this.pathMatcher.match("/?", "/a"));

		// test matching with **'s
		assertTrue(this.pathMatcher.match("/**", "/testing/testing"));
		assertTrue(this.pathMatcher.match("/*/**", "/testing/testing"));
		assertTrue(this.pathMatcher.match("/**/*", "/testing/testing"));
		assertTrue(this.pathMatcher.match("/bla/**/bla", "/bla/testing/testing/bla"));
		assertTrue(this.pathMatcher.match("/bla/**/bla", "/bla/testing/testing/bla/bla"));
		assertTrue(this.pathMatcher.match("/**/test", "/bla/bla/test"));
		assertTrue(this.pathMatcher.match("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla"));
		assertTrue(this.pathMatcher.match("/bla*bla/test", "/blaXXXbla/test"));
		assertTrue(this.pathMatcher.match("/*bla/test", "/XXXbla/test"));
		assertFalse(this.pathMatcher.match("/bla*bla/test", "/blaXXXbl/test"));
		assertFalse(this.pathMatcher.match("/*bla/test", "XXXblab/test"));
		assertFalse(this.pathMatcher.match("/*bla/test", "XXXbl/test"));

		assertFalse(this.pathMatcher.match("/????", "/bala/bla"));
		assertFalse(this.pathMatcher.match("/**/*bla", "/bla/bla/bla/bbb"));

		assertTrue(this.pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/"));
		assertTrue(this.pathMatcher.match("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing"));
		assertTrue(this.pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing"));
		assertTrue(this.pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg"));

		assertTrue(this.pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/"));
		assertTrue(this.pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing"));
		assertTrue(this.pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing"));
		assertFalse(this.pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing"));

		assertFalse(this.pathMatcher.match("/x/x/**/bla", "/x/x/x/"));

		assertTrue(this.pathMatcher.match("/foo/bar/**", "/foo/bar")) ;

		assertTrue(this.pathMatcher.match("", ""));

		assertTrue(this.pathMatcher.match("/{bla}.*", "/testing.html"));
	}

	// SPR-14247
	@Test
	public void matchWithTrimTokensEnabled() throws Exception {
		this.pathMatcher.setTrimTokens(true);

		assertTrue(this.pathMatcher.match("/foo/bar", "/foo /bar"));
	}

	@Test
	public void withMatchStart() {
		// test exact matching
		assertTrue(this.pathMatcher.matchStart("test", "test"));
		assertTrue(this.pathMatcher.matchStart("/test", "/test"));
		assertFalse(this.pathMatcher.matchStart("/test.jpg", "test.jpg"));
		assertFalse(this.pathMatcher.matchStart("test", "/test"));
		assertFalse(this.pathMatcher.matchStart("/test", "test"));

		// test matching with ?'s
		assertTrue(this.pathMatcher.matchStart("t?st", "test"));
		assertTrue(this.pathMatcher.matchStart("??st", "test"));
		assertTrue(this.pathMatcher.matchStart("tes?", "test"));
		assertTrue(this.pathMatcher.matchStart("te??", "test"));
		assertTrue(this.pathMatcher.matchStart("?es?", "test"));
		assertFalse(this.pathMatcher.matchStart("tes?", "tes"));
		assertFalse(this.pathMatcher.matchStart("tes?", "testt"));
		assertFalse(this.pathMatcher.matchStart("tes?", "tsst"));

		// test matching with *'s
		assertTrue(this.pathMatcher.matchStart("*", "test"));
		assertTrue(this.pathMatcher.matchStart("test*", "test"));
		assertTrue(this.pathMatcher.matchStart("test*", "testTest"));
		assertTrue(this.pathMatcher.matchStart("test/*", "test/Test"));
		assertTrue(this.pathMatcher.matchStart("test/*", "test/t"));
		assertTrue(this.pathMatcher.matchStart("test/*", "test/"));
		assertTrue(this.pathMatcher.matchStart("*test*", "AnothertestTest"));
		assertTrue(this.pathMatcher.matchStart("*test", "Anothertest"));
		assertTrue(this.pathMatcher.matchStart("*.*", "test."));
		assertTrue(this.pathMatcher.matchStart("*.*", "test.test"));
		assertTrue(this.pathMatcher.matchStart("*.*", "test.test.test"));
		assertTrue(this.pathMatcher.matchStart("test*aaa", "testblaaaa"));
		assertFalse(this.pathMatcher.matchStart("test*", "tst"));
		assertFalse(this.pathMatcher.matchStart("test*", "test/"));
		assertFalse(this.pathMatcher.matchStart("test*", "tsttest"));
		assertFalse(this.pathMatcher.matchStart("test*", "test/"));
		assertFalse(this.pathMatcher.matchStart("test*", "test/t"));
		assertTrue(this.pathMatcher.matchStart("test/*", "test"));
		assertTrue(this.pathMatcher.matchStart("test/t*.txt", "test"));
		assertFalse(this.pathMatcher.matchStart("*test*", "tsttst"));
		assertFalse(this.pathMatcher.matchStart("*test", "tsttst"));
		assertFalse(this.pathMatcher.matchStart("*.*", "tsttst"));
		assertFalse(this.pathMatcher.matchStart("test*aaa", "test"));
		assertFalse(this.pathMatcher.matchStart("test*aaa", "testblaaab"));

		// test matching with ?'s and /'s
		assertTrue(this.pathMatcher.matchStart("/?", "/a"));
		assertTrue(this.pathMatcher.matchStart("/?/a", "/a/a"));
		assertTrue(this.pathMatcher.matchStart("/a/?", "/a/b"));
		assertTrue(this.pathMatcher.matchStart("/??/a", "/aa/a"));
		assertTrue(this.pathMatcher.matchStart("/a/??", "/a/bb"));
		assertTrue(this.pathMatcher.matchStart("/?", "/a"));

		// test matching with **'s
		assertTrue(this.pathMatcher.matchStart("/**", "/testing/testing"));
		assertTrue(this.pathMatcher.matchStart("/*/**", "/testing/testing"));
		assertTrue(this.pathMatcher.matchStart("/**/*", "/testing/testing"));
		assertTrue(this.pathMatcher.matchStart("test*/**", "test/"));
		assertTrue(this.pathMatcher.matchStart("test*/**", "test/t"));
		assertTrue(this.pathMatcher.matchStart("/bla/**/bla", "/bla/testing/testing/bla"));
		assertTrue(this.pathMatcher.matchStart("/bla/**/bla", "/bla/testing/testing/bla/bla"));
		assertTrue(this.pathMatcher.matchStart("/**/test", "/bla/bla/test"));
		assertTrue(this.pathMatcher.matchStart("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla"));
		assertTrue(this.pathMatcher.matchStart("/bla*bla/test", "/blaXXXbla/test"));
		assertTrue(this.pathMatcher.matchStart("/*bla/test", "/XXXbla/test"));
		assertFalse(this.pathMatcher.matchStart("/bla*bla/test", "/blaXXXbl/test"));
		assertFalse(this.pathMatcher.matchStart("/*bla/test", "XXXblab/test"));
		assertFalse(this.pathMatcher.matchStart("/*bla/test", "XXXbl/test"));

		assertFalse(this.pathMatcher.matchStart("/????", "/bala/bla"));
		assertTrue(this.pathMatcher.matchStart("/**/*bla", "/bla/bla/bla/bbb"));

		assertTrue(this.pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/"));
		assertTrue(this.pathMatcher.matchStart("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing"));
		assertTrue(this.pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing"));
		assertTrue(this.pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg"));

		assertTrue(this.pathMatcher.matchStart("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/"));
		assertTrue(this.pathMatcher.matchStart("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing"));
		assertTrue(this.pathMatcher.matchStart("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing"));
		assertTrue(this.pathMatcher.matchStart("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing"));

		assertTrue(this.pathMatcher.matchStart("/x/x/**/bla", "/x/x/x/"));

		assertTrue(this.pathMatcher.matchStart("", ""));
	}

	@Test
	public void uniqueDeliminator() {
		this.pathMatcher.setPathSeparator(".");

		// test exact matching
		assertTrue(this.pathMatcher.match("test", "test"));
		assertTrue(this.pathMatcher.match(".test", ".test"));
		assertFalse(this.pathMatcher.match(".test/jpg", "test/jpg"));
		assertFalse(this.pathMatcher.match("test", ".test"));
		assertFalse(this.pathMatcher.match(".test", "test"));

		// test matching with ?'s
		assertTrue(this.pathMatcher.match("t?st", "test"));
		assertTrue(this.pathMatcher.match("??st", "test"));
		assertTrue(this.pathMatcher.match("tes?", "test"));
		assertTrue(this.pathMatcher.match("te??", "test"));
		assertTrue(this.pathMatcher.match("?es?", "test"));
		assertFalse(this.pathMatcher.match("tes?", "tes"));
		assertFalse(this.pathMatcher.match("tes?", "testt"));
		assertFalse(this.pathMatcher.match("tes?", "tsst"));

		// test matching with *'s
		assertTrue(this.pathMatcher.match("*", "test"));
		assertTrue(this.pathMatcher.match("test*", "test"));
		assertTrue(this.pathMatcher.match("test*", "testTest"));
		assertTrue(this.pathMatcher.match("*test*", "AnothertestTest"));
		assertTrue(this.pathMatcher.match("*test", "Anothertest"));
		assertTrue(this.pathMatcher.match("*/*", "test/"));
		assertTrue(this.pathMatcher.match("*/*", "test/test"));
		assertTrue(this.pathMatcher.match("*/*", "test/test/test"));
		assertTrue(this.pathMatcher.match("test*aaa", "testblaaaa"));
		assertFalse(this.pathMatcher.match("test*", "tst"));
		assertFalse(this.pathMatcher.match("test*", "tsttest"));
		assertFalse(this.pathMatcher.match("*test*", "tsttst"));
		assertFalse(this.pathMatcher.match("*test", "tsttst"));
		assertFalse(this.pathMatcher.match("*/*", "tsttst"));
		assertFalse(this.pathMatcher.match("test*aaa", "test"));
		assertFalse(this.pathMatcher.match("test*aaa", "testblaaab"));

		// test matching with ?'s and .'s
		assertTrue(this.pathMatcher.match(".?", ".a"));
		assertTrue(this.pathMatcher.match(".?.a", ".a.a"));
		assertTrue(this.pathMatcher.match(".a.?", ".a.b"));
		assertTrue(this.pathMatcher.match(".??.a", ".aa.a"));
		assertTrue(this.pathMatcher.match(".a.??", ".a.bb"));
		assertTrue(this.pathMatcher.match(".?", ".a"));

		// test matching with **'s
		assertTrue(this.pathMatcher.match(".**", ".testing.testing"));
		assertTrue(this.pathMatcher.match(".*.**", ".testing.testing"));
		assertTrue(this.pathMatcher.match(".**.*", ".testing.testing"));
		assertTrue(this.pathMatcher.match(".bla.**.bla", ".bla.testing.testing.bla"));
		assertTrue(this.pathMatcher.match(".bla.**.bla", ".bla.testing.testing.bla.bla"));
		assertTrue(this.pathMatcher.match(".**.test", ".bla.bla.test"));
		assertTrue(this.pathMatcher.match(".bla.**.**.bla", ".bla.bla.bla.bla.bla.bla"));
		assertTrue(this.pathMatcher.match(".bla*bla.test", ".blaXXXbla.test"));
		assertTrue(this.pathMatcher.match(".*bla.test", ".XXXbla.test"));
		assertFalse(this.pathMatcher.match(".bla*bla.test", ".blaXXXbl.test"));
		assertFalse(this.pathMatcher.match(".*bla.test", "XXXblab.test"));
		assertFalse(this.pathMatcher.match(".*bla.test", "XXXbl.test"));
	}

	@Test
	public void extractPathWithinPattern() throws Exception {
		assertEquals("", this.pathMatcher.extractPathWithinPattern("/docs/commit.html", "/docs/commit.html"));

		assertEquals("cvs/commit", this.pathMatcher.extractPathWithinPattern("/docs/*", "/docs/cvs/commit"));
		assertEquals("commit.html", this.pathMatcher.extractPathWithinPattern("/docs/cvs/*.html", "/docs/cvs/commit.html"));
		assertEquals("cvs/commit", this.pathMatcher.extractPathWithinPattern("/docs/**", "/docs/cvs/commit"));
		assertEquals("cvs/commit.html",
				this.pathMatcher.extractPathWithinPattern("/docs/**/*.html", "/docs/cvs/commit.html"));
		assertEquals("commit.html", this.pathMatcher.extractPathWithinPattern("/docs/**/*.html", "/docs/commit.html"));
		assertEquals("commit.html", this.pathMatcher.extractPathWithinPattern("/*.html", "/commit.html"));
		assertEquals("docs/commit.html", this.pathMatcher.extractPathWithinPattern("/*.html", "/docs/commit.html"));
		assertEquals("/commit.html", this.pathMatcher.extractPathWithinPattern("*.html", "/commit.html"));
		assertEquals("/docs/commit.html", this.pathMatcher.extractPathWithinPattern("*.html", "/docs/commit.html"));
		assertEquals("/docs/commit.html", this.pathMatcher.extractPathWithinPattern("**/*.*", "/docs/commit.html"));
		assertEquals("/docs/commit.html", this.pathMatcher.extractPathWithinPattern("*", "/docs/commit.html"));
		// SPR-10515
		assertEquals("/docs/cvs/other/commit.html", this.pathMatcher.extractPathWithinPattern("**/commit.html", "/docs/cvs/other/commit.html"));
		assertEquals("cvs/other/commit.html", this.pathMatcher.extractPathWithinPattern("/docs/**/commit.html", "/docs/cvs/other/commit.html"));
		assertEquals("cvs/other/commit.html", this.pathMatcher.extractPathWithinPattern("/docs/**/**/**/**", "/docs/cvs/other/commit.html"));

		assertEquals("docs/cvs/commit", this.pathMatcher.extractPathWithinPattern("/d?cs/*", "/docs/cvs/commit"));
		assertEquals("cvs/commit.html",
				this.pathMatcher.extractPathWithinPattern("/docs/c?s/*.html", "/docs/cvs/commit.html"));
		assertEquals("docs/cvs/commit", this.pathMatcher.extractPathWithinPattern("/d?cs/**", "/docs/cvs/commit"));
		assertEquals("docs/cvs/commit.html",
				this.pathMatcher.extractPathWithinPattern("/d?cs/**/*.html", "/docs/cvs/commit.html"));
	}

	@Test
	public void extractUriTemplateVariables() throws Exception {
		Map<String, String> result = this.pathMatcher.extractUriTemplateVariables("/hotels/{hotel}", "/hotels/1");
		assertEquals(Collections.singletonMap("hotel", "1"), result);

		result = this.pathMatcher.extractUriTemplateVariables("/h?tels/{hotel}", "/hotels/1");
		assertEquals(Collections.singletonMap("hotel", "1"), result);

		result = this.pathMatcher.extractUriTemplateVariables("/hotels/{hotel}/bookings/{booking}", "/hotels/1/bookings/2");
		Map<String, String> expected = new LinkedHashMap<>();
		expected.put("hotel", "1");
		expected.put("booking", "2");
		assertEquals(expected, result);

		result = this.pathMatcher.extractUriTemplateVariables("/**/hotels/**/{hotel}", "/foo/hotels/bar/1");
		assertEquals(Collections.singletonMap("hotel", "1"), result);

		result = this.pathMatcher.extractUriTemplateVariables("/{page}.html", "/42.html");
		assertEquals(Collections.singletonMap("page", "42"), result);

		result = this.pathMatcher.extractUriTemplateVariables("/{page}.*", "/42.html");
		assertEquals(Collections.singletonMap("page", "42"), result);

		result = this.pathMatcher.extractUriTemplateVariables("/A-{B}-C", "/A-b-C");
		assertEquals(Collections.singletonMap("B", "b"), result);

		result = this.pathMatcher.extractUriTemplateVariables("/{name}.{extension}", "/test.html");
		expected = new LinkedHashMap<>();
		expected.put("name", "test");
		expected.put("extension", "html");
		assertEquals(expected, result);
	}

	@Test
	public void extractUriTemplateVariablesRegex() {
		Map<String, String> result = this.pathMatcher
				.extractUriTemplateVariables("{symbolicName:[\\w\\.]+}-{version:[\\w\\.]+}.jar",
						"com.example-1.0.0.jar");
		assertEquals("com.example", result.get("symbolicName"));
		assertEquals("1.0.0", result.get("version"));

		result = this.pathMatcher.extractUriTemplateVariables("{symbolicName:[\\w\\.]+}-sources-{version:[\\w\\.]+}.jar",
				"com.example-sources-1.0.0.jar");
		assertEquals("com.example", result.get("symbolicName"));
		assertEquals("1.0.0", result.get("version"));
	}

	/**
	 * SPR-7787
	 */
	@Test
	public void extractUriTemplateVarsRegexQualifiers() {
		Map<String, String> result = this.pathMatcher.extractUriTemplateVariables(
				"{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.]+}.jar",
				"com.example-sources-1.0.0.jar");
		assertEquals("com.example", result.get("symbolicName"));
		assertEquals("1.0.0", result.get("version"));

		result = this.pathMatcher.extractUriTemplateVariables(
				"{symbolicName:[\\w\\.]+}-sources-{version:[\\d\\.]+}-{year:\\d{4}}{month:\\d{2}}{day:\\d{2}}.jar",
				"com.example-sources-1.0.0-20100220.jar");
		assertEquals("com.example", result.get("symbolicName"));
		assertEquals("1.0.0", result.get("version"));
		assertEquals("2010", result.get("year"));
		assertEquals("02", result.get("month"));
		assertEquals("20", result.get("day"));

		result = this.pathMatcher.extractUriTemplateVariables(
				"{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.\\{\\}]+}.jar",
				"com.example-sources-1.0.0.{12}.jar");
		assertEquals("com.example", result.get("symbolicName"));
		assertEquals("1.0.0.{12}", result.get("version"));
	}

	/**
	 * SPR-8455
	 */
	@Test
	public void extractUriTemplateVarsRegexCapturingGroups() {
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage(containsString("The number of capturing groups in the pattern"));
		this.pathMatcher.extractUriTemplateVariables("/web/{id:foo(bar)?}", "/web/foobar");
	}

	@Test
	public void combine() {
		assertEquals("", this.pathMatcher.combine(null, null));
		assertEquals("/hotels", this.pathMatcher.combine("/hotels", null));
		assertEquals("/hotels", this.pathMatcher.combine(null, "/hotels"));
		assertEquals("/hotels/booking", this.pathMatcher.combine("/hotels/*", "booking"));
		assertEquals("/hotels/booking", this.pathMatcher.combine("/hotels/*", "/booking"));
		assertEquals("/hotels/**/booking", this.pathMatcher.combine("/hotels/**", "booking"));
		assertEquals("/hotels/**/booking", this.pathMatcher.combine("/hotels/**", "/booking"));
		assertEquals("/hotels/booking", this.pathMatcher.combine("/hotels", "/booking"));
		assertEquals("/hotels/booking", this.pathMatcher.combine("/hotels", "booking"));
		assertEquals("/hotels/booking", this.pathMatcher.combine("/hotels/", "booking"));
		assertEquals("/hotels/{hotel}", this.pathMatcher.combine("/hotels/*", "{hotel}"));
		assertEquals("/hotels/**/{hotel}", this.pathMatcher.combine("/hotels/**", "{hotel}"));
		assertEquals("/hotels/{hotel}", this.pathMatcher.combine("/hotels", "{hotel}"));
		assertEquals("/hotels/{hotel}.*", this.pathMatcher.combine("/hotels", "{hotel}.*"));
		assertEquals("/hotels/*/booking/{booking}", this.pathMatcher.combine("/hotels/*/booking", "{booking}"));
		assertEquals("/hotel.html", this.pathMatcher.combine("/*.html", "/hotel.html"));
		assertEquals("/hotel.html", this.pathMatcher.combine("/*.html", "/hotel"));
		assertEquals("/hotel.html", this.pathMatcher.combine("/*.html", "/hotel.*"));
		assertEquals("/*.html", this.pathMatcher.combine("/**", "/*.html"));
		assertEquals("/*.html", this.pathMatcher.combine("/*", "/*.html"));
		assertEquals("/*.html", this.pathMatcher.combine("/*.*", "/*.html"));
		assertEquals("/{foo}/bar", this.pathMatcher.combine("/{foo}", "/bar"));    // SPR-8858
		assertEquals("/user/user", this.pathMatcher.combine("/user", "/user"));    // SPR-7970
		assertEquals("/{foo:.*[^0-9].*}/edit/", this.pathMatcher.combine("/{foo:.*[^0-9].*}", "/edit/")); // SPR-10062
		assertEquals("/1.0/foo/test", this.pathMatcher.combine("/1.0", "/foo/test")); // SPR-10554
		assertEquals("/hotel", this.pathMatcher.combine("/", "/hotel")); // SPR-12975
		assertEquals("/hotel/booking", this.pathMatcher.combine("/hotel/", "/booking")); // SPR-12975
	}

	@Test
	public void combineWithTwoFileExtensionPatterns() {
		this.exception.expect(IllegalArgumentException.class);
		this.pathMatcher.combine("/*.html", "/*.txt");
	}

	@Test
	public void patternComparator() {
		Comparator<String> comparator = this.pathMatcher.getPatternComparator("/hotels/new");

		assertEquals(0, comparator.compare(null, null));
		assertEquals(1, comparator.compare(null, "/hotels/new"));
		assertEquals(-1, comparator.compare("/hotels/new", null));

		assertEquals(0, comparator.compare("/hotels/new", "/hotels/new"));

		assertEquals(-1, comparator.compare("/hotels/new", "/hotels/*"));
		assertEquals(1, comparator.compare("/hotels/*", "/hotels/new"));
		assertEquals(0, comparator.compare("/hotels/*", "/hotels/*"));

		assertEquals(-1, comparator.compare("/hotels/new", "/hotels/{hotel}"));
		assertEquals(1, comparator.compare("/hotels/{hotel}", "/hotels/new"));
		assertEquals(0, comparator.compare("/hotels/{hotel}", "/hotels/{hotel}"));
		assertEquals(-1, comparator.compare("/hotels/{hotel}/booking", "/hotels/{hotel}/bookings/{booking}"));
		assertEquals(1, comparator.compare("/hotels/{hotel}/bookings/{booking}", "/hotels/{hotel}/booking"));

		// SPR-10550
		assertEquals(-1, comparator.compare("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}", "/**"));
		assertEquals(1, comparator.compare("/**", "/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"));
		assertEquals(0, comparator.compare("/**", "/**"));

		assertEquals(-1, comparator.compare("/hotels/{hotel}", "/hotels/*"));
		assertEquals(1, comparator.compare("/hotels/*", "/hotels/{hotel}"));

		assertEquals(-1, comparator.compare("/hotels/*", "/hotels/*/**"));
		assertEquals(1, comparator.compare("/hotels/*/**", "/hotels/*"));

		assertEquals(-1, comparator.compare("/hotels/new", "/hotels/new.*"));
		assertEquals(2, comparator.compare("/hotels/{hotel}", "/hotels/{hotel}.*"));

		// SPR-6741
		assertEquals(-1, comparator.compare("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}", "/hotels/**"));
		assertEquals(1, comparator.compare("/hotels/**", "/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"));
		assertEquals(1, comparator.compare("/hotels/foo/bar/**", "/hotels/{hotel}"));
		assertEquals(-1, comparator.compare("/hotels/{hotel}", "/hotels/foo/bar/**"));
		assertEquals(2, comparator.compare("/hotels/**/bookings/**", "/hotels/**"));
		assertEquals(-2, comparator.compare("/hotels/**", "/hotels/**/bookings/**"));

		// SPR-8683
		assertEquals(1, comparator.compare("/**", "/hotels/{hotel}"));

		// longer is better
		assertEquals(1, comparator.compare("/hotels", "/hotels2"));

		// SPR-13139
		assertEquals(-1, comparator.compare("*", "*/**"));
		assertEquals(1, comparator.compare("*/**", "*"));
	}

	@Test
	public void patternComparatorSort() {
		Comparator<String> comparator = this.pathMatcher.getPatternComparator("/hotels/new");
		List<String> paths = new ArrayList<>(3);

		paths.add(null);
		paths.add("/hotels/new");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertNull(paths.get(1));
		paths.clear();

		paths.add("/hotels/new");
		paths.add(null);
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertNull(paths.get(1));
		paths.clear();

		paths.add("/hotels/*");
		paths.add("/hotels/new");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertEquals("/hotels/*", paths.get(1));
		paths.clear();

		paths.add("/hotels/new");
		paths.add("/hotels/*");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertEquals("/hotels/*", paths.get(1));
		paths.clear();

		paths.add("/hotels/**");
		paths.add("/hotels/*");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/*", paths.get(0));
		assertEquals("/hotels/**", paths.get(1));
		paths.clear();

		paths.add("/hotels/*");
		paths.add("/hotels/**");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/*", paths.get(0));
		assertEquals("/hotels/**", paths.get(1));
		paths.clear();

		paths.add("/hotels/{hotel}");
		paths.add("/hotels/new");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertEquals("/hotels/{hotel}", paths.get(1));
		paths.clear();

		paths.add("/hotels/new");
		paths.add("/hotels/{hotel}");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertEquals("/hotels/{hotel}", paths.get(1));
		paths.clear();

		paths.add("/hotels/*");
		paths.add("/hotels/{hotel}");
		paths.add("/hotels/new");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertEquals("/hotels/{hotel}", paths.get(1));
		assertEquals("/hotels/*", paths.get(2));
		paths.clear();

		paths.add("/hotels/ne*");
		paths.add("/hotels/n*");
		Collections.shuffle(paths);
		Collections.sort(paths, comparator);
		assertEquals("/hotels/ne*", paths.get(0));
		assertEquals("/hotels/n*", paths.get(1));
		paths.clear();

		comparator = this.pathMatcher.getPatternComparator("/hotels/new.html");
		paths.add("/hotels/new.*");
		paths.add("/hotels/{hotel}");
		Collections.shuffle(paths);
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new.*", paths.get(0));
		assertEquals("/hotels/{hotel}", paths.get(1));
		paths.clear();

		comparator = this.pathMatcher.getPatternComparator("/web/endUser/action/login.html");
		paths.add("/**/login.*");
		paths.add("/**/endUser/action/login.*");
		Collections.sort(paths, comparator);
		assertEquals("/**/endUser/action/login.*", paths.get(0));
		assertEquals("/**/login.*", paths.get(1));
		paths.clear();
	}

	@Test  // SPR-8687
	public void trimTokensOff() {
		this.pathMatcher.setTrimTokens(false);

		assertTrue(this.pathMatcher.match("/group/{groupName}/members", "/group/sales/members"));
		assertTrue(this.pathMatcher.match("/group/{groupName}/members", "/group/  sales/members"));
		assertFalse(this.pathMatcher.match("/group/{groupName}/members", "/Group/  Sales/Members"));
	}

	@Test  // SPR-13286
	public void caseInsensitive() {
		this.pathMatcher.setCaseSensitive(false);

		assertTrue(this.pathMatcher.match("/group/{groupName}/members", "/group/sales/members"));
		assertTrue(this.pathMatcher.match("/group/{groupName}/members", "/Group/Sales/Members"));
		assertTrue(this.pathMatcher.match("/Group/{groupName}/Members", "/group/Sales/members"));
	}

	@Test
	public void defaultCacheSetting() {
		match();
		assertTrue(this.pathMatcher.stringMatcherCache.size() > 20);

		for (int i = 0; i < 65536; i++) {
			this.pathMatcher.match("test" + i, "test");
		}
		// Cache turned off because it went beyond the threshold
		assertTrue(this.pathMatcher.stringMatcherCache.isEmpty());
	}

	@Test
	public void cachePatternsSetToTrue() {
		this.pathMatcher.setCachePatterns(true);
		match();
		assertTrue(this.pathMatcher.stringMatcherCache.size() > 20);

		for (int i = 0; i < 65536; i++) {
			this.pathMatcher.match("test" + i, "test" + i);
		}
		// Cache keeps being alive due to the explicit cache setting
		assertTrue(this.pathMatcher.stringMatcherCache.size() > 65536);
	}

	@Test
	public void preventCreatingStringMatchersIfPathDoesNotStartsWithPatternPrefix() {
		this.pathMatcher.setCachePatterns(true);
		assertEquals(0, this.pathMatcher.stringMatcherCache.size());

		this.pathMatcher.match("test?", "test");
		assertEquals(1, this.pathMatcher.stringMatcherCache.size());

		this.pathMatcher.match("test?", "best");
		this.pathMatcher.match("test/*", "view/test.jpg");
		this.pathMatcher.match("test/**/test.jpg", "view/test.jpg");
		this.pathMatcher.match("test/{name}.jpg", "view/test.jpg");
		assertEquals(1, this.pathMatcher.stringMatcherCache.size());
	}

	@Test
	public void creatingStringMatchersIfPatternPrefixCannotDetermineIfPathMatch() {
		this.pathMatcher.setCachePatterns(true);
		assertEquals(0, this.pathMatcher.stringMatcherCache.size());

		this.pathMatcher.match("test", "testian");
		this.pathMatcher.match("test?", "testFf");
		this.pathMatcher.match("test/*", "test/dir/name.jpg");
		this.pathMatcher.match("test/{name}.jpg", "test/lorem.jpg");
		this.pathMatcher.match("bla/**/test.jpg", "bla/test.jpg");
		this.pathMatcher.match("**/{name}.jpg", "test/lorem.jpg");
		this.pathMatcher.match("/**/{name}.jpg", "/test/lorem.jpg");
		this.pathMatcher.match("/*/dir/{name}.jpg", "/*/dir/lorem.jpg");

		assertEquals(7, this.pathMatcher.stringMatcherCache.size());
	}

	@Test
	public void cachePatternsSetToFalse() {
		this.pathMatcher.setCachePatterns(false);
		match();
		assertTrue(this.pathMatcher.stringMatcherCache.isEmpty());
	}

	@Test
	public void extensionMappingWithDotPathSeparator() {
		this.pathMatcher.setPathSeparator(".");
		assertEquals("Extension mapping should be disabled with \".\" as path separator",
				"/*.html.hotel.*", this.pathMatcher.combine("/*.html", "hotel.*"));
	}

}
