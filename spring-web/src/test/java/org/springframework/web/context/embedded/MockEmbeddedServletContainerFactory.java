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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.web.ServletContextInitializer;

import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Mock {@link EmbeddedServletContainerFactory}.
 *
 * @author Phillip Webb
 */
public class MockEmbeddedServletContainerFactory implements
		EmbeddedServletContainerFactory {

	private MockEmbeddedServletContainer container;

	private int port;


	public EmbeddedServletContainer getEmbdeddedServletContainer(
			ServletContextInitializer... initializers) {
		this.container = spy(new MockEmbeddedServletContainer(initializers, port));
		return this.container;
	}

	public MockEmbeddedServletContainer getContainer() {
		return this.container;
	}

	public ServletContext getServletContext() {
		return getContainer().servletContext;
	}

	public RegisteredServlet getRegisteredServlet(int index) {
		return getContainer().getRegisteredServlets().get(index);
	}

	public RegisteredFilter getRegisteredFilter(int index) {
		return getContainer().getRegisteredFilters().get(index);
	}

	public void setPort(int port) {
		this.port = port;
	}


	public static class MockEmbeddedServletContainer implements EmbeddedServletContainer {

		private ServletContext servletContext;

		private ServletContextInitializer[] initializers;

		private List<RegisteredServlet> registeredServlets = new ArrayList<RegisteredServlet>();

		private List<RegisteredFilter> registeredFilters = new ArrayList<RegisteredFilter>();

		private int port;


		public MockEmbeddedServletContainer(ServletContextInitializer[] initializers, int port) {
			this.initializers = initializers;
			this.port = port;
		}


		public void start() {
			try {
				this.servletContext = mock(ServletContext.class);
				given(this.servletContext.addServlet(anyString(), (Servlet) anyObject())).willAnswer(
						new Answer<ServletRegistration.Dynamic>() {
					public ServletRegistration.Dynamic answer(InvocationOnMock invocation) throws Throwable {
						RegisteredServlet registeredServlet = new RegisteredServlet((Servlet) invocation.getArguments()[1]);
						MockEmbeddedServletContainer.this.registeredServlets.add(registeredServlet);
						return registeredServlet.getRegistration();
					}
				});
				given(this.servletContext.addFilter(anyString(), (Filter) anyObject())).willAnswer(
						new Answer<FilterRegistration.Dynamic>() {
					public FilterRegistration.Dynamic answer(InvocationOnMock invocation) throws Throwable {
						RegisteredFilter registeredFilter = new RegisteredFilter((Filter) invocation.getArguments()[1]);
						MockEmbeddedServletContainer.this.registeredFilters.add(registeredFilter);
						return registeredFilter.getRegistration();
					}
				});
				given(this.servletContext.getInitParameterNames()).willReturn(
						Collections.<String> emptyEnumeration());
				given(this.servletContext.getAttributeNames()).willReturn(
						Collections.<String> emptyEnumeration());
				for (ServletContextInitializer initializer : this.initializers) {
					initializer.onStartup(this.servletContext);
				}
			}
			catch (ServletException ex) {
				throw new RuntimeException(ex);
			}
		}

		public void stop() {
			this.servletContext = null;
			this.registeredServlets.clear();
		}

		public Servlet[] getServlets() {
			Servlet[] servlets = new Servlet[this.registeredServlets.size()];
			for (int i = 0; i < servlets.length; i++) {
				servlets[i] = this.registeredServlets.get(i).getServlet();
			}
			return servlets;
		}

		public List<RegisteredServlet> getRegisteredServlets() {
			return this.registeredServlets;
		}

		public List<RegisteredFilter> getRegisteredFilters() {
			return this.registeredFilters;
		}

		public int getPort() {
			return port;
		}
	}


	public static class RegisteredServlet {

		private Servlet servlet;

		private ServletRegistration.Dynamic registration;


		public RegisteredServlet(Servlet servlet) {
			this.servlet = servlet;
			this.registration = mock(ServletRegistration.Dynamic.class);
		}


		public ServletRegistration.Dynamic getRegistration() {
			return this.registration;
		}

		public Servlet getServlet() {
			return this.servlet;
		}
	}

	public static class RegisteredFilter {

		private Filter filter;

		private FilterRegistration.Dynamic registration;


		public RegisteredFilter(Filter filter) {
			this.filter = filter;
			this.registration = mock(FilterRegistration.Dynamic.class);
		}


		public FilterRegistration.Dynamic getRegistration() {
			return this.registration;
		}

		public Filter getFilter() {
			return this.filter;
		}
	}
}
