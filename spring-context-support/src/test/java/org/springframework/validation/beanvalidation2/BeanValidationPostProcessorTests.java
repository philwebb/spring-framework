/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.validation.beanvalidation2;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.validation.beanvalidation.BeanValidationPostProcessor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Juergen Hoeller
 */
public class BeanValidationPostProcessorTests {

	@Test
	public void testNotNullConstraint() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("bvpp", new RootBeanDefinition(BeanValidationPostProcessor.class));
		ac.registerBeanDefinition("capp", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
		ac.registerBeanDefinition("bean", new RootBeanDefinition(NotNullConstrainedBean.class));
		try {
			ac.refresh();
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getRootCause().getMessage().contains("testBean"));
			assertTrue(ex.getRootCause().getMessage().contains("invalid"));
		}
		ac.close();
	}

	@Test
	public void testNotNullConstraintSatisfied() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("bvpp", new RootBeanDefinition(BeanValidationPostProcessor.class));
		ac.registerBeanDefinition("capp", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
		RootBeanDefinition bd = new RootBeanDefinition(NotNullConstrainedBean.class);
		bd.getPropertyValues().add("testBean", new TestBean());
		ac.registerBeanDefinition("bean", bd);
		ac.refresh();
		ac.close();
	}

	@Test
	public void testNotNullConstraintAfterInitialization() {
		GenericApplicationContext ac = new GenericApplicationContext();
		RootBeanDefinition bvpp = new RootBeanDefinition(BeanValidationPostProcessor.class);
		bvpp.getPropertyValues().add("afterInitialization", true);
		ac.registerBeanDefinition("bvpp", bvpp);
		ac.registerBeanDefinition("capp", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
		ac.registerBeanDefinition("bean", new RootBeanDefinition(AfterInitConstraintBean.class));
		ac.refresh();
		ac.close();
	}

	@Test
	public void testSizeConstraint() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("bvpp", new RootBeanDefinition(BeanValidationPostProcessor.class));
		RootBeanDefinition bd = new RootBeanDefinition(NotNullConstrainedBean.class);
		bd.getPropertyValues().add("testBean", new TestBean());
		bd.getPropertyValues().add("stringValue", "s");
		ac.registerBeanDefinition("bean", bd);
		try {
			ac.refresh();
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getRootCause().getMessage().contains("stringValue"));
			assertTrue(ex.getRootCause().getMessage().contains("invalid"));
		}
		ac.close();
	}

	@Test
	public void testSizeConstraintSatisfied() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("bvpp", new RootBeanDefinition(BeanValidationPostProcessor.class));
		RootBeanDefinition bd = new RootBeanDefinition(NotNullConstrainedBean.class);
		bd.getPropertyValues().add("testBean", new TestBean());
		bd.getPropertyValues().add("stringValue", "ss");
		ac.registerBeanDefinition("bean", bd);
		ac.refresh();
		ac.close();
	}


	public static class NotNullConstrainedBean {

		@NotNull
		private TestBean testBean;

		@Size(min = 2)
		private String stringValue;

		public TestBean getTestBean() {
			return this.testBean;
		}

		public void setTestBean(TestBean testBean) {
			this.testBean = testBean;
		}

		public String getStringValue() {
			return this.stringValue;
		}

		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}

		@PostConstruct
		public void init() {
			assertNotNull("Shouldn't be here after constraint checking", this.testBean);
		}
	}


	public static class AfterInitConstraintBean {

		@NotNull
		private TestBean testBean;

		public TestBean getTestBean() {
			return this.testBean;
		}

		public void setTestBean(TestBean testBean) {
			this.testBean = testBean;
		}

		@PostConstruct
		public void init() {
			this.testBean = new TestBean();
		}
	}

}
