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
		assertThat(annotation.a()).isEqualTo("foo");
//		MergedAnnotation<One> annotation = MergedAnnotations.from(WithThree.class).get(One.class);
//		//assertThat(annotation.getString("oneValue")).isEqualTo("foo");
//		assertThat(annotation.getString("twoValue")).isEqualTo("12");
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface One {

		String a() default "1a";

		String b() default "1b";

	}

	@One
	@Retention(RetentionPolicy.RUNTIME)
	static @interface Two {

		@AliasFor(annotation = One.class, attribute = "b")
		String a() default "2a";

	}

	@Two
	@Retention(RetentionPolicy.RUNTIME)
	static @interface Three {

		@AliasFor(annotation = One.class, attribute = "a")
		String oneA() default "3a";

	}

	@Three(oneA = "foo")
	static class WithThree {

	}

}
