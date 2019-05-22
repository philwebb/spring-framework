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

package org.springframework.remoting.httpinvoker;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * @author Stephane Nicoll
 */
public class HttpComponentsHttpInvokerRequestExecutorTests {

	@Test
	public void customizeConnectionTimeout() throws IOException {
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor();
		executor.setConnectTimeout(5000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertThat((long) httpPost.getConfig().getConnectTimeout()).isEqualTo((long) 5000);
	}

	@Test
	public void customizeConnectionRequestTimeout() throws IOException {
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor();
		executor.setConnectionRequestTimeout(7000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertThat((long) httpPost.getConfig().getConnectionRequestTimeout()).isEqualTo((long) 7000);
	}

	@Test
	public void customizeReadTimeout() throws IOException {
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor();
		executor.setReadTimeout(10000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertThat((long) httpPost.getConfig().getSocketTimeout()).isEqualTo((long) 10000);
	}

	@Test
	public void defaultSettingsOfHttpClientMergedOnExecutorCustomization() throws IOException {
		RequestConfig defaultConfig = RequestConfig.custom().setConnectTimeout(1234).build();
		CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		given(configurable.getConfig()).willReturn(defaultConfig);

		HttpComponentsHttpInvokerRequestExecutor executor =
				new HttpComponentsHttpInvokerRequestExecutor(client);
		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertThat((Object) httpPost.getConfig()).as("Default client configuration is expected").isSameAs(defaultConfig);

		executor.setConnectionRequestTimeout(4567);
		HttpPost httpPost2 = executor.createHttpPost(config);
		assertThat((Object) httpPost2.getConfig()).isNotNull();
		assertThat((long) httpPost2.getConfig().getConnectionRequestTimeout()).isEqualTo((long) 4567);
		// Default connection timeout merged
		assertThat((long) httpPost2.getConfig().getConnectTimeout()).isEqualTo((long) 1234);
	}

	@Test
	public void localSettingsOverrideClientDefaultSettings() throws Exception {
		RequestConfig defaultConfig = RequestConfig.custom()
				.setConnectTimeout(1234).setConnectionRequestTimeout(6789).build();
		CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		given(configurable.getConfig()).willReturn(defaultConfig);

		HttpComponentsHttpInvokerRequestExecutor executor =
				new HttpComponentsHttpInvokerRequestExecutor(client);
		executor.setConnectTimeout(5000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		RequestConfig requestConfig = httpPost.getConfig();
		assertThat((long) requestConfig.getConnectTimeout()).isEqualTo((long) 5000);
		assertThat((long) requestConfig.getConnectionRequestTimeout()).isEqualTo((long) 6789);
		assertThat((long) requestConfig.getSocketTimeout()).isEqualTo((long) -1);
	}

	@Test
	public void mergeBasedOnCurrentHttpClient() throws Exception {
		RequestConfig defaultConfig = RequestConfig.custom()
				.setSocketTimeout(1234).build();
		final CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		given(configurable.getConfig()).willReturn(defaultConfig);

		HttpComponentsHttpInvokerRequestExecutor executor =
				new HttpComponentsHttpInvokerRequestExecutor() {
					@Override
					public HttpClient getHttpClient() {
						return client;
					}
				};
		executor.setReadTimeout(5000);
		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		RequestConfig requestConfig = httpPost.getConfig();
		assertThat((long) requestConfig.getConnectTimeout()).isEqualTo((long) -1);
		assertThat((long) requestConfig.getConnectionRequestTimeout()).isEqualTo((long) -1);
		assertThat((long) requestConfig.getSocketTimeout()).isEqualTo((long) 5000);

		// Update the Http client so that it returns an updated  config
		RequestConfig updatedDefaultConfig = RequestConfig.custom()
				.setConnectTimeout(1234).build();
		given(configurable.getConfig()).willReturn(updatedDefaultConfig);
		executor.setReadTimeout(7000);
		HttpPost httpPost2 = executor.createHttpPost(config);
		RequestConfig requestConfig2 = httpPost2.getConfig();
		assertThat((long) requestConfig2.getConnectTimeout()).isEqualTo((long) 1234);
		assertThat((long) requestConfig2.getConnectionRequestTimeout()).isEqualTo((long) -1);
		assertThat((long) requestConfig2.getSocketTimeout()).isEqualTo((long) 7000);
	}

	@Test
	public void ignoreFactorySettings() throws IOException {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor(httpClient) {
			@Override
			protected RequestConfig createRequestConfig(HttpInvokerClientConfiguration config) {
				return null;
			}
		};

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertThat((Object) httpPost.getConfig()).as("custom request config should not be set").isNull();
	}

	private HttpInvokerClientConfiguration mockHttpInvokerClientConfiguration(String serviceUrl) {
		HttpInvokerClientConfiguration config = mock(HttpInvokerClientConfiguration.class);
		given(config.getServiceUrl()).willReturn(serviceUrl);
		return config;
	}

}
