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

package org.springframework.format.datetime.standard;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import org.springframework.format.annotation.DateTimeFormat.ISO;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Phillip Webb
 * @author Sam Brannen
 */
public class DateTimeFormatterFactoryTests {

	// Potential test timezone, both have daylight savings on October 21st
	private static final TimeZone ZURICH = TimeZone.getTimeZone("Europe/Zurich");
	private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

	// Ensure that we are testing against a timezone other than the default.
	private static final TimeZone TEST_TIMEZONE = ZURICH.equals(TimeZone.getDefault()) ? NEW_YORK : ZURICH;


	private DateTimeFormatterFactory factory = new DateTimeFormatterFactory();

	private LocalDateTime dateTime = LocalDateTime.of(2009, 10, 21, 12, 10, 00, 00);


	@Test
	public void createDateTimeFormatter() {
		assertThat(this.factory.createDateTimeFormatter().toString(), is(equalTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).toString())));
	}

	@Test
	public void createDateTimeFormatterWithPattern() {
		this.factory = new DateTimeFormatterFactory("yyyyMMddHHmmss");
		DateTimeFormatter formatter = this.factory.createDateTimeFormatter();
		assertThat(formatter.format(this.dateTime), is("20091021121000"));
	}

	@Test
	public void createDateTimeFormatterWithNullFallback() {
		DateTimeFormatter formatter = this.factory.createDateTimeFormatter(null);
		assertThat(formatter, is(nullValue()));
	}

	@Test
	public void createDateTimeFormatterWithFallback() {
		DateTimeFormatter fallback = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG);
		DateTimeFormatter formatter = this.factory.createDateTimeFormatter(fallback);
		assertThat(formatter, is(sameInstance(fallback)));
	}

	@Test
	public void createDateTimeFormatterInOrderOfPropertyPriority() {
		this.factory.setStylePattern("SS");
		String value = applyLocale(this.factory.createDateTimeFormatter()).format(this.dateTime);
		assertTrue(value.startsWith("10/21/09"));
		assertTrue(value.endsWith("12:10 PM"));

		this.factory.setIso(ISO.DATE);
		assertThat(applyLocale(this.factory.createDateTimeFormatter()).format(this.dateTime), is("2009-10-21"));

		this.factory.setPattern("yyyyMMddHHmmss");
		assertThat(this.factory.createDateTimeFormatter().format(this.dateTime), is("20091021121000"));
	}

	@Test
	public void createDateTimeFormatterWithTimeZone() {
		this.factory.setPattern("yyyyMMddHHmmss Z");
		this.factory.setTimeZone(TEST_TIMEZONE);
		ZoneId dateTimeZone = TEST_TIMEZONE.toZoneId();
		ZonedDateTime dateTime = ZonedDateTime.of(2009, 10, 21, 12, 10, 00, 00, dateTimeZone);
		String offset = (TEST_TIMEZONE.equals(NEW_YORK) ? "-0400" : "+0200");
		assertThat(this.factory.createDateTimeFormatter().format(dateTime), is("20091021121000 " + offset));
	}

	private DateTimeFormatter applyLocale(DateTimeFormatter dateTimeFormatter) {
		return dateTimeFormatter.withLocale(Locale.US);
	}

}
