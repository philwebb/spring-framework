/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jmx.export.assembler;

import org.junit.Test;
import org.springframework.jmx.JmxTestBean;

import static org.junit.Assert.assertTrue;

/**
 * @author Rob Harrop
 */
public abstract class AbstractAutodetectTests {

	@Test
	public void autodetect() throws Exception {
		JmxTestBean bean = new JmxTestBean();

		AutodetectCapableMBeanInfoAssembler assembler = getAssembler();
		assertTrue("The bean should be included", assembler.includeBean(bean.getClass(), "testBean"));
	}

	protected abstract AutodetectCapableMBeanInfoAssembler getAssembler();

}
