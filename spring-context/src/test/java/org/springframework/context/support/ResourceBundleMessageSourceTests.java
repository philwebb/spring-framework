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

package org.springframework.context.support;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;

/**
 * @author Juergen Hoeller
 * @since 03.02.2004
 */
public class ResourceBundleMessageSourceTests {

	@Test
	public void testMessageAccessWithDefaultMessageSource() {
		doTestMessageAccess(false, true, false, false, false);
	}

	@Test
	public void testMessageAccessWithDefaultMessageSourceAndMessageFormat() {
		doTestMessageAccess(false, true, false, false, true);
	}

	@Test
	public void testMessageAccessWithDefaultMessageSourceAndFallbackToGerman() {
		doTestMessageAccess(false, true, true, true, false);
	}

	@Test
	public void testMessageAccessWithDefaultMessageSourceAndFallbackTurnedOff() {
		doTestMessageAccess(false, false, false, false, false);
	}

	@Test
	public void testMessageAccessWithDefaultMessageSourceAndFallbackTurnedOffAndFallbackToGerman() {
		doTestMessageAccess(false, false, true, true, false);
	}

	@Test
	public void testMessageAccessWithReloadableMessageSource() {
		doTestMessageAccess(true, true, false, false, false);
	}

	@Test
	public void testMessageAccessWithReloadableMessageSourceAndMessageFormat() {
		doTestMessageAccess(true, true, false, false, true);
	}

	@Test
	public void testMessageAccessWithReloadableMessageSourceAndFallbackToGerman() {
		doTestMessageAccess(true, true, true, true, false);
	}

	@Test
	public void testMessageAccessWithReloadableMessageSourceAndFallbackTurnedOff() {
		doTestMessageAccess(true, false, false, false, false);
	}

	@Test
	public void testMessageAccessWithReloadableMessageSourceAndFallbackTurnedOffAndFallbackToGerman() {
		doTestMessageAccess(true, false, true, true, false);
	}

	protected void doTestMessageAccess(
			boolean reloadable, boolean fallbackToSystemLocale,
			boolean expectGermanFallback, boolean useCodeAsDefaultMessage, boolean alwaysUseMessageFormat) {

		StaticApplicationContext ac = new StaticApplicationContext();
		if (reloadable) {
			StaticApplicationContext parent = new StaticApplicationContext();
			parent.refresh();
			ac.setParent(parent);
		}

		MutablePropertyValues pvs = new MutablePropertyValues();
		String basepath = "org/springframework/context/support/";
		String[] basenames;
		if (reloadable) {
			basenames = new String[] {
					"classpath:" + basepath + "messages",
					"classpath:" + basepath + "more-messages"};
		}
		else {
			basenames = new String[] {
					basepath + "messages",
					basepath + "more-messages"};
		}
		pvs.add("basenames", basenames);
		if (!fallbackToSystemLocale) {
			pvs.add("fallbackToSystemLocale", Boolean.FALSE);
		}
		if (useCodeAsDefaultMessage) {
			pvs.add("useCodeAsDefaultMessage", Boolean.TRUE);
		}
		if (alwaysUseMessageFormat) {
			pvs.add("alwaysUseMessageFormat", Boolean.TRUE);
		}
		Class<?> clazz = reloadable ?
				(Class<?>) ReloadableResourceBundleMessageSource.class : ResourceBundleMessageSource.class;
		ac.registerSingleton("messageSource", clazz, pvs);
		ac.refresh();

		Locale.setDefault(expectGermanFallback ? Locale.GERMAN : Locale.CANADA);
		assertThat((Object) ac.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		Object expected = fallbackToSystemLocale && expectGermanFallback ? "nachricht2" : "message2";
		assertThat((Object) ac.getMessage("code2", null, Locale.ENGLISH)).isEqualTo(expected);

		assertThat((Object) ac.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
		assertThat((Object) ac.getMessage("code2", null, new Locale("DE", "at"))).isEqualTo("nochricht2");
		assertThat((Object) ac.getMessage("code2", null, new Locale("DE", "at", "oo"))).isEqualTo("noochricht2");

		if (reloadable) {
			assertThat((Object) ac.getMessage("code2", null, Locale.GERMANY)).isEqualTo("nachricht2xml");
		}

		MessageSourceAccessor accessor = new MessageSourceAccessor(ac);
		LocaleContextHolder.setLocale(new Locale("DE", "at"));
		try {
			assertThat((Object) accessor.getMessage("code2")).isEqualTo("nochricht2");
		}
		finally {
			LocaleContextHolder.setLocale(null);
		}

		assertThat((Object) ac.getMessage("code3", null, Locale.ENGLISH)).isEqualTo("message3");
		MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable("code3");
		assertThat((Object) ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("message3");
		resolvable = new DefaultMessageSourceResolvable(new String[] {"code4", "code3"});
		assertThat((Object) ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("message3");

		assertThat((Object) ac.getMessage("code3", null, Locale.ENGLISH)).isEqualTo("message3");
		resolvable = new DefaultMessageSourceResolvable(new String[] {"code4", "code3"});
		assertThat((Object) ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("message3");

		Object[] args = new Object[] {"Hello", new DefaultMessageSourceResolvable(new String[] {"code1"})};
		assertThat((Object) ac.getMessage("hello", args, Locale.ENGLISH)).isEqualTo("Hello, message1");

		// test default message without and with args
		assertNull(ac.getMessage(null, null, null, Locale.ENGLISH));
		assertThat((Object) ac.getMessage(null, null, "default", Locale.ENGLISH)).isEqualTo("default");
		assertThat((Object) ac.getMessage(null, args, "default", Locale.ENGLISH)).isEqualTo("default");
		assertThat((Object) ac.getMessage(null, null, "{0}, default", Locale.ENGLISH)).isEqualTo("{0}, default");
		assertThat((Object) ac.getMessage(null, args, "{0}, default", Locale.ENGLISH)).isEqualTo("Hello, default");

		// test resolvable with default message, without and with args
		resolvable = new DefaultMessageSourceResolvable(null, null, "default");
		assertThat((Object) ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("default");
		resolvable = new DefaultMessageSourceResolvable(null, args, "default");
		assertThat((Object) ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("default");
		resolvable = new DefaultMessageSourceResolvable(null, null, "{0}, default");
		assertThat((Object) ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("{0}, default");
		resolvable = new DefaultMessageSourceResolvable(null, args, "{0}, default");
		assertThat((Object) ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("Hello, default");

		// test message args
		assertThat((Object) ac.getMessage("hello", new Object[]{"Arg1", "Arg2"}, Locale.ENGLISH)).isEqualTo("Arg1, Arg2");
		assertThat((Object) ac.getMessage("hello", null, Locale.ENGLISH)).isEqualTo("{0}, {1}");

		if (alwaysUseMessageFormat) {
			assertThat((Object) ac.getMessage("escaped", null, Locale.ENGLISH)).isEqualTo("I'm");
		}
		else {
			assertThat((Object) ac.getMessage("escaped", null, Locale.ENGLISH)).isEqualTo("I''m");
		}
		assertThat((Object) ac.getMessage("escaped", new Object[]{"some arg"}, Locale.ENGLISH)).isEqualTo("I'm");

		if (useCodeAsDefaultMessage) {
			assertThat((Object) ac.getMessage("code4", null, Locale.GERMAN)).isEqualTo("code4");
		}
		else {
			assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
					ac.getMessage("code4", null, Locale.GERMAN));
		}
	}

	@Test
	public void testDefaultApplicationContextMessageSource() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.refresh();
		assertThat((Object) ac.getMessage("code1", null, "default", Locale.ENGLISH)).isEqualTo("default");
		assertThat((Object) ac.getMessage("code1", new Object[]{"value"}, "default {0}", Locale.ENGLISH)).isEqualTo("default value");
	}

	@Test
	public void testDefaultApplicationContextMessageSourceWithParent() {
		GenericApplicationContext ac = new GenericApplicationContext();
		GenericApplicationContext parent = new GenericApplicationContext();
		parent.refresh();
		ac.setParent(parent);
		ac.refresh();
		assertThat((Object) ac.getMessage("code1", null, "default", Locale.ENGLISH)).isEqualTo("default");
		assertThat((Object) ac.getMessage("code1", new Object[]{"value"}, "default {0}", Locale.ENGLISH)).isEqualTo("default value");
	}

	@Test
	public void testStaticApplicationContextMessageSourceWithStaticParent() {
		StaticApplicationContext ac = new StaticApplicationContext();
		StaticApplicationContext parent = new StaticApplicationContext();
		parent.refresh();
		ac.setParent(parent);
		ac.refresh();
		assertThat((Object) ac.getMessage("code1", null, "default", Locale.ENGLISH)).isEqualTo("default");
		assertThat((Object) ac.getMessage("code1", new Object[]{"value"}, "default {0}", Locale.ENGLISH)).isEqualTo("default value");
	}

	@Test
	public void testStaticApplicationContextMessageSourceWithDefaultParent() {
		StaticApplicationContext ac = new StaticApplicationContext();
		GenericApplicationContext parent = new GenericApplicationContext();
		parent.refresh();
		ac.setParent(parent);
		ac.refresh();
		assertThat((Object) ac.getMessage("code1", null, "default", Locale.ENGLISH)).isEqualTo("default");
		assertThat((Object) ac.getMessage("code1", new Object[]{"value"}, "default {0}", Locale.ENGLISH)).isEqualTo("default value");
	}

	@Test
	public void testResourceBundleMessageSourceStandalone() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		assertThat((Object) ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat((Object) ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	public void testResourceBundleMessageSourceWithWhitespaceInBasename() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("  org/springframework/context/support/messages  ");
		assertThat((Object) ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat((Object) ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	public void testResourceBundleMessageSourceWithDefaultCharset() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setDefaultEncoding("ISO-8859-1");
		assertThat((Object) ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat((Object) ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	public void testResourceBundleMessageSourceWithInappropriateDefaultCharset() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setDefaultEncoding("argh");
		ms.setFallbackToSystemLocale(false);
		assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
				ms.getMessage("code1", null, Locale.ENGLISH));
	}

	@Test
	public void testReloadableResourceBundleMessageSourceStandalone() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		assertThat((Object) ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat((Object) ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithCacheSeconds() throws InterruptedException {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setCacheSeconds(1);
		// Initial cache attempt
		assertThat((Object) ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat((Object) ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
		Thread.sleep(1100);
		// Late enough for a re-cache attempt
		assertThat((Object) ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat((Object) ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithNonConcurrentRefresh() throws InterruptedException {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setCacheSeconds(1);
		ms.setConcurrentRefresh(false);
		// Initial cache attempt
		assertThat((Object) ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat((Object) ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
		Thread.sleep(1100);
		// Late enough for a re-cache attempt
		assertThat((Object) ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat((Object) ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithCommonMessages() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		Properties commonMessages = new Properties();
		commonMessages.setProperty("warning", "Do not do {0}");
		ms.setCommonMessages(commonMessages);
		ms.setBasename("org/springframework/context/support/messages");
		assertThat((Object) ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat((Object) ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
		assertThat((Object) ms.getMessage("warning", new Object[]{"this"}, Locale.ENGLISH)).isEqualTo("Do not do this");
		assertThat((Object) ms.getMessage("warning", new Object[]{"that"}, Locale.GERMAN)).isEqualTo("Do not do that");
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithWhitespaceInBasename() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("  org/springframework/context/support/messages  ");
		assertThat((Object) ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat((Object) ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithDefaultCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setDefaultEncoding("ISO-8859-1");
		assertThat((Object) ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat((Object) ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithInappropriateDefaultCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setDefaultEncoding("unicode");
		Properties fileCharsets = new Properties();
		fileCharsets.setProperty("org/springframework/context/support/messages_de", "unicode");
		ms.setFileEncodings(fileCharsets);
		ms.setFallbackToSystemLocale(false);
		assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
				ms.getMessage("code1", null, Locale.ENGLISH));
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithInappropriateEnglishCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setFallbackToSystemLocale(false);
		Properties fileCharsets = new Properties();
		fileCharsets.setProperty("org/springframework/context/support/messages", "unicode");
		ms.setFileEncodings(fileCharsets);
		assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
				ms.getMessage("code1", null, Locale.ENGLISH));
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithInappropriateGermanCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setFallbackToSystemLocale(false);
		Properties fileCharsets = new Properties();
		fileCharsets.setProperty("org/springframework/context/support/messages_de", "unicode");
		ms.setFileEncodings(fileCharsets);
		assertThat((Object) ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat((Object) ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("message2");
	}

	@Test
	public void testReloadableResourceBundleMessageSourceFileNameCalculation() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();

		List<String> filenames = ms.calculateFilenamesForLocale("messages", Locale.ENGLISH);
		assertEquals(1, filenames.size());
		assertThat((Object) filenames.get(0)).isEqualTo("messages_en");

		filenames = ms.calculateFilenamesForLocale("messages", Locale.UK);
		assertEquals(2, filenames.size());
		assertThat((Object) filenames.get(1)).isEqualTo("messages_en");
		assertThat((Object) filenames.get(0)).isEqualTo("messages_en_GB");

		filenames = ms.calculateFilenamesForLocale("messages", new Locale("en", "GB", "POSIX"));
		assertEquals(3, filenames.size());
		assertThat((Object) filenames.get(2)).isEqualTo("messages_en");
		assertThat((Object) filenames.get(1)).isEqualTo("messages_en_GB");
		assertThat((Object) filenames.get(0)).isEqualTo("messages_en_GB_POSIX");

		filenames = ms.calculateFilenamesForLocale("messages", new Locale("en", "", "POSIX"));
		assertEquals(2, filenames.size());
		assertThat((Object) filenames.get(1)).isEqualTo("messages_en");
		assertThat((Object) filenames.get(0)).isEqualTo("messages_en__POSIX");

		filenames = ms.calculateFilenamesForLocale("messages", new Locale("", "UK", "POSIX"));
		assertEquals(2, filenames.size());
		assertThat((Object) filenames.get(1)).isEqualTo("messages__UK");
		assertThat((Object) filenames.get(0)).isEqualTo("messages__UK_POSIX");

		filenames = ms.calculateFilenamesForLocale("messages", new Locale("", "", "POSIX"));
		assertEquals(0, filenames.size());
	}

	@Test
	public void testMessageSourceResourceBundle() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		MessageSourceResourceBundle rbe = new MessageSourceResourceBundle(ms, Locale.ENGLISH);
		assertThat((Object) rbe.getString("code1")).isEqualTo("message1");
		assertThat(rbe.containsKey("code1")).isTrue();
		MessageSourceResourceBundle rbg = new MessageSourceResourceBundle(ms, Locale.GERMAN);
		assertThat((Object) rbg.getString("code2")).isEqualTo("nachricht2");
		assertThat(rbg.containsKey("code2")).isTrue();
	}


	@After
	public void tearDown() {
		ResourceBundle.clearCache();
	}

}
