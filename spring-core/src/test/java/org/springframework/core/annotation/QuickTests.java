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

package org.springframework.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.springframework.core.annotation.QuickTests.Foo;

import static org.junit.Assert.*;

/**
 * 
 * @author pwebb
 * @since 5.1
 */
public class QuickTests {

	@Test
	public void testName() {
		Foo foo = InternalAnnotatedElementUtils.findMergedAnnotation(WithInheritedFoo.class, Foo.class);
		System.out.println(foo.value());
	}
	
	
	@Retention(RetentionPolicy.RUNTIME)
	static @interface Foo {
		
		String value();
		
	}
	
	@Foo("super")
	static class SuperWithFoo {
		
	}
	
	@Foo("interface")
	static interface InterfaceWithFoo {
		
	}
	
	static class WithInheritedFoo extends SuperWithFoo implements InterfaceWithFoo {
		
	}
	
}
