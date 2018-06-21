/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.jdbc.core.support;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test cases for the sql lob value:
 *
 * BLOB:
 *   1. Types.BLOB: setBlobAsBytes (byte[])
 *   2. String: setBlobAsBytes (byte[])
 *   3. else: IllegalArgumentException
 *
 * CLOB:
 *   4. String or NULL: setClobAsString (String)
 *   5. InputStream: setClobAsAsciiStream (InputStream)
 *   6. Reader: setClobAsCharacterStream (Reader)
 *   7. else: IllegalArgumentException
 *
 * @author Alef Arendsen
 */
public class SqlLobValueTests  {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private PreparedStatement preparedStatement;
	private LobHandler handler;
	private LobCreator creator;

	@Captor
	private ArgumentCaptor<InputStream> inputStreamCaptor;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		this.preparedStatement = mock(PreparedStatement.class);
		this.handler = mock(LobHandler.class);
		this.creator = mock(LobCreator.class);
		given(this.handler.getLobCreator()).willReturn(this.creator);
	}

	@Test
	public void test1() throws SQLException {
		byte[] testBytes = "Bla".getBytes();
		SqlLobValue lob = new SqlLobValue(testBytes, this.handler);
		lob.setTypeValue(this.preparedStatement, 1, Types.BLOB, "test");
		verify(this.creator).setBlobAsBytes(this.preparedStatement, 1, testBytes);
	}

	@Test
	public void test2() throws SQLException {
		String testString = "Bla";
		SqlLobValue lob = new SqlLobValue(testString, this.handler);
		lob.setTypeValue(this.preparedStatement, 1, Types.BLOB, "test");
		verify(this.creator).setBlobAsBytes(this.preparedStatement, 1, testString.getBytes());
	}

	@Test
	public void test3() throws SQLException {
		SqlLobValue lob = new SqlLobValue(new InputStreamReader(new ByteArrayInputStream("Bla".getBytes())), 12);
		this.thrown.expect(IllegalArgumentException.class);
		lob.setTypeValue(this.preparedStatement, 1, Types.BLOB, "test");
	}

	@Test
	public void test4() throws SQLException {
		String testContent = "Bla";
		SqlLobValue lob = new SqlLobValue(testContent, this.handler);
		lob.setTypeValue(this.preparedStatement, 1, Types.CLOB, "test");
		verify(this.creator).setClobAsString(this.preparedStatement, 1, testContent);
	}

	@Test
	public void test5() throws Exception {
		byte[] testContent = "Bla".getBytes();
		SqlLobValue lob = new SqlLobValue(new ByteArrayInputStream(testContent), 3, this.handler);
		lob.setTypeValue(this.preparedStatement, 1, Types.CLOB, "test");
		verify(this.creator).setClobAsAsciiStream(eq(this.preparedStatement), eq(1), this.inputStreamCaptor.capture(), eq(3));
		byte[] bytes = new byte[3];
		this.inputStreamCaptor.getValue().read(bytes);
		assertThat(bytes, equalTo(testContent));
	}

	@Test
	public void test6() throws SQLException {
		byte[] testContent = "Bla".getBytes();
		ByteArrayInputStream bais = new ByteArrayInputStream(testContent);
		InputStreamReader reader = new InputStreamReader(bais);
		SqlLobValue lob = new SqlLobValue(reader, 3, this.handler);
		lob.setTypeValue(this.preparedStatement, 1, Types.CLOB, "test");
		verify(this.creator).setClobAsCharacterStream(eq(this.preparedStatement), eq(1), eq(reader), eq(3));
	}

	@Test
	public void test7() throws SQLException {
		SqlLobValue lob = new SqlLobValue("bla".getBytes());
		this.thrown.expect(IllegalArgumentException.class);
		lob.setTypeValue(this.preparedStatement, 1, Types.CLOB, "test");
	}

	@Test
	public void testOtherConstructors() throws SQLException {
		// a bit BS, but we need to test them, as long as they don't throw exceptions

		SqlLobValue lob = new SqlLobValue("bla");
		lob.setTypeValue(this.preparedStatement, 1, Types.CLOB, "test");

		try {
			lob = new SqlLobValue("bla".getBytes());
			lob.setTypeValue(this.preparedStatement, 1, Types.CLOB, "test");
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

		lob = new SqlLobValue(new ByteArrayInputStream("bla".getBytes()), 3);
		lob.setTypeValue(this.preparedStatement, 1, Types.CLOB, "test");

		lob = new SqlLobValue(new InputStreamReader(new ByteArrayInputStream(
				"bla".getBytes())), 3);
		lob.setTypeValue(this.preparedStatement, 1, Types.CLOB, "test");

		// same for BLOB
		lob = new SqlLobValue("bla");
		lob.setTypeValue(this.preparedStatement, 1, Types.BLOB, "test");

		lob = new SqlLobValue("bla".getBytes());
		lob.setTypeValue(this.preparedStatement, 1, Types.BLOB, "test");

		lob = new SqlLobValue(new ByteArrayInputStream("bla".getBytes()), 3);
		lob.setTypeValue(this.preparedStatement, 1, Types.BLOB, "test");

		lob = new SqlLobValue(new InputStreamReader(new ByteArrayInputStream(
				"bla".getBytes())), 3);

		try {
			lob.setTypeValue(this.preparedStatement, 1, Types.BLOB, "test");
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test
	public void testCorrectCleanup() throws SQLException {
		SqlLobValue lob = new SqlLobValue("Bla", this.handler);
		lob.setTypeValue(this.preparedStatement, 1, Types.CLOB, "test");
		lob.cleanup();
		verify(this.creator).setClobAsString(this.preparedStatement, 1, "Bla");
		verify(this.creator).close();
	}

	@Test
	public void testOtherSqlType() throws SQLException {
		SqlLobValue lob = new SqlLobValue("Bla", this.handler);
		this.thrown.expect(IllegalArgumentException.class);
		lob.setTypeValue(this.preparedStatement, 1, Types.SMALLINT, "test");
	}

}
