/*
 * Copyright 2002-2015 the original author or authors.
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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.tests.Matchers.*;

/**
 * Tests for {@link SimpleJdbcCall}.
 *
 * @author Thomas Risberg
 * @author Kiril Nugmanov
 */
public class SimpleJdbcCallTests {

	private Connection connection;

	private DatabaseMetaData databaseMetaData;

	private DataSource dataSource;

	private CallableStatement callableStatement;

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Before
	public void setUp() throws Exception {
		this.connection = mock(Connection.class);
		this.databaseMetaData = mock(DatabaseMetaData.class);
		this.dataSource = mock(DataSource.class);
		this.callableStatement = mock(CallableStatement.class);
		given(this.connection.getMetaData()).willReturn(this.databaseMetaData);
		given(this.dataSource.getConnection()).willReturn(this.connection);
	}


	@Test
	public void testNoSuchStoredProcedure() throws Exception {
		final String NO_SUCH_PROC = "x";
		SQLException sqlException = new SQLException("Syntax error or access violation exception", "42000");
		given(this.databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(this.databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(this.databaseMetaData.getUserName()).willReturn("me");
		given(this.databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
		given(this.callableStatement.execute()).willThrow(sqlException);
		given(this.connection.prepareCall("{call " + NO_SUCH_PROC + "()}")).willReturn(this.callableStatement);
		SimpleJdbcCall sproc = new SimpleJdbcCall(this.dataSource).withProcedureName(NO_SUCH_PROC);
		this.thrown.expect(BadSqlGrammarException.class);
		this.thrown.expect(exceptionCause(sameInstance(sqlException)));
		try {
			sproc.execute();
		}
		finally {
			verify(this.callableStatement).close();
			verify(this.connection, atLeastOnce()).close();
		}
	}

	@Test
	public void testUnnamedParameterHandling() throws Exception {
		final String MY_PROC = "my_proc";
		SimpleJdbcCall sproc = new SimpleJdbcCall(this.dataSource).withProcedureName(MY_PROC);
		// Shouldn't succeed in adding unnamed parameter
		this.thrown.expect(InvalidDataAccessApiUsageException.class);
		sproc.addDeclaredParameter(new SqlParameter(1));
	}

	@Test
	public void testAddInvoiceProcWithoutMetaDataUsingMapParamSource() throws Exception {
		initializeAddInvoiceWithoutMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(this.dataSource).withProcedureName("add_invoice");
		adder.declareParameters(
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER),
				new SqlOutParameter("newid", Types.INTEGER));
		Number newId = adder.executeObject(Number.class, new MapSqlParameterSource().
				addValue("amount", 1103).
				addValue("custid", 3));
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithoutMetaData(false);
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testAddInvoiceProcWithoutMetaDataUsingArrayParams() throws Exception {
		initializeAddInvoiceWithoutMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(this.dataSource).withProcedureName("add_invoice");
		adder.declareParameters(
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER),
				new SqlOutParameter("newid", Types.INTEGER));
		Number newId = adder.executeObject(Number.class, 1103, 3);
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithoutMetaData(false);
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testAddInvoiceProcWithMetaDataUsingMapParamSource() throws Exception {
		initializeAddInvoiceWithMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(this.dataSource).withProcedureName("add_invoice");
		Number newId = adder.executeObject(Number.class, new MapSqlParameterSource()
				.addValue("amount", 1103)
				.addValue("custid", 3));
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithMetaData(false);
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testAddInvoiceProcWithMetaDataUsingArrayParams() throws Exception {
		initializeAddInvoiceWithMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(this.dataSource).withProcedureName("add_invoice");
		Number newId = adder.executeObject(Number.class, 1103, 3);
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithMetaData(false);
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testAddInvoiceFuncWithoutMetaDataUsingMapParamSource() throws Exception {
		initializeAddInvoiceWithoutMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(this.dataSource).withFunctionName("add_invoice");
		adder.declareParameters(
				new SqlOutParameter("return", Types.INTEGER),
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER));
		Number newId = adder.executeFunction(Number.class, new MapSqlParameterSource()
				.addValue("amount", 1103)
				.addValue("custid", 3));
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithoutMetaData(true);
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testAddInvoiceFuncWithoutMetaDataUsingArrayParams() throws Exception {
		initializeAddInvoiceWithoutMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(this.dataSource).withFunctionName("add_invoice");
		adder.declareParameters(
				new SqlOutParameter("return", Types.INTEGER),
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER));
		Number newId = adder.executeFunction(Number.class, 1103, 3);
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithoutMetaData(true);
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testAddInvoiceFuncWithMetaDataUsingMapParamSource() throws Exception {
		initializeAddInvoiceWithMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(this.dataSource).withFunctionName("add_invoice");
		Number newId = adder.executeFunction(Number.class, new MapSqlParameterSource()
				.addValue("amount", 1103)
				.addValue("custid", 3));
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithMetaData(true);
		verify(this.connection, atLeastOnce()).close();

	}

	@Test
	public void testAddInvoiceFuncWithMetaDataUsingArrayParams() throws Exception {
		initializeAddInvoiceWithMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(this.dataSource).withFunctionName("add_invoice");
		Number newId = adder.executeFunction(Number.class, 1103, 3);
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithMetaData(true);
		verify(this.connection, atLeastOnce()).close();

	}

	@Test
	public void testCorrectFunctionStatement() throws Exception {
		initializeAddInvoiceWithMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(this.dataSource).withFunctionName("add_invoice");
		adder.compile();
		verifyStatement(adder, "{? = call ADD_INVOICE(?, ?)}");
	}

	@Test
	public void testCorrectFunctionStatementNamed() throws Exception {
		initializeAddInvoiceWithMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(this.dataSource).withNamedBinding().withFunctionName("add_invoice");
		adder.compile();
		verifyStatement(adder, "{? = call ADD_INVOICE(AMOUNT => ?, CUSTID => ?)}");
	}

	@Test
	public void testCorrectProcedureStatementNamed() throws Exception {
		initializeAddInvoiceWithMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(this.dataSource).withNamedBinding().withProcedureName("add_invoice");
		adder.compile();
		verifyStatement(adder, "{call ADD_INVOICE(AMOUNT => ?, CUSTID => ?, NEWID => ?)}");
	}


	private void verifyStatement(SimpleJdbcCall adder, String expected) {
		Assert.assertEquals("Incorrect call statement", expected, adder.getCallString());
	}

	private void initializeAddInvoiceWithoutMetaData(boolean isFunction) throws SQLException {
		given(this.databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(this.databaseMetaData.getUserName()).willReturn("me");
		given(this.databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
		given(this.callableStatement.execute()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		if (isFunction) {
			given(this.callableStatement.getObject(1)).willReturn(4L);
			given(this.connection.prepareCall("{? = call add_invoice(?, ?)}")
					).willReturn(this.callableStatement);
		}
		else {
			given(this.callableStatement.getObject(3)).willReturn(4L);
			given(this.connection.prepareCall("{call add_invoice(?, ?, ?)}")
					).willReturn(this.callableStatement);
		}
	}

	private void verifyAddInvoiceWithoutMetaData(boolean isFunction) throws SQLException {
		if (isFunction) {
			verify(this.callableStatement).registerOutParameter(1, 4);
			verify(this.callableStatement).setObject(2, 1103, 4);
			verify(this.callableStatement).setObject(3, 3, 4);
		}
		else {
			verify(this.callableStatement).setObject(1, 1103, 4);
			verify(this.callableStatement).setObject(2, 3, 4);
			verify(this.callableStatement).registerOutParameter(3, 4);
		}
		verify(this.callableStatement).close();
	}

	private void initializeAddInvoiceWithMetaData(boolean isFunction) throws SQLException {
		ResultSet proceduresResultSet = mock(ResultSet.class);
		ResultSet procedureColumnsResultSet = mock(ResultSet.class);
		given(this.databaseMetaData.getDatabaseProductName()).willReturn("Oracle");
		given(this.databaseMetaData.getUserName()).willReturn("ME");
		given(this.databaseMetaData.storesUpperCaseIdentifiers()).willReturn(true);
		given(this.databaseMetaData.getProcedures("", "ME", "ADD_INVOICE")).willReturn(proceduresResultSet);
		given(this.databaseMetaData.getProcedureColumns("", "ME", "ADD_INVOICE", null)).willReturn(procedureColumnsResultSet);

		given(proceduresResultSet.next()).willReturn(true, false);
		given(proceduresResultSet.getString("PROCEDURE_NAME")).willReturn("add_invoice");

		given(procedureColumnsResultSet.next()).willReturn(true, true, true, false);
		given(procedureColumnsResultSet.getInt("DATA_TYPE")).willReturn(4);
		if (isFunction) {
			given(procedureColumnsResultSet.getString("COLUMN_NAME")).willReturn(null,"amount", "custid");
			given(procedureColumnsResultSet.getInt("COLUMN_TYPE")).willReturn(5, 1, 1);
			given(this.connection.prepareCall("{? = call ADD_INVOICE(?, ?)}")).willReturn(this.callableStatement);
			given(this.callableStatement.getObject(1)).willReturn(4L);
		}
		else {
			given(procedureColumnsResultSet.getString("COLUMN_NAME")).willReturn("amount", "custid", "newid");
			given(procedureColumnsResultSet.getInt("COLUMN_TYPE")).willReturn(1, 1, 4);
			given(this.connection.prepareCall("{call ADD_INVOICE(?, ?, ?)}")).willReturn(this.callableStatement);
			given(this.callableStatement.getObject(3)).willReturn(4L);
		}
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
	}

	private void verifyAddInvoiceWithMetaData(boolean isFunction) throws SQLException {
		ResultSet proceduresResultSet = this.databaseMetaData.getProcedures("", "ME", "ADD_INVOICE");
		ResultSet procedureColumnsResultSet = this.databaseMetaData.getProcedureColumns("", "ME", "ADD_INVOICE", null);
		if (isFunction) {
			verify(this.callableStatement).registerOutParameter(1, 4);
			verify(this.callableStatement).setObject(2, 1103, 4);
			verify(this.callableStatement).setObject(3, 3, 4);
		}
		else {
			verify(this.callableStatement).setObject(1, 1103, 4);
			verify(this.callableStatement).setObject(2, 3, 4);
			verify(this.callableStatement).registerOutParameter(3, 4);
		}
		verify(this.callableStatement).close();
		verify(proceduresResultSet).close();
		verify(procedureColumnsResultSet).close();
	}

}
