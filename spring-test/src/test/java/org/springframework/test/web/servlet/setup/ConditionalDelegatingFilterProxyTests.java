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

package org.springframework.test.web.servlet.setup;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Rob Winch
 */
public class ConditionalDelegatingFilterProxyTests {

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private MockFilterChain filterChain;

	private MockFilter delegate;

	private PatternMappingFilterProxy filter;


	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.request.setContextPath("/context");
		this.response = new MockHttpServletResponse();
		this.filterChain = new MockFilterChain();
		this.delegate = new MockFilter();
	}


	@Test
	public void init() throws Exception {
		FilterConfig config = new MockFilterConfig();
		this.filter = new PatternMappingFilterProxy(this.delegate, "/");
		this.filter.init(config);
		assertThat(this.delegate.filterConfig, is(config));
	}

	@Test
	public void destroy() throws Exception {
		this.filter = new PatternMappingFilterProxy(this.delegate, "/");
		this.filter.destroy();
		assertThat(this.delegate.destroy, is(true));
	}

	@Test
	public void matchExact() throws Exception {
		assertFilterInvoked("/test", "/test");
	}

	@Test
	public void matchExactEmpty() throws Exception {
		assertFilterInvoked("", "");
	}

	@Test
	public void matchPathMappingAllFolder() throws Exception {
		assertFilterInvoked("/test/this", "/*");
	}

	@Test
	public void matchPathMappingAll() throws Exception {
		assertFilterInvoked("/test", "/*");
	}

	@Test
	public void matchPathMappingAllContextRoot() throws Exception {
		assertFilterInvoked("", "/*");
	}

	@Test
	public void matchPathMappingContextRootAndSlash() throws Exception {
		assertFilterInvoked("/", "/*");
	}

	@Test
	public void matchPathMappingFolderPatternWithMultiFolderPath() throws Exception {
		assertFilterInvoked("/test/this/here", "/test/*");
	}

	@Test
	public void matchPathMappingFolderPattern() throws Exception {
		assertFilterInvoked("/test/this", "/test/*");
	}

	@Test
	public void matchPathMappingNoSuffix() throws Exception {
		assertFilterInvoked("/test/", "/test/*");
	}

	@Test
	public void matchPathMappingMissingSlash() throws Exception {
		assertFilterInvoked("/test", "/test/*");
	}

	@Test
	public void noMatchPathMappingMulti() throws Exception {
		assertFilterNotInvoked("/this/test/here", "/test/*");
	}

	@Test
	public void noMatchPathMappingEnd() throws Exception {
		assertFilterNotInvoked("/this/test", "/test/*");
	}

	@Test
	public void noMatchPathMappingEndSuffix() throws Exception {
		assertFilterNotInvoked("/test2/", "/test/*");
	}

	@Test
	public void noMatchPathMappingMissingSlash() throws Exception {
		assertFilterNotInvoked("/test2", "/test/*");
	}

	@Test
	public void matchExtensionMulti() throws Exception {
		assertFilterInvoked("/test/this/here.html", "*.html");
	}

	@Test
	public void matchExtension() throws Exception {
		assertFilterInvoked("/test/this.html", "*.html");
	}

	@Test
	public void matchExtensionNoPrefix() throws Exception {
		assertFilterInvoked("/.html", "*.html");
	}

	@Test
	public void matchExtensionNoFolder() throws Exception {
		assertFilterInvoked("/test.html", "*.html");
	}

	@Test
	public void noMatchExtensionNoSlash() throws Exception {
		assertFilterNotInvoked(".html", "*.html");
	}

	@Test
	public void noMatchExtensionSlashEnd() throws Exception {
		assertFilterNotInvoked("/index.html/", "*.html");
	}

	@Test
	public void noMatchExtensionPeriodEnd() throws Exception {
		assertFilterNotInvoked("/index.html.", "*.html");
	}

	@Test
	public void noMatchExtensionLarger() throws Exception {
		assertFilterNotInvoked("/index.htm", "*.html");
	}

	@Test
	public void noMatchInvalidPattern() throws Exception {
		// pattern uses extension mapping but starts with / (treated as exact match)
		assertFilterNotInvoked("/index.html", "/*.html");
	}

	/*
	 * Below are tests from Table 12-1 of the Servlet Specification
	 */
	@Test
	public void specPathMappingMultiFolderPattern() throws Exception {
		assertFilterInvoked("/foo/bar/index.html", "/foo/bar/*");
	}

	@Test
	public void specPathMappingMultiFolderPatternAlternate() throws Exception {
		assertFilterInvoked("/foo/bar/index.bop", "/foo/bar/*");
	}

	@Test
	public void specPathMappingNoSlash() throws Exception {
		assertFilterInvoked("/baz", "/baz/*");
	}

	@Test
	public void specPathMapping() throws Exception {
		assertFilterInvoked("/baz/index.html", "/baz/*");
	}

	@Test
	public void specExactMatch() throws Exception {
		assertFilterInvoked("/catalog", "/catalog");
	}

	@Test
	public void specExtensionMappingSingleFolder() throws Exception {
		assertFilterInvoked("/catalog/racecar.bop", "*.bop");
	}

	@Test
	public void specExtensionMapping() throws Exception {
		assertFilterInvoked("/index.bop", "*.bop");
	}

	private void assertFilterNotInvoked(String requestUri, String pattern) throws Exception {
		this.request.setRequestURI(this.request.getContextPath() + requestUri);
		this.filter = new PatternMappingFilterProxy(this.delegate, pattern);
		this.filter.doFilter(this.request, this.response, this.filterChain);

		assertThat(this.delegate.request, equalTo((ServletRequest) null));
		assertThat(this.delegate.response, equalTo((ServletResponse) null));
		assertThat(this.delegate.chain, equalTo((FilterChain) null));

		assertThat(this.filterChain.getRequest(), equalTo((ServletRequest) this.request));
		assertThat(this.filterChain.getResponse(), equalTo((ServletResponse) this.response));
		this.filterChain = new MockFilterChain();
	}

	private void assertFilterInvoked(String requestUri, String pattern) throws Exception {
		this.request.setRequestURI(this.request.getContextPath() + requestUri);
		this.filter = new PatternMappingFilterProxy(this.delegate, pattern);
		this.filter.doFilter(this.request, this.response, this.filterChain);

		assertThat(this.delegate.request, equalTo((ServletRequest) this.request));
		assertThat(this.delegate.response, equalTo((ServletResponse) this.response));
		assertThat(this.delegate.chain, equalTo((FilterChain) this.filterChain));
		this.delegate = new MockFilter();
	}


	private static class MockFilter implements Filter {

		private FilterConfig filterConfig;

		private ServletRequest request;

		private ServletResponse response;

		private FilterChain chain;

		private boolean destroy;

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			this.filterConfig = filterConfig;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
			this.request = request;
			this.response = response;
			this.chain = chain;
		}

		@Override
		public void destroy() {
			this.destroy = true;
		}
	}

}
