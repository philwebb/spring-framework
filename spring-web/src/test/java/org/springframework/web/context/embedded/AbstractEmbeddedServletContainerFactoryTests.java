/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.embedded;

import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.ServletContextInitializer;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Base for testing classes that extends {@link AbstractEmbeddedServletContainerFactory}.
 *
 * @author Phillip Webb
 */
public abstract class AbstractEmbeddedServletContainerFactoryTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	protected EmbeddedServletContainer container;


	@After
	public void teardown() {
		if (this.container != null) {
			try {
				this.container.stop();
			}
			catch (Exception e) {
			}
		}
	}


	@Test
	public void startServlet() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory.getEmbdeddedServletContainer(exampleServletRegistration());
		this.container.start();
		assertThat(getResponse("http://localhost:8080/hello"), equalTo("Hello World"));
	}

	@Test
	public void stopServlet() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory.getEmbdeddedServletContainer(exampleServletRegistration());
		this.container.start();
		this.container.stop();
		thrown.expect(ConnectException.class);
		getResponse("http://localhost:8080/hello");
	}

	@Test
	public void startServletAndFilter() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory.getEmbdeddedServletContainer(
				exampleServletRegistration(),
				new FilterRegistrationBean(new ExampleFilter()));
		this.container.start();
		assertThat(getResponse("http://localhost:8080/hello"), equalTo("[Hello World]"));
	}

	@Test
	public void startBlocksUntilReadyToServe() throws Exception {
		Assume.group(TestGroup.LONG_RUNNING);
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		final Date[] date = new Date[1];
		this.container = factory.getEmbdeddedServletContainer(new ServletContextInitializer() {
			@Override
			public void onStartup(ServletContext servletContext) throws ServletException {
				try {
					Thread.sleep(500);
					date[0] = new Date();
				}
				catch (InterruptedException ex) {
					throw new ServletException(ex);
				}
			}
		});
		this.container.start();
		assertThat(date[0], notNullValue());
	}

	@Test
	public void specificPort() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setPort(8081);
		this.container = factory.getEmbdeddedServletContainer(exampleServletRegistration());
		this.container.start();
		assertThat(getResponse("http://localhost:8081/hello"), equalTo("Hello World"));
	}


	@Test
	public void specificContextRoot() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setContextPath("/say");
		this.container = factory.getEmbdeddedServletContainer(exampleServletRegistration());
		this.container.start();
		assertThat(getResponse("http://localhost:8080/say/hello"), equalTo("Hello World"));
	}

	@Test
	public void contextPathMustStartWithSlash() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("ContextPath must start with '/ and not end with '/'");
		getFactory().setContextPath("missingslash");
	}

	@Test
	public void contextPathMustNotEndWithSlash() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("ContextPath must start with '/ and not end with '/'");
		getFactory().setContextPath("extraslash/");
	}

	@Test
	public void contextRootPathMustNotBeSlash() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Root ContextPath must be specified using an empty string");
		getFactory().setContextPath("/");
	}

	@Test
	public void doubleStart() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory.getEmbdeddedServletContainer(exampleServletRegistration());
		this.container.start();
		this.container.start();
		assertThat(getResponse("http://localhost:8080/hello"), equalTo("Hello World"));
	}

	@Test
	public void doubleStop() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory.getEmbdeddedServletContainer(exampleServletRegistration());
		this.container.start();
		this.container.stop();
		this.container.stop();
	}

	@Test
	public void multipleConfigurations() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		ServletContextInitializer[] initializers = new ServletContextInitializer[6];
		for(int i=0; i<initializers.length; i++) {
			initializers[i] = mock(ServletContextInitializer.class);
		}
		factory.setInitializers(Arrays.asList(initializers[2], initializers[3]));
		factory.addInitializers(initializers[4], initializers[5]);
		this.container = factory.getEmbdeddedServletContainer(initializers[0], initializers[1]);
		this.container.start();
		InOrder ordered = inOrder((Object[]) initializers);
		for (ServletContextInitializer initializer : initializers) {
			ordered.verify(initializer).onStartup((ServletContext) anyObject());
		}
	}

	@Test
	public void documentRoot() throws Exception {
		FileCopyUtils.copy("test", new FileWriter(temporaryFolder.newFile("test.txt")));
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setDocumentRoot(temporaryFolder.getRoot());
		this.container = factory.getEmbdeddedServletContainer();
		this.container.start();
		assertThat(getResponse("http://localhost:8080/test.txt"), equalTo("test"));
	}

	protected String getResponse(String url) throws IOException, URISyntaxException {
		SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		ClientHttpRequest request = clientHttpRequestFactory.createRequest(new URI(url), HttpMethod.GET);
		ClientHttpResponse response = request.execute();
		try {
			return StreamUtils.copyToString(response.getBody(), Charset.forName("UTF-8"));
		}
		finally {
			response.close();
		}
	}

	protected abstract AbstractEmbeddedServletContainerFactory getFactory();

	private ServletContextInitializer exampleServletRegistration() {
		return new ServletRegistrationBean(new ExampleServlet(), "/hello");
	}
}
