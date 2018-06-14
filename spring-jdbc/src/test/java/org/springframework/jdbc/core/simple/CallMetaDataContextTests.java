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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.metadata.CallMetaDataContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Mock object based tests for CallMetaDataContext.
 *
 * @author Thomas Risberg
 */
public class CallMetaDataContextTests {

	private DataSource dataSource;

	private Connection connection;

	private DatabaseMetaData databaseMetaData;

	private CallMetaDataContext context = new CallMetaDataContext();


	@Before
	public void setUp() throws Exception {
		this.connection = mock(Connection.class);
		this.databaseMetaData = mock(DatabaseMetaData.class);
		given(this.connection.getMetaData()).willReturn(this.databaseMetaData);
		this.dataSource = mock(DataSource.class);
		given(this.dataSource.getConnection()).willReturn(this.connection);
	}

	@After
	public void verifyClosed() throws Exception {
		verify(this.connection).close();
	}


	@Test
	public void testMatchParameterValuesAndSqlInOutParameters() throws Exception {
		final String TABLE = "customers";
		final String USER = "me";
		given(this.databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(this.databaseMetaData.getUserName()).willReturn(USER);
		given(this.databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);

		List<SqlParameter> parameters = new ArrayList<>();
		parameters.add(new SqlParameter("id", Types.NUMERIC));
		parameters.add(new SqlInOutParameter("name", Types.NUMERIC));
		parameters.add(new SqlOutParameter("customer_no", Types.NUMERIC));

		MapSqlParameterSource parameterSource = new MapSqlParameterSource();
		parameterSource.addValue("id", 1);
		parameterSource.addValue("name", "Sven");
		parameterSource.addValue("customer_no", "12345XYZ");

		this.context.setProcedureName(TABLE);
		this.context.initializeMetaData(this.dataSource);
		this.context.processParameters(parameters);

		Map<String, Object> inParameters = this.context.matchInParameterValuesWithCallParameters(parameterSource);
		assertEquals("Wrong number of matched in parameter values", 2, inParameters.size());
		assertTrue("in parameter value missing", inParameters.containsKey("id"));
		assertTrue("in out parameter value missing", inParameters.containsKey("name"));
		assertTrue("out parameter value matched", !inParameters.containsKey("customer_no"));

		List<String> names = this.context.getOutParameterNames();
		assertEquals("Wrong number of out parameters", 2, names.size());

		List<SqlParameter> callParameters = this.context.getCallParameters();
		assertEquals("Wrong number of call parameters", 3, callParameters.size());
	}

}
