/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.support;

import java.sql.Types;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;

/**
 * Unit tests for {@link JdbcUtils}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class JdbcUtilsTests {

	@Test
	public void commonDatabaseName() {
		assertThat((Object) JdbcUtils.commonDatabaseName("Oracle")).isEqualTo("Oracle");
		assertThat((Object) JdbcUtils.commonDatabaseName("DB2-for-Spring")).isEqualTo("DB2");
		assertThat((Object) JdbcUtils.commonDatabaseName("Sybase SQL Server")).isEqualTo("Sybase");
		assertThat((Object) JdbcUtils.commonDatabaseName("Adaptive Server Enterprise")).isEqualTo("Sybase");
		assertThat((Object) JdbcUtils.commonDatabaseName("MySQL")).isEqualTo("MySQL");
	}

	@Test
	public void resolveTypeName() {
		assertThat((Object) JdbcUtils.resolveTypeName(Types.VARCHAR)).isEqualTo("VARCHAR");
		assertThat((Object) JdbcUtils.resolveTypeName(Types.NUMERIC)).isEqualTo("NUMERIC");
		assertThat((Object) JdbcUtils.resolveTypeName(Types.INTEGER)).isEqualTo("INTEGER");
		assertNull(JdbcUtils.resolveTypeName(JdbcUtils.TYPE_UNKNOWN));
	}

	@Test
	public void convertUnderscoreNameToPropertyName() {
		assertThat((Object) JdbcUtils.convertUnderscoreNameToPropertyName("MY_NAME")).isEqualTo("myName");
		assertThat((Object) JdbcUtils.convertUnderscoreNameToPropertyName("yOUR_nAME")).isEqualTo("yourName");
		assertThat((Object) JdbcUtils.convertUnderscoreNameToPropertyName("a_name")).isEqualTo("AName");
		assertThat((Object) JdbcUtils.convertUnderscoreNameToPropertyName("someone_elses_name")).isEqualTo("someoneElsesName");
	}

}
