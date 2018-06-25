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

package org.springframework.test.web.servlet.htmlunit;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import com.gargoylesoftware.htmlunit.FormEncodingType;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Unit tests for {@link HtmlUnitRequestBuilder}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 */
public class HtmlUnitRequestBuilderTests {

	private final WebClient webClient = new WebClient();

	private final ServletContext servletContext = new MockServletContext();

	private final Map<String, MockHttpSession> sessions = new HashMap<>();

	private WebRequest webRequest;

	private HtmlUnitRequestBuilder requestBuilder;


	@Before
	public void setup() throws Exception {
		this.webRequest = new WebRequest(new URL("http://example.com:80/test/this/here"));
		this.webRequest.setHttpMethod(HttpMethod.GET);
		this.requestBuilder = new HtmlUnitRequestBuilder(this.sessions, this.webClient, this.webRequest);
	}


	// --- constructor

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullSessions() {
		new HtmlUnitRequestBuilder(null, this.webClient, this.webRequest);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullWebClient() {
		new HtmlUnitRequestBuilder(this.sessions, null, this.webRequest);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullWebRequest() {
		new HtmlUnitRequestBuilder(this.sessions, this.webClient, null);
	}


	// --- buildRequest

	@Test
	@SuppressWarnings("deprecation")
	public void buildRequestBasicAuth() {
		String base64Credentials = "dXNlcm5hbWU6cGFzc3dvcmQ=";
		String authzHeaderValue = "Basic: " + base64Credentials;
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(base64Credentials);
		this.webRequest.setCredentials(credentials);
		this.webRequest.setAdditionalHeader("Authorization", authzHeaderValue);

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getAuthType(), equalTo("Basic"));
		assertThat(actualRequest.getHeader("Authorization"), equalTo(authzHeaderValue));
	}

	@Test
	public void buildRequestCharacterEncoding() {
		this.webRequest.setCharset(StandardCharsets.UTF_8);

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getCharacterEncoding(), equalTo("UTF-8"));
	}

	@Test
	public void buildRequestDefaultCharacterEncoding() {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getCharacterEncoding(), equalTo("ISO-8859-1"));
	}

	@Test
	public void buildRequestContentLength() {
		String content = "some content that has length";
		this.webRequest.setHttpMethod(HttpMethod.POST);
		this.webRequest.setRequestBody(content);

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getContentLength(), equalTo(content.length()));
	}

	@Test
	public void buildRequestContentType() {
		String contentType = "text/html;charset=UTF-8";
		this.webRequest.setAdditionalHeader("Content-Type", contentType);

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getContentType(), equalTo(contentType));
		assertThat(actualRequest.getHeader("Content-Type"), equalTo(contentType));
	}

	@Test  // SPR-14916
	public void buildRequestContentTypeWithFormSubmission() {
		this.webRequest.setEncodingType(FormEncodingType.URL_ENCODED);

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getContentType(), equalTo("application/x-www-form-urlencoded"));
		assertThat(actualRequest.getHeader("Content-Type"),
				equalTo("application/x-www-form-urlencoded;charset=ISO-8859-1"));
	}


	@Test
	public void buildRequestContextPathUsesFirstSegmentByDefault() {
		String contextPath = this.requestBuilder.buildRequest(this.servletContext).getContextPath();

		assertThat(contextPath, equalTo("/test"));
	}

	@Test
	public void buildRequestContextPathUsesNoFirstSegmentWithDefault() throws MalformedURLException {
		this.webRequest.setUrl(new URL("http://example.com/"));
		String contextPath = this.requestBuilder.buildRequest(this.servletContext).getContextPath();

		assertThat(contextPath, equalTo(""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void buildRequestContextPathInvalid() {
		this.requestBuilder.setContextPath("/invalid");

		this.requestBuilder.buildRequest(this.servletContext).getContextPath();
	}

	@Test
	public void buildRequestContextPathEmpty() {
		String expected = "";
		this.requestBuilder.setContextPath(expected);

		String contextPath = this.requestBuilder.buildRequest(this.servletContext).getContextPath();

		assertThat(contextPath, equalTo(expected));
	}

	@Test
	public void buildRequestContextPathExplicit() {
		String expected = "/test";
		this.requestBuilder.setContextPath(expected);

		String contextPath = this.requestBuilder.buildRequest(this.servletContext).getContextPath();

		assertThat(contextPath, equalTo(expected));
	}

	@Test
	public void buildRequestContextPathMulti() {
		String expected = "/test/this";
		this.requestBuilder.setContextPath(expected);

		String contextPath = this.requestBuilder.buildRequest(this.servletContext).getContextPath();

		assertThat(contextPath, equalTo(expected));
	}

	@Test
	public void buildRequestCookiesNull() {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getCookies(), nullValue());
	}

	@Test
	public void buildRequestCookiesSingle() {
		this.webRequest.setAdditionalHeader("Cookie", "name=value");

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		Cookie[] cookies = actualRequest.getCookies();
		assertThat(cookies.length, equalTo(1));
		assertThat(cookies[0].getName(), equalTo("name"));
		assertThat(cookies[0].getValue(), equalTo("value"));
	}

	@Test
	public void buildRequestCookiesMulti() {
		this.webRequest.setAdditionalHeader("Cookie", "name=value; name2=value2");

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		Cookie[] cookies = actualRequest.getCookies();
		assertThat(cookies.length, equalTo(2));
		Cookie cookie = cookies[0];
		assertThat(cookie.getName(), equalTo("name"));
		assertThat(cookie.getValue(), equalTo("value"));
		cookie = cookies[1];
		assertThat(cookie.getName(), equalTo("name2"));
		assertThat(cookie.getValue(), equalTo("value2"));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void buildRequestInputStream() throws Exception {
		String content = "some content that has length";
		this.webRequest.setHttpMethod(HttpMethod.POST);
		this.webRequest.setRequestBody(content);

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(IOUtils.toString(actualRequest.getInputStream()), equalTo(content));
	}

	@Test
	public void buildRequestLocalAddr() {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getLocalAddr(), equalTo("127.0.0.1"));
	}

	@Test
	public void buildRequestLocaleDefault() {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getLocale(), equalTo(Locale.getDefault()));
	}

	@Test
	public void buildRequestLocaleDa() {
		this.webRequest.setAdditionalHeader("Accept-Language", "da");

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getLocale(), equalTo(new Locale("da")));
	}

	@Test
	public void buildRequestLocaleEnGbQ08() {
		this.webRequest.setAdditionalHeader("Accept-Language", "en-gb;q=0.8");

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getLocale(), equalTo(new Locale("en", "gb")));
	}

	@Test
	public void buildRequestLocaleEnQ07() {
		this.webRequest.setAdditionalHeader("Accept-Language", "en");

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getLocale(), equalTo(new Locale("en", "")));
	}

	@Test
	public void buildRequestLocaleEnUs() {
		this.webRequest.setAdditionalHeader("Accept-Language", "en-US");

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getLocale(), equalTo(Locale.US));
	}

	@Test
	public void buildRequestLocaleFr() {
		this.webRequest.setAdditionalHeader("Accept-Language", "fr");

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getLocale(), equalTo(Locale.FRENCH));
	}

	@Test
	public void buildRequestLocaleMulti() {
		this.webRequest.setAdditionalHeader("Accept-Language", "en-gb;q=0.8, da, en;q=0.7");

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		List<Locale> expected = asList(new Locale("da"), new Locale("en", "gb"), new Locale("en", ""));
		assertThat(Collections.list(actualRequest.getLocales()), equalTo(expected));
	}

	@Test
	public void buildRequestLocalName() {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getLocalName(), equalTo("localhost"));
	}

	@Test
	public void buildRequestLocalPort() {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getLocalPort(), equalTo(80));
	}

	@Test
	public void buildRequestLocalMissing() throws Exception {
		this.webRequest.setUrl(new URL("http://localhost/test/this"));
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getLocalPort(), equalTo(-1));
	}

	@Test
	public void buildRequestMethods() {
		for (HttpMethod expectedMethod : HttpMethod.values()) {
			this.webRequest.setHttpMethod(expectedMethod);
			String actualMethod = this.requestBuilder.buildRequest(this.servletContext).getMethod();
			assertThat(actualMethod, equalTo(expectedMethod.name()));
		}
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithSingleRequestParam() {
		this.webRequest.setRequestParameters(asList(new NameValuePair("name", "value")));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo("value"));
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithSingleRequestParamWithNullValue() {
		this.webRequest.setRequestParameters(asList(new NameValuePair("name", null)));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), nullValue());
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithSingleRequestParamWithEmptyValue() {
		this.webRequest.setRequestParameters(asList(new NameValuePair("name", "")));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo(""));
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithSingleRequestParamWithValueSetToSpace() {
		this.webRequest.setRequestParameters(asList(new NameValuePair("name", " ")));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo(" "));
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithMultipleRequestParams() {
		this.webRequest.setRequestParameters(asList(new NameValuePair("name1", "value1"), new NameValuePair("name2", "value2")));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(2));
		assertThat(actualRequest.getParameter("name1"), equalTo("value1"));
		assertThat(actualRequest.getParameter("name2"), equalTo("value2"));
	}

	@Test
	public void buildRequestParameterMapFromSingleQueryParam() throws Exception {
		this.webRequest.setUrl(new URL("http://example.com/example/?name=value"));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo("value"));
	}

	// SPR-14177
	@Test
	public void buildRequestParameterMapDecodesParameterName() throws Exception {
		this.webRequest.setUrl(new URL("http://example.com/example/?row%5B0%5D=value"));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("row[0]"), equalTo("value"));
	}

	@Test
	public void buildRequestParameterMapDecodesParameterValue() throws Exception {
		this.webRequest.setUrl(new URL("http://example.com/example/?name=row%5B0%5D"));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo("row[0]"));
	}

	@Test
	public void buildRequestParameterMapFromSingleQueryParamWithoutValueAndWithoutEqualsSign() throws Exception {
		this.webRequest.setUrl(new URL("http://example.com/example/?name"));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo(""));
	}

	@Test
	public void buildRequestParameterMapFromSingleQueryParamWithoutValueButWithEqualsSign() throws Exception {
		this.webRequest.setUrl(new URL("http://example.com/example/?name="));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo(""));
	}

	@Test
	public void buildRequestParameterMapFromSingleQueryParamWithValueSetToEncodedSpace() throws Exception {
		this.webRequest.setUrl(new URL("http://example.com/example/?name=%20"));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo(" "));
	}

	@Test
	public void buildRequestParameterMapFromMultipleQueryParams() throws Exception {
		this.webRequest.setUrl(new URL("http://example.com/example/?name=value&param2=value+2"));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(2));
		assertThat(actualRequest.getParameter("name"), equalTo("value"));
		assertThat(actualRequest.getParameter("param2"), equalTo("value 2"));
	}

	@Test
	public void buildRequestPathInfo() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getPathInfo(), nullValue());
	}

	@Test
	public void buildRequestPathInfoNull() throws Exception {
		this.webRequest.setUrl(new URL("http://example.com/example"));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getPathInfo(), nullValue());
	}

	@Test
	public void buildRequestAndAntPathRequestMatcher() throws Exception {
		this.webRequest.setUrl(new URL("http://example.com/app/login/authenticate"));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		// verify it is going to work with Spring Security's AntPathRequestMatcher
		assertThat(actualRequest.getPathInfo(), nullValue());
		assertThat(actualRequest.getServletPath(), equalTo("/login/authenticate"));
	}

	@Test
	public void buildRequestProtocol() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getProtocol(), equalTo("HTTP/1.1"));
	}

	@Test
	public void buildRequestQueryWithSingleQueryParam() throws Exception {
		String expectedQuery = "param=value";
		this.webRequest.setUrl(new URL("http://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getQueryString(), equalTo(expectedQuery));
	}

	@Test
	public void buildRequestQueryWithSingleQueryParamWithoutValueAndWithoutEqualsSign() throws Exception {
		String expectedQuery = "param";
		this.webRequest.setUrl(new URL("http://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getQueryString(), equalTo(expectedQuery));
	}

	@Test
	public void buildRequestQueryWithSingleQueryParamWithoutValueButWithEqualsSign() throws Exception {
		String expectedQuery = "param=";
		this.webRequest.setUrl(new URL("http://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getQueryString(), equalTo(expectedQuery));
	}

	@Test
	public void buildRequestQueryWithSingleQueryParamWithValueSetToEncodedSpace() throws Exception {
		String expectedQuery = "param=%20";
		this.webRequest.setUrl(new URL("http://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getQueryString(), equalTo(expectedQuery));
	}

	@Test
	public void buildRequestQueryWithMultipleQueryParams() throws Exception {
		String expectedQuery = "param1=value1&param2=value2";
		this.webRequest.setUrl(new URL("http://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getQueryString(), equalTo(expectedQuery));
	}

	@Test
	public void buildRequestReader() throws Exception {
		String expectedBody = "request body";
		this.webRequest.setHttpMethod(HttpMethod.POST);
		this.webRequest.setRequestBody(expectedBody);

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(IOUtils.toString(actualRequest.getReader()), equalTo(expectedBody));
	}

	@Test
	public void buildRequestRemoteAddr() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getRemoteAddr(), equalTo("127.0.0.1"));
	}

	@Test
	public void buildRequestRemoteHost() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getRemoteAddr(), equalTo("127.0.0.1"));
	}

	@Test
	public void buildRequestRemotePort() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getRemotePort(), equalTo(80));
	}

	@Test
	public void buildRequestRemotePort8080() throws Exception {
		this.webRequest.setUrl(new URL("http://example.com:8080/"));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getRemotePort(), equalTo(8080));
	}

	@Test
	public void buildRequestRemotePort80WithDefault() throws Exception {
		this.webRequest.setUrl(new URL("http://example.com/"));

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getRemotePort(), equalTo(80));
	}

	@Test
	public void buildRequestRequestedSessionId() throws Exception {
		String sessionId = "session-id";
		this.webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getRequestedSessionId(), equalTo(sessionId));
	}

	@Test
	public void buildRequestRequestedSessionIdNull() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getRequestedSessionId(), nullValue());
	}

	@Test
	public void buildRequestUri() {
		String uri = this.requestBuilder.buildRequest(this.servletContext).getRequestURI();
		assertThat(uri, equalTo("/test/this/here"));
	}

	@Test
	public void buildRequestUrl() {
		String uri = this.requestBuilder.buildRequest(this.servletContext).getRequestURL().toString();
		assertThat(uri, equalTo("http://example.com/test/this/here"));
	}

	@Test
	public void buildRequestSchemeHttp() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getScheme(), equalTo("http"));
	}

	@Test
	public void buildRequestSchemeHttps() throws Exception {
		this.webRequest.setUrl(new URL("https://example.com/"));
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getScheme(), equalTo("https"));
	}

	@Test
	public void buildRequestServerName() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getServerName(), equalTo("example.com"));
	}

	@Test
	public void buildRequestServerPort() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getServerPort(), equalTo(80));
	}

	@Test
	public void buildRequestServerPortDefault() throws Exception {
		this.webRequest.setUrl(new URL("https://example.com/"));
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getServerPort(), equalTo(-1));
	}

	@Test
	public void buildRequestServletContext() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getServletContext(), equalTo(this.servletContext));
	}

	@Test
	public void buildRequestServletPath() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getServletPath(), equalTo("/this/here"));
	}

	@Test
	public void buildRequestSession() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		HttpSession newSession = actualRequest.getSession();
		assertThat(newSession, notNullValue());
		assertSingleSessionCookie(
				"JSESSIONID=" + newSession.getId() + "; Path=/test; Domain=example.com");

		this.webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + newSession.getId());

		this.requestBuilder = new HtmlUnitRequestBuilder(this.sessions, this.webClient, this.webRequest);
		actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getSession(), sameInstance(newSession));
	}

	@Test
	public void buildRequestSessionWithExistingSession() throws Exception {
		String sessionId = "session-id";
		this.webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		HttpSession session = actualRequest.getSession();
		assertThat(session.getId(), equalTo(sessionId));
		assertSingleSessionCookie("JSESSIONID=" + session.getId() + "; Path=/test; Domain=example.com");

		this.requestBuilder = new HtmlUnitRequestBuilder(this.sessions, this.webClient, this.webRequest);
		actualRequest = this.requestBuilder.buildRequest(this.servletContext);
		assertThat(actualRequest.getSession(), equalTo(session));

		this.webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId + "NEW");
		actualRequest = this.requestBuilder.buildRequest(this.servletContext);
		assertThat(actualRequest.getSession(), not(equalTo(session)));
		assertSingleSessionCookie("JSESSIONID=" + actualRequest.getSession().getId()
				+ "; Path=/test; Domain=example.com");
	}

	@Test
	public void buildRequestSessionTrue() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		HttpSession session = actualRequest.getSession(true);
		assertThat(session, notNullValue());
	}

	@Test
	public void buildRequestSessionFalseIsNull() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		HttpSession session = actualRequest.getSession(false);
		assertThat(session, nullValue());
	}

	@Test
	public void buildRequestSessionFalseWithExistingSession() throws Exception {
		String sessionId = "session-id";
		this.webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		HttpSession session = actualRequest.getSession(false);
		assertThat(session, notNullValue());
	}

	@Test
	public void buildRequestSessionIsNew() throws Exception {
		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getSession().isNew(), equalTo(true));
	}

	@Test
	public void buildRequestSessionIsNewFalse() throws Exception {
		String sessionId = "session-id";
		this.webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getSession().isNew(), equalTo(false));
	}

	@Test
	public void buildRequestSessionInvalidate() throws Exception {
		String sessionId = "session-id";
		this.webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);

		MockHttpServletRequest actualRequest = this.requestBuilder.buildRequest(this.servletContext);
		HttpSession sessionToRemove = actualRequest.getSession();
		sessionToRemove.invalidate();

		assertThat(this.sessions.containsKey(sessionToRemove.getId()), equalTo(false));
		assertSingleSessionCookie("JSESSIONID=" + sessionToRemove.getId()
				+ "; Expires=Thu, 01-Jan-1970 00:00:01 GMT; Path=/test; Domain=example.com");

		this.webRequest.removeAdditionalHeader("Cookie");
		this.requestBuilder = new HtmlUnitRequestBuilder(this.sessions, this.webClient, this.webRequest);

		actualRequest = this.requestBuilder.buildRequest(this.servletContext);

		assertThat(actualRequest.getSession().isNew(), equalTo(true));
		assertThat(this.sessions.containsKey(sessionToRemove.getId()), equalTo(false));
	}

	// --- setContextPath

	@Test
	public void setContextPathNull() {
		this.requestBuilder.setContextPath(null);

		assertThat(getContextPath(), nullValue());
	}

	@Test
	public void setContextPathEmptyString() {
		this.requestBuilder.setContextPath("");

		assertThat(getContextPath(), isEmptyString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void setContextPathDoesNotStartWithSlash() {
		this.requestBuilder.setContextPath("abc/def");
	}

	@Test(expected = IllegalArgumentException.class)
	public void setContextPathEndsWithSlash() {
		this.requestBuilder.setContextPath("/abc/def/");
	}

	@Test
	public void setContextPath() {
		String expectedContextPath = "/abc/def";
		this.requestBuilder.setContextPath(expectedContextPath);

		assertThat(getContextPath(), equalTo(expectedContextPath));
	}

	@Test
	public void mergeHeader() throws Exception {
		String headerName = "PARENT";
		String headerValue = "VALUE";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
					.defaultRequest(get("/").header(headerName, headerValue))
					.build();

		assertThat(mockMvc.perform(this.requestBuilder).andReturn().getRequest().getHeader(headerName), equalTo(headerValue));
	}

	@Test
	public void mergeSession() throws Exception {
		String attrName = "PARENT";
		String attrValue = "VALUE";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/").sessionAttr(attrName, attrValue))
				.build();

		assertThat(mockMvc.perform(this.requestBuilder).andReturn().getRequest().getSession().getAttribute(attrName), equalTo(attrValue));
	}

	@Test
	public void mergeSessionNotInitialized() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/"))
				.build();

		assertThat(mockMvc.perform(this.requestBuilder).andReturn().getRequest().getSession(false), nullValue());
	}

	@Test
	public void mergeParameter() throws Exception {
		String paramName = "PARENT";
		String paramValue = "VALUE";
		String paramValue2 = "VALUE2";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/").param(paramName, paramValue, paramValue2))
				.build();

		MockHttpServletRequest performedRequest = mockMvc.perform(this.requestBuilder).andReturn().getRequest();
		assertThat(asList(performedRequest.getParameterValues(paramName)), contains(paramValue, paramValue2));
	}

	@Test
	public void mergeCookie() throws Exception {
		String cookieName = "PARENT";
		String cookieValue = "VALUE";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/").cookie(new Cookie(cookieName, cookieValue)))
				.build();

		Cookie[] cookies = mockMvc.perform(this.requestBuilder).andReturn().getRequest().getCookies();
		assertThat(cookies, notNullValue());
		assertThat(cookies.length, equalTo(1));
		Cookie cookie = cookies[0];
		assertThat(cookie.getName(), equalTo(cookieName));
		assertThat(cookie.getValue(), equalTo(cookieValue));
	}

	@Test
	public void mergeRequestAttribute() throws Exception {
		String attrName = "PARENT";
		String attrValue = "VALUE";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/").requestAttr(attrName, attrValue))
				.build();

		assertThat(mockMvc.perform(this.requestBuilder).andReturn().getRequest().getAttribute(attrName), equalTo(attrValue));
	}

	@Test // SPR-14584
	public void mergeDoesNotCorruptPathInfoOnParent() throws Exception {
		String pathInfo = "/foo/bar";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/"))
				.build();

		assertThat(mockMvc.perform(get(pathInfo)).andReturn().getRequest().getPathInfo(), equalTo(pathInfo));

		mockMvc.perform(this.requestBuilder);

		assertThat(mockMvc.perform(get(pathInfo)).andReturn().getRequest().getPathInfo(), equalTo(pathInfo));
	}


	private void assertSingleSessionCookie(String expected) {
		com.gargoylesoftware.htmlunit.util.Cookie jsessionidCookie = this.webClient.getCookieManager().getCookie("JSESSIONID");
		if (expected == null || expected.contains("Expires=Thu, 01-Jan-1970 00:00:01 GMT")) {
			assertThat(jsessionidCookie, nullValue());
			return;
		}
		String actual = jsessionidCookie.getValue();
		assertThat("JSESSIONID=" + actual + "; Path=/test; Domain=example.com", equalTo(expected));
	}

	private String getContextPath() {
		return (String) ReflectionTestUtils.getField(this.requestBuilder, "contextPath");
	}

}
