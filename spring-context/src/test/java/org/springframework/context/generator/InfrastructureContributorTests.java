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

package org.springframework.context.generator;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.javapoet.support.CodeSnippet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InfrastructureContributor}.
 *
 * @author Stephane Nicoll
 */
class InfrastructureContributorTests {

	@Test
	void contributeInfrastructure() {
		InfrastructureContributor contributor = new InfrastructureContributor();
		CodeSnippet contribution = CodeSnippet.of(contributor.contribute(mock(GeneratedTypeContext.class)));
		assertThat(contribution.getSnippet()).isEqualTo("""
				// infrastructure
				beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
				""");
		assertThat(contribution.hasImport(ContextAnnotationAutowireCandidateResolver.class)).isTrue();
	}

}
