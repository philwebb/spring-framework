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

package org.springframework.web.reactive.result.method.annotation;


import java.util.HashSet;

import org.junit.Test;

import org.springframework.mock.web.test.server.MockWebSession;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.server.WebSession;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;

/**
 * Test fixture with {@link SessionAttributesHandler}.
 * @author Rossen Stoyanchev
 */
public class SessionAttributesHandlerTests {

	private final SessionAttributesHandler sessionAttributesHandler =
			new SessionAttributesHandler(TestController.class);


	@Test
	public void isSessionAttribute() {
		assertThat(this.sessionAttributesHandler.isHandlerSessionAttribute("attr1", String.class)).isTrue();
		assertThat(this.sessionAttributesHandler.isHandlerSessionAttribute("attr2", String.class)).isTrue();
		assertThat(this.sessionAttributesHandler.isHandlerSessionAttribute("simple", TestBean.class)).isTrue();
		assertThat(this.sessionAttributesHandler.isHandlerSessionAttribute("simple", String.class)).isFalse();
	}

	@Test
	public void retrieveAttributes() {
		WebSession session = new MockWebSession();
		session.getAttributes().put("attr1", "value1");
		session.getAttributes().put("attr2", "value2");
		session.getAttributes().put("attr3", new TestBean());
		session.getAttributes().put("attr4", new TestBean());

		assertThat(sessionAttributesHandler.retrieveAttributes(session).keySet()).as("Named attributes (attr1, attr2) should be 'known' right away").isEqualTo(new HashSet<>(asList("attr1", "attr2")));

		// Resolve 'attr3' by type
		sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class);

		assertThat(sessionAttributesHandler.retrieveAttributes(session).keySet()).as("Named attributes (attr1, attr2) and resolved attribute (att3) should be 'known'").isEqualTo(new HashSet<>(asList("attr1", "attr2", "attr3")));
	}

	@Test
	public void cleanupAttributes() {
		WebSession session = new MockWebSession();
		session.getAttributes().put("attr1", "value1");
		session.getAttributes().put("attr2", "value2");
		session.getAttributes().put("attr3", new TestBean());

		this.sessionAttributesHandler.cleanupAttributes(session);

		assertNull(session.getAttributes().get("attr1"));
		assertNull(session.getAttributes().get("attr2"));
		assertNotNull(session.getAttributes().get("attr3"));

		// Resolve 'attr3' by type
		this.sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class);
		this.sessionAttributesHandler.cleanupAttributes(session);

		assertNull(session.getAttributes().get("attr3"));
	}

	@Test
	public void storeAttributes() {

		ModelMap model = new ModelMap();
		model.put("attr1", "value1");
		model.put("attr2", "value2");
		model.put("attr3", new TestBean());

		WebSession session = new MockWebSession();
		sessionAttributesHandler.storeAttributes(session, model);

		assertThat(session.getAttributes().get("attr1")).isEqualTo("value1");
		assertThat(session.getAttributes().get("attr2")).isEqualTo("value2");
		boolean condition = session.getAttributes().get("attr3") instanceof TestBean;
		assertThat(condition).isTrue();
	}


	@SessionAttributes(names = { "attr1", "attr2" }, types = { TestBean.class })
	private static class TestController {
	}

}
