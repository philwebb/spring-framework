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

package org.springframework.jdbc.core.simple;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Mock object based tests for SimpleJdbcInsert.
 *
 * @author Thomas Risberg
 */
public class SimpleJdbcInsertTests {

	private Connection connection;

	private DatabaseMetaData databaseMetaData;

	private DataSource dataSource;

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Before
	public void setUp() throws Exception {
		this.connection = mock(Connection.class);
		this.databaseMetaData = mock(DatabaseMetaData.class);
		this.dataSource = mock(DataSource.class);
		given(this.connection.getMetaData()).willReturn(this.databaseMetaData);
		given(this.dataSource.getConnection()).willReturn(this.connection);
	}

	@After
	public void verifyClosed() throws Exception {
		verify(this.connection).close();
	}


	@Test
	public void testNoSuchTable() throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		given(resultSet.next()).willReturn(false);
		given(this.databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(this.databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(this.databaseMetaData.getDatabaseProductVersion()).willReturn("1.0");
		given(this.databaseMetaData.getUserName()).willReturn("me");
		given(this.databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
		given(this.databaseMetaData.getTables(null, null, "x", null)).willReturn(resultSet);

		SimpleJdbcInsert insert = new SimpleJdbcInsert(this.dataSource).withTableName("x");
		// Shouldn't succeed in inserting into table which doesn't exist
		this.thrown.expect(InvalidDataAccessApiUsageException.class);
		try {
			insert.execute(new HashMap<>());
		}
		finally {
			verify(resultSet).close();
		}
	}

}
