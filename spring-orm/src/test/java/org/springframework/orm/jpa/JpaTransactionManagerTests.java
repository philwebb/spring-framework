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

package org.springframework.orm.jpa;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class JpaTransactionManagerTests {

	private EntityManagerFactory factory;

	private EntityManager manager;

	private EntityTransaction tx;

	private JpaTransactionManager tm;

	private TransactionTemplate tt;


	@Before
	public void setup() {
		this.factory = mock(EntityManagerFactory.class);
		this.manager = mock(EntityManager.class);
		this.tx = mock(EntityTransaction.class);

		this.tm = new JpaTransactionManager(this.factory);
		this.tt = new TransactionTemplate(this.tm);

		given(this.factory.createEntityManager()).willReturn(this.manager);
		given(this.manager.getTransaction()).willReturn(this.tx);
		given(this.manager.isOpen()).willReturn(true);
	}

	@After
	public void verifyTransactionSynchronizationManagerState() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}


	@Test
	public void testTransactionCommit() {
		given(this.manager.getTransaction()).willReturn(this.tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		Object result = this.tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
				EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
				return l;
			}
		});
		assertSame(l, result);

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.tx).commit();
		verify(this.manager).flush();
		verify(this.manager).close();
	}

	@Test
	public void testTransactionCommitWithRollbackException() {
		given(this.manager.getTransaction()).willReturn(this.tx);
		given(this.tx.getRollbackOnly()).willReturn(true);
		willThrow(new RollbackException()).given(this.tx).commit();

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		try {
			Object result = this.tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
					EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
					return l;
				}
			});
			assertSame(l, result);
		}
		catch (TransactionSystemException tse) {
			// expected
			assertTrue(tse.getCause() instanceof RollbackException);
		}

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.manager).flush();
		verify(this.manager).close();
	}

	@Test
	public void testTransactionRollback() {
		given(this.manager.getTransaction()).willReturn(this.tx);
		given(this.tx.isActive()).willReturn(true);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		try {
			this.tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
					EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory);
					throw new RuntimeException("some exception");
				}
			});
			fail("Should have propagated RuntimeException");
		}
		catch (RuntimeException ex) {
			// expected
			assertEquals("some exception", ex.getMessage());
		}

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.tx).rollback();
		verify(this.manager).close();
	}

	@Test
	public void testTransactionRollbackWithAlreadyRolledBack() {
		given(this.manager.getTransaction()).willReturn(this.tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		try {
			this.tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
					EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory);
					throw new RuntimeException("some exception");
				}
			});
			fail("Should have propagated RuntimeException");
		}
		catch (RuntimeException ex) {
			// expected
		}

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.manager).close();
	}

	@Test
	public void testTransactionRollbackOnly() {
		given(this.manager.getTransaction()).willReturn(this.tx);
		given(this.tx.isActive()).willReturn(true);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		this.tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));

				EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
				status.setRollbackOnly();

				return l;
			}
		});

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.manager).flush();
		verify(this.tx).rollback();
		verify(this.manager).close();
	}

	@Test
	public void testParticipatingTransactionWithCommit() {
		given(this.manager.getTransaction()).willReturn(this.tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		this.tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));

				return JpaTransactionManagerTests.this.tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
						return l;
					}
				});
			}
		});

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.manager).flush();
		verify(this.tx).commit();
		verify(this.manager).close();
	}

	@Test
	public void testParticipatingTransactionWithRollback() {
		given(this.manager.getTransaction()).willReturn(this.tx);
		given(this.tx.isActive()).willReturn(true);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		try {
			this.tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
					return JpaTransactionManagerTests.this.tt.execute(new TransactionCallback() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory);
							throw new RuntimeException("some exception");
						}
					});
				}
			});
			fail("Should have propagated RuntimeException");
		}
		catch (RuntimeException ex) {
			// expected
		}

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.tx).setRollbackOnly();
		verify(this.tx).rollback();
		verify(this.manager).close();
	}

	@Test
	public void testParticipatingTransactionWithRollbackOnly() {
		given(this.manager.getTransaction()).willReturn(this.tx);
		given(this.tx.isActive()).willReturn(true);
		given(this.tx.getRollbackOnly()).willReturn(true);
		willThrow(new RollbackException()).given(this.tx).commit();

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		try {
			this.tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));

					return JpaTransactionManagerTests.this.tt.execute(new TransactionCallback() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
							status.setRollbackOnly();
							return null;
						}
					});
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException tse) {
			// expected
			assertTrue(tse.getCause() instanceof RollbackException);
		}

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.manager).flush();
		verify(this.tx).setRollbackOnly();
		verify(this.manager).close();
	}

	@Test
	public void testParticipatingTransactionWithRequiresNew() {
		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		given(this.factory.createEntityManager()).willReturn(this.manager);
		given(this.manager.getTransaction()).willReturn(this.tx);
		given(this.manager.isOpen()).willReturn(true);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		Object result = this.tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
				return JpaTransactionManagerTests.this.tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
						return l;
					}
				});
			}
		});
		assertSame(l, result);

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.manager).flush();
		verify(this.manager, times(2)).close();
		verify(this.tx, times(2)).begin();
	}

	@Test
	public void testParticipatingTransactionWithRequiresNewAndPrebound() {
		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		given(this.manager.getTransaction()).willReturn(this.tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		TransactionSynchronizationManager.bindResource(this.factory, new EntityManagerHolder(this.manager));

		try {
			Object result = this.tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory);

					assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
					return JpaTransactionManagerTests.this.tt.execute(new TransactionCallback() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
							return l;
						}
					});
				}
			});
			assertSame(l, result);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(this.factory);
		}

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.tx, times(2)).begin();
		verify(this.tx, times(2)).commit();
		verify(this.manager).flush();
		verify(this.manager).close();
	}

	@Test
	public void testPropagationSupportsAndRequiresNew() {
		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		given(this.manager.getTransaction()).willReturn(this.tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		Object result = this.tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertFalse(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
				TransactionTemplate tt2 = new TransactionTemplate(JpaTransactionManagerTests.this.tm);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				return tt2.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
						return l;
					}
				});
			}
		});
		assertSame(l, result);

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.tx).commit();
		verify(this.manager).flush();
		verify(this.manager).close();
	}

	@Test
	public void testPropagationSupportsAndRequiresNewAndEarlyAccess() {
		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		given(this.factory.createEntityManager()).willReturn(this.manager);
		given(this.manager.getTransaction()).willReturn(this.tx);
		given(this.manager.isOpen()).willReturn(true);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		Object result = this.tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory);

				assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
				TransactionTemplate tt2 = new TransactionTemplate(JpaTransactionManagerTests.this.tm);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				return tt2.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
						return l;
					}
				});
			}
		});
		assertSame(l, result);

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.tx).commit();
		verify(this.manager).flush();
		verify(this.manager, times(2)).close();
	}

	@Test
	public void testTransactionWithRequiresNewInAfterCompletion() {
		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		EntityManager manager2 = mock(EntityManager.class);
		EntityTransaction tx2 = mock(EntityTransaction.class);

		given(this.manager.getTransaction()).willReturn(this.tx);
		given(this.factory.createEntityManager()).willReturn(this.manager, manager2);
		given(manager2.getTransaction()).willReturn(tx2);
		given(manager2.isOpen()).willReturn(true);

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		this.tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
					@Override
					public void afterCompletion(int status) {
						JpaTransactionManagerTests.this.tt.execute(new TransactionCallback() {
							@Override
							public Object doInTransaction(TransactionStatus status) {
								EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
								return null;
							}
						});
					}
				});
				return null;
			}
		});

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.tx).commit();
		verify(tx2).begin();
		verify(tx2).commit();
		verify(this.manager).flush();
		verify(this.manager).close();
		verify(manager2).flush();
		verify(manager2).close();
	}

	@Test
	public void testTransactionCommitWithPropagationSupports() {
		given(this.manager.isOpen()).willReturn(true);

		final List<String> l = new ArrayList<>();
		l.add("test");

		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		Object result = this.tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(!TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue(!status.isNewTransaction());
				EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
				return l;
			}
		});
		assertSame(l, result);

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.manager).flush();
		verify(this.manager).close();
	}

	@Test
	public void testTransactionRollbackWithPropagationSupports() {
		given(this.manager.isOpen()).willReturn(true);

		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		this.tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(!TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue(!status.isNewTransaction());
				EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
				status.setRollbackOnly();
				return null;
			}
		});

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.manager).flush();
		verify(this.manager).close();
	}

	@Test
	public void testTransactionCommitWithPrebound() {
		given(this.manager.getTransaction()).willReturn(this.tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(this.factory, new EntityManagerHolder(this.manager));

		try {
			Object result = this.tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory);
					return l;
				}
			});
			assertSame(l, result);

			assertTrue(TransactionSynchronizationManager.hasResource(this.factory));
			assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(this.factory);
		}

		verify(this.tx).begin();
		verify(this.tx).commit();
	}

	@Test
	public void testTransactionRollbackWithPrebound() {
		given(this.manager.getTransaction()).willReturn(this.tx);
		given(this.tx.isActive()).willReturn(true);

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(this.factory, new EntityManagerHolder(this.manager));

		try {
			this.tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory);
					status.setRollbackOnly();
					return null;
				}
			});

			assertTrue(TransactionSynchronizationManager.hasResource(this.factory));
			assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(this.factory);
		}

		verify(this.tx).begin();
		verify(this.tx).rollback();
		verify(this.manager).clear();
	}

	@Test
	public void testTransactionCommitWithPreboundAndPropagationSupports() {
		final List<String> l = new ArrayList<>();
		l.add("test");

		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(this.factory, new EntityManagerHolder(this.manager));

		try {
			Object result = this.tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue(!status.isNewTransaction());
					EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
					return l;
				}
			});
			assertSame(l, result);

			assertTrue(TransactionSynchronizationManager.hasResource(this.factory));
			assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(this.factory);
		}

		verify(this.manager).flush();
	}

	@Test
	public void testTransactionRollbackWithPreboundAndPropagationSupports() {
		this.tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(this.factory, new EntityManagerHolder(this.manager));

		try {
			this.tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue(!status.isNewTransaction());
					EntityManagerFactoryUtils.getTransactionalEntityManager(JpaTransactionManagerTests.this.factory).flush();
					status.setRollbackOnly();
					return null;
				}
			});

			assertTrue(TransactionSynchronizationManager.hasResource(this.factory));
			assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(this.factory);
		}

		verify(this.manager).flush();
		verify(this.manager).clear();
	}

	@Test
	public void testInvalidIsolation() {
		this.tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

		given(this.manager.isOpen()).willReturn(true);

		try {
			this.tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
				}
			});
			fail("Should have thrown InvalidIsolationLevelException");
		}
		catch (InvalidIsolationLevelException ex) {
			// expected
		}

		verify(this.manager).close();
	}

	@Test
	public void testTransactionFlush() {
		given(this.manager.getTransaction()).willReturn(this.tx);

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		this.tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.hasResource(JpaTransactionManagerTests.this.factory));
				status.flush();
			}
		});

		assertTrue(!TransactionSynchronizationManager.hasResource(this.factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		verify(this.tx).commit();
		verify(this.manager).flush();
		verify(this.manager).close();
	}

}
