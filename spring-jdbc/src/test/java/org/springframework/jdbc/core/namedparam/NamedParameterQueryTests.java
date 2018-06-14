/*
 * Copyright 2002-2017 the original author or authors.
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

/**
 * @author Thomas Risberg
 * @author Phillip Webb
 */
public class NamedParameterQueryTests {

	private DataSource dataSource;

	private Connection connection;

	private PreparedStatement preparedStatement;

	private ResultSet resultSet;

	private ResultSetMetaData resultSetMetaData;

	private NamedParameterJdbcTemplate template;


	@Before
	public void setup() throws Exception {
		this.connection = mock(Connection.class);
		this.dataSource = mock(DataSource.class);
		this.preparedStatement = mock(PreparedStatement.class);
		this.resultSet = mock(ResultSet.class);
		this.resultSetMetaData = mock(ResultSetMetaData.class);
		this.template = new NamedParameterJdbcTemplate(this.dataSource);
		given(this.dataSource.getConnection()).willReturn(this.connection);
		given(this.resultSetMetaData.getColumnCount()).willReturn(1);
		given(this.resultSetMetaData.getColumnLabel(1)).willReturn("age");
		given(this.connection.prepareStatement(anyString())).willReturn(this.preparedStatement);
		given(this.preparedStatement.executeQuery()).willReturn(this.resultSet);
	}

	@After
	public void verifyClose() throws Exception {
		verify(this.preparedStatement).close();
		verify(this.resultSet).close();
		verify(this.connection).close();
	}


	@Test
	public void testQueryForListWithParamMap() throws Exception {
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSet.next()).willReturn(true, true, false);
		given(this.resultSet.getObject(1)).willReturn(11, 12);

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", 3);
		List<Map<String, Object>> li = this.template.queryForList(
				"SELECT AGE FROM CUSTMR WHERE ID < :id", params);

		assertEquals("All rows returned", 2, li.size());
		assertEquals("First row is Integer", 11,
				((Integer) li.get(0).get("age")).intValue());
		assertEquals("Second row is Integer", 12,
				((Integer) li.get(1).get("age")).intValue());

		verify(this.connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(this.preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForListWithParamMapAndEmptyResult() throws Exception {
		given(this.resultSet.next()).willReturn(false);

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", 3);
		List<Map<String, Object>> li = this.template.queryForList(
				"SELECT AGE FROM CUSTMR WHERE ID < :id", params);

		assertEquals("All rows returned", 0, li.size());
		verify(this.connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(this.preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForListWithParamMapAndSingleRowAndColumn() throws Exception {
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1)).willReturn(11);

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", 3);
		List<Map<String, Object>> li = this.template.queryForList(
				"SELECT AGE FROM CUSTMR WHERE ID < :id", params);

		assertEquals("All rows returned", 1, li.size());
		assertEquals("First row is Integer", 11,
				((Integer) li.get(0).get("age")).intValue());
		verify(this.connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(this.preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForListWithParamMapAndIntegerElementAndSingleRowAndColumn()
			throws Exception {
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(11);

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", 3);
		List<Integer> li = this.template.queryForList("SELECT AGE FROM CUSTMR WHERE ID < :id",
				params, Integer.class);

		assertEquals("All rows returned", 1, li.size());
		assertEquals("First row is Integer", 11, li.get(0).intValue());
		verify(this.connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(this.preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForMapWithParamMapAndSingleRowAndColumn() throws Exception {
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1)).willReturn(11);

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", 3);
		Map<String, Object> map = this.template.queryForMap("SELECT AGE FROM CUSTMR WHERE ID < :id", params);

		assertEquals("Row is Integer", 11, ((Integer) map.get("age")).intValue());
		verify(this.connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(this.preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForObjectWithParamMapAndRowMapper() throws Exception {
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", 3);
		Object o = this.template.queryForObject("SELECT AGE FROM CUSTMR WHERE ID = :id",
				params, new RowMapper<Object>() {
			@Override
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getInt(1);
			}
		});

		assertTrue("Correct result type", o instanceof Integer);
		verify(this.connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(this.preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForObjectWithMapAndInteger() throws Exception {
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);

		Map<String, Object> params = new HashMap<>();
		params.put("id", 3);
		Object o = this.template.queryForObject("SELECT AGE FROM CUSTMR WHERE ID = :id",
				params, Integer.class);

		assertTrue("Correct result type", o instanceof Integer);
		verify(this.connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(this.preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForObjectWithParamMapAndInteger() throws Exception {
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", 3);
		Object o = this.template.queryForObject("SELECT AGE FROM CUSTMR WHERE ID = :id",
				params, Integer.class);

		assertTrue("Correct result type", o instanceof Integer);
		verify(this.connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(this.preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForObjectWithParamMapAndList() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID IN (:ids)";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID IN (?, ?)";
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("ids", Arrays.asList(3, 4));
		Object o = this.template.queryForObject(sql, params, Integer.class);

		assertTrue("Correct result type", o instanceof Integer);
		verify(this.connection).prepareStatement(sqlToUse);
		verify(this.preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForObjectWithParamMapAndListOfExpressionLists() throws Exception {
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);

		MapSqlParameterSource params = new MapSqlParameterSource();
		List<Object[]> l1 = new ArrayList<>();
		l1.add(new Object[] {3, "Rod"});
		l1.add(new Object[] {4, "Juergen"});
		params.addValue("multiExpressionList", l1);
		Object o = this.template.queryForObject(
				"SELECT AGE FROM CUSTMR WHERE (ID, NAME) IN (:multiExpressionList)",
				params, Integer.class);

		assertTrue("Correct result type", o instanceof Integer);
		verify(this.connection).prepareStatement(
				"SELECT AGE FROM CUSTMR WHERE (ID, NAME) IN ((?, ?), (?, ?))");
		verify(this.preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForIntWithParamMap() throws Exception {
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", 3);
		int i = this.template.queryForObject("SELECT AGE FROM CUSTMR WHERE ID = :id", params, Integer.class).intValue();

		assertEquals("Return of an int", 22, i);
		verify(this.connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(this.preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForLongWithParamBean() throws Exception {
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getLong(1)).willReturn(87L);

		BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(new ParameterBean(3));
		long l = this.template.queryForObject("SELECT AGE FROM CUSTMR WHERE ID = :id", params, Long.class).longValue();

		assertEquals("Return of a long", 87, l);
		verify(this.connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(this.preparedStatement).setObject(1, 3, Types.INTEGER);
	}

	@Test
	public void testQueryForLongWithParamBeanWithCollection() throws Exception {
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getLong(1)).willReturn(87L);

		BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(new ParameterCollectionBean(3, 5));
		long l = this.template.queryForObject("SELECT AGE FROM CUSTMR WHERE ID IN (:ids)", params, Long.class).longValue();

		assertEquals("Return of a long", 87, l);
		verify(this.connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID IN (?, ?)");
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.preparedStatement).setObject(2, 5);
	}


	static class ParameterBean {

		private final int id;

		public ParameterBean(int id) {
			this.id = id;
		}

		public int getId() {
			return this.id;
		}
	}


	static class ParameterCollectionBean {

		private final Collection<Integer> ids;

		public ParameterCollectionBean(Integer... ids) {
			this.ids = Arrays.asList(ids);
		}

		public Collection<Integer> getIds() {
			return this.ids;
		}
	}

}
