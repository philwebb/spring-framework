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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.PathMatcher;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.util.UrlPathHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * A test fixture for {@link DelegatingWebMvcConfiguration} tests.
 *
 * @author Rossen Stoyanchev
 */
public class DelegatingWebMvcConfigurationTests {

	private DelegatingWebMvcConfiguration delegatingConfig;

	@Mock
	private WebMvcConfigurer webMvcConfigurer;

	@Captor
	private ArgumentCaptor<List<HttpMessageConverter<?>>> converters;

	@Captor
	private ArgumentCaptor<ContentNegotiationConfigurer> contentNegotiationConfigurer;

	@Captor
	private ArgumentCaptor<FormattingConversionService> conversionService;

	@Captor
	private ArgumentCaptor<List<HandlerMethodArgumentResolver>> resolvers;

	@Captor
	private ArgumentCaptor<List<HandlerMethodReturnValueHandler>> handlers;

	@Captor
	private ArgumentCaptor<AsyncSupportConfigurer> asyncConfigurer;

	@Captor
	private ArgumentCaptor<List<HandlerExceptionResolver>> exceptionResolvers;


	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		this.delegatingConfig = new DelegatingWebMvcConfiguration();
	}


	@Test
	public void requestMappingHandlerAdapter() throws Exception {
		this.delegatingConfig.setConfigurers(Collections.singletonList(this.webMvcConfigurer));
		RequestMappingHandlerAdapter adapter = this.delegatingConfig.requestMappingHandlerAdapter();

		ConfigurableWebBindingInitializer initializer =
				(ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();

		verify(this.webMvcConfigurer).configureMessageConverters(this.converters.capture());
		verify(this.webMvcConfigurer).configureContentNegotiation(this.contentNegotiationConfigurer.capture());
		verify(this.webMvcConfigurer).addFormatters(this.conversionService.capture());
		verify(this.webMvcConfigurer).addArgumentResolvers(this.resolvers.capture());
		verify(this.webMvcConfigurer).addReturnValueHandlers(this.handlers.capture());
		verify(this.webMvcConfigurer).configureAsyncSupport(this.asyncConfigurer.capture());

		assertNotNull(initializer);
		assertSame(this.conversionService.getValue(), initializer.getConversionService());
		assertTrue(initializer.getValidator() instanceof LocalValidatorFactoryBean);
		assertEquals(0, this.resolvers.getValue().size());
		assertEquals(0, this.handlers.getValue().size());
		assertEquals(this.converters.getValue(), adapter.getMessageConverters());
		assertNotNull(this.asyncConfigurer);
	}

	@Test
	public void configureMessageConverters() {
		final HttpMessageConverter<?> customConverter = mock(HttpMessageConverter.class);
		final StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
		List<WebMvcConfigurer> configurers = new ArrayList<>();
		configurers.add(new WebMvcConfigurer() {
			@Override
			public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
				converters.add(stringConverter);
			}
			@Override
			public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
				converters.add(0, customConverter);
			}
		});
		this.delegatingConfig = new DelegatingWebMvcConfiguration();
		this.delegatingConfig.setConfigurers(configurers);

		RequestMappingHandlerAdapter adapter = this.delegatingConfig.requestMappingHandlerAdapter();
		assertEquals("Only one custom converter should be registered", 2, adapter.getMessageConverters().size());
		assertSame(customConverter, adapter.getMessageConverters().get(0));
		assertSame(stringConverter, adapter.getMessageConverters().get(1));
	}

	@Test
	public void getCustomValidator() {
		given(this.webMvcConfigurer.getValidator()).willReturn(new LocalValidatorFactoryBean());

		this.delegatingConfig.setConfigurers(Collections.singletonList(this.webMvcConfigurer));
		this.delegatingConfig.mvcValidator();

		verify(this.webMvcConfigurer).getValidator();
	}

	@Test
	public void getCustomMessageCodesResolver() {
		given(this.webMvcConfigurer.getMessageCodesResolver()).willReturn(new DefaultMessageCodesResolver());

		this.delegatingConfig.setConfigurers(Collections.singletonList(this.webMvcConfigurer));
		this.delegatingConfig.getMessageCodesResolver();

		verify(this.webMvcConfigurer).getMessageCodesResolver();
	}

	@Test
	public void handlerExceptionResolver() throws Exception {
		this.delegatingConfig.setConfigurers(Collections.singletonList(this.webMvcConfigurer));
		this.delegatingConfig.handlerExceptionResolver();

		verify(this.webMvcConfigurer).configureMessageConverters(this.converters.capture());
		verify(this.webMvcConfigurer).configureContentNegotiation(this.contentNegotiationConfigurer.capture());
		verify(this.webMvcConfigurer).configureHandlerExceptionResolvers(this.exceptionResolvers.capture());

		assertEquals(3, this.exceptionResolvers.getValue().size());
		assertTrue(this.exceptionResolvers.getValue().get(0) instanceof ExceptionHandlerExceptionResolver);
		assertTrue(this.exceptionResolvers.getValue().get(1) instanceof ResponseStatusExceptionResolver);
		assertTrue(this.exceptionResolvers.getValue().get(2) instanceof DefaultHandlerExceptionResolver);
		assertTrue(this.converters.getValue().size() > 0);
	}

	@Test
	public void configureExceptionResolvers() throws Exception {
		List<WebMvcConfigurer> configurers = new ArrayList<>();
		configurers.add(new WebMvcConfigurer() {
			@Override
			public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
				exceptionResolvers.add(new DefaultHandlerExceptionResolver());
			}
		});
		this.delegatingConfig.setConfigurers(configurers);

		HandlerExceptionResolverComposite composite =
				(HandlerExceptionResolverComposite) this.delegatingConfig.handlerExceptionResolver();
		assertEquals("Only one custom converter is expected", 1, composite.getExceptionResolvers().size());
	}

	@Test
	public void configurePathMatch() throws Exception {
		final PathMatcher pathMatcher = mock(PathMatcher.class);
		final UrlPathHelper pathHelper = mock(UrlPathHelper.class);

		List<WebMvcConfigurer> configurers = new ArrayList<>();
		configurers.add(new WebMvcConfigurer() {
			@Override
			public void configurePathMatch(PathMatchConfigurer configurer) {
				configurer.setUseRegisteredSuffixPatternMatch(true)
						.setUseTrailingSlashMatch(false)
						.setUrlPathHelper(pathHelper)
						.setPathMatcher(pathMatcher);
			}
		});
		this.delegatingConfig.setConfigurers(configurers);

		RequestMappingHandlerMapping handlerMapping = this.delegatingConfig.requestMappingHandlerMapping();
		assertNotNull(handlerMapping);
		assertEquals("PathMatchConfigurer should configure RegisteredSuffixPatternMatch",
				true, handlerMapping.useRegisteredSuffixPatternMatch());
		assertEquals("PathMatchConfigurer should configure SuffixPatternMatch",
				true, handlerMapping.useSuffixPatternMatch());
		assertEquals("PathMatchConfigurer should configure TrailingSlashMatch",
				false, handlerMapping.useTrailingSlashMatch());
		assertEquals("PathMatchConfigurer should configure UrlPathHelper",
				pathHelper, handlerMapping.getUrlPathHelper());
		assertEquals("PathMatchConfigurer should configure PathMatcher",
				pathMatcher, handlerMapping.getPathMatcher());
	}

}
