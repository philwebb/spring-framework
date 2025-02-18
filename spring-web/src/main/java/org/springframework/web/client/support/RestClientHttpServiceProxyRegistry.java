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

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.registry.AbstractHttpServiceProxyRegistry;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

/**
 * {@link HttpServiceProxyRegistry} for a {@link RestClient.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public final class RestClientHttpServiceProxyRegistry extends AbstractHttpServiceProxyRegistry<RestClientHttpServiceGroup> {

	private final RestClient.Builder baseClientBuilder;


	private RestClientHttpServiceProxyRegistry(RestClient.Builder baseClientBuilder) {
		this.baseClientBuilder = baseClientBuilder;
	}


	@Override
	protected RestClientHttpServiceGroup createGroup(
			String id, ClassPathScanningCandidateComponentProvider componentProvider) {

		RestClient.Builder builder = this.baseClientBuilder.clone();
		return new RestClientHttpServiceGroup(id, builder, componentProvider);
	}


	public static RestClientHttpServiceProxyRegistry create(RestClient.Builder baseClientBuilder) {
		return new RestClientHttpServiceProxyRegistry(baseClientBuilder);
	}

}
