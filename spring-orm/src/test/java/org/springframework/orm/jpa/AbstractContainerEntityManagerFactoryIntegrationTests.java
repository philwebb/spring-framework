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

package org.springframework.orm.jpa;

import java.lang.reflect.Proxy;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.junit.Test;

import org.springframework.orm.jpa.domain.DriversLicense;
import org.springframework.orm.jpa.domain.Person;
import org.springframework.util.SerializationTestUtils;

import static org.assertj.core.api.Assertions.*;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNotSame;

/**
 * Integration tests for LocalContainerEntityManagerFactoryBean.
 * Uses an in-memory database.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class AbstractContainerEntityManagerFactoryIntegrationTests
		extends AbstractEntityManagerFactoryIntegrationTests {

	@Test
	public void testEntityManagerFactoryImplementsEntityManagerFactoryInfo() {
		boolean condition = entityManagerFactory instanceof EntityManagerFactoryInfo;
		assertThat(condition).as("Must have introduced config interface").isTrue();
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		assertThat((Object) emfi.getPersistenceUnitName()).isEqualTo("Person");
		assertNotNull("PersistenceUnitInfo must be available", emfi.getPersistenceUnitInfo());
		assertNotNull("Raw EntityManagerFactory must be available", emfi.getNativeEntityManagerFactory());
	}

	@Test
	public void testStateClean() {
		assertEquals("Should be no people from previous transactions", 0, countRowsInTable("person"));
	}

	@Test
	public void testJdbcTx1_1() {
		testJdbcTx2();
	}

	@Test
	public void testJdbcTx1_2() {
		testJdbcTx2();
	}

	@Test
	public void testJdbcTx1_3() {
		testJdbcTx2();
	}

	@Test
	public void testJdbcTx2() {
		assertEquals("Any previous tx must have been rolled back", 0, countRowsInTable("person"));
		executeSqlScript("/org/springframework/orm/jpa/insertPerson.sql");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testEntityManagerProxyIsProxy() {
		assertThat(Proxy.isProxyClass(sharedEntityManager.getClass())).isTrue();
		Query q = sharedEntityManager.createQuery("select p from Person as p");
		q.getResultList();

		assertThat(sharedEntityManager.isOpen()).as("Should be open to start with").isTrue();
		sharedEntityManager.close();
		assertThat(sharedEntityManager.isOpen()).as("Close should have been silently ignored").isTrue();
	}

	@Test
	public void testBogusQuery() {
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
			Query query = sharedEntityManager.createQuery("It's raining toads");
			// required in OpenJPA case
			query.executeUpdate();
		});
	}

	@Test
	public void testGetReferenceWhenNoRow() {
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> {
				Person notThere = sharedEntityManager.getReference(Person.class, 666);
				// We may get here (as with Hibernate). Either behaviour is valid:
				// throw exception on first access or on getReference itself.
				notThere.getFirstName();
			})
		.matches(ex -> ex.getClass().getName().endsWith("NotFoundException"));
	}

	@Test
	public void testLazyLoading() {
		try {
			Person tony = new Person();
			tony.setFirstName("Tony");
			tony.setLastName("Blair");
			tony.setDriversLicense(new DriversLicense("8439DK"));
			sharedEntityManager.persist(tony);
			setComplete();
			endTransaction();

			startNewTransaction();
			sharedEntityManager.clear();
			Person newTony = entityManagerFactory.createEntityManager().getReference(Person.class, tony.getId());
			assertNotSame(newTony, tony);
			endTransaction();

			assertNotNull(newTony.getDriversLicense());

			newTony.getDriversLicense().getSerialNumber();
		}
		finally {
			deleteFromTables("person", "drivers_license");
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMultipleResults() {
		// Add with JDBC
		String firstName = "Tony";
		insertPerson(firstName);

		assertThat(Proxy.isProxyClass(sharedEntityManager.getClass())).isTrue();
		Query q = sharedEntityManager.createQuery("select p from Person as p");
		List<Person> people = q.getResultList();

		assertEquals(1, people.size());
		assertThat((Object) people.get(0).getFirstName()).isEqualTo(firstName);
	}

	protected void insertPerson(String firstName) {
		String INSERT_PERSON = "INSERT INTO PERSON (ID, FIRST_NAME, LAST_NAME) VALUES (?, ?, ?)";
		jdbcTemplate.update(INSERT_PERSON, 1, firstName, "Blair");
	}

	@Test
	public void testEntityManagerProxyRejectsProgrammaticTxManagement() {
		assertThatIllegalStateException().as("Should not be able to create transactions on container managed EntityManager").isThrownBy(
				sharedEntityManager::getTransaction);
	}

	@Test
	public void testInstantiateAndSaveWithSharedEmProxy() {
		testInstantiateAndSave(sharedEntityManager);
	}

	protected void testInstantiateAndSave(EntityManager em) {
		assertEquals("Should be no people from previous transactions", 0, countRowsInTable("person"));
		Person p = new Person();
		p.setFirstName("Tony");
		p.setLastName("Blair");
		em.persist(p);

		em.flush();
		assertEquals("1 row must have been inserted", 1, countRowsInTable("person"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testQueryNoPersons() {
		EntityManager em = entityManagerFactory.createEntityManager();
		Query q = em.createQuery("select p from Person as p");
		List<Person> people = q.getResultList();
		assertEquals(0, people.size());
		assertThatExceptionOfType(NoResultException.class).isThrownBy(
				q::getSingleResult);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testQueryNoPersonsNotTransactional() {
		endTransaction();

		EntityManager em = entityManagerFactory.createEntityManager();
		Query q = em.createQuery("select p from Person as p");
		List<Person> people = q.getResultList();
		assertEquals(0, people.size());
		assertThatExceptionOfType(NoResultException.class).isThrownBy(
				q::getSingleResult);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testQueryNoPersonsShared() {
		Query q = this.sharedEntityManager.createQuery("select p from Person as p");
		q.setFlushMode(FlushModeType.AUTO);
		List<Person> people = q.getResultList();
		assertEquals(0, people.size());
		assertThatExceptionOfType(NoResultException.class).isThrownBy(
				q::getSingleResult);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testQueryNoPersonsSharedNotTransactional() {
		endTransaction();

		EntityManager em = this.sharedEntityManager;
		Query q = em.createQuery("select p from Person as p");
		q.setFlushMode(FlushModeType.AUTO);
		List<Person> people = q.getResultList();
		assertEquals(0, people.size());
		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				q.getSingleResult())
			.withMessageContaining("closed");
		// We would typically expect an IllegalStateException, but Hibernate throws a
		// PersistenceException. So we assert the contents of the exception message instead.

		Query q2 = em.createQuery("select p from Person as p");
		q2.setFlushMode(FlushModeType.AUTO);
		assertThatExceptionOfType(NoResultException.class).isThrownBy(
				q2::getSingleResult);
	}

	@Test
	public void testCanSerializeProxies() throws Exception {
		assertNotNull(SerializationTestUtils.serializeAndDeserialize(entityManagerFactory));
		assertNotNull(SerializationTestUtils.serializeAndDeserialize(sharedEntityManager));
	}

}
