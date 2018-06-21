/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jdbc.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 17.12.2003
 */
public class DefaultLobHandlerTests {

	private ResultSet rs = mock(ResultSet.class);

	private PreparedStatement ps = mock(PreparedStatement.class);

	private LobHandler lobHandler = new DefaultLobHandler();

	private LobCreator lobCreator = this.lobHandler.getLobCreator();


	@Test
	public void testGetBlobAsBytes() throws SQLException {
		this.lobHandler.getBlobAsBytes(this.rs, 1);
		verify(this.rs).getBytes(1);
	}

	@Test
	public void testGetBlobAsBinaryStream() throws SQLException {
		this.lobHandler.getBlobAsBinaryStream(this.rs, 1);
		verify(this.rs).getBinaryStream(1);
	}

	@Test
	public void testGetClobAsString() throws SQLException {
		this.lobHandler.getClobAsString(this.rs, 1);
		verify(this.rs).getString(1);
	}

	@Test
	public void testGetClobAsAsciiStream() throws SQLException {
		this.lobHandler.getClobAsAsciiStream(this.rs, 1);
		verify(this.rs).getAsciiStream(1);
	}

	@Test
	public void testGetClobAsCharacterStream() throws SQLException {
		this.lobHandler.getClobAsCharacterStream(this.rs, 1);
		verify(this.rs).getCharacterStream(1);
	}

	@Test
	public void testSetBlobAsBytes() throws SQLException {
		byte[] content = "testContent".getBytes();
		this.lobCreator.setBlobAsBytes(this.ps, 1, content);
		verify(this.ps).setBytes(1, content);
	}

	@Test
	public void testSetBlobAsBinaryStream() throws SQLException, IOException {
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());
		this.lobCreator.setBlobAsBinaryStream(this.ps, 1, bis, 11);
		verify(this.ps).setBinaryStream(1, bis, 11);
	}

	@Test
	public void testSetBlobAsBinaryStreamWithoutLength() throws SQLException, IOException {
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());
		this.lobCreator.setBlobAsBinaryStream(this.ps, 1, bis, -1);
		verify(this.ps).setBinaryStream(1, bis);
	}

	@Test
	public void testSetClobAsString() throws SQLException, IOException {
		String content = "testContent";
		this.lobCreator.setClobAsString(this.ps, 1, content);
		verify(this.ps).setString(1, content);
	}

	@Test
	public void testSetClobAsAsciiStream() throws SQLException, IOException {
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());
		this.lobCreator.setClobAsAsciiStream(this.ps, 1, bis, 11);
		verify(this.ps).setAsciiStream(1, bis, 11);
	}

	@Test
	public void testSetClobAsAsciiStreamWithoutLength() throws SQLException, IOException {
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());
		this.lobCreator.setClobAsAsciiStream(this.ps, 1, bis, -1);
		verify(this.ps).setAsciiStream(1, bis);
	}

	@Test
	public void testSetClobAsCharacterStream() throws SQLException, IOException {
		Reader str = new StringReader("testContent");
		this.lobCreator.setClobAsCharacterStream(this.ps, 1, str, 11);
		verify(this.ps).setCharacterStream(1, str, 11);
	}

	@Test
	public void testSetClobAsCharacterStreamWithoutLength() throws SQLException, IOException {
		Reader str = new StringReader("testContent");
		this.lobCreator.setClobAsCharacterStream(this.ps, 1, str, -1);
		verify(this.ps).setCharacterStream(1, str);
	}

}
