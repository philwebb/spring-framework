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

package org.springframework.web.reactive.config;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Test fixture for {@link DelegatingWebFluxConfiguration} tests.
 *
 * @author Brian Clozel
 */
public class DelegatingWebFluxConfigurationTests {

	private DelegatingWebFluxConfiguration delegatingConfig;

	@Mock
	private WebFluxConfigurer webFluxConfigurer;

	@Captor
	private ArgumentCaptor<ServerCodecConfigurer> codecsConfigurer;

	@Captor
	private ArgumentCaptor<List<HttpMessageWriter<?>>> writers;

	@Captor
	private ArgumentCaptor<FormatterRegistry> formatterRegistry;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.delegatingConfig = new DelegatingWebFluxConfiguration();
		this.delegatingConfig.setApplicationContext(new StaticApplicationContext());
		given(this.webFluxConfigurer.getValidator()).willReturn(null);
		given(this.webFluxConfigurer.getMessageCodesResolver()).willReturn(null);
	}


	@Test
	public void requestMappingHandlerMapping() throws Exception {
		this.delegatingConfig.setConfigurers(Collections.singletonList(this.webFluxConfigurer));
		this.delegatingConfig.requestMappingHandlerMapping();

		verify(this.webFluxConfigurer).configureContentTypeResolver(any(RequestedContentTypeResolverBuilder.class));
		verify(this.webFluxConfigurer).addCorsMappings(any(CorsRegistry.class));
		verify(this.webFluxConfigurer).configurePathMatching(any(PathMatchConfigurer.class));
	}

	@Test
	public void requestMappingHandlerAdapter() throws Exception {
		this.delegatingConfig.setConfigurers(Collections.singletonList(this.webFluxConfigurer));

		ConfigurableWebBindingInitializer initializer = (ConfigurableWebBindingInitializer)
				this.delegatingConfig.requestMappingHandlerAdapter().getWebBindingInitializer();

		verify(this.webFluxConfigurer).configureHttpMessageCodecs(this.codecsConfigurer.capture());
		verify(this.webFluxConfigurer).getValidator();
		verify(this.webFluxConfigurer).getMessageCodesResolver();
		verify(this.webFluxConfigurer).addFormatters(this.formatterRegistry.capture());
		verify(this.webFluxConfigurer).configureArgumentResolvers(any());

		assertNotNull(initializer);
		assertTrue(initializer.getValidator() instanceof LocalValidatorFactoryBean);
		assertSame(this.formatterRegistry.getValue(), initializer.getConversionService());
		assertEquals(12, this.codecsConfigurer.getValue().getReaders().size());
	}

	@Test
	public void resourceHandlerMapping() throws Exception {
		this.delegatingConfig.setConfigurers(Collections.singletonList(this.webFluxConfigurer));
		doAnswer((invocation) -> {
			ResourceHandlerRegistry registry = invocation.getArgument(0);
			registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static");
			return null;
		}).when(this.webFluxConfigurer).addResourceHandlers(any(ResourceHandlerRegistry.class));

		this.delegatingConfig.resourceHandlerMapping();
		verify(this.webFluxConfigurer).addResourceHandlers(any(ResourceHandlerRegistry.class));
		verify(this.webFluxConfigurer).configurePathMatching(any(PathMatchConfigurer.class));
	}

	@Test
	public void responseBodyResultHandler() throws Exception {
		this.delegatingConfig.setConfigurers(Collections.singletonList(this.webFluxConfigurer));
		this.delegatingConfig.responseBodyResultHandler();

		verify(this.webFluxConfigurer).configureHttpMessageCodecs(this.codecsConfigurer.capture());
		verify(this.webFluxConfigurer).configureContentTypeResolver(any(RequestedContentTypeResolverBuilder.class));
	}

	@Test
	public void viewResolutionResultHandler() throws Exception {
		this.delegatingConfig.setConfigurers(Collections.singletonList(this.webFluxConfigurer));
		this.delegatingConfig.viewResolutionResultHandler();

		verify(this.webFluxConfigurer).configureViewResolvers(any(ViewResolverRegistry.class));
	}

}
