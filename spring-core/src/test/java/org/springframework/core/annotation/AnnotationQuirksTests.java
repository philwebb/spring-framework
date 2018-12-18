/*
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author pwebb
 * @since 5.0
 */
public class AnnotationQuirksTests {

	@Test
	public void foo() {
		One annotation = AnnotatedElementUtils.getMergedAnnotation(WithThree.class,
				One.class);
		assertThat(annotation.oneValue()).isEqualTo("foo");
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface One {

		String oneValue() default "11";

		String twoValue() default "12";

	}

	@One
	@Retention(RetentionPolicy.RUNTIME)
	static @interface Two {

		@AliasFor(annotation = One.class, attribute = "twoValue")
		String threeValue() default "21";

	}

	@Two
	@Retention(RetentionPolicy.RUNTIME)
	static @interface Three {

		@AliasFor(annotation = One.class, attribute = "oneValue")
		String threeValue() default "";

	}

	@Three(threeValue = "foo")
	static class WithThree {

	}

}
