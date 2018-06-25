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

package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockBodyContent;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link ArgumentTag}
 *
 * @author Nicholas Williams
 */
public class ArgumentTagTests extends AbstractTagTests {

	private ArgumentTag tag;

	private MockArgumentSupportTag parent;

	@Before
	public void setUp() throws Exception {
		PageContext context = createPageContext();
		this.parent = new MockArgumentSupportTag();
		this.tag = new ArgumentTag();
		this.tag.setPageContext(context);
		this.tag.setParent(this.parent);
	}

	@Test
	public void argumentWithStringValue() throws JspException {
		this.tag.setValue("value1");

		int action = this.tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("value1", this.parent.getArgument());
	}

	@Test
	public void argumentWithImplicitNullValue() throws JspException {
		int action = this.tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertNull(this.parent.getArgument());
	}

	@Test
	public void argumentWithExplicitNullValue() throws JspException {
		this.tag.setValue(null);

		int action = this.tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertNull(this.parent.getArgument());
	}

	@Test
	public void argumentWithBodyValue() throws JspException {
		this.tag.setBodyContent(new MockBodyContent("value2",
				new MockHttpServletResponse()));

		int action = this.tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("value2", this.parent.getArgument());
	}

	@Test
	public void argumentWithValueThenReleaseThenBodyValue() throws JspException {
		this.tag.setValue("value3");

		int action = this.tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("value3", this.parent.getArgument());

		this.tag.release();

		this.parent = new MockArgumentSupportTag();
		this.tag.setPageContext(createPageContext());
		this.tag.setParent(this.parent);
		this.tag.setBodyContent(new MockBodyContent("value4",
				new MockHttpServletResponse()));

		action = this.tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("value4", this.parent.getArgument());
	}

	@SuppressWarnings("serial")
	private class MockArgumentSupportTag extends TagSupport implements ArgumentAware {

		Object argument;

		@Override
		public void addArgument(Object argument) {
			this.argument = argument;
		}

		private Object getArgument() {
			return this.argument;
		}
	}

}
