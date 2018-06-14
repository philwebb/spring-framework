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

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

/**
 * @author Juergen Hoeller
 * @since 04.07.2003
 */
public class DataSourceTransactionManagerTests  {

	private DataSource ds;

	private Connection con;

	private DataSourceTransactionManager tm;


	@Before
	public void setup() throws Exception {
		this.ds = mock(DataSource.class);
		this.con = mock(Connection.class);
		given(this.ds.getConnection()).willReturn(this.con);
		this.tm = new DataSourceTransactionManager(this.ds);
	}

	@After
	public void verifyTransactionSynchronizationManagerState() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}


	@Test
	public void testTransactionCommitWithAutoCommitTrue() throws Exception {
		doTestTransactionCommitRestoringAutoCommit(true, false, false);
	}

	@Test
	public void testTransactionCommitWithAutoCommitFalse() throws Exception {
		doTestTransactionCommitRestoringAutoCommit(false, false, false);
	}

	@Test
	public void testTransactionCommitWithAutoCommitTrueAndLazyConnection() throws Exception {
		doTestTransactionCommitRestoringAutoCommit(true, true, false);
	}

	@Test
	public void testTransactionCommitWithAutoCommitFalseAndLazyConnection() throws Exception {
		doTestTransactionCommitRestoringAutoCommit(false, true, false);
	}

	@Test
	public void testTransactionCommitWithAutoCommitTrueAndLazyConnectionAndStatementCreated() throws Exception {
		doTestTransactionCommitRestoringAutoCommit(true, true, true);
	}

	@Test
	public void testTransactionCommitWithAutoCommitFalseAndLazyConnectionAndStatementCreated() throws Exception {
		doTestTransactionCommitRestoringAutoCommit(false, true, true);
	}

	private void doTestTransactionCommitRestoringAutoCommit(
			boolean autoCommit, boolean lazyConnection, final boolean createStatement) throws Exception {

		if (lazyConnection) {
			given(this.con.getAutoCommit()).willReturn(autoCommit);
			given(this.con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
		}

		if (!lazyConnection || createStatement) {
			given(this.con.getAutoCommit()).willReturn(autoCommit);
		}

		final DataSource dsToUse = (lazyConnection ? new LazyConnectionDataSourceProxy(this.ds) : this.ds);
		this.tm = new DataSourceTransactionManager(dsToUse);
		TransactionTemplate tt = new TransactionTemplate(this.tm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
				assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue("Is new transaction", status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				Connection tCon = DataSourceUtils.getConnection(dsToUse);
				try {
					if (createStatement) {
						tCon.createStatement();
					}
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		if (autoCommit && (!lazyConnection || createStatement)) {
			InOrder ordered = inOrder(this.con);
			ordered.verify(this.con).setAutoCommit(false);
			ordered.verify(this.con).commit();
			ordered.verify(this.con).setAutoCommit(true);
		}
		if (createStatement) {
			verify(this.con, times(2)).close();
		}
		else {
			verify(this.con).close();
		}
	}

	@Test
	public void testTransactionRollbackWithAutoCommitTrue() throws Exception  {
		doTestTransactionRollbackRestoringAutoCommit(true, false, false);
	}

	@Test
	public void testTransactionRollbackWithAutoCommitFalse() throws Exception  {
		doTestTransactionRollbackRestoringAutoCommit(false, false, false);
	}

	@Test
	public void testTransactionRollbackWithAutoCommitTrueAndLazyConnection() throws Exception  {
		doTestTransactionRollbackRestoringAutoCommit(true, true, false);
	}

	@Test
	public void testTransactionRollbackWithAutoCommitFalseAndLazyConnection() throws Exception  {
		doTestTransactionRollbackRestoringAutoCommit(false, true, false);
	}

	@Test
	public void testTransactionRollbackWithAutoCommitTrueAndLazyConnectionAndCreateStatement() throws Exception  {
		doTestTransactionRollbackRestoringAutoCommit(true, true, true);
	}

	@Test
	public void testTransactionRollbackWithAutoCommitFalseAndLazyConnectionAndCreateStatement() throws Exception  {
		doTestTransactionRollbackRestoringAutoCommit(false, true, true);
	}

	private void doTestTransactionRollbackRestoringAutoCommit(
			boolean autoCommit, boolean lazyConnection, final boolean createStatement) throws Exception {

		if (lazyConnection) {
			given(this.con.getAutoCommit()).willReturn(autoCommit);
			given(this.con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
		}

		if (!lazyConnection || createStatement) {
			given(this.con.getAutoCommit()).willReturn(autoCommit);
		}

		final DataSource dsToUse = (lazyConnection ? new LazyConnectionDataSourceProxy(this.ds) : this.ds);
		 this.tm = new DataSourceTransactionManager(dsToUse);
		TransactionTemplate tt = new TransactionTemplate(this.tm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		final RuntimeException ex = new RuntimeException("Application exception");
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
					assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Is new transaction", status.isNewTransaction());
					Connection con = DataSourceUtils.getConnection(dsToUse);
					if (createStatement) {
						try {
							con.createStatement();
						}
						catch (SQLException ex) {
							throw new UncategorizedSQLException("", "", ex);
						}
					}
					throw ex;
				}
			});
			fail("Should have thrown RuntimeException");
		}
		catch (RuntimeException ex2) {
			// expected
			assertTrue("Correct exception thrown", ex2.equals(ex));
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		if (autoCommit && (!lazyConnection || createStatement)) {
			InOrder ordered = inOrder(this.con);
			ordered.verify(this.con).setAutoCommit(false);
			ordered.verify(this.con).rollback();
			ordered.verify(this.con).setAutoCommit(true);
		}
		if (createStatement) {
			verify(this.con, times(2)).close();
		}
		else {
			verify(this.con).close();
		}
	}

	@Test
	public void testTransactionRollbackOnly() throws Exception {
		this.tm.setTransactionSynchronization(DataSourceTransactionManager.SYNCHRONIZATION_NEVER);
		TransactionTemplate tt = new TransactionTemplate(this.tm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		ConnectionHolder conHolder = new ConnectionHolder(this.con);
		conHolder.setTransactionActive(true);
		TransactionSynchronizationManager.bindResource(this.ds, conHolder);
		final RuntimeException ex = new RuntimeException("Application exception");
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
					assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Is existing transaction", !status.isNewTransaction());
					throw ex;
				}
			});
			fail("Should have thrown RuntimeException");
		}
		catch (RuntimeException ex2) {
			// expected
			assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());
			assertEquals("Correct exception thrown", ex, ex2);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(this.ds);
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
	}

	@Test
	public void testParticipatingTransactionWithRollbackOnly() throws Exception {
		doTestParticipatingTransactionWithRollbackOnly(false);
	}

	@Test
	public void testParticipatingTransactionWithRollbackOnlyAndFailEarly() throws Exception {
		doTestParticipatingTransactionWithRollbackOnly(true);
	}

	private void doTestParticipatingTransactionWithRollbackOnly(boolean failEarly) throws Exception {
		given(this.con.isReadOnly()).willReturn(false);
		if (failEarly) {
			this.tm.setFailEarlyOnGlobalRollbackOnly(true);
		}
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		TransactionStatus ts = this.tm.getTransaction(new DefaultTransactionDefinition());
		TestTransactionSynchronization synch =
				new TestTransactionSynchronization(this.ds, TransactionSynchronization.STATUS_ROLLED_BACK);
		TransactionSynchronizationManager.registerSynchronization(synch);

		boolean outerTransactionBoundaryReached = false;
		try {
			assertTrue("Is new transaction", ts.isNewTransaction());

			final TransactionTemplate tt = new TransactionTemplate(this.tm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Is existing transaction", !status.isNewTransaction());
					assertFalse("Is not rollback-only", status.isRollbackOnly());
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
							assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
							assertTrue("Is existing transaction", !status.isNewTransaction());
							status.setRollbackOnly();
						}
					});
					assertTrue("Is existing transaction", !status.isNewTransaction());
					assertTrue("Is rollback-only", status.isRollbackOnly());
				}
			});

			outerTransactionBoundaryReached = true;
			this.tm.commit(ts);

			fail("Should have thrown UnexpectedRollbackException");
		}
		catch (UnexpectedRollbackException ex) {
			// expected
			if (!outerTransactionBoundaryReached) {
				this.tm.rollback(ts);
			}
			if (failEarly) {
				assertFalse(outerTransactionBoundaryReached);
			}
			else {
				assertTrue(outerTransactionBoundaryReached);
			}
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertFalse(synch.beforeCommitCalled);
		assertTrue(synch.beforeCompletionCalled);
		assertFalse(synch.afterCommitCalled);
		assertTrue(synch.afterCompletionCalled);
		verify(this.con).rollback();
		verify(this.con).close();
	}

	@Test
	public void testParticipatingTransactionWithIncompatibleIsolationLevel() throws Exception {
		this.tm.setValidateExistingTransaction(true);

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			final TransactionTemplate tt = new TransactionTemplate(this.tm);
			final TransactionTemplate tt2 = new TransactionTemplate(this.tm);
			tt2.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertFalse("Is not rollback-only", status.isRollbackOnly());
					tt2.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							status.setRollbackOnly();
						}
					});
					assertTrue("Is rollback-only", status.isRollbackOnly());
				}
			});

			fail("Should have thrown IllegalTransactionStateException");
		}
		catch (IllegalTransactionStateException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).rollback();
		verify(this.con).close();
	}

	@Test
	public void testParticipatingTransactionWithIncompatibleReadOnly() throws Exception {
		willThrow(new SQLException("read-only not supported")).given(this.con).setReadOnly(true);
		this.tm.setValidateExistingTransaction(true);

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			final TransactionTemplate tt = new TransactionTemplate(this.tm);
			tt.setReadOnly(true);
			final TransactionTemplate tt2 = new TransactionTemplate(this.tm);
			tt2.setReadOnly(false);

			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertFalse("Is not rollback-only", status.isRollbackOnly());
					tt2.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							status.setRollbackOnly();
						}
					});
					assertTrue("Is rollback-only", status.isRollbackOnly());
				}
			});

			fail("Should have thrown IllegalTransactionStateException");
		}
		catch (IllegalTransactionStateException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).rollback();
		verify(this.con).close();
	}

	@Test
	public void testParticipatingTransactionWithTransactionStartedFromSynch() throws Exception {
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		final TestTransactionSynchronization synch =
				new TestTransactionSynchronization(this.ds, TransactionSynchronization.STATUS_COMMITTED) {
					@Override
					protected void doAfterCompletion(int status) {
						super.doAfterCompletion(status);
						tt.execute(new TransactionCallbackWithoutResult() {
							@Override
							protected void doInTransactionWithoutResult(TransactionStatus status) {
							}
						});
						TransactionSynchronizationManager.registerSynchronization(
								new TransactionSynchronizationAdapter() {
								});
					}
				};

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				TransactionSynchronizationManager.registerSynchronization(synch);
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue(synch.beforeCommitCalled);
		assertTrue(synch.beforeCompletionCalled);
		assertTrue(synch.afterCommitCalled);
		assertTrue(synch.afterCompletionCalled);
		assertTrue(synch.afterCompletionException instanceof IllegalStateException);
		verify(this.con, times(2)).commit();
		verify(this.con, times(2)).close();
	}

	@Test
	public void testParticipatingTransactionWithDifferentConnectionObtainedFromSynch() throws Exception {
		DataSource ds2 = mock(DataSource.class);
		final Connection con2 = mock(Connection.class);
		given(ds2.getConnection()).willReturn(con2);

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		final TransactionTemplate tt = new TransactionTemplate(this.tm);

		final TestTransactionSynchronization synch =
				new TestTransactionSynchronization(this.ds, TransactionSynchronization.STATUS_COMMITTED) {
					@Override
					protected void doAfterCompletion(int status) {
						super.doAfterCompletion(status);
						Connection con = DataSourceUtils.getConnection(ds2);
						DataSourceUtils.releaseConnection(con, ds2);
					}
				};

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				TransactionSynchronizationManager.registerSynchronization(synch);
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue(synch.beforeCommitCalled);
		assertTrue(synch.beforeCompletionCalled);
		assertTrue(synch.afterCommitCalled);
		assertTrue(synch.afterCompletionCalled);
		assertNull(synch.afterCompletionException);
		verify(this.con).commit();
		verify(this.con).close();
		verify(con2).close();
	}

	@Test
	public void testParticipatingTransactionWithRollbackOnlyAndInnerSynch() throws Exception {
		this.tm.setTransactionSynchronization(DataSourceTransactionManager.SYNCHRONIZATION_NEVER);
		DataSourceTransactionManager tm2 = new DataSourceTransactionManager(this.ds);
		// tm has no synch enabled (used at outer level), tm2 has synch enabled (inner level)

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		TransactionStatus ts = this.tm.getTransaction(new DefaultTransactionDefinition());
		final TestTransactionSynchronization synch =
				new TestTransactionSynchronization(this.ds, TransactionSynchronization.STATUS_UNKNOWN);

		try {
			assertTrue("Is new transaction", ts.isNewTransaction());

			final TransactionTemplate tt = new TransactionTemplate(tm2);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Is existing transaction", !status.isNewTransaction());
					assertFalse("Is not rollback-only", status.isRollbackOnly());
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
							assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
							assertTrue("Is existing transaction", !status.isNewTransaction());
							status.setRollbackOnly();
						}
					});
					assertTrue("Is existing transaction", !status.isNewTransaction());
					assertTrue("Is rollback-only", status.isRollbackOnly());
					TransactionSynchronizationManager.registerSynchronization(synch);
				}
			});

			this.tm.commit(ts);

			fail("Should have thrown UnexpectedRollbackException");
		}
		catch (UnexpectedRollbackException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertFalse(synch.beforeCommitCalled);
		assertTrue(synch.beforeCompletionCalled);
		assertFalse(synch.afterCommitCalled);
		assertTrue(synch.afterCompletionCalled);
		verify(this.con).rollback();
		verify(this.con).close();
	}

	@Test
	public void testPropagationRequiresNewWithExistingTransaction() throws Exception {
		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
						assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Is new transaction", status.isNewTransaction());
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						status.setRollbackOnly();
					}
				});
				assertTrue("Is new transaction", status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).rollback();
		verify(this.con).commit();
		verify(this.con, times(2)).close();
	}

	@Test
	public void testPropagationRequiresNewWithExistingTransactionAndUnrelatedDataSource() throws Exception {
		Connection con2 = mock(Connection.class);
		final DataSource ds2 = mock(DataSource.class);
		given(ds2.getConnection()).willReturn(con2);

		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		PlatformTransactionManager tm2 = new DataSourceTransactionManager(ds2);
		final TransactionTemplate tt2 = new TransactionTemplate(tm2);
		tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds2));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				tt2.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
						assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Is new transaction", status.isNewTransaction());
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						status.setRollbackOnly();
					}
				});
				assertTrue("Is new transaction", status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds2));
		verify(this.con).commit();
		verify(this.con).close();
		verify(con2).rollback();
		verify(con2).close();
	}

	@Test
	public void testPropagationRequiresNewWithExistingTransactionAndUnrelatedFailingDataSource() throws Exception {
		final DataSource ds2 = mock(DataSource.class);
		SQLException failure = new SQLException();
		given(ds2.getConnection()).willThrow(failure);


		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		DataSourceTransactionManager tm2 = new DataSourceTransactionManager(ds2);
		tm2.setTransactionSynchronization(DataSourceTransactionManager.SYNCHRONIZATION_NEVER);
		final TransactionTemplate tt2 = new TransactionTemplate(tm2);
		tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds2));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Is new transaction", status.isNewTransaction());
					assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
					assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
					assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
					tt2.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							status.setRollbackOnly();
						}
					});
				}
			});
			fail("Should have thrown CannotCreateTransactionException");
		}
		catch (CannotCreateTransactionException ex) {
			assertSame(failure, ex.getCause());
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds2));
		verify(this.con).rollback();
		verify(this.con).close();
	}

	@Test
	public void testPropagationNotSupportedWithExistingTransaction() throws Exception {
		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
						assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Isn't new transaction", !status.isNewTransaction());
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
						status.setRollbackOnly();
					}
				});
				assertTrue("Is new transaction", status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).commit();
		verify(this.con).close();
	}

	@Test
	public void testPropagationNeverWithExistingTransaction() throws Exception {
		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Is new transaction", status.isNewTransaction());
					tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							fail("Should have thrown IllegalTransactionStateException");
						}
					});
					fail("Should have thrown IllegalTransactionStateException");
				}
			});
		}
		catch (IllegalTransactionStateException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).rollback();
		verify(this.con).close();
	}

	@Test
	public void testPropagationSupportsAndRequiresNew() throws Exception {
		TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
				TransactionTemplate tt2 = new TransactionTemplate(DataSourceTransactionManagerTests.this.tm);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				tt2.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
						assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Is new transaction", status.isNewTransaction());
						assertSame(DataSourceTransactionManagerTests.this.con, DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds));
						assertSame(DataSourceTransactionManagerTests.this.con, DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds));
					}
				});
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).commit();
		verify(this.con).close();
	}

	@Test
	public void testPropagationSupportsAndRequiresNewWithEarlyAccess() throws Exception {
		final Connection con1 = mock(Connection.class);
		final Connection con2 = mock(Connection.class);
		given(this.ds.getConnection()).willReturn(con1, con2);

		final
		TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
				assertSame(con1, DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds));
				assertSame(con1, DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds));
				TransactionTemplate tt2 = new TransactionTemplate(DataSourceTransactionManagerTests.this.tm);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				tt2.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
						assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Is new transaction", status.isNewTransaction());
						assertSame(con2, DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds));
						assertSame(con2, DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds));
					}
				});
				assertSame(con1, DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds));
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(con1).close();
		verify(con2).commit();
		verify(con2).close();
	}

	@Test
	public void testTransactionWithIsolationAndReadOnly() throws Exception {
		given(this.con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
		given(this.con.getAutoCommit()).willReturn(true);

		TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		tt.setReadOnly(true);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				// something transactional
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		InOrder ordered = inOrder(this.con);
		ordered.verify(this.con).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		ordered.verify(this.con).setAutoCommit(false);
		ordered.verify(this.con).commit();
		ordered.verify(this.con).setAutoCommit(true);
		ordered.verify(this.con).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		verify(this.con).close();
	}

	@Test
	public void testTransactionWithEnforceReadOnly() throws Exception {
		this.tm.setEnforceReadOnly(true);

		given(this.con.getAutoCommit()).willReturn(true);
		Statement stmt = mock(Statement.class);
		given(this.con.createStatement()).willReturn(stmt);

		TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		tt.setReadOnly(true);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				// something transactional
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		InOrder ordered = inOrder(this.con, stmt);
		ordered.verify(this.con).setAutoCommit(false);
		ordered.verify(stmt).executeUpdate("SET TRANSACTION READ ONLY");
		ordered.verify(stmt).close();
		ordered.verify(this.con).commit();
		ordered.verify(this.con).setAutoCommit(true);
		ordered.verify(this.con).close();
	}

	@Test
	public void testTransactionWithLongTimeout() throws Exception {
		doTestTransactionWithTimeout(10);
	}

	@Test
	public void testTransactionWithShortTimeout() throws Exception {
		doTestTransactionWithTimeout(1);
	}

	private void doTestTransactionWithTimeout(int timeout) throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

		PreparedStatement ps = mock(PreparedStatement.class);
		given(this.con.getAutoCommit()).willReturn(true);
		given(this.con.prepareStatement("some SQL statement")).willReturn(ps);

		TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setTimeout(timeout);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					try {
						Thread.sleep(1500);
					}
					catch (InterruptedException ex) {
					}
					try {
						Connection con = DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds);
						PreparedStatement ps = con.prepareStatement("some SQL statement");
						DataSourceUtils.applyTransactionTimeout(ps, DataSourceTransactionManagerTests.this.ds);
					}
					catch (SQLException ex) {
						throw new DataAccessResourceFailureException("", ex);
					}
				}
			});
			if (timeout <= 1) {
				fail("Should have thrown TransactionTimedOutException");
			}
		}
		catch (TransactionTimedOutException ex) {
			if (timeout <= 1) {
				// expected
			}
			else {
				throw ex;
			}
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		if (timeout > 1) {
			verify(ps).setQueryTimeout(timeout - 1);
			verify(this.con).commit();
		}
		else {
			verify(this.con).rollback();
		}
		InOrder ordered = inOrder(this.con);
		ordered.verify(this.con).setAutoCommit(false);
		ordered.verify(this.con).setAutoCommit(true);
		verify(this.con).close();

	}

	@Test
	public void testTransactionAwareDataSourceProxy() throws Exception {
		given(this.con.getAutoCommit()).willReturn(true);

		TransactionTemplate tt = new TransactionTemplate(this.tm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertEquals(DataSourceTransactionManagerTests.this.con, DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds));
				TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(DataSourceTransactionManagerTests.this.ds);
				try {
					assertEquals(DataSourceTransactionManagerTests.this.con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
					// should be ignored
					dsProxy.getConnection().close();
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		InOrder ordered = inOrder(this.con);
		ordered.verify(this.con).setAutoCommit(false);
		ordered.verify(this.con).commit();
		ordered.verify(this.con).setAutoCommit(true);
		verify(this.con).close();
	}

	@Test
	public void testTransactionAwareDataSourceProxyWithSuspension() throws Exception {
		given(this.con.getAutoCommit()).willReturn(true);

		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertEquals(DataSourceTransactionManagerTests.this.con, DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds));
				final TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(DataSourceTransactionManagerTests.this.ds);
				try {
					assertEquals(DataSourceTransactionManagerTests.this.con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
					// should be ignored
					dsProxy.getConnection().close();
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}

				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						// something transactional
						assertEquals(DataSourceTransactionManagerTests.this.con, DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds));
						try {
							assertEquals(DataSourceTransactionManagerTests.this.con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
							// should be ignored
							dsProxy.getConnection().close();
						}
						catch (SQLException ex) {
							throw new UncategorizedSQLException("", "", ex);
						}
					}
				});

				try {
					assertEquals(DataSourceTransactionManagerTests.this.con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
					// should be ignored
					dsProxy.getConnection().close();
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		InOrder ordered = inOrder(this.con);
		ordered.verify(this.con).setAutoCommit(false);
		ordered.verify(this.con).commit();
		ordered.verify(this.con).setAutoCommit(true);
		verify(this.con, times(2)).close();
	}

	@Test
	public void testTransactionAwareDataSourceProxyWithSuspensionAndReobtaining() throws Exception {
		given(this.con.getAutoCommit()).willReturn(true);

		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertEquals(DataSourceTransactionManagerTests.this.con, DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds));
				final TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(DataSourceTransactionManagerTests.this.ds);
				dsProxy.setReobtainTransactionalConnections(true);
				try {
					assertEquals(DataSourceTransactionManagerTests.this.con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
					// should be ignored
					dsProxy.getConnection().close();
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}

				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						// something transactional
						assertEquals(DataSourceTransactionManagerTests.this.con, DataSourceUtils.getConnection(DataSourceTransactionManagerTests.this.ds));
						try {
							assertEquals(DataSourceTransactionManagerTests.this.con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
							// should be ignored
							dsProxy.getConnection().close();
						}
						catch (SQLException ex) {
							throw new UncategorizedSQLException("", "", ex);
						}
					}
				});

				try {
					assertEquals(DataSourceTransactionManagerTests.this.con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
					// should be ignored
					dsProxy.getConnection().close();
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		InOrder ordered = inOrder(this.con);
		ordered.verify(this.con).setAutoCommit(false);
		ordered.verify(this.con).commit();
		ordered.verify(this.con).setAutoCommit(true);
		verify(this.con, times(2)).close();
	}

	/**
	 * Test behavior if the first operation on a connection (getAutoCommit) throws SQLException.
	 */
	@Test
	public void testTransactionWithExceptionOnBegin() throws Exception {
		willThrow(new SQLException("Cannot begin")).given(this.con).getAutoCommit();

		TransactionTemplate tt = new TransactionTemplate(this.tm);
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
			fail("Should have thrown CannotCreateTransactionException");
		}
		catch (CannotCreateTransactionException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).close();
	}

	@Test
	public void testTransactionWithExceptionOnCommit() throws Exception {
		willThrow(new SQLException("Cannot commit")).given(this.con).commit();

		TransactionTemplate tt = new TransactionTemplate(this.tm);
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).close();
	}

	@Test
	public void testTransactionWithExceptionOnCommitAndRollbackOnCommitFailure() throws Exception {
		willThrow(new SQLException("Cannot commit")).given(this.con).commit();

		this.tm.setRollbackOnCommitFailure(true);
		TransactionTemplate tt = new TransactionTemplate(this.tm);
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).rollback();
		verify(this.con).close();
	}

	@Test
	public void testTransactionWithExceptionOnRollback() throws Exception {
		given(this.con.getAutoCommit()).willReturn(true);
		willThrow(new SQLException("Cannot rollback")).given(this.con).rollback();

		TransactionTemplate tt = new TransactionTemplate(this.tm);
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					status.setRollbackOnly();
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		InOrder ordered = inOrder(this.con);
		ordered.verify(this.con).setAutoCommit(false);
		ordered.verify(this.con).rollback();
		ordered.verify(this.con).setAutoCommit(true);
		verify(this.con).close();
	}

	@Test
	public void testTransactionWithPropagationSupports() throws Exception {
		TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
	}

	@Test
	public void testTransactionWithPropagationNotSupported() throws Exception {
		TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
				assertTrue("Is not new transaction", !status.isNewTransaction());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
	}

	@Test
	public void testTransactionWithPropagationNever() throws Exception {
		TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
				assertTrue("Is not new transaction", !status.isNewTransaction());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
	}

	@Test
	public void testExistingTransactionWithPropagationNested() throws Exception {
		doTestExistingTransactionWithPropagationNested(1);
	}

	@Test
	public void testExistingTransactionWithPropagationNestedTwice() throws Exception {
		doTestExistingTransactionWithPropagationNested(2);
	}

	private void doTestExistingTransactionWithPropagationNested(final int count) throws Exception {
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		Savepoint sp = mock(Savepoint.class);

		given(md.supportsSavepoints()).willReturn(true);
		given(this.con.getMetaData()).willReturn(md);
		for (int i = 1; i <= count; i++) {
			given(this.con.setSavepoint(ConnectionHolder.SAVEPOINT_NAME_PREFIX + i)).willReturn(sp);
		}

		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Isn't nested transaction", !status.hasSavepoint());
				for (int i = 0; i < count; i++) {
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
							assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
							assertTrue("Isn't new transaction", !status.isNewTransaction());
							assertTrue("Is nested transaction", status.hasSavepoint());
						}
					});
				}
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Isn't nested transaction", !status.hasSavepoint());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con, times(count)).releaseSavepoint(sp);
		verify(this.con).commit();
		verify(this.con).close();
	}

	@Test
	public void testExistingTransactionWithPropagationNestedAndRollback() throws Exception {
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		Savepoint sp = mock(Savepoint.class);

		given(md.supportsSavepoints()).willReturn(true);
		given(this.con.getMetaData()).willReturn(md);
		given(this.con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Isn't nested transaction", !status.hasSavepoint());
				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
						assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Isn't new transaction", !status.isNewTransaction());
						assertTrue("Is nested transaction", status.hasSavepoint());
						status.setRollbackOnly();
					}
				});
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Isn't nested transaction", !status.hasSavepoint());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).rollback(sp);
		verify(this.con).releaseSavepoint(sp);
		verify(this.con).commit();
		verify(this.con).isReadOnly();
		verify(this.con).close();
	}

	@Test
	public void testExistingTransactionWithPropagationNestedAndRequiredRollback() throws Exception {
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		Savepoint sp = mock(Savepoint.class);

		given(md.supportsSavepoints()).willReturn(true);
		given(this.con.getMetaData()).willReturn(md);
		given(this.con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Isn't nested transaction", !status.hasSavepoint());
				try {
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
							assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
							assertTrue("Isn't new transaction", !status.isNewTransaction());
							assertTrue("Is nested transaction", status.hasSavepoint());
							TransactionTemplate ntt = new TransactionTemplate(DataSourceTransactionManagerTests.this.tm);
							ntt.execute(new TransactionCallbackWithoutResult() {
								@Override
								protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
									assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
									assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
									assertTrue("Isn't new transaction", !status.isNewTransaction());
									assertTrue("Is regular transaction", !status.hasSavepoint());
									throw new IllegalStateException();
								}
							});
						}
					});
					fail("Should have thrown IllegalStateException");
				}
				catch (IllegalStateException ex) {
					// expected
				}
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Isn't nested transaction", !status.hasSavepoint());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).rollback(sp);
		verify(this.con).releaseSavepoint(sp);
		verify(this.con).commit();
		verify(this.con).isReadOnly();
		verify(this.con).close();
	}

	@Test
	public void testExistingTransactionWithPropagationNestedAndRequiredRollbackOnly() throws Exception {
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		Savepoint sp = mock(Savepoint.class);

		given(md.supportsSavepoints()).willReturn(true);
		given(this.con.getMetaData()).willReturn(md);
		given(this.con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Isn't nested transaction", !status.hasSavepoint());
				try {
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
							assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
							assertTrue("Isn't new transaction", !status.isNewTransaction());
							assertTrue("Is nested transaction", status.hasSavepoint());
							TransactionTemplate ntt = new TransactionTemplate(DataSourceTransactionManagerTests.this.tm);
							ntt.execute(new TransactionCallbackWithoutResult() {
								@Override
								protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
									assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(DataSourceTransactionManagerTests.this.ds));
									assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
									assertTrue("Isn't new transaction", !status.isNewTransaction());
									assertTrue("Is regular transaction", !status.hasSavepoint());
									status.setRollbackOnly();
								}
							});
						}
					});
					fail("Should have thrown UnexpectedRollbackException");
				}
				catch (UnexpectedRollbackException ex) {
					// expected
				}
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Isn't nested transaction", !status.hasSavepoint());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).rollback(sp);
		verify(this.con).releaseSavepoint(sp);
		verify(this.con).commit();
		verify(this.con).isReadOnly();
		verify(this.con).close();
	}

	@Test
	public void testExistingTransactionWithManualSavepoint() throws Exception {
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		Savepoint sp = mock(Savepoint.class);

		given(md.supportsSavepoints()).willReturn(true);
		given(this.con.getMetaData()).willReturn(md);
		given(this.con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				Object savepoint = status.createSavepoint();
				status.releaseSavepoint(savepoint);
				assertTrue("Is new transaction", status.isNewTransaction());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).releaseSavepoint(sp);
		verify(this.con).commit();
		verify(this.con).close();
		verify(this.ds).getConnection();
	}

	@Test
	public void testExistingTransactionWithManualSavepointAndRollback() throws Exception {
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		Savepoint sp = mock(Savepoint.class);

		given(md.supportsSavepoints()).willReturn(true);
		given(this.con.getMetaData()).willReturn(md);
		given(this.con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				Object savepoint = status.createSavepoint();
				status.rollbackToSavepoint(savepoint);
				assertTrue("Is new transaction", status.isNewTransaction());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).rollback(sp);
		verify(this.con).commit();
		verify(this.con).close();
	}

	@Test
	public void testTransactionWithPropagationNested() throws Exception {
		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).commit();
		verify(this.con).close();
	}

	@Test
	public void testTransactionWithPropagationNestedAndRollback() throws Exception {
		final TransactionTemplate tt = new TransactionTemplate(this.tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				status.setRollbackOnly();
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(this.ds));
		verify(this.con).rollback();
		verify(this.con).close();
	}


	private static class TestTransactionSynchronization implements TransactionSynchronization {

		private DataSource dataSource;

		private int status;

		public boolean beforeCommitCalled;

		public boolean beforeCompletionCalled;

		public boolean afterCommitCalled;

		public boolean afterCompletionCalled;

		public Throwable afterCompletionException;

		public TestTransactionSynchronization(DataSource dataSource, int status) {
			this.dataSource = dataSource;
			this.status = status;
		}

		@Override
		public void suspend() {
		}

		@Override
		public void resume() {
		}

		@Override
		public void flush() {
		}

		@Override
		public void beforeCommit(boolean readOnly) {
			if (this.status != TransactionSynchronization.STATUS_COMMITTED) {
				fail("Should never be called");
			}
			assertFalse(this.beforeCommitCalled);
			this.beforeCommitCalled = true;
		}

		@Override
		public void beforeCompletion() {
			assertFalse(this.beforeCompletionCalled);
			this.beforeCompletionCalled = true;
		}

		@Override
		public void afterCommit() {
			if (this.status != TransactionSynchronization.STATUS_COMMITTED) {
				fail("Should never be called");
			}
			assertFalse(this.afterCommitCalled);
			this.afterCommitCalled = true;
		}

		@Override
		public void afterCompletion(int status) {
			try {
				doAfterCompletion(status);
			}
			catch (Throwable ex) {
				this.afterCompletionException = ex;
			}
		}

		protected void doAfterCompletion(int status) {
			assertFalse(this.afterCompletionCalled);
			this.afterCompletionCalled = true;
			assertTrue(status == this.status);
			assertTrue(TransactionSynchronizationManager.hasResource(this.dataSource));
		}
	}

}
