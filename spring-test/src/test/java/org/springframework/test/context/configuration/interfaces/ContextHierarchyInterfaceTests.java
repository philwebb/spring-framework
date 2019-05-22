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

package org.springframework.test.context.configuration.interfaces;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;

/**
 * @author Sam Brannen
 * @since 4.3
 */
@RunWith(SpringRunner.class)
public class ContextHierarchyInterfaceTests implements ContextHierarchyTestInterface {

	@Autowired
	String foo;

	@Autowired
	String bar;

	@Autowired
	String baz;

	@Autowired
	ApplicationContext context;


	@Test
	public void loadContextHierarchy() {
		assertNotNull("child ApplicationContext", context);
		assertNotNull("parent ApplicationContext", context.getParent());
		assertNull("grandparent ApplicationContext", context.getParent().getParent());
		assertThat((Object) foo).isEqualTo("foo");
		assertThat((Object) bar).isEqualTo("bar");
		assertThat((Object) baz).isEqualTo("baz-child");
	}

}
