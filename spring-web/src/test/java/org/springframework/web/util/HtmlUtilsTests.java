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

package org.springframework.web.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;

/**
 * @author Alef Arendsen
 * @author Martin Kersten
 * @author Rick Evans
 */
public class HtmlUtilsTests {

	@Test
	public void testHtmlEscape() {
		String unescaped = "\"This is a quote'";
		String escaped = HtmlUtils.htmlEscape(unescaped);
		assertThat((Object) escaped).isEqualTo("&quot;This is a quote&#39;");
		escaped = HtmlUtils.htmlEscapeDecimal(unescaped);
		assertThat((Object) escaped).isEqualTo("&#34;This is a quote&#39;");
		escaped = HtmlUtils.htmlEscapeHex(unescaped);
		assertThat((Object) escaped).isEqualTo("&#x22;This is a quote&#x27;");
	}

	@Test
	public void testHtmlUnescape() {
		String escaped = "&quot;This is a quote&#39;";
		String unescaped = HtmlUtils.htmlUnescape(escaped);
		assertThat((Object) unescaped).isEqualTo("\"This is a quote'");
	}

	@Test
	public void testEncodeIntoHtmlCharacterSet() {
		assertThat((Object) HtmlUtils.htmlEscape("")).as("An empty string should be converted to an empty string").isEqualTo("");
		assertThat((Object) HtmlUtils.htmlEscape("A sentence containing no special characters.")).as("A string containing no special characters should not be affected").isEqualTo("A sentence containing no special characters.");

		assertThat((Object) HtmlUtils.htmlEscape("< >")).as("'< >' should be encoded to '&lt; &gt;'").isEqualTo("&lt; &gt;");
		assertThat((Object) HtmlUtils.htmlEscapeDecimal("< >")).as("'< >' should be encoded to '&#60; &#62;'").isEqualTo("&#60; &#62;");

		assertThat((Object) HtmlUtils.htmlEscape("" + (char) 8709)).as("The special character 8709 should be encoded to '&empty;'").isEqualTo("&empty;");
		assertThat((Object) HtmlUtils.htmlEscapeDecimal("" + (char) 8709)).as("The special character 8709 should be encoded to '&#8709;'").isEqualTo("&#8709;");

		assertThat((Object) HtmlUtils.htmlEscape("" + (char) 977)).as("The special character 977 should be encoded to '&thetasym;'").isEqualTo("&thetasym;");
		assertThat((Object) HtmlUtils.htmlEscapeDecimal("" + (char) 977)).as("The special character 977 should be encoded to '&#977;'").isEqualTo("&#977;");
	}

	// SPR-9293
	@Test
	public void testEncodeIntoHtmlCharacterSetFromUtf8() {
		String utf8 = ("UTF-8");
		assertThat((Object) HtmlUtils.htmlEscape("", utf8)).as("An empty string should be converted to an empty string").isEqualTo("");
		assertThat((Object) HtmlUtils.htmlEscape("A sentence containing no special characters.")).as("A string containing no special characters should not be affected").isEqualTo("A sentence containing no special characters.");

		assertThat((Object) HtmlUtils.htmlEscape("< >", utf8)).as("'< >' should be encoded to '&lt; &gt;'").isEqualTo("&lt; &gt;");
		assertThat((Object) HtmlUtils.htmlEscapeDecimal("< >", utf8)).as("'< >' should be encoded to '&#60; &#62;'").isEqualTo("&#60; &#62;");

		assertThat((Object) HtmlUtils.htmlEscape("Μερικοί Ελληνικοί \"χαρακτήρες\"", utf8)).as("UTF-8 supported chars should not be escaped").isEqualTo("Μερικοί Ελληνικοί &quot;χαρακτήρες&quot;");
	}

	@Test
	public void testDecodeFromHtmlCharacterSet() {
		assertThat((Object) HtmlUtils.htmlUnescape("")).as("An empty string should be converted to an empty string").isEqualTo("");
		assertThat((Object) HtmlUtils.htmlUnescape("This is a sentence containing no special characters.")).as("A string containing no special characters should not be affected").isEqualTo("This is a sentence containing no special characters.");

		assertThat((Object) HtmlUtils.htmlUnescape("A&nbsp;B")).as("'A&nbsp;B' should be decoded to 'A B'").isEqualTo(("A" + (char) 160 + "B"));

		assertThat((Object) HtmlUtils.htmlUnescape("&lt; &gt;")).as("'&lt; &gt;' should be decoded to '< >'").isEqualTo("< >");
		assertThat((Object) HtmlUtils.htmlUnescape("&#60; &#62;")).as("'&#60; &#62;' should be decoded to '< >'").isEqualTo("< >");

		assertThat((Object) HtmlUtils.htmlUnescape("&#x41;&#X42;&#x43;")).as("'&#x41;&#X42;&#x43;' should be decoded to 'ABC'").isEqualTo("ABC");

		assertThat((Object) HtmlUtils.htmlUnescape("&phi;")).as("'&phi;' should be decoded to uni-code character 966").isEqualTo(("" + (char) 966));

		assertThat((Object) HtmlUtils.htmlUnescape("&Prime;")).as("'&Prime;' should be decoded to uni-code character 8243").isEqualTo(("" + (char) 8243));

		assertThat((Object) HtmlUtils.htmlUnescape("&prIme;")).as("A not supported named reference leads should be ignored").isEqualTo("&prIme;");

		assertThat((Object) HtmlUtils.htmlUnescape("&;")).as("An empty reference '&;' should be survive the decoding").isEqualTo("&;");

		assertThat((Object) HtmlUtils.htmlUnescape("&thetasym;")).as("The longest character entity reference '&thetasym;' should be processable").isEqualTo(("" + (char) 977));

		assertThat((Object) HtmlUtils.htmlUnescape("&#notADecimalNumber;")).as("A malformed decimal reference should survive the decoding").isEqualTo("&#notADecimalNumber;");
		assertThat((Object) HtmlUtils.htmlUnescape("&#XnotAHexNumber;")).as("A malformed hex reference should survive the decoding").isEqualTo("&#XnotAHexNumber;");

		assertThat((Object) HtmlUtils.htmlUnescape("&#1;")).as("The numerical reference '&#1;' should be converted to char 1").isEqualTo(("" + (char) 1));

		assertThat((Object) HtmlUtils.htmlUnescape("&#x;")).as("The malformed hex reference '&#x;' should remain '&#x;'").isEqualTo("&#x;");
	}

}
