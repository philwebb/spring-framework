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

package org.springframework.test.context.junit4;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionTestUtils.assertInTransaction;

/**
 * JUnit 4 based integration test which verifies
 * {@link BeforeTransaction @BeforeTransaction} and
 * {@link AfterTransaction @AfterTransaction} behavior.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@Transactional
public class BeforeAndAfterTransactionAnnotationTests extends AbstractTransactionalSpringRunnerTests {

	protected static JdbcTemplate jdbcTemplate;

	protected static int numBeforeTransactionCalls = 0;
	protected static int numAfterTransactionCalls = 0;

	protected boolean inTransaction = false;

	@Rule
	public final TestName testName = new TestName();


	@Autowired
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}


	@BeforeClass
	public static void beforeClass() {
		BeforeAndAfterTransactionAnnotationTests.numBeforeTransactionCalls = 0;
		BeforeAndAfterTransactionAnnotationTests.numAfterTransactionCalls = 0;
	}

	@AfterClass
	public static void afterClass() {
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the final number of rows in the person table after all tests.").isEqualTo((long) 3);
		assertThat(BeforeAndAfterTransactionAnnotationTests.numBeforeTransactionCalls).as("Verifying the total number of calls to beforeTransaction().").isEqualTo((long) 2);
		assertThat(BeforeAndAfterTransactionAnnotationTests.numAfterTransactionCalls).as("Verifying the total number of calls to afterTransaction().").isEqualTo((long) 2);
	}

	@BeforeTransaction
	void beforeTransaction() {
		assertInTransaction(false);
		this.inTransaction = true;
		BeforeAndAfterTransactionAnnotationTests.numBeforeTransactionCalls++;
		clearPersonTable(jdbcTemplate);
		assertThat(addPerson(jdbcTemplate, YODA)).as("Adding yoda").isEqualTo((long) 1);
	}

	@AfterTransaction
	void afterTransaction() {
		assertInTransaction(false);
		this.inTransaction = false;
		BeforeAndAfterTransactionAnnotationTests.numAfterTransactionCalls++;
		assertThat(deletePerson(jdbcTemplate, YODA)).as("Deleting yoda").isEqualTo((long) 1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table after a transactional test method.").isEqualTo((long) 0);
	}

	@Before
	public void before() {
		assertShouldBeInTransaction();
		long expected = (this.inTransaction ? 1
				: 0);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table before a test method.").isEqualTo(expected);
	}

	private void assertShouldBeInTransaction() {
		boolean shouldBeInTransaction = !testName.getMethodName().equals("nonTransactionalMethod");
		assertInTransaction(shouldBeInTransaction);
	}

	@After
	public void after() {
		assertShouldBeInTransaction();
	}

	@Test
	public void transactionalMethod1() {
		assertInTransaction(true);
		assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo((long) 1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within transactionalMethod1().").isEqualTo((long) 2);
	}

	@Test
	public void transactionalMethod2() {
		assertInTransaction(true);
		assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo((long) 1);
		assertThat(addPerson(jdbcTemplate, SUE)).as("Adding sue").isEqualTo((long) 1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within transactionalMethod2().").isEqualTo((long) 3);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void nonTransactionalMethod() {
		assertInTransaction(false);
		assertThat(addPerson(jdbcTemplate, LUKE)).as("Adding luke").isEqualTo((long) 1);
		assertThat(addPerson(jdbcTemplate, LEIA)).as("Adding leia").isEqualTo((long) 1);
		assertThat(addPerson(jdbcTemplate, YODA)).as("Adding yoda").isEqualTo((long) 1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table without a transaction.").isEqualTo((long) 3);
	}

}
