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

package org.springframework.web.client.support;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.OverridingClassLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;
import org.springframework.web.service.registry.ImportHttpServices;
import org.springframework.web.service.registry.ImportHttpServices.Include;
import org.springframework.web.service.registry.annotated.AnnotatedA;
import org.springframework.web.service.registry.annotated.AnnotatedB;
import org.springframework.web.service.registry.annotated.Unannotated;
import org.springframework.web.service.registry.annotatedgreeting.AnnotatedGreetingA;
import org.springframework.web.service.registry.annotatedgreeting.AnnotatedGreetingB;
import org.springframework.web.service.registry.annotatedgreeting.UnannotatedGreeting;
import org.springframework.web.service.registry.echo.EchoA;
import org.springframework.web.service.registry.echo.EchoB;
import org.springframework.web.service.registry.greeting.GreetingA;
import org.springframework.web.service.registry.greeting.GreetingB;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Integration tests for {@link HttpServiceProxyRegistry} with a
 * {@link org.springframework.web.client.RestClient}.
 *
 * @author Rossen Stoyanchev
 */
public class RestClientProxyRegistryIntegrationTests {

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

		GreetingA greetingA = context.getBean(GreetingA.class);
		GreetingB greetingB = context.getBean(GreetingB.class);

		HttpServiceProxyRegistry registry = context.getBean(HttpServiceProxyRegistry.class);

		assertThat(registry.getClient(EchoA.class)).isSameAs(echoA);
		assertThat(registry.getClient(EchoB.class)).isSameAs(echoB);

		assertThat(registry.getClient(GreetingA.class)).isSameAs(greetingA);
		assertThat(registry.getClient(GreetingB.class)).isSameAs(greetingB);

		for (int i = 0; i < 4; i++) {
			this.server.enqueue(new MockResponse().setBody("body"));
		}

		echoA.handle("a");
		echoB.handle("b");

		RecordedRequest request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/echoA?input=a");

		request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/echoB?input=b");

		greetingA.handle("a");
		greetingB.handle("b");

		request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greetingA?input=a");

		request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greetingB?input=b");
	}

	@Test
	void beansAreCreatedUsingBeanClassLoader() {
		ClassLoader beanClassLoader = new OverridingClassLoader(getClass().getClassLoader()) {

			protected boolean isEligibleForOverriding(String className) {
				return className.contains("EchoA");
			};
		};
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.setClassLoader(beanClassLoader);
		context.register(ClassUtils.resolveClassName(ListingConfig.class.getName(), beanClassLoader));
		context.refresh();
		assertThat(context.getBean(ClassUtils.resolveClassName(EchoA.class.getName(), beanClassLoader))
			.getClass()
			.getClassLoader()).isSameAs(beanClassLoader);
	}

	@Test
	void includeAnnotatedConfig() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(IncludeAnnotatedConfig.class);
		HttpServiceProxyRegistry registry = context.getBean(HttpServiceProxyRegistry.class);
		assertThat(registry.getClientTypesInGroup(HttpServiceGroup.DEFAULT_GROUP_NAME))
				.containsOnly(AnnotatedA.class, AnnotatedB.class);
	}

	@Test
	void includeAllConfig() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(IncludeAllConfig.class);
		HttpServiceProxyRegistry registry = context.getBean(HttpServiceProxyRegistry.class);
		assertThat(registry.getClientTypesInGroup(HttpServiceGroup.DEFAULT_GROUP_NAME))
				.containsOnly(AnnotatedA.class, AnnotatedB.class, Unannotated.class);
	}

	@Test
	void includeAnnotatedConfigInGroup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(IncludeAnnotatedConfigInGroup.class);
		HttpServiceProxyRegistry registry = context.getBean(HttpServiceProxyRegistry.class);
		assertThat(registry.getGroupNames()).containsOnly("test");
		assertThat(registry.getClientTypesInGroup("test")).containsOnly(AnnotatedA.class, AnnotatedB.class);
	}

	@Test
	void includeAnnotatedWithGroupOnHttpServiceType() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(IncludeAnnotatedWithGroupOnHttpServiceType.class);
		HttpServiceProxyRegistry registry = context.getBean(HttpServiceProxyRegistry.class);
		assertThat(registry.getGroupNames()).containsOnly("greeting");
		assertThat(registry.getClientTypesInGroup("greeting")).containsOnly(AnnotatedGreetingA.class, AnnotatedGreetingB.class);
	}

	@Test
	void includeAllWithGroupOnHttpServiceType() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(IncludeAllWithGroupOnHttpServiceType.class);
		HttpServiceProxyRegistry registry = context.getBean(HttpServiceProxyRegistry.class);
		assertThat(registry.getGroupNames()).containsOnly("greeting", "default");
		assertThat(registry.getClientTypesInGroup("greeting")).containsOnly(AnnotatedGreetingA.class, AnnotatedGreetingB.class);
		assertThat(registry.getClientTypesInGroup("default")).containsOnly(UnannotatedGreeting.class);
	}

	@Test
	void includeAnnotatedWithGroupOnHttpServiceTypeAndMatchingGroup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(IncludeAnnotatedWithGroupOnHttpServiceTypeAndMatchingGroup.class);
		HttpServiceProxyRegistry registry = context.getBean(HttpServiceProxyRegistry.class);
		assertThat(registry.getGroupNames()).containsOnly("greeting");
		assertThat(registry.getClientTypesInGroup("greeting")).containsOnly(AnnotatedGreetingA.class, AnnotatedGreetingB.class);
	}

	@Test
	void includeAnnotatedWithGroupOnHttpServiceTypeAndNonMatchingGroup() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new AnnotationConfigApplicationContext(IncludeAnnotatedWithGroupOnHttpServiceTypeAndNonMatchingGroup.class))
			.withMessage("HTTP Service 'group' attributes cannot be specified on both @HttpServiceClient and @ImportHttpServices annotations");
	}

	@Test
	void includeAllWithGroupOnHttpServiceTypeAndNonMatchingGroup() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new AnnotationConfigApplicationContext(IncludeAllWithGroupOnHttpServiceTypeAndNonMatchingGroup.class))
			.withMessage("HTTP Service 'group' attributes cannot be specified on both @HttpServiceClient and @ImportHttpServices annotations");
	}

	@Test
	void includeAllWithGroupOnHttpServiceTypeAndMatchingGroup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(IncludeAllWithGroupOnHttpServiceTypeAndMatchingGroup.class);
		HttpServiceProxyRegistry registry = context.getBean(HttpServiceProxyRegistry.class);
		assertThat(registry.getGroupNames()).containsOnly("greeting");
		assertThat(registry.getClientTypesInGroup("greeting")).containsOnly(AnnotatedGreetingA.class,
				AnnotatedGreetingB.class, UnannotatedGreeting.class);
	}


	private static class ClientConfig {

		@Bean
		public RestClientHttpServiceGroupConfigurer groupConfigurer() {
			return groups -> groups.filterByName("echo", "greeting")
					.configureClient((group, builder) -> builder.baseUrl("http://localhost:9090"));
		}
	}


	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "echo", types = {EchoA.class, EchoB.class})
	@ImportHttpServices(group = "greeting", types = {GreetingA.class, GreetingB.class})
	private static class ListingConfig extends ClientConfig {
	}


	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "echo", basePackageClasses = EchoA.class)
	@ImportHttpServices(group = "greeting", basePackageClasses = GreetingA.class)
	private static class DetectConfig extends ClientConfig {
	}


	@Configuration(proxyBeanMethods = false)
	@Import(ManualListingRegistrar.class)
	private static class ManualListingConfig extends ClientConfig {
	}

	private static class ManualListingRegistrar extends AbstractHttpServiceRegistrar {

		@Override
		protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {
			registry.forGroup("echo").register(EchoA.class, EchoB.class);
			registry.forGroup("greeting").register(GreetingA.class, GreetingB.class);
		}
	}


	@Configuration(proxyBeanMethods = false)
	@Import(ManualDetectionRegistrar.class)
	private static class ManualDetectionConfig extends ClientConfig {
	}

	private static class ManualDetectionRegistrar extends AbstractHttpServiceRegistrar {

		@Override
		protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {
			registry.forGroup("echo").detectInBasePackages(EchoA.class);
			registry.forGroup("greeting").detectInBasePackages(GreetingA.class);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(include = Include.ANNOTATED_CLIENTS, basePackageClasses = AnnotatedA.class)
	private static class IncludeAnnotatedConfig extends ClientConfig {
	}

	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(include = Include.ALL, basePackageClasses = AnnotatedA.class)
	private static class IncludeAllConfig extends ClientConfig {
	}

	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(include = Include.ANNOTATED_CLIENTS, group = "test",
			basePackageClasses = AnnotatedA.class)
	private static class IncludeAnnotatedConfigInGroup extends ClientConfig {
	}

	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(include = Include.ANNOTATED_CLIENTS,
			basePackageClasses = AnnotatedGreetingA.class)
	private static class IncludeAnnotatedWithGroupOnHttpServiceType extends ClientConfig {
	}

	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(basePackageClasses = AnnotatedGreetingA.class)
	private static class IncludeAllWithGroupOnHttpServiceType extends ClientConfig {
	}

	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(include = Include.ANNOTATED_CLIENTS, group = "greeting",
			basePackageClasses = AnnotatedGreetingA.class)
	private static class IncludeAnnotatedWithGroupOnHttpServiceTypeAndMatchingGroup extends ClientConfig {
	}

	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(include = Include.ANNOTATED_CLIENTS, group = "bad",
			basePackageClasses = AnnotatedGreetingA.class)
	private static class IncludeAnnotatedWithGroupOnHttpServiceTypeAndNonMatchingGroup extends ClientConfig {
	}

	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(include = Include.ALL, group = "bad",
			basePackageClasses = AnnotatedGreetingA.class)
	private static class IncludeAllWithGroupOnHttpServiceTypeAndNonMatchingGroup extends ClientConfig {
	}

	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(include = Include.ALL, group = "greeting",
			basePackageClasses = AnnotatedGreetingA.class)
	private static class IncludeAllWithGroupOnHttpServiceTypeAndMatchingGroup extends ClientConfig {
	}
}
