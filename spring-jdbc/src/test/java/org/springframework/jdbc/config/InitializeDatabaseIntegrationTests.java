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

package org.springframework.jdbc.config;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class InitializeDatabaseIntegrationTests {

	private String enabled;

	private ClassPathXmlApplicationContext context;


	@Before
	public void init() {
		this.enabled = System.setProperty("ENABLED", "true");
	}

	@After
	public void after() {
		if (this.enabled != null) {
			System.setProperty("ENABLED", this.enabled);
		}
		else {
			System.clearProperty("ENABLED");
		}
		if (this.context != null) {
			this.context.close();
		}
	}


	@Test
	public void testCreateEmbeddedDatabase() throws Exception {
		this.context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-config.xml");
		assertCorrectSetup(this.context.getBean("dataSource", DataSource.class));
	}

	@Test(expected = BadSqlGrammarException.class)
	public void testDisableCreateEmbeddedDatabase() throws Exception {
		System.setProperty("ENABLED", "false");
		this.context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-config.xml");
		assertCorrectSetup(this.context.getBean("dataSource", DataSource.class));
	}

	@Test
	public void testIgnoreFailedDrops() throws Exception {
		this.context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-fail-config.xml");
		assertCorrectSetup(this.context.getBean("dataSource", DataSource.class));
	}

	@Test
	public void testScriptNameWithPattern() throws Exception {
		this.context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-pattern-config.xml");
		DataSource dataSource = this.context.getBean("dataSource", DataSource.class);
		assertCorrectSetup(dataSource);
		JdbcTemplate t = new JdbcTemplate(dataSource);
		assertEquals("Dave", t.queryForObject("select name from T_TEST", String.class));
	}

	@Test
	public void testScriptNameWithPlaceholder() throws Exception {
		this.context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-placeholder-config.xml");
		DataSource dataSource = this.context.getBean("dataSource", DataSource.class);
		assertCorrectSetup(dataSource);
	}

	@Test
	public void testScriptNameWithExpressions() throws Exception {
		this.context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-expression-config.xml");
		DataSource dataSource = this.context.getBean("dataSource", DataSource.class);
		assertCorrectSetup(dataSource);
	}

	@Test
	public void testCacheInitialization() throws Exception {
		this.context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-cache-config.xml");
		assertCorrectSetup(this.context.getBean("dataSource", DataSource.class));
		CacheData cache = this.context.getBean(CacheData.class);
		assertEquals(1, cache.getCachedData().size());
	}

	private void assertCorrectSetup(DataSource dataSource) {
		JdbcTemplate jt = new JdbcTemplate(dataSource);
		assertEquals(1, jt.queryForObject("select count(*) from T_TEST", Integer.class).intValue());
	}


	public static class CacheData implements InitializingBean {

		private JdbcTemplate jdbcTemplate;

		private List<Map<String,Object>> cache;

		public void setDataSource(DataSource dataSource) {
			this.jdbcTemplate = new JdbcTemplate(dataSource);
		}

		public List<Map<String,Object>> getCachedData() {
			return this.cache;
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			this.cache = this.jdbcTemplate.queryForList("SELECT * FROM T_TEST");
		}
	}

}
