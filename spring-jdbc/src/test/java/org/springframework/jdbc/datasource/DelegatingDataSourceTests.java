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

package org.springframework.jdbc.datasource;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import javax.sql.DataSource;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests for {@link DelegatingDataSource}.
 *
 * @author Phillip Webb
 */
public class DelegatingDataSourceTests {

	private final DataSource delegate = mock(DataSource.class);

	private DelegatingDataSource dataSource = new DelegatingDataSource(this.delegate);

	@Test
	public void shouldDelegateGetConnection() throws Exception {
		Connection connection = mock(Connection.class);
		given(this.delegate.getConnection()).willReturn(connection);
		assertThat(this.dataSource.getConnection(), is(connection));
	}

	@Test
	public void shouldDelegateGetConnectionWithUsernameAndPassword() throws Exception {
		Connection connection = mock(Connection.class);
		String username = "username";
		String password = "password";
		given(this.delegate.getConnection(username, password)).willReturn(connection);
		assertThat(this.dataSource.getConnection(username, password), is(connection));
	}

	@Test
	public void shouldDelegateGetLogWriter() throws Exception {
		PrintWriter writer = new PrintWriter(new ByteArrayOutputStream());
		given(this.delegate.getLogWriter()).willReturn(writer);
		assertThat(this.dataSource.getLogWriter(), is(writer));
	}

	@Test
	public void shouldDelegateSetLogWriter() throws Exception {
		PrintWriter writer = new PrintWriter(new ByteArrayOutputStream());
		this.dataSource.setLogWriter(writer);
		verify(this.delegate).setLogWriter(writer);
	}

	@Test
	public void shouldDelegateGetLoginTimeout() throws Exception {
		int timeout = 123;
		given(this.delegate.getLoginTimeout()).willReturn(timeout);
		assertThat(this.dataSource.getLoginTimeout(), is(timeout));
	}

	@Test
	public void shouldDelegateSetLoginTimeoutWithSeconds() throws Exception {
		int timeout = 123;
		this.dataSource.setLoginTimeout(timeout);
		verify(this.delegate).setLoginTimeout(timeout);
	}

	@Test
	public void shouldDelegateUnwrapWithoutImplementing() throws Exception {
		ExampleWrapper wrapper = mock(ExampleWrapper.class);
		given(this.delegate.unwrap(ExampleWrapper.class)).willReturn(wrapper);
		assertThat(this.dataSource.unwrap(ExampleWrapper.class), is(wrapper));
	}

	@Test
	public void shouldDelegateUnwrapImplementing() throws Exception {
		this.dataSource = new DelegatingDataSourceWithWrapper();
		assertThat(this.dataSource.unwrap(ExampleWrapper.class),
				is((ExampleWrapper) this.dataSource));
	}

	@Test
	public void shouldDelegateIsWrapperForWithoutImplementing() throws Exception {
		given(this.delegate.isWrapperFor(ExampleWrapper.class)).willReturn(true);
		assertThat(this.dataSource.isWrapperFor(ExampleWrapper.class), is(true));
	}

	@Test
	public void shouldDelegateIsWrapperForImplementing() throws Exception {
		this.dataSource = new DelegatingDataSourceWithWrapper();
		assertThat(this.dataSource.isWrapperFor(ExampleWrapper.class), is(true));
	}

	public static interface ExampleWrapper {
	}

	private static class DelegatingDataSourceWithWrapper extends DelegatingDataSource
			implements ExampleWrapper {
	}
}
