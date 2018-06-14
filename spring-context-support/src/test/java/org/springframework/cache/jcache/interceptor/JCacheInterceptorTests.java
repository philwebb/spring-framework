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

package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;

import org.junit.Test;

import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.NamedCacheResolver;
import org.springframework.cache.jcache.AbstractJCacheTests;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class JCacheInterceptorTests extends AbstractJCacheTests {

	private final CacheOperationInvoker dummyInvoker = new DummyInvoker(null);

	@Test
	public void severalCachesNotSupported() {
		JCacheInterceptor interceptor = createInterceptor(createOperationSource(
				this.cacheManager, new NamedCacheResolver(this.cacheManager, "default", "simpleCache"),
				this.defaultExceptionCacheResolver, this.defaultKeyGenerator));

		AnnotatedJCacheableService service = new AnnotatedJCacheableService(this.cacheManager.getCache("default"));
		Method m = ReflectionUtils.findMethod(AnnotatedJCacheableService.class, "cache", String.class);

		try {
			interceptor.execute(this.dummyInvoker, service, m, new Object[] {"myId"});
		}
		catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("JSR-107 only supports a single cache"));
		}
		catch (Throwable ex) {
			fail("Unexpected: " + ex);
		}
	}

	@Test
	public void noCacheCouldBeResolved() {
		JCacheInterceptor interceptor = createInterceptor(createOperationSource(
				this.cacheManager, new NamedCacheResolver(this.cacheManager), // Returns empty list
				this.defaultExceptionCacheResolver, this.defaultKeyGenerator));

		AnnotatedJCacheableService service = new AnnotatedJCacheableService(this.cacheManager.getCache("default"));
		Method m = ReflectionUtils.findMethod(AnnotatedJCacheableService.class, "cache", String.class);

		try {
			interceptor.execute(this.dummyInvoker, service, m, new Object[] {"myId"});
		}
		catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("Cache could not have been resolved for"));
		}
		catch (Throwable ex) {
			fail("Unexpected: " + ex);
		}
	}

	@Test
	public void cacheManagerMandatoryIfCacheResolverNotSet() {
		this.thrown.expect(IllegalStateException.class);
		createOperationSource(null, null, null, this.defaultKeyGenerator);
	}

	@Test
	public void cacheManagerOptionalIfCacheResolversSet() {
		createOperationSource(null, this.defaultCacheResolver, this.defaultExceptionCacheResolver, this.defaultKeyGenerator);
	}

	@Test
	public void cacheResultReturnsProperType() throws Throwable {
		JCacheInterceptor interceptor = createInterceptor(createOperationSource(
				this.cacheManager, this.defaultCacheResolver, this.defaultExceptionCacheResolver, this.defaultKeyGenerator));

		AnnotatedJCacheableService service = new AnnotatedJCacheableService(this.cacheManager.getCache("default"));
		Method method = ReflectionUtils.findMethod(AnnotatedJCacheableService.class, "cache", String.class);

		CacheOperationInvoker invoker = new DummyInvoker(0L);
		Object execute = interceptor.execute(invoker, service, method, new Object[] {"myId"});
		assertNotNull("result cannot be null.", execute);
		assertEquals("Wrong result type", Long.class, execute.getClass());
		assertEquals("Wrong result", 0L, execute);
	}

	protected JCacheOperationSource createOperationSource(CacheManager cacheManager,
			CacheResolver cacheResolver, CacheResolver exceptionCacheResolver, KeyGenerator keyGenerator) {

		DefaultJCacheOperationSource source = new DefaultJCacheOperationSource();
		source.setCacheManager(cacheManager);
		source.setCacheResolver(cacheResolver);
		source.setExceptionCacheResolver(exceptionCacheResolver);
		source.setKeyGenerator(keyGenerator);
		source.setBeanFactory(new StaticListableBeanFactory());
		source.afterPropertiesSet();
		source.afterSingletonsInstantiated();
		return source;
	}


	protected JCacheInterceptor createInterceptor(JCacheOperationSource source) {
		JCacheInterceptor interceptor = new JCacheInterceptor();
		interceptor.setCacheOperationSource(source);
		interceptor.afterPropertiesSet();
		return interceptor;
	}


	private static class DummyInvoker implements CacheOperationInvoker {

		private final Object result;

		private DummyInvoker(Object result) {
			this.result = result;
		}

		@Override
		public Object invoke() throws ThrowableWrapper {
			return this.result;
		}
	}

}
