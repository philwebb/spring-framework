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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.Customer;
import org.springframework.jdbc.datasource.TestDataSourceWrapper;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class GenericSqlQueryTests {

	private static final String SELECT_ID_FORENAME_NAMED_PARAMETERS_PARSED =
			"select id, forename from custmr where id = ? and country = ?";

	private DefaultListableBeanFactory beanFactory;

	private Connection connection;

	private PreparedStatement preparedStatement;

	private ResultSet resultSet;


	@Before
	public void setUp() throws Exception {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				new ClassPathResource("org/springframework/jdbc/object/GenericSqlQueryTests-context.xml"));
		DataSource dataSource = mock(DataSource.class);
		this.connection = mock(Connection.class);
		this.preparedStatement = mock(PreparedStatement.class);
		this.resultSet = mock(ResultSet.class);
		given(dataSource.getConnection()).willReturn(this.connection);
		TestDataSourceWrapper testDataSource = (TestDataSourceWrapper) this.beanFactory.getBean("dataSource");
		testDataSource.setTarget(dataSource);
	}

	@Test
	public void testCustomerQueryWithPlaceholders() throws SQLException {
		SqlQuery<?> query = (SqlQuery<?>) this.beanFactory.getBean("queryWithPlaceholders");
		doTestCustomerQuery(query, false);
	}

	@Test
	public void testCustomerQueryWithNamedParameters() throws SQLException {
		SqlQuery<?> query = (SqlQuery<?>) this.beanFactory.getBean("queryWithNamedParameters");
		doTestCustomerQuery(query, true);
	}

	@Test
	public void testCustomerQueryWithRowMapperInstance() throws SQLException {
		SqlQuery<?> query = (SqlQuery<?>) this.beanFactory.getBean("queryWithRowMapperBean");
		doTestCustomerQuery(query, true);
	}

	private void doTestCustomerQuery(SqlQuery<?> query, boolean namedParameters) throws SQLException {
		given(this.resultSet.next()).willReturn(true);
		given(this.resultSet.getInt("id")).willReturn(1);
		given(this.resultSet.getString("forename")).willReturn("rod");
		given(this.resultSet.next()).willReturn(true, false);
		given(this.preparedStatement.executeQuery()).willReturn(this.resultSet);
		given(this.connection.prepareStatement(SELECT_ID_FORENAME_NAMED_PARAMETERS_PARSED)).willReturn(this.preparedStatement);

		List<?> queryResults;
		if (namedParameters) {
			Map<String, Object> params = new HashMap<>(2);
			params.put("id", 1);
			params.put("country", "UK");
			queryResults = query.executeByNamedParam(params);
		}
		else {
			Object[] params = new Object[] {1, "UK"};
			queryResults = query.execute(params);
		}
		assertTrue("Customer was returned correctly", queryResults.size() == 1);
		Customer cust = (Customer) queryResults.get(0);
		assertTrue("Customer id was assigned correctly", cust.getId() == 1);
		assertTrue("Customer forename was assigned correctly", cust.getForename().equals("rod"));

		verify(this.resultSet).close();
		verify(this.preparedStatement).setObject(1, 1, Types.INTEGER);
		verify(this.preparedStatement).setString(2, "UK");
		verify(this.preparedStatement).close();
	}

}
