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

package org.springframework.jdbc.object;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 * @author Trevor Cook
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class RdbmsOperationTests {

	private final TestRdbmsOperation operation = new TestRdbmsOperation();

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void emptySql() {
		this.exception.expect(InvalidDataAccessApiUsageException.class);
		this.operation.compile();
	}

	@Test
	public void setTypeAfterCompile() {
		this.operation.setDataSource(new DriverManagerDataSource());
		this.operation.setSql("select * from mytable");
		this.operation.compile();
		this.exception.expect(InvalidDataAccessApiUsageException.class);
		this.operation.setTypes(new int[] { Types.INTEGER });
	}

	@Test
	public void declareParameterAfterCompile() {
		this.operation.setDataSource(new DriverManagerDataSource());
		this.operation.setSql("select * from mytable");
		this.operation.compile();
		this.exception.expect(InvalidDataAccessApiUsageException.class);
		this.operation.declareParameter(new SqlParameter(Types.INTEGER));
	}

	@Test
	public void tooFewParameters() {
		this.operation.setSql("select * from mytable");
		this.operation.setTypes(new int[] { Types.INTEGER });
		this.exception.expect(InvalidDataAccessApiUsageException.class);
		this.operation.validateParameters((Object[]) null);
	}

	@Test
	public void tooFewMapParameters() {
		this.operation.setSql("select * from mytable");
		this.operation.setTypes(new int[] { Types.INTEGER });
		this.exception.expect(InvalidDataAccessApiUsageException.class);
		this.operation.validateNamedParameters((Map<String, String>) null);
	}

	@Test
	public void operationConfiguredViaJdbcTemplateMustGetDataSource() throws Exception {
		this.operation.setSql("foo");

		this.exception.expect(InvalidDataAccessApiUsageException.class);
		this.exception.expectMessage(containsString("ataSource"));
		this.operation.compile();
	}

	@Test
	public void tooManyParameters() {
		this.operation.setSql("select * from mytable");
		this.exception.expect(InvalidDataAccessApiUsageException.class);
		this.operation.validateParameters(new Object[] { 1, 2 });
	}

	@Test
	public void unspecifiedMapParameters() {
		this.operation.setSql("select * from mytable");
		Map<String, String> params = new HashMap<>();
		params.put("col1", "value");
		this.exception.expect(InvalidDataAccessApiUsageException.class);
		this.operation.validateNamedParameters(params);
	}

	@Test
	public void compileTwice() {
		this.operation.setDataSource(new DriverManagerDataSource());
		this.operation.setSql("select * from mytable");
		this.operation.setTypes(null);
		this.operation.compile();
		this.operation.compile();
	}

	@Test
	public void emptyDataSource() {
		SqlOperation operation = new SqlOperation() {};
		operation.setSql("select * from mytable");
		this.exception.expect(InvalidDataAccessApiUsageException.class);
		operation.compile();
	}

	@Test
	public void parameterPropagation() {
		SqlOperation operation = new SqlOperation() {};
		DataSource ds = new DriverManagerDataSource();
		operation.setDataSource(ds);
		operation.setFetchSize(10);
		operation.setMaxRows(20);
		JdbcTemplate jt = operation.getJdbcTemplate();
		assertEquals(ds, jt.getDataSource());
		assertEquals(10, jt.getFetchSize());
		assertEquals(20, jt.getMaxRows());
	}

	@Test
	public void validateInOutParameter() {
		this.operation.setDataSource(new DriverManagerDataSource());
		this.operation.setSql("DUMMY_PROC");
		this.operation.declareParameter(new SqlOutParameter("DUMMY_OUT_PARAM", Types.VARCHAR));
		this.operation.declareParameter(new SqlInOutParameter("DUMMY_IN_OUT_PARAM", Types.VARCHAR));
		this.operation.validateParameters(new Object[] {"DUMMY_VALUE1", "DUMMY_VALUE2"});
	}

	@Test
	public void parametersSetWithList() {
		DataSource ds = new DriverManagerDataSource();
		this.operation.setDataSource(ds);
		this.operation.setSql("select * from mytable where one = ? and two = ?");
		this.operation.setParameters(new SqlParameter[] {
				new SqlParameter("one", Types.NUMERIC),
				new SqlParameter("two", Types.NUMERIC)});
		this.operation.afterPropertiesSet();
		this.operation.validateParameters(new Object[] { 1, "2" });
		assertEquals(2, this.operation.getDeclaredParameters().size());
	}


	private static class TestRdbmsOperation extends RdbmsOperation {

		@Override
		protected void compileInternal() {
		}
	}

}
