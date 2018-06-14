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

package org.springframework.jdbc.core.namedparam;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.jdbc.Customer;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.SqlParameterValue;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Nikita Khateev
 */
public class NamedParameterJdbcTemplateTests {

	private static final String SELECT_NAMED_PARAMETERS =
			"select id, forename from custmr where id = :id and country = :country";
	private static final String SELECT_NAMED_PARAMETERS_PARSED =
			"select id, forename from custmr where id = ? and country = ?";
	private static final String SELECT_NO_PARAMETERS =
			"select id, forename from custmr";

	private static final String UPDATE_NAMED_PARAMETERS =
			"update seat_status set booking_id = null where performance_id = :perfId and price_band_id = :priceId";
	private static final String UPDATE_NAMED_PARAMETERS_PARSED =
			"update seat_status set booking_id = null where performance_id = ? and price_band_id = ?";

	private static final String UPDATE_ARRAY_PARAMETERS =
			"update customer set type = array[:typeIds] where id = :id";
	private static final String UPDATE_ARRAY_PARAMETERS_PARSED =
			"update customer set type = array[?, ?, ?] where id = ?";

	private static final String[] COLUMN_NAMES = new String[] {"id", "forename"};


	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Connection connection;

	private DataSource dataSource;

	private PreparedStatement preparedStatement;

	private ResultSet resultSet;

	private DatabaseMetaData databaseMetaData;

	private Map<String, Object> params = new HashMap<>();

	private NamedParameterJdbcTemplate namedParameterTemplate;


	@Before
	public void setup() throws Exception {
		this.connection = mock(Connection.class);
		this.dataSource = mock(DataSource.class);
		this.preparedStatement =	mock(PreparedStatement.class);
		this.resultSet = mock(ResultSet.class);
		this.namedParameterTemplate = new NamedParameterJdbcTemplate(this.dataSource);
		this.databaseMetaData = mock(DatabaseMetaData.class);
		given(this.dataSource.getConnection()).willReturn(this.connection);
		given(this.connection.prepareStatement(anyString())).willReturn(this.preparedStatement);
		given(this.preparedStatement.getConnection()).willReturn(this.connection);
		given(this.preparedStatement.executeQuery()).willReturn(this.resultSet);
		given(this.databaseMetaData.getDatabaseProductName()).willReturn("MySQL");
		given(this.databaseMetaData.supportsBatchUpdates()).willReturn(true);
	}


	@Test
	public void testNullDataSourceProvidedToCtor() {
		this.thrown.expect(IllegalArgumentException.class);
		new NamedParameterJdbcTemplate((DataSource) null);
	}

	@Test
	public void testNullJdbcTemplateProvidedToCtor() {
		this.thrown.expect(IllegalArgumentException.class);
		new NamedParameterJdbcTemplate((JdbcOperations) null);
	}

	@Test
	public void testTemplateConfiguration() {
		assertSame(this.dataSource, this.namedParameterTemplate.getJdbcTemplate().getDataSource());
	}

	@Test
	public void testExecute() throws SQLException {
		given(this.preparedStatement.executeUpdate()).willReturn(1);

		this.params.put("perfId", 1);
		this.params.put("priceId", 1);
		Object result = this.namedParameterTemplate.execute(UPDATE_NAMED_PARAMETERS, this.params,
				(PreparedStatementCallback<Object>) ps -> {
					assertEquals(this.preparedStatement, ps);
					ps.executeUpdate();
					return "result";
				});

		assertEquals("result", result);
		verify(this.connection).prepareStatement(UPDATE_NAMED_PARAMETERS_PARSED);
		verify(this.preparedStatement).setObject(1, 1);
		verify(this.preparedStatement).setObject(2, 1);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Ignore("SPR-16340")
	@Test
	public void testExecuteArray() throws SQLException {
		given(this.preparedStatement.executeUpdate()).willReturn(1);

		List<Integer> typeIds = Arrays.asList(1, 2, 3);

		this.params.put("typeIds", typeIds);
		this.params.put("id", 1);
		Object result = this.namedParameterTemplate.execute(UPDATE_ARRAY_PARAMETERS, this.params,
				(PreparedStatementCallback<Object>) ps -> {
					assertEquals(this.preparedStatement, ps);
					ps.executeUpdate();
					return "result";
				});

		assertEquals("result", result);
		verify(this.connection).prepareStatement(UPDATE_ARRAY_PARAMETERS_PARSED);
		verify(this.preparedStatement).setObject(1, 1);
		verify(this.preparedStatement).setObject(2, 2);
		verify(this.preparedStatement).setObject(3, 3);
		verify(this.preparedStatement).setObject(4, 1);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testExecuteWithTypedParameters() throws SQLException {
		given(this.preparedStatement.executeUpdate()).willReturn(1);

		this.params.put("perfId", new SqlParameterValue(Types.DECIMAL, 1));
		this.params.put("priceId", new SqlParameterValue(Types.INTEGER, 1));
		Object result = this.namedParameterTemplate.execute(UPDATE_NAMED_PARAMETERS, this.params,
				(PreparedStatementCallback<Object>) ps -> {
					assertEquals(this.preparedStatement, ps);
					ps.executeUpdate();
					return "result";
				});

		assertEquals("result", result);
		verify(this.connection).prepareStatement(UPDATE_NAMED_PARAMETERS_PARSED);
		verify(this.preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(this.preparedStatement).setObject(2, 1, Types.INTEGER);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testExecuteNoParameters() throws SQLException {
		given(this.preparedStatement.executeUpdate()).willReturn(1);

		Object result = this.namedParameterTemplate.execute(SELECT_NO_PARAMETERS,
				(PreparedStatementCallback<Object>) ps -> {
					assertEquals(this.preparedStatement, ps);
					ps.executeQuery();
					return "result";
				});

		assertEquals("result", result);
		verify(this.connection).prepareStatement(SELECT_NO_PARAMETERS);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testQueryWithResultSetExtractor() throws SQLException {
		given(this.resultSet.next()).willReturn(true);
		given(this.resultSet.getInt("id")).willReturn(1);
		given(this.resultSet.getString("forename")).willReturn("rod");

		this.params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
		this.params.put("country", "UK");
		Customer cust = this.namedParameterTemplate.query(SELECT_NAMED_PARAMETERS, this.params,
				rs -> {
					rs.next();
					Customer cust1 = new Customer();
					cust1.setId(rs.getInt(COLUMN_NAMES[0]));
					cust1.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust1;
				});

		assertTrue("Customer id was assigned correctly", cust.getId() == 1);
		assertTrue("Customer forename was assigned correctly", cust.getForename().equals("rod"));
		verify(this.connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
		verify(this.preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(this.preparedStatement).setString(2, "UK");
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testQueryWithResultSetExtractorNoParameters() throws SQLException {
		given(this.resultSet.next()).willReturn(true);
		given(this.resultSet.getInt("id")).willReturn(1);
		given(this.resultSet.getString("forename")).willReturn("rod");

		Customer cust = this.namedParameterTemplate.query(SELECT_NO_PARAMETERS,
				rs -> {
					rs.next();
					Customer cust1 = new Customer();
					cust1.setId(rs.getInt(COLUMN_NAMES[0]));
					cust1.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust1;
				});

		assertTrue("Customer id was assigned correctly", cust.getId() == 1);
		assertTrue("Customer forename was assigned correctly", cust.getForename().equals("rod"));
		verify(this.connection).prepareStatement(SELECT_NO_PARAMETERS);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testQueryWithRowCallbackHandler() throws SQLException {
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt("id")).willReturn(1);
		given(this.resultSet.getString("forename")).willReturn("rod");

		this.params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
		this.params.put("country", "UK");
		final List<Customer> customers = new LinkedList<>();
		this.namedParameterTemplate.query(SELECT_NAMED_PARAMETERS, this.params, rs -> {
			Customer cust = new Customer();
			cust.setId(rs.getInt(COLUMN_NAMES[0]));
			cust.setForename(rs.getString(COLUMN_NAMES[1]));
			customers.add(cust);
		});

		assertEquals(1, customers.size());
		assertTrue("Customer id was assigned correctly", customers.get(0).getId() == 1);
		assertTrue("Customer forename was assigned correctly", customers.get(0).getForename().equals("rod"));
		verify(this.connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
		verify(this.preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(this.preparedStatement).setString(2, "UK");
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testQueryWithRowCallbackHandlerNoParameters() throws SQLException {
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt("id")).willReturn(1);
		given(this.resultSet.getString("forename")).willReturn("rod");

		final List<Customer> customers = new LinkedList<>();
		this.namedParameterTemplate.query(SELECT_NO_PARAMETERS, rs -> {
			Customer cust = new Customer();
			cust.setId(rs.getInt(COLUMN_NAMES[0]));
			cust.setForename(rs.getString(COLUMN_NAMES[1]));
			customers.add(cust);
		});

		assertEquals(1, customers.size());
		assertTrue("Customer id was assigned correctly", customers.get(0).getId() == 1);
		assertTrue("Customer forename was assigned correctly", customers.get(0).getForename().equals("rod"));
		verify(this.connection).prepareStatement(SELECT_NO_PARAMETERS);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testQueryWithRowMapper() throws SQLException {
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt("id")).willReturn(1);
		given(this.resultSet.getString("forename")).willReturn("rod");

		this.params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
		this.params.put("country", "UK");
		List<Customer> customers = this.namedParameterTemplate.query(SELECT_NAMED_PARAMETERS, this.params,
				(rs, rownum) -> {
					Customer cust = new Customer();
					cust.setId(rs.getInt(COLUMN_NAMES[0]));
					cust.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust;
				});
		assertEquals(1, customers.size());
		assertTrue("Customer id was assigned correctly", customers.get(0).getId() == 1);
		assertTrue("Customer forename was assigned correctly", customers.get(0).getForename().equals("rod"));
		verify(this.connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
		verify(this.preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(this.preparedStatement).setString(2, "UK");
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testQueryWithRowMapperNoParameters() throws SQLException {
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt("id")).willReturn(1);
		given(this.resultSet.getString("forename")).willReturn("rod");

		List<Customer> customers = this.namedParameterTemplate.query(SELECT_NO_PARAMETERS,
				(rs, rownum) -> {
					Customer cust = new Customer();
					cust.setId(rs.getInt(COLUMN_NAMES[0]));
					cust.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust;
				});
		assertEquals(1, customers.size());
		assertTrue("Customer id was assigned correctly", customers.get(0).getId() == 1);
		assertTrue("Customer forename was assigned correctly", customers.get(0).getForename().equals("rod"));
		verify(this.connection).prepareStatement(SELECT_NO_PARAMETERS);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testQueryForObjectWithRowMapper() throws SQLException {
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt("id")).willReturn(1);
		given(this.resultSet.getString("forename")).willReturn("rod");

		this.params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
		this.params.put("country", "UK");
		Customer cust = this.namedParameterTemplate.queryForObject(SELECT_NAMED_PARAMETERS, this.params,
				(rs, rownum) -> {
					Customer cust1 = new Customer();
					cust1.setId(rs.getInt(COLUMN_NAMES[0]));
					cust1.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust1;
				});
		assertTrue("Customer id was assigned correctly", cust.getId() == 1);
		assertTrue("Customer forename was assigned correctly", cust.getForename().equals("rod"));
		verify(this.connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
		verify(this.preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(this.preparedStatement).setString(2, "UK");
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testUpdate() throws SQLException {
		given(this.preparedStatement.executeUpdate()).willReturn(1);

		this.params.put("perfId", 1);
		this.params.put("priceId", 1);
		int rowsAffected = this.namedParameterTemplate.update(UPDATE_NAMED_PARAMETERS, this.params);

		assertEquals(1, rowsAffected);
		verify(this.connection).prepareStatement(UPDATE_NAMED_PARAMETERS_PARSED);
		verify(this.preparedStatement).setObject(1, 1);
		verify(this.preparedStatement).setObject(2, 1);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testUpdateWithTypedParameters() throws SQLException {
		given(this.preparedStatement.executeUpdate()).willReturn(1);

		this.params.put("perfId", new SqlParameterValue(Types.DECIMAL, 1));
		this.params.put("priceId", new SqlParameterValue(Types.INTEGER, 1));
		int rowsAffected = this.namedParameterTemplate.update(UPDATE_NAMED_PARAMETERS, this.params);

		assertEquals(1, rowsAffected);
		verify(this.connection).prepareStatement(UPDATE_NAMED_PARAMETERS_PARSED);
		verify(this.preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(this.preparedStatement).setObject(2, 1, Types.INTEGER);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testBatchUpdateWithPlainMap() throws Exception {
		@SuppressWarnings("unchecked")
		final Map<String, Integer>[] ids = new Map[2];
		ids[0] = Collections.singletonMap("id", 100);
		ids[1] = Collections.singletonMap("id", 200);
		final int[] rowsAffected = new int[] {1, 2};

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
		given(this.connection.getMetaData()).willReturn(this.databaseMetaData);

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);
		this.namedParameterTemplate = new NamedParameterJdbcTemplate(template);
		assertSame(template, this.namedParameterTemplate.getJdbcTemplate());

		int[] actualRowsAffected = this.namedParameterTemplate.batchUpdate("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id", ids);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);
		verify(this.connection).prepareStatement("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?");
		verify(this.preparedStatement).setObject(1, 100);
		verify(this.preparedStatement).setObject(1, 200);
		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement, atLeastOnce()).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testBatchUpdateWithSqlParameterSource() throws Exception {
		SqlParameterSource[] ids = new SqlParameterSource[2];
		ids[0] = new MapSqlParameterSource("id", 100);
		ids[1] = new MapSqlParameterSource("id", 200);
		final int[] rowsAffected = new int[] {1, 2};

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
		given(this.connection.getMetaData()).willReturn(this.databaseMetaData);

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);
		this.namedParameterTemplate = new NamedParameterJdbcTemplate(template);
		assertSame(template, this.namedParameterTemplate.getJdbcTemplate());

		int[] actualRowsAffected = this.namedParameterTemplate.batchUpdate("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id", ids);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);
		verify(this.connection).prepareStatement("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?");
		verify(this.preparedStatement).setObject(1, 100);
		verify(this.preparedStatement).setObject(1, 200);
		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement, atLeastOnce()).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testBatchUpdateWithSqlParameterSourcePlusTypeInfo() throws Exception {
		SqlParameterSource[] ids = new SqlParameterSource[2];
		ids[0] = new MapSqlParameterSource().addValue("id", 100, Types.NUMERIC);
		ids[1] = new MapSqlParameterSource().addValue("id", 200, Types.NUMERIC);
		final int[] rowsAffected = new int[] {1, 2};

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
		given(this.connection.getMetaData()).willReturn(this.databaseMetaData);

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);
		this.namedParameterTemplate = new NamedParameterJdbcTemplate(template);
		assertSame(template, this.namedParameterTemplate.getJdbcTemplate());

		int[] actualRowsAffected = this.namedParameterTemplate.batchUpdate("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id", ids);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);
		verify(this.connection).prepareStatement("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?");
		verify(this.preparedStatement).setObject(1, 100, Types.NUMERIC);
		verify(this.preparedStatement).setObject(1, 200, Types.NUMERIC);
		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement, atLeastOnce()).close();
		verify(this.connection, atLeastOnce()).close();
	}

}
