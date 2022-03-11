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

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.aot.DefaultBeanRegistrationCode.BeanDefinitionCustomizeCode;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultBeanRegistrationCode}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class DefaultBeanRegistrationCodeTests {

	/**
	 * Tests for {@link BeanDefinitionCustomizeCode}.
	 */
	@Nested
	class BeanDefinitionCustomizeCodeTests {

		// FIXME convert to TestCompiler based test

		private RootBeanDefinition bd = new RootBeanDefinition();

		@Test
		void setPrimaryWhenFalse() {
			this.bd.setPrimary(false);
			assertThatCodeBlock(bd).isEmpty();
		}

		@Test
		void setPrimaryWhenTrue() {
			this.bd.setPrimary(true);
			assertThatCodeBlock(this.bd).contains("bd.setPrimary(true);");
		}

		@Test
		void setScopeWhenEmptyString() {
			this.bd.setScope("");
			assertThatCodeBlock(this.bd).isEmpty();
		}

		@Test
		void setScopeWhenSingleton() {
			this.bd.setScope("singleton");
			assertThatCodeBlock(this.bd).isEmpty();
		}

		@Test
		void setScopeWhenOther() {
			this.bd.setScope("prototype");
			assertThatCodeBlock(this.bd).contains("bd.setScope(\"prototype\");");
		}

		@Test
		void setDependsOnWhenEmpty() {
			this.bd.setDependsOn();
			assertThatCodeBlock(this.bd).isEmpty();
		}

		@Test
		void setDependsOnWhenNotEmpty() {
			this.bd.setDependsOn("a", "b", "c");
			assertThatCodeBlock(this.bd).contains("bd.setDependsOn(\"a\", \"b\", \"c\");");
		}

		@Test
		void setLazyInitWhenFalse() {
			this.bd.setLazyInit(false);
			assertThatCodeBlock(this.bd).isEmpty();
		}

		@Test
		void setLazyInitWhenTrue() {
			this.bd.setLazyInit(true);
			assertThatCodeBlock(this.bd).contains("bd.setLazyInit(true);");
		}

		@Test
		void setAutowireCandidateWhenFalse() {
			this.bd.setAutowireCandidate(false);
			assertThatCodeBlock(this.bd).contains("bd.setAutowireCandidate(false);");
		}

		@Test
		void setAutowireCandidateWhenTrue() {
			this.bd.setAutowireCandidate(true);
			assertThatCodeBlock(this.bd).isEmpty();
		}

		@Test
		void setSyntheticWhenFalse() {
			this.bd.setSynthetic(false);
			assertThatCodeBlock(this.bd).isEmpty();
		}

		@Test
		void setSyntheticWhenTrue() {
			this.bd.setSynthetic(true);
			assertThatCodeBlock(this.bd).contains("bd.setSynthetic(true);");
		}

		@Test
		void setRoleWhenApplication() {
			this.bd.setRole(BeanDefinition.ROLE_APPLICATION);
			assertThatCodeBlock(this.bd).isEmpty();
		}

		@Test
		void setRoleWhenInfrastructure() {
			this.bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			assertThatCodeBlock(this.bd).contains("bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);");
		}

		@Test
		void setRoleWhenSupport() {
			this.bd.setRole(BeanDefinition.ROLE_SUPPORT);
			assertThatCodeBlock(this.bd).contains("bd.setRole(BeanDefinition.ROLE_SUPPORT);");
		}

		@Test
		void setRoleWhenOther() {
			this.bd.setRole(999);
			assertThatCodeBlock(this.bd).contains("bd.setRole(999);");
		}

		@Test
		void constructorArgumentValuesWhenValues() {
			this.bd.getConstructorArgumentValues().addIndexedArgumentValue(0, String.class);
			this.bd.getConstructorArgumentValues().addIndexedArgumentValue(1, "test");
			this.bd.getConstructorArgumentValues().addIndexedArgumentValue(2, 123);
			assertThatCodeBlock(this.bd)
					.contains("bd.getConstructorArgumentValues().addIndexedArgumentValue(0,java.lang.String.class);")
					.contains("bd.getConstructorArgumentValues().addIndexedArgumentValue(1,\"test\");")
					.contains("bd.getConstructorArgumentValues().addIndexedArgumentValue(2,123);");
		}

		@Test
		void propertyValuesWhenValues() {
			this.bd.getPropertyValues().add("test", String.class);
			this.bd.getPropertyValues().add("spring", "framework");
			assertThatCodeBlock(this.bd)
					.contains("bd.getPropertyValues().addPropertyValue(\"test\", java.lang.String.class);");
			assertThatCodeBlock(this.bd)
					.contains("bd.getPropertyValues().addPropertyValue(\"spring\", \"framework\");");
		}

		@Test
		void attributesWhenAllFiltered() {
			this.bd.setAttribute("a", "A");
			this.bd.setAttribute("b", "B");
			assertThatCodeBlock(bd).isEmpty();
		}

		@Test
		void attributesWhenSomeFiltered() {
			this.bd.setAttribute("a", "A");
			this.bd.setAttribute("b", "B");
			BeanDefinitionCustomizeCode code = new BeanDefinitionCustomizeCode(this.bd, "bd",
					attribute -> "a".equals(attribute));
			assertThat(code.getCodeBlock().toString()).contains("bd.setAttribute(\"a\", \"A\");")
					.doesNotContain("\"b\"");
		}

		private AbstractStringAssert<?> assertThatCodeBlock(RootBeanDefinition bd) {
			BeanDefinitionCustomizeCode code = new BeanDefinitionCustomizeCode(bd, "bd");
			return assertThat(code.getCodeBlock().toString());
		}

	}

}
