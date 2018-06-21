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

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * @author Phillip Webb
 * @author Sam Brannen
 */
public class DateTimeFormatterFactoryBeanTests {

	private final DateTimeFormatterFactoryBean factory = new DateTimeFormatterFactoryBean();


	@Test
	public void isSingleton() {
		assertThat(this.factory.isSingleton(), is(true));
	}

	@Test
	public void getObjectType() {
		assertThat(this.factory.getObjectType(), is(equalTo(DateTimeFormatter.class)));
	}

	@Test
	public void getObject() {
		this.factory.afterPropertiesSet();
		assertThat(this.factory.getObject().toString(), is(equalTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).toString())));
	}

	@Test
	public void getObjectIsAlwaysSingleton() {
		this.factory.afterPropertiesSet();
		DateTimeFormatter formatter = this.factory.getObject();
		assertThat(formatter.toString(), is(equalTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).toString())));
		this.factory.setStylePattern("LL");
		assertThat(this.factory.getObject(), is(sameInstance(formatter)));
	}

}
