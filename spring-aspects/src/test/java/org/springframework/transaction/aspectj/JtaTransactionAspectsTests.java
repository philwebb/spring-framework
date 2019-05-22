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

package org.springframework.transaction.aspectj;

import java.io.IOException;
import javax.transaction.Transactional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.tests.transaction.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Stephane Nicoll
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JtaTransactionAspectsTests.Config.class)
public class JtaTransactionAspectsTests {

	@Autowired
	private CallCountingTransactionManager txManager;

	@Before
	public void setUp() {
		this.txManager.clear();
	}

	@Test
	public void commitOnAnnotatedPublicMethod() throws Throwable {
		assertThat((long) this.txManager.begun).isEqualTo((long) 0);
		new JtaAnnotationPublicAnnotatedMember().echo(null);
		assertThat((long) this.txManager.commits).isEqualTo((long) 1);
	}

	@Test
	public void matchingRollbackOnApplied() throws Throwable {
		assertThat((long) this.txManager.begun).isEqualTo((long) 0);
		InterruptedException test = new InterruptedException();
		assertThatExceptionOfType(InterruptedException.class).isThrownBy(() ->
				new JtaAnnotationPublicAnnotatedMember().echo(test))
			.isSameAs(test);
		assertThat((long) this.txManager.rollbacks).isEqualTo((long) 1);
		assertThat((long) this.txManager.commits).isEqualTo((long) 0);
	}

	@Test
	public void nonMatchingRollbackOnApplied() throws Throwable {
		assertThat((long) this.txManager.begun).isEqualTo((long) 0);
		IOException test = new IOException();
		assertThatIOException().isThrownBy(() ->
				new JtaAnnotationPublicAnnotatedMember().echo(test))
			.isSameAs(test);
		assertThat((long) this.txManager.commits).isEqualTo((long) 1);
		assertThat((long) this.txManager.rollbacks).isEqualTo((long) 0);
	}

	@Test
	public void commitOnAnnotatedProtectedMethod() {
		assertThat((long) this.txManager.begun).isEqualTo((long) 0);
		new JtaAnnotationProtectedAnnotatedMember().doInTransaction();
		assertThat((long) this.txManager.commits).isEqualTo((long) 1);
	}

	@Test
	public void nonAnnotatedMethodCallingProtectedMethod() {
		assertThat((long) this.txManager.begun).isEqualTo((long) 0);
		new JtaAnnotationProtectedAnnotatedMember().doSomething();
		assertThat((long) this.txManager.commits).isEqualTo((long) 1);
	}

	@Test
	public void commitOnAnnotatedPrivateMethod() {
		assertThat((long) this.txManager.begun).isEqualTo((long) 0);
		new JtaAnnotationPrivateAnnotatedMember().doInTransaction();
		assertThat((long) this.txManager.commits).isEqualTo((long) 1);
	}

	@Test
	public void nonAnnotatedMethodCallingPrivateMethod() {
		assertThat((long) this.txManager.begun).isEqualTo((long) 0);
		new JtaAnnotationPrivateAnnotatedMember().doSomething();
		assertThat((long) this.txManager.commits).isEqualTo((long) 1);
	}

	@Test
	public void notTransactional() {
		assertThat((long) this.txManager.begun).isEqualTo((long) 0);
		new TransactionAspectTests.NotTransactional().noop();
		assertThat((long) this.txManager.begun).isEqualTo((long) 0);
	}


	public static class JtaAnnotationPublicAnnotatedMember {

		@Transactional(rollbackOn = InterruptedException.class)
		public void echo(Throwable t) throws Throwable {
			if (t != null) {
				throw t;
			}
		}

	}


	protected static class JtaAnnotationProtectedAnnotatedMember {

		public void doSomething() {
			doInTransaction();
		}

		@Transactional
		protected void doInTransaction() {
		}
	}


	protected static class JtaAnnotationPrivateAnnotatedMember {

		public void doSomething() {
			doInTransaction();
		}

		@Transactional
		private void doInTransaction() {
		}
	}


	@Configuration
	protected static class Config {

		@Bean
		public CallCountingTransactionManager transactionManager() {
			return new CallCountingTransactionManager();
		}

		@Bean
		public JtaAnnotationTransactionAspect transactionAspect() {
			JtaAnnotationTransactionAspect aspect = JtaAnnotationTransactionAspect.aspectOf();
			aspect.setTransactionManager(transactionManager());
			return aspect;
		}
	}

}
