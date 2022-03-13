/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.aot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.CollectionFactory;

/**
 *
 * @author pwebb
 * @since 6.0
 */
public class DunnoGenerator {


	void testName() {
		Collection<Object> collection = CollectionFactory.createCollection(ArrayList.class, String.class, 102);
		RootBeanDefinition bd = null;
		bd.getPropertyValues().add("foo", getTestNameFoo());

	}

	private Object getTestNameFoo() {
		Map<String, Map<String, Integer>> map = new LinkedHashMap<>();
		return map;
	}


}
