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

package org.springframework.scheduling.support;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.scheduling.TriggerContext;

import static org.junit.Assert.*;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
@RunWith(Parameterized.class)
public class CronTriggerTests {

	private final Calendar calendar = new GregorianCalendar();

	private final Date date;

	private final TimeZone timeZone;


	public CronTriggerTests(Date date, TimeZone timeZone) {
		this.date = date;
		this.timeZone = timeZone;
	}

	@Parameters(name = "date [{0}], time zone [{1}]")
	public static List<Object[]> getParameters() {
		List<Object[]> list = new ArrayList<>();
		list.add(new Object[] { new Date(), TimeZone.getTimeZone("PST") });
		list.add(new Object[] { new Date(), TimeZone.getTimeZone("CET") });
		return list;
	}

	private void roundup(Calendar calendar) {
		calendar.add(Calendar.SECOND, 1);
		calendar.set(Calendar.MILLISECOND, 0);
	}


	@Before
	public void setUp() {
		this.calendar.setTimeZone(this.timeZone);
		this.calendar.setTime(this.date);
		roundup(this.calendar);
	}

	@Test
	public void testMatchAll() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * *", this.timeZone);
		TriggerContext context = getTriggerContext(this.date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testMatchLastSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * *", this.timeZone);
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.SECOND, 58);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	public void testMatchSpecificSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("10 * * * * *", this.timeZone);
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.SECOND, 9);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	public void testIncrementSecondByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("11 * * * * *", this.timeZone);
		this.calendar.set(Calendar.SECOND, 10);
		Date date = this.calendar.getTime();
		this.calendar.add(Calendar.SECOND, 1);
		TriggerContext context = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementSecondWithPreviousExecutionTooEarly() throws Exception {
		CronTrigger trigger = new CronTrigger("11 * * * * *", this.timeZone);
		this.calendar.set(Calendar.SECOND, 11);
		SimpleTriggerContext context = new SimpleTriggerContext();
		context.update(this.calendar.getTime(), new Date(this.calendar.getTimeInMillis() - 100),
				new Date(this.calendar.getTimeInMillis() - 90));
		this.calendar.add(Calendar.MINUTE, 1);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementSecondAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("10 * * * * *", this.timeZone);
		this.calendar.set(Calendar.SECOND, 11);
		Date date = this.calendar.getTime();
		this.calendar.add(Calendar.SECOND, 59);
		TriggerContext context = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testSecondRange() throws Exception {
		CronTrigger trigger = new CronTrigger("10-15 * * * * *", this.timeZone);
		this.calendar.set(Calendar.SECOND, 9);
		assertMatchesNextSecond(trigger, this.calendar);
		this.calendar.set(Calendar.SECOND, 14);
		assertMatchesNextSecond(trigger, this.calendar);
	}

	@Test
	public void testIncrementMinute() throws Exception {
		CronTrigger trigger = new CronTrigger("0 * * * * *", this.timeZone);
		this.calendar.set(Calendar.MINUTE, 10);
		Date date = this.calendar.getTime();
		this.calendar.add(Calendar.MINUTE, 1);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context1 = getTriggerContext(date);
		date = trigger.nextExecutionTime(context1);
		assertEquals(this.calendar.getTime(), date);
		this.calendar.add(Calendar.MINUTE, 1);
		TriggerContext context2 = getTriggerContext(date);
		date = trigger.nextExecutionTime(context2);
		assertEquals(this.calendar.getTime(), date);
	}

	@Test
	public void testIncrementMinuteByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("0 11 * * * *", this.timeZone);
		this.calendar.set(Calendar.MINUTE, 10);
		TriggerContext context = getTriggerContext(this.calendar.getTime());
		this.calendar.add(Calendar.MINUTE, 1);
		this.calendar.set(Calendar.SECOND, 0);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementMinuteAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("0 10 * * * *", this.timeZone);
		this.calendar.set(Calendar.MINUTE, 11);
		this.calendar.set(Calendar.SECOND, 0);
		Date date = this.calendar.getTime();
		this.calendar.add(Calendar.MINUTE, 59);
		TriggerContext context = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementHour() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 * * * *", this.timeZone);
		this.calendar.set(Calendar.MONTH, 9);
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		this.calendar.set(Calendar.HOUR_OF_DAY, 11);
		this.calendar.set(Calendar.MINUTE, 1);
		this.calendar.set(Calendar.SECOND, 0);
		Date date = this.calendar.getTime();
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.HOUR_OF_DAY, 12);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		this.calendar.set(Calendar.HOUR_OF_DAY, 13);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testIncrementHourAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 * * * *", this.timeZone);
		this.calendar.set(Calendar.MONTH, 9);
		this.calendar.set(Calendar.DAY_OF_MONTH, 10);
		this.calendar.set(Calendar.HOUR_OF_DAY, 23);
		this.calendar.set(Calendar.MINUTE, 1);
		this.calendar.set(Calendar.SECOND, 0);
		Date date = this.calendar.getTime();
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.DAY_OF_MONTH, 11);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		this.calendar.set(Calendar.HOUR_OF_DAY, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testIncrementDayOfMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *", this.timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		Date date = this.calendar.getTime();
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		assertEquals(2, this.calendar.get(Calendar.DAY_OF_MONTH));
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context2));
		assertEquals(3, this.calendar.get(Calendar.DAY_OF_MONTH));
	}

	@Test
	public void testIncrementDayOfMonthByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * 10 * *", this.timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 9);
		Date date = this.calendar.getTime();
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementDayOfMonthAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * 10 * *", this.timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 11);
		Date date = this.calendar.getTime();
		this.calendar.add(Calendar.MONTH, 1);
		this.calendar.set(Calendar.DAY_OF_MONTH, 10);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testDailyTriggerInShortMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *", this.timeZone);
		this.calendar.set(Calendar.MONTH, 8); // September: 30 days
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = this.calendar.getTime();
		this.calendar.set(Calendar.MONTH, 9); // October
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		this.calendar.set(Calendar.DAY_OF_MONTH, 2);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testDailyTriggerInLongMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *", this.timeZone);
		this.calendar.set(Calendar.MONTH, 7); // August: 31 days and not a daylight saving boundary
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = this.calendar.getTime();
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		this.calendar.set(Calendar.DAY_OF_MONTH, 31);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		this.calendar.set(Calendar.MONTH, 8); // September
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testDailyTriggerOnDaylightSavingBoundary() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *", this.timeZone);
		this.calendar.set(Calendar.MONTH, 9); // October: 31 days and a daylight saving boundary in CET
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = this.calendar.getTime();
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		this.calendar.set(Calendar.DAY_OF_MONTH, 31);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		this.calendar.set(Calendar.MONTH, 10); // November
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testIncrementMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *", this.timeZone);
		this.calendar.set(Calendar.MONTH, 9);
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = this.calendar.getTime();
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		this.calendar.set(Calendar.MONTH, 10);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		this.calendar.set(Calendar.MONTH, 11);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testIncrementMonthAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *", this.timeZone);
		this.calendar.set(Calendar.MONTH, 11);
		this.calendar.set(Calendar.DAY_OF_MONTH, 31);
		this.calendar.set(Calendar.YEAR, 2010);
		Date date = this.calendar.getTime();
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		this.calendar.set(Calendar.MONTH, 0);
		this.calendar.set(Calendar.YEAR, 2011);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		this.calendar.set(Calendar.MONTH, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testMonthlyTriggerInLongMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 31 * *", this.timeZone);
		this.calendar.set(Calendar.MONTH, 9);
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = this.calendar.getTime();
		this.calendar.set(Calendar.DAY_OF_MONTH, 31);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testMonthlyTriggerInShortMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *", this.timeZone);
		this.calendar.set(Calendar.MONTH, 9);
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = this.calendar.getTime();
		this.calendar.set(Calendar.MONTH, 10);
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementDayOfWeekByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * 2", this.timeZone);
		this.calendar.set(Calendar.DAY_OF_WEEK, 2);
		Date date = this.calendar.getTime();
		this.calendar.add(Calendar.DAY_OF_WEEK, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context));
		assertEquals(Calendar.TUESDAY, this.calendar.get(Calendar.DAY_OF_WEEK));
	}

	@Test
	public void testIncrementDayOfWeekAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * 2", this.timeZone);
		this.calendar.set(Calendar.DAY_OF_WEEK, 4);
		Date date = this.calendar.getTime();
		this.calendar.add(Calendar.DAY_OF_MONTH, 6);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), trigger.nextExecutionTime(context));
		assertEquals(Calendar.TUESDAY, this.calendar.get(Calendar.DAY_OF_WEEK));
	}

	@Test
	public void testSpecificMinuteSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("55 5 * * * *", this.timeZone);
		this.calendar.set(Calendar.MINUTE, 4);
		this.calendar.set(Calendar.SECOND, 54);
		Date date = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		this.calendar.add(Calendar.MINUTE, 1);
		this.calendar.set(Calendar.SECOND, 55);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		this.calendar.add(Calendar.HOUR, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test
	public void testSpecificHourSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("55 * 10 * * *", this.timeZone);
		this.calendar.set(Calendar.HOUR_OF_DAY, 9);
		this.calendar.set(Calendar.SECOND, 54);
		Date date = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		this.calendar.add(Calendar.HOUR_OF_DAY, 1);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 55);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		this.calendar.add(Calendar.MINUTE, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test
	public void testSpecificMinuteHour() throws Exception {
		CronTrigger trigger = new CronTrigger("* 5 10 * * *", this.timeZone);
		this.calendar.set(Calendar.MINUTE, 4);
		this.calendar.set(Calendar.HOUR_OF_DAY, 9);
		Date date = this.calendar.getTime();
		this.calendar.add(Calendar.MINUTE, 1);
		this.calendar.add(Calendar.HOUR_OF_DAY, 1);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		// next trigger is in one second because second is wildcard
		this.calendar.add(Calendar.SECOND, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test
	public void testSpecificDayOfMonthSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("55 * * 3 * *", this.timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 2);
		this.calendar.set(Calendar.SECOND, 54);
		Date date = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 55);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		this.calendar.add(Calendar.MINUTE, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test
	public void testSpecificDate() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * 3 11 *", this.timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 2);
		this.calendar.set(Calendar.MONTH, 9);
		Date date = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MONTH, 10); // 10=November
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		this.calendar.add(Calendar.SECOND, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonExistentSpecificDate() throws Exception {
		// TODO: maybe try and detect this as a special case in parser?
		CronTrigger trigger = new CronTrigger("0 0 0 31 6 *", this.timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 10);
		this.calendar.set(Calendar.MONTH, 2);
		Date date = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		trigger.nextExecutionTime(context1);
		// new CronTrigger("0 0 0 30 1 ?", timeZone);
	}

	@Test
	public void testLeapYearSpecificDate() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 29 2 *", this.timeZone);
		this.calendar.set(Calendar.YEAR, 2007);
		this.calendar.set(Calendar.DAY_OF_MONTH, 10);
		this.calendar.set(Calendar.MONTH, 1); // 2=February
		Date date = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		this.calendar.set(Calendar.YEAR, 2008);
		this.calendar.set(Calendar.DAY_OF_MONTH, 29);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		this.calendar.add(Calendar.YEAR, 4);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test
	public void testWeekDaySequence() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 7 ? * MON-FRI", this.timeZone);
		// This is a Saturday
		this.calendar.set(2009, 8, 26);
		Date date = this.calendar.getTime();
		// 7 am is the trigger time
		this.calendar.set(Calendar.HOUR_OF_DAY, 7);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		// Add two days because we start on Saturday
		this.calendar.add(Calendar.DAY_OF_MONTH, 2);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		// Next day is a week day so add one
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context2));
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context3 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context3));
	}

	@Test
	public void testDayOfWeekIndifferent() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * 2 * *", this.timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * 2 * ?", this.timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSecondIncrementer() throws Exception {
		CronTrigger trigger1 = new CronTrigger("57,59 * * * * *", this.timeZone);
		CronTrigger trigger2 = new CronTrigger("57/2 * * * * *", this.timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSecondIncrementerWithRange() throws Exception {
		CronTrigger trigger1 = new CronTrigger("1,3,5 * * * * *", this.timeZone);
		CronTrigger trigger2 = new CronTrigger("1-6/2 * * * * *", this.timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testHourIncrementer() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * 4,8,12,16,20 * * *", this.timeZone);
		CronTrigger trigger2 = new CronTrigger("* * 4/4 * * *", this.timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testDayNames() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0-6", this.timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * * TUE,WED,THU,FRI,SAT,SUN,MON", this.timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSundayIsZero() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0", this.timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * * SUN", this.timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSundaySynonym() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0", this.timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * * 7", this.timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testMonthNames() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * 1-12 *", this.timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * FEB,JAN,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC *", this.timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testMonthNamesMixedCase() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * 2 *", this.timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * Feb *", this.timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSecondInvalid() throws Exception {
		new CronTrigger("77 * * * * *", this.timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSecondRangeInvalid() throws Exception {
		new CronTrigger("44-77 * * * * *", this.timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMinuteInvalid() throws Exception {
		new CronTrigger("* 77 * * * *", this.timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMinuteRangeInvalid() throws Exception {
		new CronTrigger("* 44-77 * * * *", this.timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHourInvalid() throws Exception {
		new CronTrigger("* * 27 * * *", this.timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHourRangeInvalid() throws Exception {
		new CronTrigger("* * 23-28 * * *", this.timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDayInvalid() throws Exception {
		new CronTrigger("* * * 45 * *", this.timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDayRangeInvalid() throws Exception {
		new CronTrigger("* * * 28-45 * *", this.timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMonthInvalid() throws Exception {
		new CronTrigger("0 0 0 25 13 ?", this.timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMonthInvalidTooSmall() throws Exception {
		new CronTrigger("0 0 0 25 0 ?", this.timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDayOfMonthInvalid() throws Exception {
		new CronTrigger("0 0 0 32 12 ?", this.timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMonthRangeInvalid() throws Exception {
		new CronTrigger("* * * * 11-13 *", this.timeZone);
	}

	@Test
	public void testWhitespace() throws Exception {
		CronTrigger trigger1 = new CronTrigger("*  *  * *  1 *", this.timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * 1 *", this.timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testMonthSequence() throws Exception {
		CronTrigger trigger = new CronTrigger("0 30 23 30 1/3 ?", this.timeZone);
		this.calendar.set(2010, 11, 30);
		Date date = this.calendar.getTime();
		// set expected next trigger time
		this.calendar.set(Calendar.HOUR_OF_DAY, 23);
		this.calendar.set(Calendar.MINUTE, 30);
		this.calendar.set(Calendar.SECOND, 0);
		this.calendar.add(Calendar.MONTH, 1);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
		// Next trigger is 3 months latter
		this.calendar.add(Calendar.MONTH, 3);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context2));
		// Next trigger is 3 months latter
		this.calendar.add(Calendar.MONTH, 3);
		TriggerContext context3 = getTriggerContext(date);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context3));
	}

	@Test
	public void testDaylightSavingMissingHour() throws Exception {
		// This trigger has to be somewhere in between 2am and 3am
		CronTrigger trigger = new CronTrigger("0 10 2 * * *", this.timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 31);
		this.calendar.set(Calendar.MONTH, Calendar.MARCH);
		this.calendar.set(Calendar.YEAR, 2013);
		this.calendar.set(Calendar.HOUR_OF_DAY, 1);
		this.calendar.set(Calendar.SECOND, 54);
		Date date = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		if (this.timeZone.equals(TimeZone.getTimeZone("CET"))) {
			// Clocks go forward an hour so 2am doesn't exist in CET for this date
			this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		}
		this.calendar.add(Calendar.HOUR_OF_DAY, 1);
		this.calendar.set(Calendar.MINUTE, 10);
		this.calendar.set(Calendar.SECOND, 0);
		assertEquals(this.calendar.getTime(), date = trigger.nextExecutionTime(context1));
	}

	private void assertMatchesNextSecond(CronTrigger trigger, Calendar calendar) {
		Date date = calendar.getTime();
		roundup(calendar);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	private static TriggerContext getTriggerContext(Date lastCompletionTime) {
		SimpleTriggerContext context = new SimpleTriggerContext();
		context.update(null, null, lastCompletionTime);
		return context;
	}

}
