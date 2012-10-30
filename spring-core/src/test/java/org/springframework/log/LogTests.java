/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.log;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

/**
 * Tests for {@link LogFactory} and {@link LogWrapper}.
 * @author Phillip Webb
 */
public class LogTests {

	@Before
	public void setup() {
		System.setProperty(org.apache.commons.logging.LogFactory.FACTORY_PROPERTY,
				MockLogFactory.class.getName());
	}

	@After
	public void cleanup() {
		org.apache.commons.logging.LogFactory.releaseAll();
	}

	@Test
	public void shouldLog() throws Exception {
		LogFactory.getLog(getClass()).debug("message");
		verify(getMockLog(getClass())).debug("message");
	}

	@Test
	public void shouldLogWithThrowable() throws Exception {
		Throwable t = new Throwable();
		LogFactory.getLog(getClass()).trace("message", t);
		verify(getMockLog(getClass())).trace("message", t);
	}

	@Test
	public void shouldLogWithNullThrowable() throws Exception {
		LogFactory.getLog(getClass()).trace("message", null);
		verify(getMockLog(getClass())).trace(eq("message"), isNull(Throwable.class));
	}

	@Test
	public void shouldDelegateIsEnabled() throws Exception {
		given(getMockLog(getClass()).isDebugEnabled()).willReturn(true);
		given(getMockLog(getClass()).isTraceEnabled()).willReturn(false);
		assertThat(LogFactory.getLog(getClass()).isDebugEnabled(), is(true));
		assertThat(LogFactory.getLog(getClass()).isTraceEnabled(), is(false));
	}

	@Test
	public void shouldFormatSingleArgument() throws Exception {
		given(getMockLog(getClass()).isInfoEnabled()).willReturn(true);
		LogFactory.getLog(getClass()).infof("a%s", "b");
		verify(getMockLog(getClass())).info("ab");
	}

	@Test
	public void shouldFormatCoupleArguemnts() throws Exception {
		given(getMockLog(getClass()).isInfoEnabled()).willReturn(true);
		LogFactory.getLog(getClass()).infof("a%s%s", "b", "c");
		verify(getMockLog(getClass())).info("abc");
	}

	@Test
	public void shouldFormatMultiArguments() throws Exception {
		given(getMockLog(getClass()).isInfoEnabled()).willReturn(true);
		LogFactory.getLog(getClass()).infof("a%s%s%s", "b", "c", "d");
		verify(getMockLog(getClass())).info("abcd");
	}

	@Test
	public void shouldSkipFormatWhenNotEnabled() throws Exception {
		given(getMockLog(getClass()).isInfoEnabled()).willReturn(false);
		LogFactory.getLog(getClass()).infof("a%s%s%s", "b", "c", "d");
		verify(getMockLog(getClass())).isInfoEnabled();
		verifyNoMoreInteractions(getMockLog(getClass()));
	}

	@Test
	public void shouldFormatWithThrowable() throws Exception {
		Throwable t = new Throwable();
		given(getMockLog(getClass()).isInfoEnabled()).willReturn(true);
		LogFactory.getLog(getClass()).infof("a%s", "b", t);
		verify(getMockLog(getClass())).info("ab", t);
	}

	@Test
	public void shouldFormatMultiWithThrowable() throws Exception {
		Throwable t = new Throwable();
		given(getMockLog(getClass()).isInfoEnabled()).willReturn(true);
		LogFactory.getLog(getClass()).infof("a%s%s%s", "b", "c", "d", t);
		verify(getMockLog(getClass())).info("abcd", t);
	}

	@Test
	public void shouldLogToCategory() throws Exception {
		LogFactory.getLog(getClass()).withCategory(Category1.class).warn("message");
		verify(getMockLog(getClass())).warn("message");
		verify(getMockLog(Category1.class)).warn("message");
	}

	@Test
	public void shouldLogToCategories() throws Exception {
		LogFactory.getLog(getClass()).withCategory(Category1.class, Category2.class).warn("message");
		verify(getMockLog(getClass())).warn("message");
		verify(getMockLog(Category1.class)).warn("message");
		verify(getMockLog(Category2.class)).warn("message");
	}

	@Test
	public void shouldLogToCategoriesFromFactory() throws Exception {
		LogFactory.getLog(getClass(), Category1.class, Category2.class).warn("message");
		verify(getMockLog(getClass())).warn("message");
		verify(getMockLog(Category1.class)).warn("message");
		verify(getMockLog(Category2.class)).warn("message");
	}

	@Test
	public void shouldBeEnabledIfEitherIsEnabled() throws Exception {
		given(getMockLog(Category2.class).isFatalEnabled()).willReturn(true);
		assertThat(LogFactory.getLog(getClass()).withCategory(Category1.class, Category2.class).isFatalEnabled(), is(true));
	}

	@Test
	public void shouldDelegateAllEnableMethods() throws Exception {
		Log log = LogFactory.getLog(getClass());
		org.apache.commons.logging.Log mock = getMockLog(getClass());
		log.isTraceEnabled();
		log.isDebugEnabled();
		log.isInfoEnabled();
		log.isWarnEnabled();
		log.isErrorEnabled();
		log.isFatalEnabled();
		InOrder ordered = inOrder(mock);
		ordered.verify(mock).isTraceEnabled();
		ordered.verify(mock).isDebugEnabled();
		ordered.verify(mock).isInfoEnabled();
		ordered.verify(mock).isWarnEnabled();
		ordered.verify(mock).isErrorEnabled();
		ordered.verify(mock).isFatalEnabled();
	}

	@Test
	public void shouldDelegateAllTrace() throws Exception {
		Throwable t = new Throwable();
		Log log = LogFactory.getLog(getClass());
		org.apache.commons.logging.Log mock = getMockLog(getClass());
		given(mock.isTraceEnabled()).willReturn(true);
		log.trace("a");
		log.trace("a", t);
		log.tracef("a%s", "b");
		log.tracef("a%s", "b", t);
		log.tracef("a%s%s", "b", "c", t);
		InOrder ordered = inOrder(mock);
		ordered.verify(mock).trace("a");
		ordered.verify(mock).trace("a",t);
		ordered.verify(mock).trace("ab");
		ordered.verify(mock).trace("ab",t);
		ordered.verify(mock).trace("abc",t);
		ordered.verifyNoMoreInteractions();
	}

	@Test
	public void shouldDelegateAllDebug() throws Exception {
		Throwable t = new Throwable();
		Log log = LogFactory.getLog(getClass());
		org.apache.commons.logging.Log mock = getMockLog(getClass());
		given(mock.isDebugEnabled()).willReturn(true);
		log.debug("a");
		log.debug("a", t);
		log.debugf("a%s", "b");
		log.debugf("a%s", "b", t);
		log.debugf("a%s%s", "b", "c", t);
		InOrder ordered = inOrder(mock);
		ordered.verify(mock).debug("a");
		ordered.verify(mock).debug("a",t);
		ordered.verify(mock).debug("ab");
		ordered.verify(mock).debug("ab",t);
		ordered.verify(mock).debug("abc",t);
		ordered.verifyNoMoreInteractions();
	}

	@Test
	public void shouldDelegateAllInfo() throws Exception {
		Throwable t = new Throwable();
		Log log = LogFactory.getLog(getClass());
		org.apache.commons.logging.Log mock = getMockLog(getClass());
		given(mock.isInfoEnabled()).willReturn(true);
		log.info("a");
		log.info("a", t);
		log.infof("a%s", "b");
		log.infof("a%s", "b", t);
		log.infof("a%s%s", "b", "c", t);
		InOrder ordered = inOrder(mock);
		ordered.verify(mock).info("a");
		ordered.verify(mock).info("a",t);
		ordered.verify(mock).info("ab");
		ordered.verify(mock).info("ab",t);
		ordered.verify(mock).info("abc",t);
		ordered.verifyNoMoreInteractions();
	}

	@Test
	public void shouldDelegateAllWarn() throws Exception {
		Throwable t = new Throwable();
		Log log = LogFactory.getLog(getClass());
		org.apache.commons.logging.Log mock = getMockLog(getClass());
		given(mock.isWarnEnabled()).willReturn(true);
		log.warn("a");
		log.warn("a", t);
		log.warnf("a%s", "b");
		log.warnf("a%s", "b", t);
		log.warnf("a%s%s", "b", "c", t);
		InOrder ordered = inOrder(mock);
		ordered.verify(mock).warn("a");
		ordered.verify(mock).warn("a",t);
		ordered.verify(mock).warn("ab");
		ordered.verify(mock).warn("ab",t);
		ordered.verify(mock).warn("abc",t);
		ordered.verifyNoMoreInteractions();
	}

	@Test
	public void shouldDelegateAllError() throws Exception {
		Throwable t = new Throwable();
		Log log = LogFactory.getLog(getClass());
		org.apache.commons.logging.Log mock = getMockLog(getClass());
		given(mock.isErrorEnabled()).willReturn(true);
		log.error("a");
		log.error("a", t);
		log.errorf("a%s", "b");
		log.errorf("a%s", "b", t);
		log.errorf("a%s%s", "b", "c", t);
		InOrder ordered = inOrder(mock);
		ordered.verify(mock).error("a");
		ordered.verify(mock).error("a",t);
		ordered.verify(mock).error("ab");
		ordered.verify(mock).error("ab",t);
		ordered.verify(mock).error("abc",t);
		ordered.verifyNoMoreInteractions();
	}

	@Test
	public void shouldDelegateAllFatal() throws Exception {
		Throwable t = new Throwable();
		Log log = LogFactory.getLog(getClass());
		org.apache.commons.logging.Log mock = getMockLog(getClass());
		given(mock.isFatalEnabled()).willReturn(true);
		log.fatal("a");
		log.fatal("a", t);
		log.fatalf("a%s", "b");
		log.fatalf("a%s", "b", t);
		log.fatalf("a%s%s", "b", "c", t);
		InOrder ordered = inOrder(mock);
		ordered.verify(mock).fatal("a");
		ordered.verify(mock).fatal("a",t);
		ordered.verify(mock).fatal("ab");
		ordered.verify(mock).fatal("ab",t);
		ordered.verify(mock).fatal("abc",t);
		ordered.verifyNoMoreInteractions();
	}

	@Test
	public void shouldRemapLevel() throws Exception {
		org.apache.commons.logging.LogFactory.getFactory().setAttribute(
				Category1.class.getName(), getClass().getName() + ".TRACE->DEBUG");
		LogFactory.getLog(getClass()).withCategory(Category1.class).trace("message");
		verify(getMockLog(getClass())).trace("message");
		verify(getMockLog(Category1.class)).debug("message");
	}

	private org.apache.commons.logging.Log getMockLog(Class<?> logClass) {
		return org.apache.commons.logging.LogFactory.getLog(logClass);
	}

	private static class Category1 {
	}

	private static class Category2 {
	}

	public static class MockLogFactory extends LogFactoryImpl {

		private final Map<String, org.apache.commons.logging.Log> logs = new HashMap<String, org.apache.commons.logging.Log>();

		@Override
		@SuppressWarnings("rawtypes")
		public org.apache.commons.logging.Log getInstance(Class clazz)
				throws LogConfigurationException {
			return getInstance(clazz.getName());
		}

		@Override
		public org.apache.commons.logging.Log getInstance(String name)
				throws LogConfigurationException {
			org.apache.commons.logging.Log log = this.logs.get(name);
			if (log == null) {
				log = mock(org.apache.commons.logging.Log.class);
				this.logs.put(name, log);
			}
			return log;
		}

		@Override
		public void release() {
		}
	}

}
