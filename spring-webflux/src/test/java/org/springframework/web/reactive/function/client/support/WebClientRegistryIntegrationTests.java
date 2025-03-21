/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.reactive.function.client.support;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;
import org.springframework.web.service.registry.ImportHttpServices;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Rossen Stoyanchev
 */
public class WebClientRegistryIntegrationTests {

	private final MockWebServer server = new MockWebServer();


	@BeforeEach
	void setUp() throws Exception {
		this.server.start(9090);
	}

	@AfterEach
	void shutdown() throws IOException {
		this.server.shutdown();
	}


	@ParameterizedTest
	@ValueSource(classes = {
			ListingConfig.class, DetectConfig.class, ManualListingConfig.class, ManualDetectionConfig.class
	})
	void basic(Class<?> configClass) throws InterruptedException {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);

		EchoA echoA = context.getBean(EchoA.class);
		EchoB echoB = context.getBean(EchoB.class);

		HttpServiceProxyRegistry registry = context.getBean(HttpServiceProxyRegistry.class);

		assertThat(registry.getClient(EchoA.class)).isSameAs(echoA);
		assertThat(registry.getClient(EchoB.class)).isSameAs(echoB);

		this.server.enqueue(new MockResponse().setBody("echo"));
		this.server.enqueue(new MockResponse().setBody("echo"));

		echoA.handle("a");
		echoB.handle("b");

		RecordedRequest request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/echoA?input=a");

		request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/echoB?input=b");
	}


	private static class BaseEchoConfig {

		@Bean
		public WebClientHttpServiceGroupConfigurer groupConfigurer() {
			return groups -> groups.filterByName("echo")
					.configureClient((group, builder) -> builder.baseUrl("http://localhost:9090"));
		}
	}


	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(
			group = "echo",
			httpServiceTypes = {EchoA.class, EchoB.class},
			clientType = ClientType.WEB_CLIENT)
	private static class ListingConfig extends BaseEchoConfig {
	}


	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(
			group = "echo",
			basePackageClasses = WebClientRegistryIntegrationTests.class,
			clientType = ClientType.WEB_CLIENT)
	private static class DetectConfig extends BaseEchoConfig {
	}


	@Configuration(proxyBeanMethods = false)
	@Import(ManualListingRegistrar.class)
	private static class ManualListingConfig extends BaseEchoConfig {
	}

	private static class ManualListingRegistrar extends AbstractHttpServiceRegistrar {

		@Override
		protected void registerHttpServices(HttpServiceRegistry registry, AnnotationMetadata metadata) {
			registry.forGroup("echo", ClientType.WEB_CLIENT)
					.registerHttpServiceTypes(EchoA.class, EchoB.class);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import(ManualDetectionRegistrar.class)
	private static class ManualDetectionConfig extends BaseEchoConfig {
	}

	private static class ManualDetectionRegistrar extends AbstractHttpServiceRegistrar {

		@Override
		protected void registerHttpServices(HttpServiceRegistry registry, AnnotationMetadata metadata) {
			registry.forGroup("echo", ClientType.WEB_CLIENT)
					.detectInBasePackages(WebClientRegistryIntegrationTests.class);
		}
	}

}
