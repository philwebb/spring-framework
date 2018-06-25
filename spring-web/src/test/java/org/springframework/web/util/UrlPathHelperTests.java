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

package org.springframework.web.util;

import java.io.UnsupportedEncodingException;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link UrlPathHelper}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Costin Leau
 */
public class UrlPathHelperTests {

	private static final String WEBSPHERE_URI_ATTRIBUTE = "com.ibm.websphere.servlet.uri_non_decoded";

	private final UrlPathHelper helper = new UrlPathHelper();

	private final MockHttpServletRequest request = new MockHttpServletRequest();


	@Test
	public void getPathWithinApplication() {
		this.request.setContextPath("/petclinic");
		this.request.setRequestURI("/petclinic/welcome.html");

		assertEquals("Incorrect path returned", "/welcome.html", this.helper.getPathWithinApplication(this.request));
	}

	@Test
	public void getPathWithinApplicationForRootWithNoLeadingSlash() {
		this.request.setContextPath("/petclinic");
		this.request.setRequestURI("/petclinic");

		assertEquals("Incorrect root path returned", "/", this.helper.getPathWithinApplication(this.request));
	}

	@Test
	public void getPathWithinApplicationForSlashContextPath() {
		this.request.setContextPath("/");
		this.request.setRequestURI("/welcome.html");

		assertEquals("Incorrect path returned", "/welcome.html", this.helper.getPathWithinApplication(this.request));
	}

	@Test
	public void getPathWithinServlet() {
		this.request.setContextPath("/petclinic");
		this.request.setServletPath("/main");
		this.request.setRequestURI("/petclinic/main/welcome.html");

		assertEquals("Incorrect path returned", "/welcome.html", this.helper.getPathWithinServletMapping(this.request));
	}

	@Test
	public void alwaysUseFullPath() {
		this.helper.setAlwaysUseFullPath(true);
		this.request.setContextPath("/petclinic");
		this.request.setServletPath("/main");
		this.request.setRequestURI("/petclinic/main/welcome.html");

		assertEquals("Incorrect path returned", "/main/welcome.html", this.helper.getLookupPathForRequest(this.request));
	}

	// SPR-11101

	@Test
	public void getPathWithinServletWithoutUrlDecoding() {
		this.request.setContextPath("/SPR-11101");
		this.request.setServletPath("/test_url_decoding/a/b");
		this.request.setRequestURI("/test_url_decoding/a%2Fb");

		this.helper.setUrlDecode(false);
		String actual = this.helper.getPathWithinServletMapping(this.request);

		assertEquals("/test_url_decoding/a%2Fb", actual);
	}

	@Test
	public void getRequestUri() {
		this.request.setRequestURI("/welcome.html");
		assertEquals("Incorrect path returned", "/welcome.html", this.helper.getRequestUri(this.request));

		this.request.setRequestURI("/foo%20bar");
		assertEquals("Incorrect path returned", "/foo bar", this.helper.getRequestUri(this.request));

		this.request.setRequestURI("/foo+bar");
		assertEquals("Incorrect path returned", "/foo+bar", this.helper.getRequestUri(this.request));
	}

	@Test
	public void getRequestRemoveSemicolonContent() throws UnsupportedEncodingException {
		this.helper.setRemoveSemicolonContent(true);

		this.request.setRequestURI("/foo;f=F;o=O;o=O/bar;b=B;a=A;r=R");
		assertEquals("/foo/bar", this.helper.getRequestUri(this.request));

		// SPR-13455

		this.request.setServletPath("/foo/1");
		this.request.setRequestURI("/foo/;test/1");

		assertEquals("/foo/1", this.helper.getRequestUri(this.request));
	}

	@Test
	public void getRequestKeepSemicolonContent() throws UnsupportedEncodingException {
		this.helper.setRemoveSemicolonContent(false);

		this.request.setRequestURI("/foo;a=b;c=d");
		assertEquals("/foo;a=b;c=d", this.helper.getRequestUri(this.request));

		this.request.setRequestURI("/foo;jsessionid=c0o7fszeb1");
		assertEquals("jsessionid should always be removed", "/foo", this.helper.getRequestUri(this.request));

		this.request.setRequestURI("/foo;a=b;jsessionid=c0o7fszeb1;c=d");
		assertEquals("jsessionid should always be removed", "/foo;a=b;c=d", this.helper.getRequestUri(this.request));

		// SPR-10398

		this.request.setRequestURI("/foo;a=b;JSESSIONID=c0o7fszeb1;c=d");
		assertEquals("JSESSIONID should always be removed", "/foo;a=b;c=d", this.helper.getRequestUri(this.request));
	}

	@Test
	public void getLookupPathWithSemicolonContent() {
		this.helper.setRemoveSemicolonContent(false);

		this.request.setContextPath("/petclinic");
		this.request.setServletPath("/main");
		this.request.setRequestURI("/petclinic;a=b/main;b=c/welcome.html;c=d");

		assertEquals("/welcome.html;c=d", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void getLookupPathWithSemicolonContentAndNullPathInfo() {
		this.helper.setRemoveSemicolonContent(false);

		this.request.setContextPath("/petclinic");
		this.request.setServletPath("/welcome.html");
		this.request.setRequestURI("/petclinic;a=b/welcome.html;c=d");

		assertEquals("/welcome.html;c=d", this.helper.getLookupPathForRequest(this.request));
	}


	//
	// suite of tests root requests for default servlets (SRV 11.2) on Websphere vs Tomcat and other containers
	// see: http://jira.springframework.org/browse/SPR-7064
	//


	//
	// / mapping (default servlet)
	//

	@Test
	public void tomcatDefaultServletRoot() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo(null);
		this.request.setServletPath("/");
		this.request.setRequestURI("/test/");
		assertEquals("/", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void tomcatDefaultServletFile() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo(null);
		this.request.setServletPath("/foo");
		this.request.setRequestURI("/test/foo");

		assertEquals("/foo", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void tomcatDefaultServletFolder() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo(null);
		this.request.setServletPath("/foo/");
		this.request.setRequestURI("/test/foo/");

		assertEquals("/foo/", this.helper.getLookupPathForRequest(this.request));
	}

	//SPR-12372 & SPR-13455
	@Test
	public void removeDuplicateSlashesInPath() throws Exception {
		this.request.setContextPath("/SPR-12372");
		this.request.setPathInfo(null);
		this.request.setServletPath("/foo/bar/");
		this.request.setRequestURI("/SPR-12372/foo//bar/");

		assertEquals("/foo/bar/", this.helper.getLookupPathForRequest(this.request));

		this.request.setServletPath("/foo/bar/");
		this.request.setRequestURI("/SPR-12372/foo/bar//");

		assertEquals("/foo/bar/", this.helper.getLookupPathForRequest(this.request));

		// "normal" case
		this.request.setServletPath("/foo/bar//");
		this.request.setRequestURI("/SPR-12372/foo/bar//");

		assertEquals("/foo/bar//", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void wasDefaultServletRoot() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo("/");
		this.request.setServletPath("");
		this.request.setRequestURI("/test/");
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/");

		assertEquals("/", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void wasDefaultServletRootWithCompliantSetting() throws Exception {
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/");
		tomcatDefaultServletRoot();
	}

	@Test
	public void wasDefaultServletFile() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo("/foo");
		this.request.setServletPath("");
		this.request.setRequestURI("/test/foo");
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo");

		assertEquals("/foo", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void wasDefaultServletFileWithCompliantSetting() throws Exception {
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo");
		tomcatDefaultServletFile();
	}

	@Test
	public void wasDefaultServletFolder() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo("/foo/");
		this.request.setServletPath("");
		this.request.setRequestURI("/test/foo/");
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/");

		assertEquals("/foo/", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void wasDefaultServletFolderWithCompliantSetting() throws Exception {
		UrlPathHelper.websphereComplianceFlag = true;
		try {
			this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/");
			tomcatDefaultServletFolder();
		}
		finally {
			UrlPathHelper.websphereComplianceFlag = false;
		}
	}


	//
	// /foo/* mapping
	//

	@Test
	public void tomcatCasualServletRoot() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo("/");
		this.request.setServletPath("/foo");
		this.request.setRequestURI("/test/foo/");

		assertEquals("/", this.helper.getLookupPathForRequest(this.request));
	}

	// test the root mapping for /foo/* w/o a trailing slash - <host>/<context>/foo
	@Test
	@Ignore
	public void tomcatCasualServletRootWithMissingSlash() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo(null);
		this.request.setServletPath("/foo");
		this.request.setRequestURI("/test/foo");

		assertEquals("/", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void tomcatCasualServletFile() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo("/foo");
		this.request.setServletPath("/foo");
		this.request.setRequestURI("/test/foo/foo");

		assertEquals("/foo", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void tomcatCasualServletFolder() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo("/foo/");
		this.request.setServletPath("/foo");
		this.request.setRequestURI("/test/foo/foo/");

		assertEquals("/foo/", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void wasCasualServletRoot() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo(null);
		this.request.setServletPath("/foo/");
		this.request.setRequestURI("/test/foo/");
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/");

		assertEquals("/", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void wasCasualServletRootWithCompliantSetting() throws Exception {
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/");
		tomcatCasualServletRoot();
	}

	// test the root mapping for /foo/* w/o a trailing slash - <host>/<context>/foo
	@Ignore
	@Test
	public void wasCasualServletRootWithMissingSlash() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo(null);
		this.request.setServletPath("/foo");
		this.request.setRequestURI("/test/foo");
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo");

		assertEquals("/", this.helper.getLookupPathForRequest(this.request));
	}

	@Ignore
	@Test
	public void wasCasualServletRootWithMissingSlashWithCompliantSetting() throws Exception {
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo");
		tomcatCasualServletRootWithMissingSlash();
	}

	@Test
	public void wasCasualServletFile() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo("/foo");
		this.request.setServletPath("/foo");
		this.request.setRequestURI("/test/foo/foo");
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/foo");

		assertEquals("/foo", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void wasCasualServletFileWithCompliantSetting() throws Exception {
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/foo");
		tomcatCasualServletFile();
	}

	@Test
	public void wasCasualServletFolder() throws Exception {
		this.request.setContextPath("/test");
		this.request.setPathInfo("/foo/");
		this.request.setServletPath("/foo");
		this.request.setRequestURI("/test/foo/foo/");
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/foo/");

		assertEquals("/foo/", this.helper.getLookupPathForRequest(this.request));
	}

	@Test
	public void wasCasualServletFolderWithCompliantSetting() throws Exception {
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/foo/");
		tomcatCasualServletFolder();
	}

	@Test
	public void getOriginatingRequestUri() {
		this.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/path");
		this.request.setRequestURI("/forwarded");
		assertEquals("/path", this.helper.getOriginatingRequestUri(this.request));
	}

	@Test
	public void getOriginatingRequestUriWebsphere() {
		this.request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/path");
		this.request.setRequestURI("/forwarded");
		assertEquals("/path", this.helper.getOriginatingRequestUri(this.request));
	}

	@Test
	public void getOriginatingRequestUriDefault() {
		this.request.setRequestURI("/forwarded");
		assertEquals("/forwarded", this.helper.getOriginatingRequestUri(this.request));
	}

	@Test
	public void getOriginatingQueryString() {
		this.request.setQueryString("forward=on");
		this.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/path");
		this.request.setAttribute(WebUtils.FORWARD_QUERY_STRING_ATTRIBUTE, "original=on");
		assertEquals("original=on", this.helper.getOriginatingQueryString(this.request));
	}

	@Test
	public void getOriginatingQueryStringNotPresent() {
		this.request.setQueryString("forward=true");
		assertEquals("forward=true", this.helper.getOriginatingQueryString(this.request));
	}

	@Test
	public void getOriginatingQueryStringIsNull() {
		this.request.setQueryString("forward=true");
		this.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/path");
		assertNull(this.helper.getOriginatingQueryString(this.request));
	}

}
