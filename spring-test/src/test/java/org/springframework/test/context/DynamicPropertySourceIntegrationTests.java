/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link DynamicPropertySource @DynamicPropertySource}.
 *
 * @author Phillip Webb
 */
@SpringJUnitConfig
@Testcontainers
class DynamicPropertySourceIntegrationTests {

	@Container
	static GenericContainer<?> redis = new GenericContainer<>(
			"redis:5.0.3-alpine").withExposedPorts(6379);

	@Autowired
	private RedisService service;

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyValues values) {
		values.add("test.redis.ip", redis::getContainerIpAddress);
		values.add("test.redis.port", redis::getFirstMappedPort);
	}

	@Test
	void hasInjectedValues() {
		assertThat(this.service.getIp()).isNotEmpty();
		assertThat(this.service.getPort()).isGreaterThan(0);
	}

	@Configuration
	@Import(RedisService.class)
	static class Config {

	}

	@Service
	static class RedisService {

		private final String ip;

		private final int port;

		RedisService(@Value("${test.redis.ip}") String ip,
				@Value("${test.redis.port}") int port) {
			this.ip = ip;
			this.port = port;
		}

		String getIp() {
			return ip;
		}

		int getPort() {
			return port;
		}

	}

}
