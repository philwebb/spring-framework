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

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.HamcrestCondition;
import org.w3c.dom.Node;
import org.xmlunit.util.Predicate;

import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

/**
 *
 * @author pwebb
 * @since 5.2
 */
public class XmlContentAssert extends AbstractAssert<XmlContentAssert, XmlContent> {

	XmlContentAssert(XmlContent actual) {
		super(actual, XmlContentAssert.class);
	}

	public XmlContent isSimilarTo(Object control) {
		return null;
	}

	public XmlContent isSimilarTo(Object control, Predicate<Node> nodefilter) {
		return null;
	}

	/**
	 * @param expectedString
	 */
	public void isSimilarToIgnoringWhitepace(Object expectedString) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

//	static HamcrestCondition<Object> similarTo(Object control) {
//		return new HamcrestCondition<Object>(isSimilarTo(control));
//	}
//
//	static HamcrestCondition<Object> similarTo(Object control,
//			Predicate<Node> nodeFilter) {
//		return new HamcrestCondition<Object>(
//				isSimilarTo(control).withNodeFilter(nodeFilter));
//	}


}
