/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.tests;

import java.io.StringWriter;

import javax.xml.transform.Source;

import org.assertj.core.api.AssertProvider;
import org.xmlunit.builder.Input;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Test util that allows {@link CompareMatcher XML Unit CompareMatchers} to be
 * used with AssertJ.
 */
public class XmlContent implements AssertProvider<XmlContentAssert> {

	private Source source;

	private XmlContent(Source source) {
	}

	@Override
	public XmlContentAssert assertThat() {
		return new XmlContentAssert(this);
	}

	public static XmlContent of(Object object) {
		if(object instanceof StringWriter) {
			return of(object.toString());
		}
		return of(Input.from(object));
	}

	public static XmlContent of(String xml) {
		return of(Input.from(xml));
	}

	public static XmlContent of(Input.Builder input) {
		return of(input.build());
	}

	public static XmlContent of(Source source) {
		return new XmlContent(source);
	}


}
