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

package org.springframework.web.client;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import io.micrometer.observation.tck.TestObservationRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import org.springframework.web.service.invoker.HttpServiceProxyCreator;
import org.springframework.web.testfixture.servlet.MockMultipartFile;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link HttpServiceProxyCreator} calls backed by a
 * {@link RestClient} or {@link RestTemplate}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Phillip Webb
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class RestHttpServiceProxyCreatorTests {

	private final MockWebServer anotherServer = anotherServer();

	@SuppressWarnings("ConstantValue")
	@AfterEach
	void shutdown() throws IOException {
		if (this.anotherServer != null) {
			this.anotherServer.shutdown();
		}
	}

	@HttpServiceProxyCreatorTest
	void greeting(MockWebServer server, HttpServiceProxyCreator proxyCreator,
			TestObservationRegistry observationRegistry) throws Exception {
		Service service = proxyCreator.serviceProxy(Service.class);
		String response = service.getGreeting();
		RecordedRequest request = server.takeRequest();
		assertThat(response).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greeting");
		assertThat(observationRegistry).hasObservationWithNameEqualTo("http.client.requests")
			.that()
			.hasLowCardinalityKeyValue("uri", "/greeting");
	}

	@HttpServiceProxyCreatorTest
	void greetingById(MockWebServer server, HttpServiceProxyCreator proxyCreator,
			TestObservationRegistry observationRegistry) throws Exception {
		Service service = proxyCreator.serviceProxy(Service.class);
		ResponseEntity<String> response = service.getGreetingById("456");
		RecordedRequest request = server.takeRequest();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greeting/456");
		assertThat(observationRegistry).hasObservationWithNameEqualTo("http.client.requests")
			.that()
			.hasLowCardinalityKeyValue("uri", "/greeting/{id}");
	}

	@HttpServiceProxyCreatorTest
	void greetingWithDynamicUri(MockWebServer server, HttpServiceProxyCreator proxyCreator,
			TestObservationRegistry observationRegistry) throws Exception {
		Service service = proxyCreator.serviceProxy(Service.class);
		URI dynamicUri = server.url("/greeting/123").uri();
		Optional<String> response = service.getGreetingWithDynamicUri(dynamicUri, "456");
		RecordedRequest request = server.takeRequest();
		assertThat(response.orElse("empty")).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getRequestUrl().uri()).isEqualTo(dynamicUri);
		assertThat(observationRegistry).hasObservationWithNameEqualTo("http.client.requests")
			.that()
			.hasLowCardinalityKeyValue("uri", "none");
	}

	@HttpServiceProxyCreatorTest
	void postWithHeader(MockWebServer server, HttpServiceProxyCreator proxyCreator) throws Exception {
		Service service = proxyCreator.serviceProxy(Service.class);
		service.postWithHeader("testHeader", "testBody");
		RecordedRequest request = server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/greeting");
		assertThat(request.getHeaders().get("testHeaderName")).isEqualTo("testHeader");
		assertThat(request.getBody().readUtf8()).isEqualTo("testBody");
	}

	@HttpServiceProxyCreatorTest
	void formData(MockWebServer server, HttpServiceProxyCreator proxyCreator) throws Exception {
		Service service = proxyCreator.serviceProxy(Service.class);
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("param1", "value 1");
		map.add("param2", "value 2");
		service.postForm(map);
		RecordedRequest request = server.takeRequest();
		assertThat(request.getHeaders().get("Content-Type")).isEqualTo("application/x-www-form-urlencoded");
		assertThat(request.getBody().readUtf8()).isEqualTo("param1=value+1&param2=value+2");
	}

	@HttpServiceProxyCreatorTest // gh-30342
	void multipart(MockWebServer server, HttpServiceProxyCreator proxyCreator) throws Exception {
		Service service = proxyCreator.serviceProxy(Service.class);
		MultipartFile file = new MockMultipartFile("testFileName", "originalTestFileName",
				MediaType.APPLICATION_JSON_VALUE, "test".getBytes());
		service.postMultipart(file, "test2");
		RecordedRequest request = server.takeRequest();
		assertThat(request.getHeaders().get("Content-Type")).startsWith("multipart/form-data;boundary=");
		assertThat(request.getBody().readUtf8()).containsSubsequence(
				"Content-Disposition: form-data; name=\"file\"; filename=\"originalTestFileName\"",
				"Content-Type: application/json", "Content-Length: 4", "test",
				"Content-Disposition: form-data; name=\"anotherPart\"", "Content-Type: text/plain;charset=UTF-8",
				"Content-Length: 5", "test2");
	}

	@HttpServiceProxyCreatorTest
	void putWithCookies(MockWebServer server, HttpServiceProxyCreator proxyCreator) throws Exception {
		Service service = proxyCreator.serviceProxy(Service.class);
		service.putWithCookies("test1", "test2");
		RecordedRequest request = server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getHeader("Cookie")).isEqualTo("firstCookie=test1; secondCookie=test2");
	}

	@HttpServiceProxyCreatorTest
	void putWithSameNameCookies(MockWebServer server, HttpServiceProxyCreator proxyCreator) throws Exception {
		Service service = proxyCreator.serviceProxy(Service.class);
		service.putWithSameNameCookies("test1", "test2");
		RecordedRequest request = server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getHeader("Cookie")).isEqualTo("testCookie=test1; testCookie=test2");
	}

	@HttpServiceProxyCreatorTest
	void getWithUriBuilderFactory(MockWebServer server, HttpServiceProxyCreator proxyCreator)
			throws InterruptedException {
		Service service = proxyCreator.serviceProxy(Service.class);
		String url = this.anotherServer.url("/").toString();
		UriBuilderFactory factory = new DefaultUriBuilderFactory(url);
		ResponseEntity<String> actualResponse = service.getWithUriBuilderFactory(factory);
		RecordedRequest request = this.anotherServer.takeRequest();
		assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(actualResponse.getBody()).isEqualTo("Hello Spring 2!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greeting");
		assertThat(server.getRequestCount()).isEqualTo(0);
	}

	@HttpServiceProxyCreatorTest
	void getWithFactoryPathVariableAndRequestParam(MockWebServer server, HttpServiceProxyCreator proxyCreator)
			throws InterruptedException {
		Service service = proxyCreator.serviceProxy(Service.class);
		String url = this.anotherServer.url("/").toString();
		UriBuilderFactory factory = new DefaultUriBuilderFactory(url);
		ResponseEntity<String> actualResponse = service.getWithUriBuilderFactory(factory, "123", "test");
		RecordedRequest request = this.anotherServer.takeRequest();
		assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(actualResponse.getBody()).isEqualTo("Hello Spring 2!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greeting/123?param=test");
		assertThat(server.getRequestCount()).isEqualTo(0);
	}

	@HttpServiceProxyCreatorTest
	void getWithIgnoredUriBuilderFactory(MockWebServer server, HttpServiceProxyCreator proxyCreator)
			throws InterruptedException {
		Service service = proxyCreator.serviceProxy(Service.class);
		URI dynamicUri = server.url("/greeting/123").uri();
		UriBuilderFactory factory = new DefaultUriBuilderFactory(this.anotherServer.url("/").toString());
		ResponseEntity<String> actualResponse = service.getWithIgnoredUriBuilderFactory(dynamicUri, factory);
		RecordedRequest request = server.takeRequest();
		assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(actualResponse.getBody()).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greeting/123");
		assertThat(this.anotherServer.getRequestCount()).isEqualTo(0);
	}

	private static MockWebServer anotherServer() {
		MockWebServer server = new MockWebServer();
		MockResponse response = new MockResponse();
		response.setHeader("Content-Type", "text/plain").setBody("Hello Spring 2!");
		server.enqueue(response);
		return server;
	}

	public static Stream<Object[]> httpServiceProxyCreatorTestArgs() {
		return Stream.of(httpServiceProxyCreatorTestArgs(RestHttpServiceProxyCreatorTests::restClient),
				httpServiceProxyCreatorTestArgs(RestHttpServiceProxyCreatorTests::restTemplate));
	}

	@SuppressWarnings("resource")
	private static Object[] httpServiceProxyCreatorTestArgs(BiFunction<String, TestObservationRegistry, HttpServiceProxyCreator> factory) {
		MockWebServer server = new MockWebServer();
		MockResponse response = new MockResponse();
		response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!");
		server.enqueue(response);
		TestObservationRegistry observationRegistry = TestObservationRegistry.create();
		HttpServiceProxyCreator proxyCreator = factory.apply(server.url("/").toString(), observationRegistry);
		return new Object[] { server, proxyCreator, observationRegistry };
	}

	private static RestClient restClient(String url, TestObservationRegistry observationRegistry) {
		return RestClient.builder().baseUrl(url).observationRegistry(observationRegistry).build();
	}

	private static RestTemplate restTemplate(String url, TestObservationRegistry observationRegistry) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setObservationRegistry(observationRegistry);
		restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(url));
		return restTemplate;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest
	@MethodSource("httpServiceProxyCreatorTestArgs")
	@interface HttpServiceProxyCreatorTest {

	}

	private interface Service {

		@GetExchange("/greeting")
		String getGreeting();

		@GetExchange("/greeting/{id}")
		ResponseEntity<String> getGreetingById(@PathVariable String id);

		@GetExchange("/greeting/{id}")
		Optional<String> getGreetingWithDynamicUri(@Nullable URI uri, @PathVariable String id);

		@PostExchange("/greeting")
		void postWithHeader(@RequestHeader("testHeaderName") String testHeader, @RequestBody String requestBody);

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@RequestParam MultiValueMap<String, String> params);

		@PostExchange
		void postMultipart(MultipartFile file, @RequestPart String anotherPart);

		@PutExchange
		void putWithCookies(@CookieValue String firstCookie, @CookieValue String secondCookie);

		@PutExchange
		void putWithSameNameCookies(@CookieValue("testCookie") String firstCookie,
				@CookieValue("testCookie") String secondCookie);

		@GetExchange("/greeting")
		ResponseEntity<String> getWithUriBuilderFactory(UriBuilderFactory uriBuilderFactory);

		@GetExchange("/greeting/{id}")
		ResponseEntity<String> getWithUriBuilderFactory(UriBuilderFactory uriBuilderFactory, @PathVariable String id,
				@RequestParam String param);

		@GetExchange("/greeting")
		ResponseEntity<String> getWithIgnoredUriBuilderFactory(URI uri, UriBuilderFactory uriBuilderFactory);

	}

}
