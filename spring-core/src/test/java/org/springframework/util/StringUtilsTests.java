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

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static temp.XAssert.assertArrayEquals;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;
import static temp.XAssert.assertSame;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public class StringUtilsTests {

	@Test
	public void testHasTextBlank() {
		String blank = "          ";
		assertEquals(false, StringUtils.hasText(blank));
	}

	@Test
	public void testHasTextNullEmpty() {
		assertEquals(false, StringUtils.hasText(null));
		assertEquals(false, StringUtils.hasText(""));
	}

	@Test
	public void testHasTextValid() {
		assertEquals(true, StringUtils.hasText("t"));
	}

	@Test
	public void testContainsWhitespace() {
		assertThat(StringUtils.containsWhitespace(null)).isFalse();
		assertThat(StringUtils.containsWhitespace("")).isFalse();
		assertThat(StringUtils.containsWhitespace("a")).isFalse();
		assertThat(StringUtils.containsWhitespace("abc")).isFalse();
		assertThat(StringUtils.containsWhitespace(" ")).isTrue();
		assertThat(StringUtils.containsWhitespace(" a")).isTrue();
		assertThat(StringUtils.containsWhitespace("abc ")).isTrue();
		assertThat(StringUtils.containsWhitespace("a b")).isTrue();
		assertThat(StringUtils.containsWhitespace("a  b")).isTrue();
	}

	@Test
	public void testTrimWhitespace() {
		assertEquals(null, StringUtils.trimWhitespace(null));
		assertEquals("", StringUtils.trimWhitespace(""));
		assertEquals("", StringUtils.trimWhitespace(" "));
		assertEquals("", StringUtils.trimWhitespace("\t"));
		assertEquals("a", StringUtils.trimWhitespace(" a"));
		assertEquals("a", StringUtils.trimWhitespace("a "));
		assertEquals("a", StringUtils.trimWhitespace(" a "));
		assertEquals("a b", StringUtils.trimWhitespace(" a b "));
		assertEquals("a b  c", StringUtils.trimWhitespace(" a b  c "));
	}

	@Test
	public void testTrimAllWhitespace() {
		assertEquals("", StringUtils.trimAllWhitespace(""));
		assertEquals("", StringUtils.trimAllWhitespace(" "));
		assertEquals("", StringUtils.trimAllWhitespace("\t"));
		assertEquals("a", StringUtils.trimAllWhitespace(" a"));
		assertEquals("a", StringUtils.trimAllWhitespace("a "));
		assertEquals("a", StringUtils.trimAllWhitespace(" a "));
		assertEquals("ab", StringUtils.trimAllWhitespace(" a b "));
		assertEquals("abc", StringUtils.trimAllWhitespace(" a b  c "));
	}

	@Test
	public void testTrimLeadingWhitespace() {
		assertEquals(null, StringUtils.trimLeadingWhitespace(null));
		assertEquals("", StringUtils.trimLeadingWhitespace(""));
		assertEquals("", StringUtils.trimLeadingWhitespace(" "));
		assertEquals("", StringUtils.trimLeadingWhitespace("\t"));
		assertEquals("a", StringUtils.trimLeadingWhitespace(" a"));
		assertEquals("a ", StringUtils.trimLeadingWhitespace("a "));
		assertEquals("a ", StringUtils.trimLeadingWhitespace(" a "));
		assertEquals("a b ", StringUtils.trimLeadingWhitespace(" a b "));
		assertEquals("a b  c ", StringUtils.trimLeadingWhitespace(" a b  c "));
	}

	@Test
	public void testTrimTrailingWhitespace() {
		assertEquals(null, StringUtils.trimTrailingWhitespace(null));
		assertEquals("", StringUtils.trimTrailingWhitespace(""));
		assertEquals("", StringUtils.trimTrailingWhitespace(" "));
		assertEquals("", StringUtils.trimTrailingWhitespace("\t"));
		assertEquals("a", StringUtils.trimTrailingWhitespace("a "));
		assertEquals(" a", StringUtils.trimTrailingWhitespace(" a"));
		assertEquals(" a", StringUtils.trimTrailingWhitespace(" a "));
		assertEquals(" a b", StringUtils.trimTrailingWhitespace(" a b "));
		assertEquals(" a b  c", StringUtils.trimTrailingWhitespace(" a b  c "));
	}

	@Test
	public void testTrimLeadingCharacter() {
		assertEquals(null, StringUtils.trimLeadingCharacter(null, ' '));
		assertEquals("", StringUtils.trimLeadingCharacter("", ' '));
		assertEquals("", StringUtils.trimLeadingCharacter(" ", ' '));
		assertEquals("\t", StringUtils.trimLeadingCharacter("\t", ' '));
		assertEquals("a", StringUtils.trimLeadingCharacter(" a", ' '));
		assertEquals("a ", StringUtils.trimLeadingCharacter("a ", ' '));
		assertEquals("a ", StringUtils.trimLeadingCharacter(" a ", ' '));
		assertEquals("a b ", StringUtils.trimLeadingCharacter(" a b ", ' '));
		assertEquals("a b  c ", StringUtils.trimLeadingCharacter(" a b  c ", ' '));
	}

	@Test
	public void testTrimTrailingCharacter() {
		assertEquals(null, StringUtils.trimTrailingCharacter(null, ' '));
		assertEquals("", StringUtils.trimTrailingCharacter("", ' '));
		assertEquals("", StringUtils.trimTrailingCharacter(" ", ' '));
		assertEquals("\t", StringUtils.trimTrailingCharacter("\t", ' '));
		assertEquals("a", StringUtils.trimTrailingCharacter("a ", ' '));
		assertEquals(" a", StringUtils.trimTrailingCharacter(" a", ' '));
		assertEquals(" a", StringUtils.trimTrailingCharacter(" a ", ' '));
		assertEquals(" a b", StringUtils.trimTrailingCharacter(" a b ", ' '));
		assertEquals(" a b  c", StringUtils.trimTrailingCharacter(" a b  c ", ' '));
	}

	@Test
	public void testStartsWithIgnoreCase() {
		String prefix = "fOo";
		assertThat(StringUtils.startsWithIgnoreCase("foo", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("Foo", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("foobar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("foobarbar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("Foobar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("FoobarBar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("foObar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("FOObar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("fOobar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase(null, prefix)).isFalse();
		assertThat(StringUtils.startsWithIgnoreCase("fOobar", null)).isFalse();
		assertThat(StringUtils.startsWithIgnoreCase("b", prefix)).isFalse();
		assertThat(StringUtils.startsWithIgnoreCase("barfoo", prefix)).isFalse();
		assertThat(StringUtils.startsWithIgnoreCase("barfoobar", prefix)).isFalse();
	}

	@Test
	public void testEndsWithIgnoreCase() {
		String suffix = "fOo";
		assertThat(StringUtils.endsWithIgnoreCase("foo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("Foo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barfoo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barbarfoo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barFoo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barBarFoo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barfoO", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barFOO", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barfOo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase(null, suffix)).isFalse();
		assertThat(StringUtils.endsWithIgnoreCase("barfOo", null)).isFalse();
		assertThat(StringUtils.endsWithIgnoreCase("b", suffix)).isFalse();
		assertThat(StringUtils.endsWithIgnoreCase("foobar", suffix)).isFalse();
		assertThat(StringUtils.endsWithIgnoreCase("barfoobar", suffix)).isFalse();
	}

	@Test
	public void testSubstringMatch() {
		assertThat(StringUtils.substringMatch("foo", 0, "foo")).isTrue();
		assertThat(StringUtils.substringMatch("foo", 1, "oo")).isTrue();
		assertThat(StringUtils.substringMatch("foo", 2, "o")).isTrue();
		assertThat(StringUtils.substringMatch("foo", 0, "fOo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 1, "fOo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 2, "fOo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 3, "fOo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 1, "Oo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 2, "Oo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 3, "Oo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 2, "O")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 3, "O")).isFalse();
	}

	@Test
	public void testCountOccurrencesOf() {
		assertThat(StringUtils.countOccurrencesOf(null, null) == 0).as("nullx2 = 0").isTrue();
		assertThat(StringUtils.countOccurrencesOf("s", null) == 0).as("null string = 0").isTrue();
		assertThat(StringUtils.countOccurrencesOf(null, "s") == 0).as("null substring = 0").isTrue();
		String s = "erowoiueoiur";
		assertThat(StringUtils.countOccurrencesOf(s, "WERWER") == 0).as("not found = 0").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "x") == 0).as("not found char = 0").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, " ") == 0).as("not found ws = 0").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "") == 0).as("not found empty string = 0").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "e") == 2).as("found char=2").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "oi") == 2).as("found substring=2").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "oiu") == 2).as("found substring=2").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "oiur") == 1).as("found substring=3").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "r") == 2).as("test last").isTrue();
	}

	@Test
	public void testReplace() {
		String inString = "a6AazAaa77abaa";
		String oldPattern = "aa";
		String newPattern = "foo";

		// Simple replace
		String s = StringUtils.replace(inString, oldPattern, newPattern);
		assertThat(s.equals("a6AazAfoo77abfoo")).as("Replace 1 worked").isTrue();

		// Non match: no change
		s = StringUtils.replace(inString, "qwoeiruqopwieurpoqwieur", newPattern);
		assertSame("Replace non-matched is returned as-is", inString, s);

		// Null new pattern: should ignore
		s = StringUtils.replace(inString, oldPattern, null);
		assertSame("Replace non-matched is returned as-is", inString, s);

		// Null old pattern: should ignore
		s = StringUtils.replace(inString, null, newPattern);
		assertSame("Replace non-matched is returned as-is", inString, s);
	}

	@Test
	public void testDelete() {
		String inString = "The quick brown fox jumped over the lazy dog";

		String noThe = StringUtils.delete(inString, "the");
		assertThat(noThe.equals("The quick brown fox jumped over  lazy dog")).as("Result has no the [" + noThe + "]").isTrue();

		String nohe = StringUtils.delete(inString, "he");
		assertThat(nohe.equals("T quick brown fox jumped over t lazy dog")).as("Result has no he [" + nohe + "]").isTrue();

		String nosp = StringUtils.delete(inString, " ");
		assertThat(nosp.equals("Thequickbrownfoxjumpedoverthelazydog")).as("Result has no spaces").isTrue();

		String killEnd = StringUtils.delete(inString, "dog");
		assertThat(killEnd.equals("The quick brown fox jumped over the lazy ")).as("Result has no dog").isTrue();

		String mismatch = StringUtils.delete(inString, "dxxcxcxog");
		assertThat(mismatch.equals(inString)).as("Result is unchanged").isTrue();

		String nochange = StringUtils.delete(inString, "");
		assertThat(nochange.equals(inString)).as("Result is unchanged").isTrue();
	}

	@Test
	public void testDeleteAny() {
		String inString = "Able was I ere I saw Elba";

		String res = StringUtils.deleteAny(inString, "I");
		assertThat(res.equals("Able was  ere  saw Elba")).as("Result has no Is [" + res + "]").isTrue();

		res = StringUtils.deleteAny(inString, "AeEba!");
		assertThat(res.equals("l ws I r I sw l")).as("Result has no Is [" + res + "]").isTrue();

		String mismatch = StringUtils.deleteAny(inString, "#@$#$^");
		assertThat(mismatch.equals(inString)).as("Result is unchanged").isTrue();

		String whitespace = "This is\n\n\n    \t   a messagy string with whitespace\n";
		assertThat(whitespace.contains("\n")).as("Has CR").isTrue();
		assertThat(whitespace.contains("\t")).as("Has tab").isTrue();
		assertThat(whitespace.contains(" ")).as("Has  sp").isTrue();
		String cleaned = StringUtils.deleteAny(whitespace, "\n\t ");
		boolean condition2 = !cleaned.contains("\n");
		assertThat(condition2).as("Has no CR").isTrue();
		boolean condition1 = !cleaned.contains("\t");
		assertThat(condition1).as("Has no tab").isTrue();
		boolean condition = !cleaned.contains(" ");
		assertThat(condition).as("Has no sp").isTrue();
		assertThat(cleaned.length() > 10).as("Still has chars").isTrue();
	}


	@Test
	public void testQuote() {
		assertEquals("'myString'", StringUtils.quote("myString"));
		assertEquals("''", StringUtils.quote(""));
		assertNull(StringUtils.quote(null));
	}

	@Test
	public void testQuoteIfString() {
		assertEquals("'myString'", StringUtils.quoteIfString("myString"));
		assertEquals("''", StringUtils.quoteIfString(""));
		assertEquals(Integer.valueOf(5), StringUtils.quoteIfString(5));
		assertNull(StringUtils.quoteIfString(null));
	}

	@Test
	public void testUnqualify() {
		String qualified = "i.am.not.unqualified";
		assertEquals("unqualified", StringUtils.unqualify(qualified));
	}

	@Test
	public void testCapitalize() {
		String capitalized = "i am not capitalized";
		assertEquals("I am not capitalized", StringUtils.capitalize(capitalized));
	}

	@Test
	public void testUncapitalize() {
		String capitalized = "I am capitalized";
		assertEquals("i am capitalized", StringUtils.uncapitalize(capitalized));
	}

	@Test
	public void testGetFilename() {
		assertEquals(null, StringUtils.getFilename(null));
		assertEquals("", StringUtils.getFilename(""));
		assertEquals("myfile", StringUtils.getFilename("myfile"));
		assertEquals("myfile", StringUtils.getFilename("mypath/myfile"));
		assertEquals("myfile.", StringUtils.getFilename("myfile."));
		assertEquals("myfile.", StringUtils.getFilename("mypath/myfile."));
		assertEquals("myfile.txt", StringUtils.getFilename("myfile.txt"));
		assertEquals("myfile.txt", StringUtils.getFilename("mypath/myfile.txt"));
	}

	@Test
	public void testGetFilenameExtension() {
		assertEquals(null, StringUtils.getFilenameExtension(null));
		assertEquals(null, StringUtils.getFilenameExtension(""));
		assertEquals(null, StringUtils.getFilenameExtension("myfile"));
		assertEquals(null, StringUtils.getFilenameExtension("myPath/myfile"));
		assertEquals(null, StringUtils.getFilenameExtension("/home/user/.m2/settings/myfile"));
		assertEquals("", StringUtils.getFilenameExtension("myfile."));
		assertEquals("", StringUtils.getFilenameExtension("myPath/myfile."));
		assertEquals("txt", StringUtils.getFilenameExtension("myfile.txt"));
		assertEquals("txt", StringUtils.getFilenameExtension("mypath/myfile.txt"));
		assertEquals("txt", StringUtils.getFilenameExtension("/home/user/.m2/settings/myfile.txt"));
	}

	@Test
	public void testStripFilenameExtension() {
		assertEquals("", StringUtils.stripFilenameExtension(""));
		assertEquals("myfile", StringUtils.stripFilenameExtension("myfile"));
		assertEquals("myfile", StringUtils.stripFilenameExtension("myfile."));
		assertEquals("myfile", StringUtils.stripFilenameExtension("myfile.txt"));
		assertEquals("mypath/myfile", StringUtils.stripFilenameExtension("mypath/myfile"));
		assertEquals("mypath/myfile", StringUtils.stripFilenameExtension("mypath/myfile."));
		assertEquals("mypath/myfile", StringUtils.stripFilenameExtension("mypath/myfile.txt"));
		assertEquals("/home/user/.m2/settings/myfile", StringUtils.stripFilenameExtension("/home/user/.m2/settings/myfile"));
		assertEquals("/home/user/.m2/settings/myfile", StringUtils.stripFilenameExtension("/home/user/.m2/settings/myfile."));
		assertEquals("/home/user/.m2/settings/myfile", StringUtils.stripFilenameExtension("/home/user/.m2/settings/myfile.txt"));
	}

	@Test
	public void testCleanPath() {
		assertEquals("mypath/myfile", StringUtils.cleanPath("mypath/myfile"));
		assertEquals("mypath/myfile", StringUtils.cleanPath("mypath\\myfile"));
		assertEquals("mypath/myfile", StringUtils.cleanPath("mypath/../mypath/myfile"));
		assertEquals("mypath/myfile", StringUtils.cleanPath("mypath/myfile/../../mypath/myfile"));
		assertEquals("../mypath/myfile", StringUtils.cleanPath("../mypath/myfile"));
		assertEquals("../mypath/myfile", StringUtils.cleanPath("../mypath/../mypath/myfile"));
		assertEquals("../mypath/myfile", StringUtils.cleanPath("mypath/../../mypath/myfile"));
		assertEquals("/../mypath/myfile", StringUtils.cleanPath("/../mypath/myfile"));
		assertEquals("/mypath/myfile", StringUtils.cleanPath("/a/:b/../../mypath/myfile"));
		assertEquals("/", StringUtils.cleanPath("/"));
		assertEquals("/", StringUtils.cleanPath("/mypath/../"));
		assertEquals("", StringUtils.cleanPath("mypath/.."));
		assertEquals("", StringUtils.cleanPath("mypath/../."));
		assertEquals("./", StringUtils.cleanPath("mypath/../"));
		assertEquals("./", StringUtils.cleanPath("././"));
		assertEquals("./", StringUtils.cleanPath("./"));
		assertEquals("../", StringUtils.cleanPath("../"));
		assertEquals("../", StringUtils.cleanPath("./../"));
		assertEquals("../", StringUtils.cleanPath(".././"));
		assertEquals("file:/", StringUtils.cleanPath("file:/"));
		assertEquals("file:/", StringUtils.cleanPath("file:/mypath/../"));
		assertEquals("file:", StringUtils.cleanPath("file:mypath/.."));
		assertEquals("file:", StringUtils.cleanPath("file:mypath/../."));
		assertEquals("file:./", StringUtils.cleanPath("file:mypath/../"));
		assertEquals("file:./", StringUtils.cleanPath("file:././"));
		assertEquals("file:./", StringUtils.cleanPath("file:./"));
		assertEquals("file:../", StringUtils.cleanPath("file:../"));
		assertEquals("file:../", StringUtils.cleanPath("file:./../"));
		assertEquals("file:../", StringUtils.cleanPath("file:.././"));
		assertEquals("file:///c:/path/the%20file.txt", StringUtils.cleanPath("file:///c:/some/../path/the%20file.txt"));
	}

	@Test
	public void testPathEquals() {
		assertThat(StringUtils.pathEquals("/dummy1/dummy2/dummy3", "/dummy1/dummy2/dummy3")).as("Must be true for the same strings").isTrue();
		assertThat(StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3", "C:\\dummy1\\dummy2\\dummy3")).as("Must be true for the same win strings").isTrue();
		assertThat(StringUtils.pathEquals("/dummy1/bin/../dummy2/dummy3", "/dummy1/dummy2/dummy3")).as("Must be true for one top path on 1").isTrue();
		assertThat(StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3", "C:\\dummy1\\bin\\..\\dummy2\\dummy3")).as("Must be true for one win top path on 2").isTrue();
		assertThat(StringUtils.pathEquals("/dummy1/bin/../dummy2/bin/../dummy3", "/dummy1/dummy2/dummy3")).as("Must be true for two top paths on 1").isTrue();
		assertThat(StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3", "C:\\dummy1\\bin\\..\\dummy2\\bin\\..\\dummy3")).as("Must be true for two win top paths on 2").isTrue();
		assertThat(StringUtils.pathEquals("/dummy1/bin/tmp/../../dummy2/dummy3", "/dummy1/dummy2/dummy3")).as("Must be true for double top paths on 1").isTrue();
		assertThat(StringUtils.pathEquals("/dummy1/dummy2/dummy3", "/dummy1/dum/dum/../../dummy2/dummy3")).as("Must be true for double top paths on 2 with similarity").isTrue();
		assertThat(StringUtils.pathEquals("./dummy1/dummy2/dummy3", "dummy1/dum/./dum/../../dummy2/dummy3")).as("Must be true for current paths").isTrue();
		assertThat(StringUtils.pathEquals("./dummy1/dummy2/dummy3", "/dummy1/dum/./dum/../../dummy2/dummy3")).as("Must be false for relative/absolute paths").isFalse();
		assertThat(StringUtils.pathEquals("/dummy1/dummy2/dummy3", "/dummy1/dummy4/dummy3")).as("Must be false for different strings").isFalse();
		assertThat(StringUtils.pathEquals("/dummy1/bin/tmp/../dummy2/dummy3", "/dummy1/dummy2/dummy3")).as("Must be false for one false path on 1").isFalse();
		assertThat(StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3", "C:\\dummy1\\bin\\tmp\\..\\dummy2\\dummy3")).as("Must be false for one false win top path on 2").isFalse();
		assertThat(StringUtils.pathEquals("/dummy1/bin/../dummy2/dummy3", "/dummy1/dummy2/dummy4")).as("Must be false for top path on 1 + difference").isFalse();
	}

	@Test
	public void testConcatenateStringArrays() {
		String[] input1 = new String[] {"myString2"};
		String[] input2 = new String[] {"myString1", "myString2"};
		String[] result = StringUtils.concatenateStringArrays(input1, input2);
		assertEquals(3, result.length);
		assertEquals("myString2", result[0]);
		assertEquals("myString1", result[1]);
		assertEquals("myString2", result[2]);

		assertArrayEquals(input1, StringUtils.concatenateStringArrays(input1, null));
		assertArrayEquals(input2, StringUtils.concatenateStringArrays(null, input2));
		assertNull(StringUtils.concatenateStringArrays(null, null));
	}

	@Test
	@Deprecated
	public void testMergeStringArrays() {
		String[] input1 = new String[] {"myString2"};
		String[] input2 = new String[] {"myString1", "myString2"};
		String[] result = StringUtils.mergeStringArrays(input1, input2);
		assertEquals(2, result.length);
		assertEquals("myString2", result[0]);
		assertEquals("myString1", result[1]);

		assertArrayEquals(input1, StringUtils.mergeStringArrays(input1, null));
		assertArrayEquals(input2, StringUtils.mergeStringArrays(null, input2));
		assertNull(StringUtils.mergeStringArrays(null, null));
	}

	@Test
	public void testSortStringArray() {
		String[] input = new String[] {"myString2"};
		input = StringUtils.addStringToArray(input, "myString1");
		assertEquals("myString2", input[0]);
		assertEquals("myString1", input[1]);

		StringUtils.sortStringArray(input);
		assertEquals("myString1", input[0]);
		assertEquals("myString2", input[1]);
	}

	@Test
	public void testRemoveDuplicateStrings() {
		String[] input = new String[] {"myString2", "myString1", "myString2"};
		input = StringUtils.removeDuplicateStrings(input);
		assertEquals("myString2", input[0]);
		assertEquals("myString1", input[1]);
	}

	@Test
	public void testSplitArrayElementsIntoProperties() {
		String[] input = new String[] {"key1=value1 ", "key2 =\"value2\""};
		Properties result = StringUtils.splitArrayElementsIntoProperties(input, "=");
		assertEquals("value1", result.getProperty("key1"));
		assertEquals("\"value2\"", result.getProperty("key2"));
	}

	@Test
	public void testSplitArrayElementsIntoPropertiesAndDeletedChars() {
		String[] input = new String[] {"key1=value1 ", "key2 =\"value2\""};
		Properties result = StringUtils.splitArrayElementsIntoProperties(input, "=", "\"");
		assertEquals("value1", result.getProperty("key1"));
		assertEquals("value2", result.getProperty("key2"));
	}

	@Test
	public void testTokenizeToStringArray() {
		String[] sa = StringUtils.tokenizeToStringArray("a,b , ,c", ",");
		assertEquals(3, sa.length);
		assertThat(sa[0].equals("a") && sa[1].equals("b") && sa[2].equals("c")).as("components are correct").isTrue();
	}

	@Test
	public void testTokenizeToStringArrayWithNotIgnoreEmptyTokens() {
		String[] sa = StringUtils.tokenizeToStringArray("a,b , ,c", ",", true, false);
		assertEquals(4, sa.length);
		assertThat(sa[0].equals("a") && sa[1].equals("b") && sa[2].equals("") && sa[3].equals("c")).as("components are correct").isTrue();
	}

	@Test
	public void testTokenizeToStringArrayWithNotTrimTokens() {
		String[] sa = StringUtils.tokenizeToStringArray("a,b ,c", ",", false, true);
		assertEquals(3, sa.length);
		assertThat(sa[0].equals("a") && sa[1].equals("b ") && sa[2].equals("c")).as("components are correct").isTrue();
	}

	@Test
	public void testCommaDelimitedListToStringArrayWithNullProducesEmptyArray() {
		String[] sa = StringUtils.commaDelimitedListToStringArray(null);
		assertThat(sa != null).as("String array isn't null with null input").isTrue();
		assertThat(sa.length == 0).as("String array length == 0 with null input").isTrue();
	}

	@Test
	public void testCommaDelimitedListToStringArrayWithEmptyStringProducesEmptyArray() {
		String[] sa = StringUtils.commaDelimitedListToStringArray("");
		assertThat(sa != null).as("String array isn't null with null input").isTrue();
		assertThat(sa.length == 0).as("String array length == 0 with null input").isTrue();
	}

	@Test
	public void testDelimitedListToStringArrayWithComma() {
		String[] sa = StringUtils.delimitedListToStringArray("a,b", ",");
		assertEquals(2, sa.length);
		assertEquals("a", sa[0]);
		assertEquals("b", sa[1]);
	}

	@Test
	public void testDelimitedListToStringArrayWithSemicolon() {
		String[] sa = StringUtils.delimitedListToStringArray("a;b", ";");
		assertEquals(2, sa.length);
		assertEquals("a", sa[0]);
		assertEquals("b", sa[1]);
	}

	@Test
	public void testDelimitedListToStringArrayWithEmptyString() {
		String[] sa = StringUtils.delimitedListToStringArray("a,b", "");
		assertEquals(3, sa.length);
		assertEquals("a", sa[0]);
		assertEquals(",", sa[1]);
		assertEquals("b", sa[2]);
	}

	@Test
	public void testDelimitedListToStringArrayWithNullDelimiter() {
		String[] sa = StringUtils.delimitedListToStringArray("a,b", null);
		assertEquals(1, sa.length);
		assertEquals("a,b", sa[0]);
	}

	@Test
	public void testCommaDelimitedListToStringArrayMatchWords() {
		// Could read these from files
		String[] sa = new String[] {"foo", "bar", "big"};
		doTestCommaDelimitedListToStringArrayLegalMatch(sa);
		doTestStringArrayReverseTransformationMatches(sa);

		sa = new String[] {"a", "b", "c"};
		doTestCommaDelimitedListToStringArrayLegalMatch(sa);
		doTestStringArrayReverseTransformationMatches(sa);

		// Test same words
		sa = new String[] {"AA", "AA", "AA", "AA", "AA"};
		doTestCommaDelimitedListToStringArrayLegalMatch(sa);
		doTestStringArrayReverseTransformationMatches(sa);
	}

	private void doTestStringArrayReverseTransformationMatches(String[] sa) {
		String[] reverse =
				StringUtils.commaDelimitedListToStringArray(StringUtils.arrayToCommaDelimitedString(sa));
		assertEquals("Reverse transformation is equal",
				Arrays.asList(sa),
				Arrays.asList(reverse));
	}

	@Test
	public void testCommaDelimitedListToStringArraySingleString() {
		// Could read these from files
		String s = "woeirqupoiewuropqiewuorpqiwueopriquwopeiurqopwieur";
		String[] sa = StringUtils.commaDelimitedListToStringArray(s);
		assertThat(sa.length == 1).as("Found one String with no delimiters").isTrue();
		assertThat(sa[0].equals(s)).as("Single array entry matches input String with no delimiters").isTrue();
	}

	@Test
	public void testCommaDelimitedListToStringArrayWithOtherPunctuation() {
		// Could read these from files
		String[] sa = new String[] {"xcvwert4456346&*.", "///", ".!", ".", ";"};
		doTestCommaDelimitedListToStringArrayLegalMatch(sa);
	}

	/**
	 * We expect to see the empty Strings in the output.
	 */
	@Test
	public void testCommaDelimitedListToStringArrayEmptyStrings() {
		// Could read these from files
		String[] sa = StringUtils.commaDelimitedListToStringArray("a,,b");
		assertEquals("a,,b produces array length 3", 3, sa.length);
		assertThat(sa[0].equals("a") && sa[1].equals("") && sa[2].equals("b")).as("components are correct").isTrue();

		sa = new String[] {"", "", "a", ""};
		doTestCommaDelimitedListToStringArrayLegalMatch(sa);
	}

	private void doTestCommaDelimitedListToStringArrayLegalMatch(String[] components) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < components.length; i++) {
			if (i != 0) {
				sb.append(",");
			}
			sb.append(components[i]);
		}
		String[] sa = StringUtils.commaDelimitedListToStringArray(sb.toString());
		assertThat(sa != null).as("String array isn't null with legal match").isTrue();
		assertEquals("String array length is correct with legal match", components.length, sa.length);
		assertThat(Arrays.equals(sa, components)).as("Output equals input").isTrue();
	}


	@Test
	public void testParseLocaleStringSunnyDay() {
		Locale expectedLocale = Locale.UK;
		Locale locale = StringUtils.parseLocaleString(expectedLocale.toString());
		assertNotNull("When given a bona-fide Locale string, must not return null.", locale);
		assertEquals(expectedLocale, locale);
	}

	@Test
	public void testParseLocaleStringWithMalformedLocaleString() {
		Locale locale = StringUtils.parseLocaleString("_banjo_on_my_knee");
		assertNotNull("When given a malformed Locale string, must not return null.", locale);
	}

	@Test
	public void testParseLocaleStringWithEmptyLocaleStringYieldsNullLocale() {
		Locale locale = StringUtils.parseLocaleString("");
		assertNull("When given an empty Locale string, must return null.", locale);
	}

	@Test  // SPR-8637
	public void testParseLocaleWithMultiSpecialCharactersInVariant() {
		String variant = "proper-northern";
		String localeString = "en_GB_" + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Multi-valued variant portion of the Locale not extracted correctly.", variant, locale.getVariant());
	}

	@Test  // SPR-3671
	public void testParseLocaleWithMultiValuedVariant() {
		String variant = "proper_northern";
		String localeString = "en_GB_" + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Multi-valued variant portion of the Locale not extracted correctly.", variant, locale.getVariant());
	}

	@Test  // SPR-3671
	public void testParseLocaleWithMultiValuedVariantUsingSpacesAsSeparators() {
		String variant = "proper northern";
		String localeString = "en GB " + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Multi-valued variant portion of the Locale not extracted correctly.", variant, locale.getVariant());
	}

	@Test  // SPR-3671
	public void testParseLocaleWithMultiValuedVariantUsingMixtureOfUnderscoresAndSpacesAsSeparators() {
		String variant = "proper northern";
		String localeString = "en_GB_" + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Multi-valued variant portion of the Locale not extracted correctly.", variant, locale.getVariant());
	}

	@Test  // SPR-3671
	public void testParseLocaleWithMultiValuedVariantUsingSpacesAsSeparatorsWithLotsOfLeadingWhitespace() {
		String variant = "proper northern";
		String localeString = "en GB            " + variant;  // lots of whitespace
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Multi-valued variant portion of the Locale not extracted correctly.", variant, locale.getVariant());
	}

	@Test  // SPR-3671
	public void testParseLocaleWithMultiValuedVariantUsingUnderscoresAsSeparatorsWithLotsOfLeadingWhitespace() {
		String variant = "proper_northern";
		String localeString = "en_GB_____" + variant;  // lots of underscores
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Multi-valued variant portion of the Locale not extracted correctly.", variant, locale.getVariant());
	}

	@Test  // SPR-7779
	public void testParseLocaleWithInvalidCharacters() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				StringUtils.parseLocaleString("%0D%0AContent-length:30%0D%0A%0D%0A%3Cscript%3Ealert%28123%29%3C/script%3E"));
	}

	@Test  // SPR-9420
	public void testParseLocaleWithSameLowercaseTokenForLanguageAndCountry() {
		assertEquals("tr_TR", StringUtils.parseLocaleString("tr_tr").toString());
		assertEquals("bg_BG_vnt", StringUtils.parseLocaleString("bg_bg_vnt").toString());
	}

	@Test  // SPR-11806
	public void testParseLocaleWithVariantContainingCountryCode() {
		String variant = "GBtest";
		String localeString = "en_GB_" + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Variant containing country code not extracted correctly", variant, locale.getVariant());
	}

	@Test  // SPR-14718, SPR-7598
	public void testParseJava7Variant() {
		assertEquals("sr__#LATN", StringUtils.parseLocaleString("sr__#LATN").toString());
	}

	@Test  // SPR-16651
	public void testAvailableLocalesWithLocaleString() {
		for (Locale locale : Locale.getAvailableLocales()) {
			Locale parsedLocale = StringUtils.parseLocaleString(locale.toString());
			if (parsedLocale == null) {
				assertEquals("", locale.getLanguage());
			}
			else {
				assertEquals(parsedLocale.toString(), locale.toString());
			}
		}
	}

	@Test  // SPR-16651
	public void testAvailableLocalesWithLanguageTag() {
		for (Locale locale : Locale.getAvailableLocales()) {
			Locale parsedLocale = StringUtils.parseLocale(locale.toLanguageTag());
			if (parsedLocale == null) {
				assertEquals("", locale.getLanguage());
			}
			else {
				assertEquals(parsedLocale.toLanguageTag(), locale.toLanguageTag());
			}
		}
	}

	@Test
	public void testInvalidLocaleWithLocaleString() {
		assertEquals(new Locale("invalid"), StringUtils.parseLocaleString("invalid"));
		assertEquals(new Locale("invalidvalue"), StringUtils.parseLocaleString("invalidvalue"));
		assertEquals(new Locale("invalidvalue", "foo"), StringUtils.parseLocaleString("invalidvalue_foo"));
		assertNull(StringUtils.parseLocaleString(""));
	}

	@Test
	public void testInvalidLocaleWithLanguageTag() {
		assertEquals(new Locale("invalid"), StringUtils.parseLocale("invalid"));
		assertEquals(new Locale("invalidvalue"), StringUtils.parseLocale("invalidvalue"));
		assertEquals(new Locale("invalidvalue", "foo"), StringUtils.parseLocale("invalidvalue_foo"));
		assertNull(StringUtils.parseLocale(""));
	}

}
