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

package org.springframework.web.servlet.i18n;

import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.junit.Test;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;

/**
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public class CookieLocaleResolverTests {

	@Test
	public void testResolveLocale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Cookie cookie = new Cookie("LanguageKoekje", "nl");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoekje");
		Locale loc = resolver.resolveLocale(request);
		assertThat((Object) loc.getLanguage()).isEqualTo("nl");
	}

	@Test
	public void testResolveLocaleContext() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Cookie cookie = new Cookie("LanguageKoekje", "nl");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoekje");
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat((Object) loc.getLocale().getLanguage()).isEqualTo("nl");
		boolean condition = loc instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isTrue();
		assertNull(((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testResolveLocaleContextWithTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Cookie cookie = new Cookie("LanguageKoekje", "nl GMT+1");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoekje");
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat((Object) loc.getLocale().getLanguage()).isEqualTo("nl");
		boolean condition = loc instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isTrue();
		assertThat((Object) ((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
	}

	@Test
	public void testResolveLocaleContextWithInvalidLocale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Cookie cookie = new Cookie("LanguageKoekje", "++ GMT+1");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoekje");
		assertThatIllegalStateException().isThrownBy(() ->
				resolver.resolveLocaleContext(request))
			.withMessageContaining("LanguageKoekje")
			.withMessageContaining("++ GMT+1");
	}

	@Test
	public void testResolveLocaleContextWithInvalidLocaleOnErrorDispatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.GERMAN);
		request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, new ServletException());
		Cookie cookie = new Cookie("LanguageKoekje", "++ GMT+1");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+2"));
		resolver.setCookieName("LanguageKoekje");
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat((Object) loc.getLocale()).isEqualTo(Locale.GERMAN);
		boolean condition = loc instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isTrue();
		assertThat((Object) ((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
	}

	@Test
	public void testResolveLocaleContextWithInvalidTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Cookie cookie = new Cookie("LanguageKoekje", "nl X-MT");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoekje");
		assertThatIllegalStateException().isThrownBy(() ->
				resolver.resolveLocaleContext(request))
			.withMessageContaining("LanguageKoekje")
			.withMessageContaining("nl X-MT");
	}

	@Test
	public void testResolveLocaleContextWithInvalidTimeZoneOnErrorDispatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, new ServletException());
		Cookie cookie = new Cookie("LanguageKoekje", "nl X-MT");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+2"));
		resolver.setCookieName("LanguageKoekje");
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat((Object) loc.getLocale().getLanguage()).isEqualTo("nl");
		boolean condition = loc instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isTrue();
		assertThat((Object) ((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
	}

	@Test
	public void testSetAndResolveLocale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocale(request, response, new Locale("nl", ""));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertNotNull(cookie);
		assertThat((Object) cookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat((Object) cookie.getDomain()).isEqualTo(null);
		assertThat((Object) cookie.getPath()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_PATH);
		assertThat(cookie.getSecure()).isFalse();

		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		Locale loc = resolver.resolveLocale(request);
		assertThat((Object) loc.getLanguage()).isEqualTo("nl");
	}

	@Test
	public void testSetAndResolveLocaleContext() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocaleContext(request, response, new SimpleLocaleContext(new Locale("nl", "")));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat((Object) loc.getLocale().getLanguage()).isEqualTo("nl");
		boolean condition = loc instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isTrue();
		assertNull(((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testSetAndResolveLocaleContextWithTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocaleContext(request, response,
				new SimpleTimeZoneAwareLocaleContext(new Locale("nl", ""), TimeZone.getTimeZone("GMT+1")));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat((Object) loc.getLocale().getLanguage()).isEqualTo("nl");
		boolean condition = loc instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isTrue();
		assertThat((Object) ((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
	}

	@Test
	public void testSetAndResolveLocaleContextWithTimeZoneOnly() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocaleContext(request, response,
				new SimpleTimeZoneAwareLocaleContext(null, TimeZone.getTimeZone("GMT+1")));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.GERMANY);
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat((Object) loc.getLocale()).isEqualTo(Locale.GERMANY);
		boolean condition = loc instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isTrue();
		assertThat((Object) ((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
	}

	@Test
	public void testSetAndResolveLocaleWithCountry() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocale(request, response, new Locale("de", "AT"));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertNotNull(cookie);
		assertThat((Object) cookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat((Object) cookie.getDomain()).isEqualTo(null);
		assertThat((Object) cookie.getPath()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_PATH);
		assertThat(cookie.getSecure()).isFalse();
		assertThat((Object) cookie.getValue()).isEqualTo("de-AT");

		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		Locale loc = resolver.resolveLocale(request);
		assertThat((Object) loc.getLanguage()).isEqualTo("de");
		assertThat((Object) loc.getCountry()).isEqualTo("AT");
	}

	@Test
	public void testSetAndResolveLocaleWithCountryAsLegacyJava() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLanguageTagCompliant(false);
		resolver.setLocale(request, response, new Locale("de", "AT"));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertNotNull(cookie);
		assertThat((Object) cookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat((Object) cookie.getDomain()).isEqualTo(null);
		assertThat((Object) cookie.getPath()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_PATH);
		assertThat(cookie.getSecure()).isFalse();
		assertThat((Object) cookie.getValue()).isEqualTo("de_AT");

		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		Locale loc = resolver.resolveLocale(request);
		assertThat((Object) loc.getLanguage()).isEqualTo("de");
		assertThat((Object) loc.getCountry()).isEqualTo("AT");
	}

	@Test
	public void testCustomCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoek");
		resolver.setCookieDomain(".springframework.org");
		resolver.setCookiePath("/mypath");
		resolver.setCookieMaxAge(10000);
		resolver.setCookieSecure(true);
		resolver.setLocale(request, response, new Locale("nl", ""));

		Cookie cookie = response.getCookie("LanguageKoek");
		assertNotNull(cookie);
		assertThat((Object) cookie.getName()).isEqualTo("LanguageKoek");
		assertThat((Object) cookie.getDomain()).isEqualTo(".springframework.org");
		assertThat((Object) cookie.getPath()).isEqualTo("/mypath");
		assertEquals(10000, cookie.getMaxAge());
		assertThat(cookie.getSecure()).isTrue();

		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoek");
		Locale loc = resolver.resolveLocale(request);
		assertThat((Object) loc.getLanguage()).isEqualTo("nl");
	}

	@Test
	public void testResolveLocaleWithoutCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);

		CookieLocaleResolver resolver = new CookieLocaleResolver();

		Locale loc = resolver.resolveLocale(request);
		assertThat((Object) loc).isEqualTo(request.getLocale());
	}

	@Test
	public void testResolveLocaleContextWithoutCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);

		CookieLocaleResolver resolver = new CookieLocaleResolver();

		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat((Object) loc.getLocale()).isEqualTo(request.getLocale());
		boolean condition = loc instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isTrue();
		assertNull(((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testResolveLocaleWithoutCookieAndDefaultLocale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setDefaultLocale(Locale.GERMAN);

		Locale loc = resolver.resolveLocale(request);
		assertThat((Object) loc).isEqualTo(Locale.GERMAN);
	}

	@Test
	public void testResolveLocaleContextWithoutCookieAndDefaultLocale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setDefaultLocale(Locale.GERMAN);
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));

		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat((Object) loc.getLocale()).isEqualTo(Locale.GERMAN);
		boolean condition = loc instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isTrue();
		assertThat((Object) ((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
	}

	@Test
	public void testResolveLocaleWithCookieWithoutLocale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, "");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();

		Locale loc = resolver.resolveLocale(request);
		assertThat((Object) loc).isEqualTo(request.getLocale());
	}

	@Test
	public void testResolveLocaleContextWithCookieWithoutLocale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, "");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();

		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat((Object) loc.getLocale()).isEqualTo(request.getLocale());
		boolean condition = loc instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isTrue();
		assertNull(((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testSetLocaleToNull() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
		request.setCookies(cookie);
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocale(request, response, null);
		Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
		assertThat((Object) locale).isEqualTo(Locale.TAIWAN);

		Cookie[] cookies = response.getCookies();
		assertEquals(1, cookies.length);
		Cookie localeCookie = cookies[0];
		assertThat((Object) localeCookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat((Object) localeCookie.getValue()).isEqualTo("");
	}

	@Test
	public void testSetLocaleContextToNull() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
		request.setCookies(cookie);
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocaleContext(request, response, null);
		Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
		assertThat((Object) locale).isEqualTo(Locale.TAIWAN);
		TimeZone timeZone = (TimeZone) request.getAttribute(CookieLocaleResolver.TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
		assertNull(timeZone);

		Cookie[] cookies = response.getCookies();
		assertEquals(1, cookies.length);
		Cookie localeCookie = cookies[0];
		assertThat((Object) localeCookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat((Object) localeCookie.getValue()).isEqualTo("");
	}

	@Test
	public void testSetLocaleToNullWithDefault() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
		request.setCookies(cookie);
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setDefaultLocale(Locale.CANADA_FRENCH);
		resolver.setLocale(request, response, null);
		Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
		assertThat((Object) locale).isEqualTo(Locale.CANADA_FRENCH);

		Cookie[] cookies = response.getCookies();
		assertEquals(1, cookies.length);
		Cookie localeCookie = cookies[0];
		assertThat((Object) localeCookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat((Object) localeCookie.getValue()).isEqualTo("");
	}

	@Test
	public void testSetLocaleContextToNullWithDefault() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
		request.setCookies(cookie);
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setDefaultLocale(Locale.CANADA_FRENCH);
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));
		resolver.setLocaleContext(request, response, null);
		Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
		assertThat((Object) locale).isEqualTo(Locale.CANADA_FRENCH);
		TimeZone timeZone = (TimeZone) request.getAttribute(CookieLocaleResolver.TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
		assertThat((Object) timeZone).isEqualTo(TimeZone.getTimeZone("GMT+1"));

		Cookie[] cookies = response.getCookies();
		assertEquals(1, cookies.length);
		Cookie localeCookie = cookies[0];
		assertThat((Object) localeCookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat((Object) localeCookie.getValue()).isEqualTo("");
	}

}
