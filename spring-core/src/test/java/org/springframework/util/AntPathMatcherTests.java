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

package org.springframework.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;

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


	@Test
	public void match() {
		// test exact matching
		assertThat(pathMatcher.match("test", "test")).isTrue();
		assertThat(pathMatcher.match("/test", "/test")).isTrue();
		// SPR-14141
		assertThat(pathMatcher.match("https://example.org", "https://example.org")).isTrue();
		assertThat(pathMatcher.match("/test.jpg", "test.jpg")).isFalse();
		assertThat(pathMatcher.match("test", "/test")).isFalse();
		assertThat(pathMatcher.match("/test", "test")).isFalse();

		// test matching with ?'s
		assertThat(pathMatcher.match("t?st", "test")).isTrue();
		assertThat(pathMatcher.match("??st", "test")).isTrue();
		assertThat(pathMatcher.match("tes?", "test")).isTrue();
		assertThat(pathMatcher.match("te??", "test")).isTrue();
		assertThat(pathMatcher.match("?es?", "test")).isTrue();
		assertThat(pathMatcher.match("tes?", "tes")).isFalse();
		assertThat(pathMatcher.match("tes?", "testt")).isFalse();
		assertThat(pathMatcher.match("tes?", "tsst")).isFalse();

		// test matching with *'s
		assertThat(pathMatcher.match("*", "test")).isTrue();
		assertThat(pathMatcher.match("test*", "test")).isTrue();
		assertThat(pathMatcher.match("test*", "testTest")).isTrue();
		assertThat(pathMatcher.match("test/*", "test/Test")).isTrue();
		assertThat(pathMatcher.match("test/*", "test/t")).isTrue();
		assertThat(pathMatcher.match("test/*", "test/")).isTrue();
		assertThat(pathMatcher.match("*test*", "AnothertestTest")).isTrue();
		assertThat(pathMatcher.match("*test", "Anothertest")).isTrue();
		assertThat(pathMatcher.match("*.*", "test.")).isTrue();
		assertThat(pathMatcher.match("*.*", "test.test")).isTrue();
		assertThat(pathMatcher.match("*.*", "test.test.test")).isTrue();
		assertThat(pathMatcher.match("test*aaa", "testblaaaa")).isTrue();
		assertThat(pathMatcher.match("test*", "tst")).isFalse();
		assertThat(pathMatcher.match("test*", "tsttest")).isFalse();
		assertThat(pathMatcher.match("test*", "test/")).isFalse();
		assertThat(pathMatcher.match("test*", "test/t")).isFalse();
		assertThat(pathMatcher.match("test/*", "test")).isFalse();
		assertThat(pathMatcher.match("*test*", "tsttst")).isFalse();
		assertThat(pathMatcher.match("*test", "tsttst")).isFalse();
		assertThat(pathMatcher.match("*.*", "tsttst")).isFalse();
		assertThat(pathMatcher.match("test*aaa", "test")).isFalse();
		assertThat(pathMatcher.match("test*aaa", "testblaaab")).isFalse();

		// test matching with ?'s and /'s
		assertThat(pathMatcher.match("/?", "/a")).isTrue();
		assertThat(pathMatcher.match("/?/a", "/a/a")).isTrue();
		assertThat(pathMatcher.match("/a/?", "/a/b")).isTrue();
		assertThat(pathMatcher.match("/??/a", "/aa/a")).isTrue();
		assertThat(pathMatcher.match("/a/??", "/a/bb")).isTrue();
		assertThat(pathMatcher.match("/?", "/a")).isTrue();

		// test matching with **'s
		assertThat(pathMatcher.match("/**", "/testing/testing")).isTrue();
		assertThat(pathMatcher.match("/*/**", "/testing/testing")).isTrue();
		assertThat(pathMatcher.match("/**/*", "/testing/testing")).isTrue();
		assertThat(pathMatcher.match("/bla/**/bla", "/bla/testing/testing/bla")).isTrue();
		assertThat(pathMatcher.match("/bla/**/bla", "/bla/testing/testing/bla/bla")).isTrue();
		assertThat(pathMatcher.match("/**/test", "/bla/bla/test")).isTrue();
		assertThat(pathMatcher.match("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla")).isTrue();
		assertThat(pathMatcher.match("/bla*bla/test", "/blaXXXbla/test")).isTrue();
		assertThat(pathMatcher.match("/*bla/test", "/XXXbla/test")).isTrue();
		assertThat(pathMatcher.match("/bla*bla/test", "/blaXXXbl/test")).isFalse();
		assertThat(pathMatcher.match("/*bla/test", "XXXblab/test")).isFalse();
		assertThat(pathMatcher.match("/*bla/test", "XXXbl/test")).isFalse();

		assertThat(pathMatcher.match("/????", "/bala/bla")).isFalse();
		assertThat(pathMatcher.match("/**/*bla", "/bla/bla/bla/bbb")).isFalse();

		assertThat(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/")).isTrue();
		assertThat(pathMatcher.match("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing")).isTrue();
		assertThat(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing")).isTrue();
		assertThat(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg")).isTrue();

		assertThat(pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/")).isTrue();
		assertThat(pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing")).isTrue();
		assertThat(pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing")).isTrue();
		assertThat(pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing")).isFalse();

		assertThat(pathMatcher.match("/x/x/**/bla", "/x/x/x/")).isFalse();

		assertThat(pathMatcher.match("/foo/bar/**", "/foo/bar")).isTrue();

		assertThat(pathMatcher.match("", "")).isTrue();

		assertThat(pathMatcher.match("/{bla}.*", "/testing.html")).isTrue();
	}

	// SPR-14247
	@Test
	public void matchWithTrimTokensEnabled() throws Exception {
		pathMatcher.setTrimTokens(true);

		assertThat(pathMatcher.match("/foo/bar", "/foo /bar")).isTrue();
	}

	@Test
	public void withMatchStart() {
		// test exact matching
		assertThat(pathMatcher.matchStart("test", "test")).isTrue();
		assertThat(pathMatcher.matchStart("/test", "/test")).isTrue();
		assertThat(pathMatcher.matchStart("/test.jpg", "test.jpg")).isFalse();
		assertThat(pathMatcher.matchStart("test", "/test")).isFalse();
		assertThat(pathMatcher.matchStart("/test", "test")).isFalse();

		// test matching with ?'s
		assertThat(pathMatcher.matchStart("t?st", "test")).isTrue();
		assertThat(pathMatcher.matchStart("??st", "test")).isTrue();
		assertThat(pathMatcher.matchStart("tes?", "test")).isTrue();
		assertThat(pathMatcher.matchStart("te??", "test")).isTrue();
		assertThat(pathMatcher.matchStart("?es?", "test")).isTrue();
		assertThat(pathMatcher.matchStart("tes?", "tes")).isFalse();
		assertThat(pathMatcher.matchStart("tes?", "testt")).isFalse();
		assertThat(pathMatcher.matchStart("tes?", "tsst")).isFalse();

		// test matching with *'s
		assertThat(pathMatcher.matchStart("*", "test")).isTrue();
		assertThat(pathMatcher.matchStart("test*", "test")).isTrue();
		assertThat(pathMatcher.matchStart("test*", "testTest")).isTrue();
		assertThat(pathMatcher.matchStart("test/*", "test/Test")).isTrue();
		assertThat(pathMatcher.matchStart("test/*", "test/t")).isTrue();
		assertThat(pathMatcher.matchStart("test/*", "test/")).isTrue();
		assertThat(pathMatcher.matchStart("*test*", "AnothertestTest")).isTrue();
		assertThat(pathMatcher.matchStart("*test", "Anothertest")).isTrue();
		assertThat(pathMatcher.matchStart("*.*", "test.")).isTrue();
		assertThat(pathMatcher.matchStart("*.*", "test.test")).isTrue();
		assertThat(pathMatcher.matchStart("*.*", "test.test.test")).isTrue();
		assertThat(pathMatcher.matchStart("test*aaa", "testblaaaa")).isTrue();
		assertThat(pathMatcher.matchStart("test*", "tst")).isFalse();
		assertThat(pathMatcher.matchStart("test*", "test/")).isFalse();
		assertThat(pathMatcher.matchStart("test*", "tsttest")).isFalse();
		assertThat(pathMatcher.matchStart("test*", "test/")).isFalse();
		assertThat(pathMatcher.matchStart("test*", "test/t")).isFalse();
		assertThat(pathMatcher.matchStart("test/*", "test")).isTrue();
		assertThat(pathMatcher.matchStart("test/t*.txt", "test")).isTrue();
		assertThat(pathMatcher.matchStart("*test*", "tsttst")).isFalse();
		assertThat(pathMatcher.matchStart("*test", "tsttst")).isFalse();
		assertThat(pathMatcher.matchStart("*.*", "tsttst")).isFalse();
		assertThat(pathMatcher.matchStart("test*aaa", "test")).isFalse();
		assertThat(pathMatcher.matchStart("test*aaa", "testblaaab")).isFalse();

		// test matching with ?'s and /'s
		assertThat(pathMatcher.matchStart("/?", "/a")).isTrue();
		assertThat(pathMatcher.matchStart("/?/a", "/a/a")).isTrue();
		assertThat(pathMatcher.matchStart("/a/?", "/a/b")).isTrue();
		assertThat(pathMatcher.matchStart("/??/a", "/aa/a")).isTrue();
		assertThat(pathMatcher.matchStart("/a/??", "/a/bb")).isTrue();
		assertThat(pathMatcher.matchStart("/?", "/a")).isTrue();

		// test matching with **'s
		assertThat(pathMatcher.matchStart("/**", "/testing/testing")).isTrue();
		assertThat(pathMatcher.matchStart("/*/**", "/testing/testing")).isTrue();
		assertThat(pathMatcher.matchStart("/**/*", "/testing/testing")).isTrue();
		assertThat(pathMatcher.matchStart("test*/**", "test/")).isTrue();
		assertThat(pathMatcher.matchStart("test*/**", "test/t")).isTrue();
		assertThat(pathMatcher.matchStart("/bla/**/bla", "/bla/testing/testing/bla")).isTrue();
		assertThat(pathMatcher.matchStart("/bla/**/bla", "/bla/testing/testing/bla/bla")).isTrue();
		assertThat(pathMatcher.matchStart("/**/test", "/bla/bla/test")).isTrue();
		assertThat(pathMatcher.matchStart("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla")).isTrue();
		assertThat(pathMatcher.matchStart("/bla*bla/test", "/blaXXXbla/test")).isTrue();
		assertThat(pathMatcher.matchStart("/*bla/test", "/XXXbla/test")).isTrue();
		assertThat(pathMatcher.matchStart("/bla*bla/test", "/blaXXXbl/test")).isFalse();
		assertThat(pathMatcher.matchStart("/*bla/test", "XXXblab/test")).isFalse();
		assertThat(pathMatcher.matchStart("/*bla/test", "XXXbl/test")).isFalse();

		assertThat(pathMatcher.matchStart("/????", "/bala/bla")).isFalse();
		assertThat(pathMatcher.matchStart("/**/*bla", "/bla/bla/bla/bbb")).isTrue();

		assertThat(pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/")).isTrue();
		assertThat(pathMatcher.matchStart("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing")).isTrue();
		assertThat(pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing")).isTrue();
		assertThat(pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg")).isTrue();

		assertThat(pathMatcher.matchStart("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/")).isTrue();
		assertThat(pathMatcher.matchStart("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing")).isTrue();
		assertThat(pathMatcher.matchStart("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing")).isTrue();
		assertThat(pathMatcher.matchStart("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing")).isTrue();

		assertThat(pathMatcher.matchStart("/x/x/**/bla", "/x/x/x/")).isTrue();

		assertThat(pathMatcher.matchStart("", "")).isTrue();
	}

	@Test
	public void uniqueDeliminator() {
		pathMatcher.setPathSeparator(".");

		// test exact matching
		assertThat(pathMatcher.match("test", "test")).isTrue();
		assertThat(pathMatcher.match(".test", ".test")).isTrue();
		assertThat(pathMatcher.match(".test/jpg", "test/jpg")).isFalse();
		assertThat(pathMatcher.match("test", ".test")).isFalse();
		assertThat(pathMatcher.match(".test", "test")).isFalse();

		// test matching with ?'s
		assertThat(pathMatcher.match("t?st", "test")).isTrue();
		assertThat(pathMatcher.match("??st", "test")).isTrue();
		assertThat(pathMatcher.match("tes?", "test")).isTrue();
		assertThat(pathMatcher.match("te??", "test")).isTrue();
		assertThat(pathMatcher.match("?es?", "test")).isTrue();
		assertThat(pathMatcher.match("tes?", "tes")).isFalse();
		assertThat(pathMatcher.match("tes?", "testt")).isFalse();
		assertThat(pathMatcher.match("tes?", "tsst")).isFalse();

		// test matching with *'s
		assertThat(pathMatcher.match("*", "test")).isTrue();
		assertThat(pathMatcher.match("test*", "test")).isTrue();
		assertThat(pathMatcher.match("test*", "testTest")).isTrue();
		assertThat(pathMatcher.match("*test*", "AnothertestTest")).isTrue();
		assertThat(pathMatcher.match("*test", "Anothertest")).isTrue();
		assertThat(pathMatcher.match("*/*", "test/")).isTrue();
		assertThat(pathMatcher.match("*/*", "test/test")).isTrue();
		assertThat(pathMatcher.match("*/*", "test/test/test")).isTrue();
		assertThat(pathMatcher.match("test*aaa", "testblaaaa")).isTrue();
		assertThat(pathMatcher.match("test*", "tst")).isFalse();
		assertThat(pathMatcher.match("test*", "tsttest")).isFalse();
		assertThat(pathMatcher.match("*test*", "tsttst")).isFalse();
		assertThat(pathMatcher.match("*test", "tsttst")).isFalse();
		assertThat(pathMatcher.match("*/*", "tsttst")).isFalse();
		assertThat(pathMatcher.match("test*aaa", "test")).isFalse();
		assertThat(pathMatcher.match("test*aaa", "testblaaab")).isFalse();

		// test matching with ?'s and .'s
		assertThat(pathMatcher.match(".?", ".a")).isTrue();
		assertThat(pathMatcher.match(".?.a", ".a.a")).isTrue();
		assertThat(pathMatcher.match(".a.?", ".a.b")).isTrue();
		assertThat(pathMatcher.match(".??.a", ".aa.a")).isTrue();
		assertThat(pathMatcher.match(".a.??", ".a.bb")).isTrue();
		assertThat(pathMatcher.match(".?", ".a")).isTrue();

		// test matching with **'s
		assertThat(pathMatcher.match(".**", ".testing.testing")).isTrue();
		assertThat(pathMatcher.match(".*.**", ".testing.testing")).isTrue();
		assertThat(pathMatcher.match(".**.*", ".testing.testing")).isTrue();
		assertThat(pathMatcher.match(".bla.**.bla", ".bla.testing.testing.bla")).isTrue();
		assertThat(pathMatcher.match(".bla.**.bla", ".bla.testing.testing.bla.bla")).isTrue();
		assertThat(pathMatcher.match(".**.test", ".bla.bla.test")).isTrue();
		assertThat(pathMatcher.match(".bla.**.**.bla", ".bla.bla.bla.bla.bla.bla")).isTrue();
		assertThat(pathMatcher.match(".bla*bla.test", ".blaXXXbla.test")).isTrue();
		assertThat(pathMatcher.match(".*bla.test", ".XXXbla.test")).isTrue();
		assertThat(pathMatcher.match(".bla*bla.test", ".blaXXXbl.test")).isFalse();
		assertThat(pathMatcher.match(".*bla.test", "XXXblab.test")).isFalse();
		assertThat(pathMatcher.match(".*bla.test", "XXXbl.test")).isFalse();
	}

	@Test
	public void extractPathWithinPattern() throws Exception {
		assertThat((Object) pathMatcher.extractPathWithinPattern("/docs/commit.html", "/docs/commit.html")).isEqualTo("");

		assertThat((Object) pathMatcher.extractPathWithinPattern("/docs/*", "/docs/cvs/commit")).isEqualTo("cvs/commit");
		assertThat((Object) pathMatcher.extractPathWithinPattern("/docs/cvs/*.html", "/docs/cvs/commit.html")).isEqualTo("commit.html");
		assertThat((Object) pathMatcher.extractPathWithinPattern("/docs/**", "/docs/cvs/commit")).isEqualTo("cvs/commit");
		assertThat((Object) pathMatcher.extractPathWithinPattern("/docs/**/*.html", "/docs/cvs/commit.html")).isEqualTo("cvs/commit.html");
		assertThat((Object) pathMatcher.extractPathWithinPattern("/docs/**/*.html", "/docs/commit.html")).isEqualTo("commit.html");
		assertThat((Object) pathMatcher.extractPathWithinPattern("/*.html", "/commit.html")).isEqualTo("commit.html");
		assertThat((Object) pathMatcher.extractPathWithinPattern("/*.html", "/docs/commit.html")).isEqualTo("docs/commit.html");
		assertThat((Object) pathMatcher.extractPathWithinPattern("*.html", "/commit.html")).isEqualTo("/commit.html");
		assertThat((Object) pathMatcher.extractPathWithinPattern("*.html", "/docs/commit.html")).isEqualTo("/docs/commit.html");
		assertThat((Object) pathMatcher.extractPathWithinPattern("**/*.*", "/docs/commit.html")).isEqualTo("/docs/commit.html");
		assertThat((Object) pathMatcher.extractPathWithinPattern("*", "/docs/commit.html")).isEqualTo("/docs/commit.html");
		// SPR-10515
		assertThat((Object) pathMatcher.extractPathWithinPattern("**/commit.html", "/docs/cvs/other/commit.html")).isEqualTo("/docs/cvs/other/commit.html");
		assertThat((Object) pathMatcher.extractPathWithinPattern("/docs/**/commit.html", "/docs/cvs/other/commit.html")).isEqualTo("cvs/other/commit.html");
		assertThat((Object) pathMatcher.extractPathWithinPattern("/docs/**/**/**/**", "/docs/cvs/other/commit.html")).isEqualTo("cvs/other/commit.html");

		assertThat((Object) pathMatcher.extractPathWithinPattern("/d?cs/*", "/docs/cvs/commit")).isEqualTo("docs/cvs/commit");
		assertThat((Object) pathMatcher.extractPathWithinPattern("/docs/c?s/*.html", "/docs/cvs/commit.html")).isEqualTo("cvs/commit.html");
		assertThat((Object) pathMatcher.extractPathWithinPattern("/d?cs/**", "/docs/cvs/commit")).isEqualTo("docs/cvs/commit");
		assertThat((Object) pathMatcher.extractPathWithinPattern("/d?cs/**/*.html", "/docs/cvs/commit.html")).isEqualTo("docs/cvs/commit.html");
	}

	@Test
	public void extractUriTemplateVariables() throws Exception {
		Map<String, String> result = pathMatcher.extractUriTemplateVariables("/hotels/{hotel}", "/hotels/1");
		assertThat((Object) result).isEqualTo(Collections.singletonMap("hotel", "1"));

		result = pathMatcher.extractUriTemplateVariables("/h?tels/{hotel}", "/hotels/1");
		assertThat((Object) result).isEqualTo(Collections.singletonMap("hotel", "1"));

		result = pathMatcher.extractUriTemplateVariables("/hotels/{hotel}/bookings/{booking}", "/hotels/1/bookings/2");
		Map<String, String> expected = new LinkedHashMap<>();
		expected.put("hotel", "1");
		expected.put("booking", "2");
		assertThat((Object) result).isEqualTo(expected);

		result = pathMatcher.extractUriTemplateVariables("/**/hotels/**/{hotel}", "/foo/hotels/bar/1");
		assertThat((Object) result).isEqualTo(Collections.singletonMap("hotel", "1"));

		result = pathMatcher.extractUriTemplateVariables("/{page}.html", "/42.html");
		assertThat((Object) result).isEqualTo(Collections.singletonMap("page", "42"));

		result = pathMatcher.extractUriTemplateVariables("/{page}.*", "/42.html");
		assertThat((Object) result).isEqualTo(Collections.singletonMap("page", "42"));

		result = pathMatcher.extractUriTemplateVariables("/A-{B}-C", "/A-b-C");
		assertThat((Object) result).isEqualTo(Collections.singletonMap("B", "b"));

		result = pathMatcher.extractUriTemplateVariables("/{name}.{extension}", "/test.html");
		expected = new LinkedHashMap<>();
		expected.put("name", "test");
		expected.put("extension", "html");
		assertThat((Object) result).isEqualTo(expected);
	}

	@Test
	public void extractUriTemplateVariablesRegex() {
		Map<String, String> result = pathMatcher
				.extractUriTemplateVariables("{symbolicName:[\\w\\.]+}-{version:[\\w\\.]+}.jar",
						"com.example-1.0.0.jar");
		assertThat((Object) result.get("symbolicName")).isEqualTo("com.example");
		assertThat((Object) result.get("version")).isEqualTo("1.0.0");

		result = pathMatcher.extractUriTemplateVariables("{symbolicName:[\\w\\.]+}-sources-{version:[\\w\\.]+}.jar",
				"com.example-sources-1.0.0.jar");
		assertThat((Object) result.get("symbolicName")).isEqualTo("com.example");
		assertThat((Object) result.get("version")).isEqualTo("1.0.0");
	}

	/**
	 * SPR-7787
	 */
	@Test
	public void extractUriTemplateVarsRegexQualifiers() {
		Map<String, String> result = pathMatcher.extractUriTemplateVariables(
				"{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.]+}.jar",
				"com.example-sources-1.0.0.jar");
		assertThat((Object) result.get("symbolicName")).isEqualTo("com.example");
		assertThat((Object) result.get("version")).isEqualTo("1.0.0");

		result = pathMatcher.extractUriTemplateVariables(
				"{symbolicName:[\\w\\.]+}-sources-{version:[\\d\\.]+}-{year:\\d{4}}{month:\\d{2}}{day:\\d{2}}.jar",
				"com.example-sources-1.0.0-20100220.jar");
		assertThat((Object) result.get("symbolicName")).isEqualTo("com.example");
		assertThat((Object) result.get("version")).isEqualTo("1.0.0");
		assertThat((Object) result.get("year")).isEqualTo("2010");
		assertThat((Object) result.get("month")).isEqualTo("02");
		assertThat((Object) result.get("day")).isEqualTo("20");

		result = pathMatcher.extractUriTemplateVariables(
				"{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.\\{\\}]+}.jar",
				"com.example-sources-1.0.0.{12}.jar");
		assertThat((Object) result.get("symbolicName")).isEqualTo("com.example");
		assertThat((Object) result.get("version")).isEqualTo("1.0.0.{12}");
	}

	/**
	 * SPR-8455
	 */
	@Test
	public void extractUriTemplateVarsRegexCapturingGroups() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				pathMatcher.extractUriTemplateVariables("/web/{id:foo(bar)?}", "/web/foobar"))
			.withMessageContaining("The number of capturing groups in the pattern");
	}

	@Test
	public void combine() {
		assertThat((Object) pathMatcher.combine(null, null)).isEqualTo("");
		assertThat((Object) pathMatcher.combine("/hotels", null)).isEqualTo("/hotels");
		assertThat((Object) pathMatcher.combine(null, "/hotels")).isEqualTo("/hotels");
		assertThat((Object) pathMatcher.combine("/hotels/*", "booking")).isEqualTo("/hotels/booking");
		assertThat((Object) pathMatcher.combine("/hotels/*", "/booking")).isEqualTo("/hotels/booking");
		assertThat((Object) pathMatcher.combine("/hotels/**", "booking")).isEqualTo("/hotels/**/booking");
		assertThat((Object) pathMatcher.combine("/hotels/**", "/booking")).isEqualTo("/hotels/**/booking");
		assertThat((Object) pathMatcher.combine("/hotels", "/booking")).isEqualTo("/hotels/booking");
		assertThat((Object) pathMatcher.combine("/hotels", "booking")).isEqualTo("/hotels/booking");
		assertThat((Object) pathMatcher.combine("/hotels/", "booking")).isEqualTo("/hotels/booking");
		assertThat((Object) pathMatcher.combine("/hotels/*", "{hotel}")).isEqualTo("/hotels/{hotel}");
		assertThat((Object) pathMatcher.combine("/hotels/**", "{hotel}")).isEqualTo("/hotels/**/{hotel}");
		assertThat((Object) pathMatcher.combine("/hotels", "{hotel}")).isEqualTo("/hotels/{hotel}");
		assertThat((Object) pathMatcher.combine("/hotels", "{hotel}.*")).isEqualTo("/hotels/{hotel}.*");
		assertThat((Object) pathMatcher.combine("/hotels/*/booking", "{booking}")).isEqualTo("/hotels/*/booking/{booking}");
		assertThat((Object) pathMatcher.combine("/*.html", "/hotel.html")).isEqualTo("/hotel.html");
		assertThat((Object) pathMatcher.combine("/*.html", "/hotel")).isEqualTo("/hotel.html");
		assertThat((Object) pathMatcher.combine("/*.html", "/hotel.*")).isEqualTo("/hotel.html");
		assertThat((Object) pathMatcher.combine("/**", "/*.html")).isEqualTo("/*.html");
		assertThat((Object) pathMatcher.combine("/*", "/*.html")).isEqualTo("/*.html");
		assertThat((Object) pathMatcher.combine("/*.*", "/*.html")).isEqualTo("/*.html");
		// SPR-8858
		assertThat((Object) pathMatcher.combine("/{foo}", "/bar")).isEqualTo("/{foo}/bar");
		// SPR-7970
		assertThat((Object) pathMatcher.combine("/user", "/user")).isEqualTo("/user/user");
		// SPR-10062
		assertThat((Object) pathMatcher.combine("/{foo:.*[^0-9].*}", "/edit/")).isEqualTo("/{foo:.*[^0-9].*}/edit/");
		// SPR-10554
		assertThat((Object) pathMatcher.combine("/1.0", "/foo/test")).isEqualTo("/1.0/foo/test");
		// SPR-12975
		assertThat((Object) pathMatcher.combine("/", "/hotel")).isEqualTo("/hotel");
		// SPR-12975
		assertThat((Object) pathMatcher.combine("/hotel/", "/booking")).isEqualTo("/hotel/booking");
	}

	@Test
	public void combineWithTwoFileExtensionPatterns() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				pathMatcher.combine("/*.html", "/*.txt"));
	}

	@Test
	public void patternComparator() {
		Comparator<String> comparator = pathMatcher.getPatternComparator("/hotels/new");

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
		Comparator<String> comparator = pathMatcher.getPatternComparator("/hotels/new");
		List<String> paths = new ArrayList<>(3);

		paths.add(null);
		paths.add("/hotels/new");
		Collections.sort(paths, comparator);
		assertThat((Object) paths.get(0)).isEqualTo("/hotels/new");
		assertNull(paths.get(1));
		paths.clear();

		paths.add("/hotels/new");
		paths.add(null);
		Collections.sort(paths, comparator);
		assertThat((Object) paths.get(0)).isEqualTo("/hotels/new");
		assertNull(paths.get(1));
		paths.clear();

		paths.add("/hotels/*");
		paths.add("/hotels/new");
		Collections.sort(paths, comparator);
		assertThat((Object) paths.get(0)).isEqualTo("/hotels/new");
		assertThat((Object) paths.get(1)).isEqualTo("/hotels/*");
		paths.clear();

		paths.add("/hotels/new");
		paths.add("/hotels/*");
		Collections.sort(paths, comparator);
		assertThat((Object) paths.get(0)).isEqualTo("/hotels/new");
		assertThat((Object) paths.get(1)).isEqualTo("/hotels/*");
		paths.clear();

		paths.add("/hotels/**");
		paths.add("/hotels/*");
		Collections.sort(paths, comparator);
		assertThat((Object) paths.get(0)).isEqualTo("/hotels/*");
		assertThat((Object) paths.get(1)).isEqualTo("/hotels/**");
		paths.clear();

		paths.add("/hotels/*");
		paths.add("/hotels/**");
		Collections.sort(paths, comparator);
		assertThat((Object) paths.get(0)).isEqualTo("/hotels/*");
		assertThat((Object) paths.get(1)).isEqualTo("/hotels/**");
		paths.clear();

		paths.add("/hotels/{hotel}");
		paths.add("/hotels/new");
		Collections.sort(paths, comparator);
		assertThat((Object) paths.get(0)).isEqualTo("/hotels/new");
		assertThat((Object) paths.get(1)).isEqualTo("/hotels/{hotel}");
		paths.clear();

		paths.add("/hotels/new");
		paths.add("/hotels/{hotel}");
		Collections.sort(paths, comparator);
		assertThat((Object) paths.get(0)).isEqualTo("/hotels/new");
		assertThat((Object) paths.get(1)).isEqualTo("/hotels/{hotel}");
		paths.clear();

		paths.add("/hotels/*");
		paths.add("/hotels/{hotel}");
		paths.add("/hotels/new");
		Collections.sort(paths, comparator);
		assertThat((Object) paths.get(0)).isEqualTo("/hotels/new");
		assertThat((Object) paths.get(1)).isEqualTo("/hotels/{hotel}");
		assertThat((Object) paths.get(2)).isEqualTo("/hotels/*");
		paths.clear();

		paths.add("/hotels/ne*");
		paths.add("/hotels/n*");
		Collections.shuffle(paths);
		Collections.sort(paths, comparator);
		assertThat((Object) paths.get(0)).isEqualTo("/hotels/ne*");
		assertThat((Object) paths.get(1)).isEqualTo("/hotels/n*");
		paths.clear();

		comparator = pathMatcher.getPatternComparator("/hotels/new.html");
		paths.add("/hotels/new.*");
		paths.add("/hotels/{hotel}");
		Collections.shuffle(paths);
		Collections.sort(paths, comparator);
		assertThat((Object) paths.get(0)).isEqualTo("/hotels/new.*");
		assertThat((Object) paths.get(1)).isEqualTo("/hotels/{hotel}");
		paths.clear();

		comparator = pathMatcher.getPatternComparator("/web/endUser/action/login.html");
		paths.add("/**/login.*");
		paths.add("/**/endUser/action/login.*");
		Collections.sort(paths, comparator);
		assertThat((Object) paths.get(0)).isEqualTo("/**/endUser/action/login.*");
		assertThat((Object) paths.get(1)).isEqualTo("/**/login.*");
		paths.clear();
	}

	@Test  // SPR-8687
	public void trimTokensOff() {
		pathMatcher.setTrimTokens(false);

		assertThat(pathMatcher.match("/group/{groupName}/members", "/group/sales/members")).isTrue();
		assertThat(pathMatcher.match("/group/{groupName}/members", "/group/  sales/members")).isTrue();
		assertThat(pathMatcher.match("/group/{groupName}/members", "/Group/  Sales/Members")).isFalse();
	}

	@Test  // SPR-13286
	public void caseInsensitive() {
		pathMatcher.setCaseSensitive(false);

		assertThat(pathMatcher.match("/group/{groupName}/members", "/group/sales/members")).isTrue();
		assertThat(pathMatcher.match("/group/{groupName}/members", "/Group/Sales/Members")).isTrue();
		assertThat(pathMatcher.match("/Group/{groupName}/Members", "/group/Sales/members")).isTrue();
	}

	@Test
	public void defaultCacheSetting() {
		match();
		assertThat(pathMatcher.stringMatcherCache.size() > 20).isTrue();

		for (int i = 0; i < 65536; i++) {
			pathMatcher.match("test" + i, "test");
		}
		// Cache turned off because it went beyond the threshold
		assertThat(pathMatcher.stringMatcherCache.isEmpty()).isTrue();
	}

	@Test
	public void cachePatternsSetToTrue() {
		pathMatcher.setCachePatterns(true);
		match();
		assertThat(pathMatcher.stringMatcherCache.size() > 20).isTrue();

		for (int i = 0; i < 65536; i++) {
			pathMatcher.match("test" + i, "test" + i);
		}
		// Cache keeps being alive due to the explicit cache setting
		assertThat(pathMatcher.stringMatcherCache.size() > 65536).isTrue();
	}

	@Test
	public void preventCreatingStringMatchersIfPathDoesNotStartsWithPatternPrefix() {
		pathMatcher.setCachePatterns(true);
		assertEquals(0, pathMatcher.stringMatcherCache.size());

		pathMatcher.match("test?", "test");
		assertEquals(1, pathMatcher.stringMatcherCache.size());

		pathMatcher.match("test?", "best");
		pathMatcher.match("test/*", "view/test.jpg");
		pathMatcher.match("test/**/test.jpg", "view/test.jpg");
		pathMatcher.match("test/{name}.jpg", "view/test.jpg");
		assertEquals(1, pathMatcher.stringMatcherCache.size());
	}

	@Test
	public void creatingStringMatchersIfPatternPrefixCannotDetermineIfPathMatch() {
		pathMatcher.setCachePatterns(true);
		assertEquals(0, pathMatcher.stringMatcherCache.size());

		pathMatcher.match("test", "testian");
		pathMatcher.match("test?", "testFf");
		pathMatcher.match("test/*", "test/dir/name.jpg");
		pathMatcher.match("test/{name}.jpg", "test/lorem.jpg");
		pathMatcher.match("bla/**/test.jpg", "bla/test.jpg");
		pathMatcher.match("**/{name}.jpg", "test/lorem.jpg");
		pathMatcher.match("/**/{name}.jpg", "/test/lorem.jpg");
		pathMatcher.match("/*/dir/{name}.jpg", "/*/dir/lorem.jpg");

		assertEquals(7, pathMatcher.stringMatcherCache.size());
	}

	@Test
	public void cachePatternsSetToFalse() {
		pathMatcher.setCachePatterns(false);
		match();
		assertThat(pathMatcher.stringMatcherCache.isEmpty()).isTrue();
	}

	@Test
	public void extensionMappingWithDotPathSeparator() {
		pathMatcher.setPathSeparator(".");
		assertThat((Object) pathMatcher.combine("/*.html", "hotel.*")).as("Extension mapping should be disabled with \".\" as path separator").isEqualTo("/*.html.hotel.*");
	}

}
