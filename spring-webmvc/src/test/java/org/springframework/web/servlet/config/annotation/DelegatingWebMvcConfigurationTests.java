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

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertSame;
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
		delegatingConfig = new DelegatingWebMvcConfiguration();
	}


	@Test
	public void requestMappingHandlerAdapter() throws Exception {
		delegatingConfig.setConfigurers(Collections.singletonList(webMvcConfigurer));
		RequestMappingHandlerAdapter adapter = this.delegatingConfig.requestMappingHandlerAdapter(
				this.delegatingConfig.mvcContentNegotiationManager(), this.delegatingConfig.mvcConversionService(),
				this.delegatingConfig.mvcValidator());

		ConfigurableWebBindingInitializer initializer =
				(ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();

		verify(webMvcConfigurer).configureMessageConverters(converters.capture());
		verify(webMvcConfigurer).configureContentNegotiation(contentNegotiationConfigurer.capture());
		verify(webMvcConfigurer).addFormatters(conversionService.capture());
		verify(webMvcConfigurer).addArgumentResolvers(resolvers.capture());
		verify(webMvcConfigurer).addReturnValueHandlers(handlers.capture());
		verify(webMvcConfigurer).configureAsyncSupport(asyncConfigurer.capture());

		assertNotNull(initializer);
		assertSame(conversionService.getValue(), initializer.getConversionService());
		boolean condition = initializer.getValidator() instanceof LocalValidatorFactoryBean;
		assertThat(condition).isTrue();
		assertEquals(0, resolvers.getValue().size());
		assertEquals(0, handlers.getValue().size());
		assertThat((Object) adapter.getMessageConverters()).isEqualTo(converters.getValue());
		assertNotNull(asyncConfigurer);
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
		delegatingConfig = new DelegatingWebMvcConfiguration();
		delegatingConfig.setConfigurers(configurers);

		RequestMappingHandlerAdapter adapter = delegatingConfig.requestMappingHandlerAdapter(
				this.delegatingConfig.mvcContentNegotiationManager(), this.delegatingConfig.mvcConversionService(),
				this.delegatingConfig.mvcValidator());
		assertEquals("Only one custom converter should be registered", 2, adapter.getMessageConverters().size());
		assertSame(customConverter, adapter.getMessageConverters().get(0));
		assertSame(stringConverter, adapter.getMessageConverters().get(1));
	}

	@Test
	public void getCustomValidator() {
		given(webMvcConfigurer.getValidator()).willReturn(new LocalValidatorFactoryBean());

		delegatingConfig.setConfigurers(Collections.singletonList(webMvcConfigurer));
		delegatingConfig.mvcValidator();

		verify(webMvcConfigurer).getValidator();
	}

	@Test
	public void getCustomMessageCodesResolver() {
		given(webMvcConfigurer.getMessageCodesResolver()).willReturn(new DefaultMessageCodesResolver());

		delegatingConfig.setConfigurers(Collections.singletonList(webMvcConfigurer));
		delegatingConfig.getMessageCodesResolver();

		verify(webMvcConfigurer).getMessageCodesResolver();
	}

	@Test
	public void handlerExceptionResolver() throws Exception {
		delegatingConfig.setConfigurers(Collections.singletonList(webMvcConfigurer));
		delegatingConfig.handlerExceptionResolver(delegatingConfig.mvcContentNegotiationManager());

		verify(webMvcConfigurer).configureMessageConverters(converters.capture());
		verify(webMvcConfigurer).configureContentNegotiation(contentNegotiationConfigurer.capture());
		verify(webMvcConfigurer).configureHandlerExceptionResolvers(exceptionResolvers.capture());

		assertEquals(3, exceptionResolvers.getValue().size());
		boolean condition2 = exceptionResolvers.getValue().get(0) instanceof ExceptionHandlerExceptionResolver;
		assertThat(condition2).isTrue();
		boolean condition1 = exceptionResolvers.getValue().get(1) instanceof ResponseStatusExceptionResolver;
		assertThat(condition1).isTrue();
		boolean condition = exceptionResolvers.getValue().get(2) instanceof DefaultHandlerExceptionResolver;
		assertThat(condition).isTrue();
		assertThat(converters.getValue().size() > 0).isTrue();
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
		delegatingConfig.setConfigurers(configurers);

		HandlerExceptionResolverComposite composite =
				(HandlerExceptionResolverComposite) delegatingConfig
						.handlerExceptionResolver(delegatingConfig.mvcContentNegotiationManager());
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
		delegatingConfig.setConfigurers(configurers);

		RequestMappingHandlerMapping handlerMapping = delegatingConfig.requestMappingHandlerMapping(
				delegatingConfig.mvcContentNegotiationManager(), delegatingConfig.mvcConversionService(),
				delegatingConfig.mvcResourceUrlProvider());
		assertNotNull(handlerMapping);
		assertThat((Object) handlerMapping.useRegisteredSuffixPatternMatch()).as("PathMatchConfigurer should configure RegisteredSuffixPatternMatch").isEqualTo(true);
		assertThat((Object) handlerMapping.useSuffixPatternMatch()).as("PathMatchConfigurer should configure SuffixPatternMatch").isEqualTo(true);
		assertThat((Object) handlerMapping.useTrailingSlashMatch()).as("PathMatchConfigurer should configure TrailingSlashMatch").isEqualTo(false);
		assertThat((Object) handlerMapping.getUrlPathHelper()).as("PathMatchConfigurer should configure UrlPathHelper").isEqualTo(pathHelper);
		assertThat((Object) handlerMapping.getPathMatcher()).as("PathMatchConfigurer should configure PathMatcher").isEqualTo(pathMatcher);
	}

}
