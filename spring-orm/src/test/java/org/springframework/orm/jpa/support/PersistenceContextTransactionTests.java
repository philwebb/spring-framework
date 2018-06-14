/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.orm.jpa.support;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 4.1.2
 */
public class PersistenceContextTransactionTests {

	private EntityManagerFactory factory;

	private EntityManager manager;

	private EntityTransaction tx;

	private TransactionTemplate tt;

	private EntityManagerHoldingBean bean;


	@Before
	public void setUp() throws Exception {
		this.factory = mock(EntityManagerFactory.class);
		this.manager = mock(EntityManager.class);
		this.tx = mock(EntityTransaction.class);

		JpaTransactionManager tm = new JpaTransactionManager(this.factory);
		this.tt = new TransactionTemplate(tm);

		given(this.factory.createEntityManager()).willReturn(this.manager);
		given(this.manager.getTransaction()).willReturn(this.tx);
		given(this.manager.isOpen()).willReturn(true);

		this.bean = new EntityManagerHoldingBean();
		@SuppressWarnings("serial")
		PersistenceAnnotationBeanPostProcessor pabpp = new PersistenceAnnotationBeanPostProcessor() {
			@Override
			protected EntityManagerFactory findEntityManagerFactory(@Nullable String unitName, String requestingBeanName) {
				return PersistenceContextTransactionTests.this.factory;
			}
		};
		pabpp.postProcessPropertyValues(null, null, this.bean, "bean");

		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

	@After
	public void tearDown() throws Exception {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}


	@Test
	public void testTransactionCommitWithSharedEntityManager() {
		given(this.manager.getTransaction()).willReturn(this.tx);

		this.tt.execute(status -> {
			this.bean.sharedEntityManager.flush();
			return null;
		});

		verify(this.tx).commit();
		verify(this.manager).flush();
		verify(this.manager).close();
	}

	@Test
	public void testTransactionCommitWithSharedEntityManagerAndPropagationSupports() {
		given(this.manager.isOpen()).willReturn(true);

		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		this.tt.execute(status -> {
			this.bean.sharedEntityManager.clear();
			return null;
		});

		verify(this.manager).clear();
		verify(this.manager).close();
	}

	@Test
	public void testTransactionCommitWithExtendedEntityManager() {
		given(this.manager.getTransaction()).willReturn(this.tx);

		this.tt.execute(status -> {
			this.bean.extendedEntityManager.flush();
			return null;
		});

		verify(this.tx, times(2)).commit();
		verify(this.manager).flush();
		verify(this.manager).close();
	}

	@Test
	public void testTransactionCommitWithExtendedEntityManagerAndPropagationSupports() {
		given(this.manager.isOpen()).willReturn(true);

		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		this.tt.execute(status -> {
			this.bean.extendedEntityManager.flush();
			return null;
		});

		verify(this.manager).flush();
	}

	@Test
	public void testTransactionCommitWithSharedEntityManagerUnsynchronized() {
		given(this.manager.getTransaction()).willReturn(this.tx);

		this.tt.execute(status -> {
			this.bean.sharedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(this.tx).commit();
		verify(this.manager).flush();
		verify(this.manager, times(2)).close();
	}

	@Test
	public void testTransactionCommitWithSharedEntityManagerUnsynchronizedAndPropagationSupports() {
		given(this.manager.isOpen()).willReturn(true);

		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		this.tt.execute(status -> {
			this.bean.sharedEntityManagerUnsynchronized.clear();
			return null;
		});

		verify(this.manager).clear();
		verify(this.manager).close();
	}

	@Test
	public void testTransactionCommitWithExtendedEntityManagerUnsynchronized() {
		given(this.manager.getTransaction()).willReturn(this.tx);

		this.tt.execute(status -> {
			this.bean.extendedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(this.tx).commit();
		verify(this.manager).flush();
		verify(this.manager).close();
	}

	@Test
	public void testTransactionCommitWithExtendedEntityManagerUnsynchronizedAndPropagationSupports() {
		given(this.manager.isOpen()).willReturn(true);

		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		this.tt.execute(status -> {
			this.bean.extendedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(this.manager).flush();
	}

	@Test
	public void testTransactionCommitWithSharedEntityManagerUnsynchronizedJoined() {
		given(this.manager.getTransaction()).willReturn(this.tx);

		this.tt.execute(status -> {
			this.bean.sharedEntityManagerUnsynchronized.joinTransaction();
			this.bean.sharedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(this.tx).commit();
		verify(this.manager).flush();
		verify(this.manager, times(2)).close();
	}

	@Test
	public void testTransactionCommitWithExtendedEntityManagerUnsynchronizedJoined() {
		given(this.manager.getTransaction()).willReturn(this.tx);

		this.tt.execute(status -> {
			this.bean.extendedEntityManagerUnsynchronized.joinTransaction();
			this.bean.extendedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(this.tx, times(2)).commit();
		verify(this.manager).flush();
		verify(this.manager).close();
	}

	@Test
	public void testTransactionCommitWithExtendedEntityManagerUnsynchronizedJoinedAndPropagationSupports() {
		given(this.manager.isOpen()).willReturn(true);

		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		this.tt.execute(status -> {
			this.bean.extendedEntityManagerUnsynchronized.joinTransaction();
			this.bean.extendedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(this.manager).flush();
	}


	public static class EntityManagerHoldingBean {

		@PersistenceContext
		public EntityManager sharedEntityManager;

		@PersistenceContext(type = PersistenceContextType.EXTENDED)
		public EntityManager extendedEntityManager;

		@PersistenceContext(synchronization = SynchronizationType.UNSYNCHRONIZED)
		public EntityManager sharedEntityManagerUnsynchronized;

		@PersistenceContext(type = PersistenceContextType.EXTENDED, synchronization = SynchronizationType.UNSYNCHRONIZED)
		public EntityManager extendedEntityManagerUnsynchronized;
	}

}
