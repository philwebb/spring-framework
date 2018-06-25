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

package org.springframework.jdbc.object;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SimpleRowCountCallbackHandler;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.core.support.AbstractSqlTypeValue;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Thomas Risberg
 * @author Trevor Cook
 * @author Rod Johnson
 */
public class StoredProcedureTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private DataSource dataSource;
	private Connection connection;
	private CallableStatement callableStatement;

	private boolean verifyClosedAfter = true;

	@Before
	public void setup() throws Exception {
		this.dataSource = mock(DataSource.class);
		this.connection = mock(Connection.class);
		this.callableStatement = mock(CallableStatement.class);
		given(this.dataSource.getConnection()).willReturn(this.connection);
		given(this.callableStatement.getConnection()).willReturn(this.connection);
	}

	@After
	public void verifyClosed() throws Exception {
		if (this.verifyClosedAfter) {
			verify(this.callableStatement).close();
			verify(this.connection, atLeastOnce()).close();
		}
	}

	@Test
	public void testNoSuchStoredProcedure() throws Exception {
		SQLException sqlException = new SQLException(
				"Syntax error or access violation exception", "42000");
		given(this.callableStatement.execute()).willThrow(sqlException);
		given(this.connection.prepareCall("{call " + NoSuchStoredProcedure.SQL + "()}")).willReturn(
				this.callableStatement);

		NoSuchStoredProcedure sproc = new NoSuchStoredProcedure(this.dataSource);
		this.thrown.expect(BadSqlGrammarException.class);
		sproc.execute();
	}

	private void testAddInvoice(final int amount, final int custid) throws Exception {
		AddInvoice adder = new AddInvoice(this.dataSource);
		int id = adder.execute(amount, custid);
		assertEquals(4, id);
	}

	private void testAddInvoiceUsingObjectArray(final int amount, final int custid)
			throws Exception {
		AddInvoiceUsingObjectArray adder = new AddInvoiceUsingObjectArray(this.dataSource);
		int id = adder.execute(amount, custid);
		assertEquals(5, id);
	}

	@Test
	public void testAddInvoices() throws Exception {
		given(this.callableStatement.execute()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getObject(3)).willReturn(4);
		given(this.connection.prepareCall("{call " + AddInvoice.SQL + "(?, ?, ?)}")
				).willReturn(this.callableStatement);
		testAddInvoice(1106, 3);
		verify(this.callableStatement).setObject(1, 1106, Types.INTEGER);
		verify(this.callableStatement).setObject(2, 3, Types.INTEGER);
		verify(this.callableStatement).registerOutParameter(3, Types.INTEGER);
	}

	@Test
	public void testAddInvoicesUsingObjectArray() throws Exception {
		given(this.callableStatement.execute()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getObject(3)).willReturn(5);
		given(this.connection.prepareCall("{call " + AddInvoice.SQL + "(?, ?, ?)}")
				).willReturn(this.callableStatement);
		testAddInvoiceUsingObjectArray(1106, 4);
		verify(this.callableStatement).setObject(1, 1106, Types.INTEGER);
		verify(this.callableStatement).setObject(2, 4, Types.INTEGER);
		verify(this.callableStatement).registerOutParameter(3, Types.INTEGER);
	}

	@Test
	public void testAddInvoicesWithinTransaction() throws Exception {
		given(this.callableStatement.execute()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getObject(3)).willReturn(4);
		given(this.connection.prepareCall("{call " + AddInvoice.SQL + "(?, ?, ?)}")
				).willReturn(this.callableStatement);
		TransactionSynchronizationManager.bindResource(this.dataSource, new ConnectionHolder(this.connection));
		try {
			testAddInvoice(1106, 3);
			verify(this.callableStatement).setObject(1, 1106, Types.INTEGER);
			verify(this.callableStatement).setObject(2, 3, Types.INTEGER);
			verify(this.callableStatement).registerOutParameter(3, Types.INTEGER);
			verify(this.connection, never()).close();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(this.dataSource);
			this.connection.close();
		}
	}

	/**
	 * Confirm no connection was used to get metadata. Does not use superclass replay
	 * mechanism.
	 *
	 * @throws Exception
	 */
	@Test
	public void testStoredProcedureConfiguredViaJdbcTemplateWithCustomExceptionTranslator()
			throws Exception {
		given(this.callableStatement.execute()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getObject(2)).willReturn(5);
		given(this.connection.prepareCall("{call " + StoredProcedureConfiguredViaJdbcTemplate.SQL + "(?, ?)}")
				).willReturn(this.callableStatement);

		class TestJdbcTemplate extends JdbcTemplate {

			int calls;

			@Override
			public Map<String, Object> call(CallableStatementCreator csc,
					List<SqlParameter> declaredParameters) throws DataAccessException {
				this.calls++;
				return super.call(csc, declaredParameters);
			}
		}
		TestJdbcTemplate t = new TestJdbcTemplate();
		t.setDataSource(this.dataSource);
		// Will fail without the following, because we're not able to get a connection
		// from the DataSource here if we need to create an ExceptionTranslator
		t.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		StoredProcedureConfiguredViaJdbcTemplate sp = new StoredProcedureConfiguredViaJdbcTemplate(t);

		assertEquals(5, sp.execute(11));
		assertEquals(1, t.calls);

		verify(this.callableStatement).setObject(1, 11, Types.INTEGER);
		verify(this.callableStatement).registerOutParameter(2, Types.INTEGER);
	}

	/**
	 * Confirm our JdbcTemplate is used
	 *
	 * @throws Exception
	 */
	@Test
	public void testStoredProcedureConfiguredViaJdbcTemplate() throws Exception {
		given(this.callableStatement.execute()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getObject(2)).willReturn(4);
		given(this.connection.prepareCall("{call " + StoredProcedureConfiguredViaJdbcTemplate.SQL + "(?, ?)}")
				).willReturn(this.callableStatement);
		JdbcTemplate t = new JdbcTemplate();
		t.setDataSource(this.dataSource);
		StoredProcedureConfiguredViaJdbcTemplate sp = new StoredProcedureConfiguredViaJdbcTemplate(t);
		assertEquals(4, sp.execute(1106));
		verify(this.callableStatement).setObject(1, 1106, Types.INTEGER);
		verify(this.callableStatement).registerOutParameter(2, Types.INTEGER);
	}

	@Test
	public void testNullArg() throws Exception {
		given(this.callableStatement.execute()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.connection.prepareCall("{call " + NullArg.SQL + "(?)}")).willReturn(this.callableStatement);
		NullArg na = new NullArg(this.dataSource);
		na.execute((String) null);
		this.callableStatement.setNull(1, Types.VARCHAR);
	}

	@Test
	public void testUnnamedParameter() throws Exception {
		this.verifyClosedAfter = false;
		// Shouldn't succeed in creating stored procedure with unnamed parameter
		this.thrown.expect(InvalidDataAccessApiUsageException.class);
		new UnnamedParameterStoredProcedure(this.dataSource);
	}

	@Test
	public void testMissingParameter() throws Exception {
		this.verifyClosedAfter = false;
		MissingParameterStoredProcedure mp = new MissingParameterStoredProcedure(this.dataSource);
		this.thrown.expect(InvalidDataAccessApiUsageException.class);
		mp.execute();
		fail("Shouldn't succeed in running stored procedure with missing required parameter");
	}

	@Test
	public void testStoredProcedureExceptionTranslator() throws Exception {
		SQLException sqlException = new SQLException(
				"Syntax error or access violation exception", "42000");
		given(this.callableStatement.execute()).willThrow(sqlException);
		given(this.connection.prepareCall("{call " + StoredProcedureExceptionTranslator.SQL + "()}")
				).willReturn(this.callableStatement);
		StoredProcedureExceptionTranslator sproc = new StoredProcedureExceptionTranslator(this.dataSource);
		this.thrown.expect(CustomDataException.class);
		sproc.execute();
	}

	@Test
	public void testStoredProcedureWithResultSet() throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		given(resultSet.next()).willReturn(true, true, false);
		given(this.callableStatement.execute()).willReturn(true);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getResultSet()).willReturn(resultSet);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.connection.prepareCall("{call " + StoredProcedureWithResultSet.SQL + "()}")
				).willReturn(this.callableStatement);
		StoredProcedureWithResultSet sproc = new StoredProcedureWithResultSet(this.dataSource);
		sproc.execute();
		assertEquals(2, sproc.getCount());
		verify(resultSet).close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testStoredProcedureWithResultSetMapped() throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getString(2)).willReturn("Foo", "Bar");
		given(this.callableStatement.execute()).willReturn(true);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getResultSet()).willReturn(resultSet);
		given(this.callableStatement.getMoreResults()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.connection.prepareCall("{call " + StoredProcedureWithResultSetMapped.SQL + "()}")
				).willReturn(this.callableStatement);
		StoredProcedureWithResultSetMapped sproc = new StoredProcedureWithResultSetMapped(this.dataSource);
		Map<String, Object> res = sproc.execute();
		List<String> rs = (List<String>) res.get("rs");
		assertEquals(2, rs.size());
		assertEquals("Foo", rs.get(0));
		assertEquals("Bar", rs.get(1));
		verify(resultSet).close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testStoredProcedureWithUndeclaredResults() throws Exception {
		ResultSet resultSet1 = mock(ResultSet.class);
		given(resultSet1.next()).willReturn(true, true, false);
		given(resultSet1.getString(2)).willReturn("Foo", "Bar");

		ResultSetMetaData resultSetMetaData = mock(ResultSetMetaData.class);
		given(resultSetMetaData.getColumnCount()).willReturn(2);
		given(resultSetMetaData.getColumnLabel(1)).willReturn("spam");
		given(resultSetMetaData.getColumnLabel(2)).willReturn("eggs");

		ResultSet resultSet2 = mock(ResultSet.class);
		given(resultSet2.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet2.next()).willReturn(true, false);
		given(resultSet2.getObject(1)).willReturn("Spam");
		given(resultSet2.getObject(2)).willReturn("Eggs");

		given(this.callableStatement.execute()).willReturn(true);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getResultSet()).willReturn(resultSet1, resultSet2);
		given(this.callableStatement.getMoreResults()).willReturn(true, false, false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1, -1, 0, -1);
		given(this.connection.prepareCall("{call " + StoredProcedureWithResultSetMapped.SQL + "()}")
				).willReturn(this.callableStatement);

		StoredProcedureWithResultSetMapped sproc = new StoredProcedureWithResultSetMapped(this.dataSource);
		Map<String, Object> res = sproc.execute();

		assertEquals("incorrect number of returns", 3, res.size());

		List<String> rs1 = (List<String>) res.get("rs");
		assertEquals(2, rs1.size());
		assertEquals("Foo", rs1.get(0));
		assertEquals("Bar", rs1.get(1));

		List<Object> rs2 = (List<Object>) res.get("#result-set-2");
		assertEquals(1, rs2.size());
		Object o2 = rs2.get(0);
		assertTrue("wron type returned for result set 2", o2 instanceof Map);
		Map<String, String> m2 = (Map<String, String>) o2;
		assertEquals("Spam", m2.get("spam"));
		assertEquals("Eggs", m2.get("eggs"));

		Number n = (Number) res.get("#update-count-1");
		assertEquals("wrong update count", 0, n.intValue());
		verify(resultSet1).close();
		verify(resultSet2).close();
	}

	@Test
	public void testStoredProcedureSkippingResultsProcessing() throws Exception {
		given(this.callableStatement.execute()).willReturn(true);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.connection.prepareCall("{call " + StoredProcedureWithResultSetMapped.SQL + "()}")
				).willReturn(this.callableStatement);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
		jdbcTemplate.setSkipResultsProcessing(true);
		StoredProcedureWithResultSetMapped sproc = new StoredProcedureWithResultSetMapped(
				jdbcTemplate);
		Map<String, Object> res = sproc.execute();
		assertEquals("incorrect number of returns", 0, res.size());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testStoredProcedureSkippingUndeclaredResults() throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getString(2)).willReturn("Foo", "Bar");
		given(this.callableStatement.execute()).willReturn(true);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getResultSet()).willReturn(resultSet);
		given(this.callableStatement.getMoreResults()).willReturn(true, false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1, -1);
		given(this.connection.prepareCall("{call " + StoredProcedureWithResultSetMapped.SQL + "()}")
				).willReturn(this.callableStatement);

		JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
		jdbcTemplate.setSkipUndeclaredResults(true);
		StoredProcedureWithResultSetMapped sproc = new StoredProcedureWithResultSetMapped(
				jdbcTemplate);
		Map<String, Object> res = sproc.execute();

		assertEquals("incorrect number of returns", 1, res.size());
		List<String> rs1 = (List<String>) res.get("rs");
		assertEquals(2, rs1.size());
		assertEquals("Foo", rs1.get(0));
		assertEquals("Bar", rs1.get(1));
		verify(resultSet).close();
	}

	@Test
	public void testParameterMapper() throws Exception {
		given(this.callableStatement.execute()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getObject(2)).willReturn("OK");
		given(this.connection.prepareCall("{call " + ParameterMapperStoredProcedure.SQL + "(?, ?)}")
				).willReturn(this.callableStatement);

		ParameterMapperStoredProcedure pmsp = new ParameterMapperStoredProcedure(this.dataSource);
		Map<String, Object> out = pmsp.executeTest();
		assertEquals("OK", out.get("out"));

		verify(this.callableStatement).setString(eq(1), startsWith("Mock for Connection"));
		verify(this.callableStatement).registerOutParameter(2, Types.VARCHAR);
	}

	@Test
	public void testSqlTypeValue() throws Exception {
		int[] testVal = new int[] { 1, 2 };
		given(this.callableStatement.execute()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getObject(2)).willReturn("OK");
		given(this.connection.prepareCall("{call " + SqlTypeValueStoredProcedure.SQL + "(?, ?)}")
				).willReturn(this.callableStatement);

		SqlTypeValueStoredProcedure stvsp = new SqlTypeValueStoredProcedure(this.dataSource);
		Map<String, Object> out = stvsp.executeTest(testVal);
		assertEquals("OK", out.get("out"));
		verify(this.callableStatement).setObject(1, testVal, Types.ARRAY);
		verify(this.callableStatement).registerOutParameter(2, Types.VARCHAR);
	}

	@Test
	public void testNumericWithScale() throws Exception {
		given(this.callableStatement.execute()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getObject(1)).willReturn(new BigDecimal("12345.6789"));
		given(this.connection.prepareCall("{call " + NumericWithScaleStoredProcedure.SQL + "(?)}")
				).willReturn(this.callableStatement);
		NumericWithScaleStoredProcedure nwssp = new NumericWithScaleStoredProcedure(this.dataSource);
		Map<String, Object> out = nwssp.executeTest();
		assertEquals(new BigDecimal("12345.6789"), out.get("out"));
		verify(this.callableStatement).registerOutParameter(1, Types.DECIMAL, 4);
	}

	private static class StoredProcedureConfiguredViaJdbcTemplate extends StoredProcedure {

		public static final String SQL = "configured_via_jt";

		public StoredProcedureConfiguredViaJdbcTemplate(JdbcTemplate t) {
			setJdbcTemplate(t);
			setSql(SQL);
			declareParameter(new SqlParameter("intIn", Types.INTEGER));
			declareParameter(new SqlOutParameter("intOut", Types.INTEGER));
			compile();
		}

		public int execute(int intIn) {
			Map<String, Integer> in = new HashMap<>();
			in.put("intIn", intIn);
			Map<String, Object> out = execute(in);
			return ((Number) out.get("intOut")).intValue();
		}
	}

	private static class AddInvoice extends StoredProcedure {

		public static final String SQL = "add_invoice";

		public AddInvoice(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlParameter("amount", Types.INTEGER));
			declareParameter(new SqlParameter("custid", Types.INTEGER));
			declareParameter(new SqlOutParameter("newid", Types.INTEGER));
			compile();
		}

		public int execute(int amount, int custid) {
			Map<String, Integer> in = new HashMap<>();
			in.put("amount", amount);
			in.put("custid", custid);
			Map<String, Object> out = execute(in);
			return ((Number) out.get("newid")).intValue();
		}
	}

	private static class AddInvoiceUsingObjectArray extends StoredProcedure {

		public static final String SQL = "add_invoice";

		public AddInvoiceUsingObjectArray(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlParameter("amount", Types.INTEGER));
			declareParameter(new SqlParameter("custid", Types.INTEGER));
			declareParameter(new SqlOutParameter("newid", Types.INTEGER));
			compile();
		}

		public int execute(int amount, int custid) {
			Map<String, Object> out = execute(new Object[] { amount, custid });
			return ((Number) out.get("newid")).intValue();
		}
	}

	private static class NullArg extends StoredProcedure {

		public static final String SQL = "takes_null";

		public NullArg(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlParameter("ptest", Types.VARCHAR));
			compile();
		}

		public void execute(String s) {
			Map<String, String> in = new HashMap<>();
			in.put("ptest", s);
			execute(in);
		}
	}

	private static class NoSuchStoredProcedure extends StoredProcedure {

		public static final String SQL = "no_sproc_with_this_name";

		public NoSuchStoredProcedure(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			compile();
		}

		public void execute() {
			execute(new HashMap<>());
		}
	}

	private static class UnnamedParameterStoredProcedure extends StoredProcedure {

		public UnnamedParameterStoredProcedure(DataSource ds) {
			super(ds, "unnamed_parameter_sp");
			declareParameter(new SqlParameter(Types.INTEGER));
			compile();
		}

	}

	private static class MissingParameterStoredProcedure extends StoredProcedure {

		public MissingParameterStoredProcedure(DataSource ds) {
			setDataSource(ds);
			setSql("takes_string");
			declareParameter(new SqlParameter("mystring", Types.VARCHAR));
			compile();
		}

		public void execute() {
			execute(new HashMap<>());
		}
	}

	private static class StoredProcedureWithResultSet extends StoredProcedure {

		public static final String SQL = "sproc_with_result_set";

		private final SimpleRowCountCallbackHandler handler = new SimpleRowCountCallbackHandler();

		public StoredProcedureWithResultSet(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlReturnResultSet("rs", this.handler));
			compile();
		}

		public void execute() {
			execute(new HashMap<>());
		}

		public int getCount() {
			return this.handler.getCount();
		}
	}

	private static class StoredProcedureWithResultSetMapped extends StoredProcedure {

		public static final String SQL = "sproc_with_result_set";

		public StoredProcedureWithResultSetMapped(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlReturnResultSet("rs", new RowMapperImpl()));
			compile();
		}

		public StoredProcedureWithResultSetMapped(JdbcTemplate jt) {
			setJdbcTemplate(jt);
			setSql(SQL);
			declareParameter(new SqlReturnResultSet("rs", new RowMapperImpl()));
			compile();
		}

		public Map<String, Object> execute() {
			return execute(new HashMap<>());
		}

		private static class RowMapperImpl implements RowMapper<String> {

			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(2);
			}
		}
	}

	private static class ParameterMapperStoredProcedure extends StoredProcedure {

		public static final String SQL = "parameter_mapper_sp";

		public ParameterMapperStoredProcedure(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlParameter("in", Types.VARCHAR));
			declareParameter(new SqlOutParameter("out", Types.VARCHAR));
			compile();
		}

		public Map<String, Object> executeTest() {
			return execute(new TestParameterMapper());
		}

		private static class TestParameterMapper implements ParameterMapper {

			private TestParameterMapper() {
			}

			@Override
			public Map<String, ?> createMap(Connection con) throws SQLException {
				Map<String, Object> inParms = new HashMap<>();
				String testValue = con.toString();
				inParms.put("in", testValue);
				return inParms;
			}
		}
	}

	private static class SqlTypeValueStoredProcedure extends StoredProcedure {

		public static final String SQL = "sql_type_value_sp";

		public SqlTypeValueStoredProcedure(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlParameter("in", Types.ARRAY, "NUMBERS"));
			declareParameter(new SqlOutParameter("out", Types.VARCHAR));
			compile();
		}

		public Map<String, Object> executeTest(final int[] inValue) {
			Map<String, AbstractSqlTypeValue> in = new HashMap<>();
			in.put("in", new AbstractSqlTypeValue() {
				@Override
				public Object createTypeValue(Connection con, int type, String typeName) {
					// assertEquals(Connection.class, con.getClass());
					// assertEquals(Types.ARRAY, type);
					// assertEquals("NUMBER", typeName);
					return inValue;
				}
			});
			return execute(in);
		}
	}

	private static class NumericWithScaleStoredProcedure extends StoredProcedure {

		public static final String SQL = "numeric_with_scale_sp";

		public NumericWithScaleStoredProcedure(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlOutParameter("out", Types.DECIMAL, 4));
			compile();
		}

		public Map<String, Object> executeTest() {
			return execute(new HashMap<>());
		}
	}

	private static class StoredProcedureExceptionTranslator extends StoredProcedure {

		public static final String SQL = "no_sproc_with_this_name";

		public StoredProcedureExceptionTranslator(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			getJdbcTemplate().setExceptionTranslator(new SQLExceptionTranslator() {
				@Override
				public DataAccessException translate(String task, @Nullable String sql, SQLException ex) {
					return new CustomDataException(sql, ex);
				}
			});
			compile();
		}

		public void execute() {
			execute(new HashMap<>());
		}
	}

	@SuppressWarnings("serial")
	private static class CustomDataException extends DataAccessException {

		public CustomDataException(String s) {
			super(s);
		}

		public CustomDataException(String s, Throwable ex) {
			super(s, ex);
		}
	}

}
