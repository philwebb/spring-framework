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

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RestClientHttpServiceProxyRegistry}.
 * @author Rossen Stoyanchev
 */
public class RestClientHttpServiceProxyRegistryTests {

	private final RestClient.Builder clientBuilder = RestClient.builder();

	private final RestClientHttpServiceProxyRegistry registry = RestClientHttpServiceProxyRegistry.create(clientBuilder);

	private final MockWebServer server1 = new MockWebServer();

	@Test
	void basic() {
		registry.registerGroup("greetingServiceA", group -> group
				.addHttpServiceTypes(GreetingServiceA.class)
				.configureClient(builder -> builder.baseUrl(server1.url("/").toString())));

		registry.afterSingletonsInstantiated();

		GreetingServiceA serviceA = registry.getClientProxy(GreetingServiceA.class);

		MockResponse response = new MockResponse();
		response.setHeader("Content-Type", "text/plain").setBody("Hello, A!");
		this.server1.enqueue(response);

		assertThat(serviceA.greet()).isEqualTo("Hello, A!");
	}


	interface GreetingServiceA {

		@GetExchange("/greetA")
		String greet();

	}

}
