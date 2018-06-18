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

package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 02.08.2004
 */
public class RowMapperTests {

	private final Connection connection = mock(Connection.class);

	private final Statement statement = mock(Statement.class);

	private final PreparedStatement preparedStatement = mock(PreparedStatement.class);

	private final ResultSet resultSet = mock(ResultSet.class);

	private final JdbcTemplate template = new JdbcTemplate();

	private final RowMapper<TestBean> testRowMapper =
			(rs, rowNum) -> new TestBean(rs.getString(1), rs.getInt(2));

	private List<TestBean> result;

	@Before
	public void setUp() throws SQLException {
		given(this.connection.createStatement()).willReturn(this.statement);
		given(this.connection.prepareStatement(anyString())).willReturn(this.preparedStatement);
		given(this.statement.executeQuery(anyString())).willReturn(this.resultSet);
		given(this.preparedStatement.executeQuery()).willReturn(this.resultSet);
		given(this.resultSet.next()).willReturn(true, true, false);
		given(this.resultSet.getString(1)).willReturn("tb1", "tb2");
		given(this.resultSet.getInt(2)).willReturn(1, 2);

		this.template.setDataSource(new SingleConnectionDataSource(this.connection, false));
		this.template.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		this.template.afterPropertiesSet();
	}

	@After
	public void verifyClosed() throws Exception {
		verify(this.resultSet).close();
		// verify(connection).close();
	}

	@After
	public void verifyResults() {
		assertNotNull(this.result);
		assertEquals(2, this.result.size());
		TestBean testBean1 = this.result.get(0);
		TestBean testBean2 = this.result.get(1);
		assertEquals("tb1", testBean1.getName());
		assertEquals("tb2", testBean2.getName());
		assertEquals(1, testBean1.getAge());
		assertEquals(2, testBean2.getAge());
	}

	@Test
	public void staticQueryWithRowMapper() throws SQLException {
		this.result = this.template.query("some SQL", this.testRowMapper);
		verify(this.statement).close();
	}

	@Test
	public void preparedStatementCreatorWithRowMapper() throws SQLException {
		this.result = this.template.query((con) -> this.preparedStatement, this.testRowMapper);
		verify(this.preparedStatement).close();
	}

	@Test
	public void preparedStatementSetterWithRowMapper() throws SQLException {
		this.result = this.template.query("some SQL", ps -> ps.setString(1, "test"), this.testRowMapper);
		verify(this.preparedStatement).setString(1, "test");
		verify(this.preparedStatement).close();
	}

	@Test
	public void queryWithArgsAndRowMapper() throws SQLException {
		this.result = this.template.query("some SQL", new Object[] { "test1", "test2" }, this.testRowMapper);
		this.preparedStatement.setString(1, "test1");
		this.preparedStatement.setString(2, "test2");
		this.preparedStatement.close();
	}

	@Test
	public void queryWithArgsAndTypesAndRowMapper() throws SQLException {
		this.result = this.template.query("some SQL",
				new Object[] { "test1", "test2" },
				new int[] { Types.VARCHAR, Types.VARCHAR },
				this.testRowMapper);
		verify(this.preparedStatement).setString(1, "test1");
		verify(this.preparedStatement).setString(2, "test2");
		verify(this.preparedStatement).close();
	}

}
