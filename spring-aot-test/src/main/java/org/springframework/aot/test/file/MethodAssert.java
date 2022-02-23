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

package org.springframework.aot.test.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;

import org.assertj.core.api.AbstractAssert;

import com.thoughtworks.qdox.model.JavaMethod;

/**
 * Assertion methods for {@code SourceFile} methods.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class MethodAssert extends AbstractAssert<MethodAssert, JavaMethod> {

	MethodAssert(JavaMethod actual) {
		super(actual, MethodAssert.class);
		as(describe(actual));
	}

	private String describe(JavaMethod actual) {
		return actual.getName() + "(" + actual.getParameters().stream()
				.map((parameter) -> parameter.getType().getFullyQualifiedName()).collect(Collectors.joining(", "))
				+ ")";
	}

	public void withBody(String expected) {
		assertThat(actual.getSourceCode()).as(this.info.description()).isEqualToNormalizingWhitespace(expected);
	}

	public void withBodyContaining(CharSequence... values) {
		assertThat(actual.getSourceCode()).as(this.info.description()).contains(values);
	}

}
