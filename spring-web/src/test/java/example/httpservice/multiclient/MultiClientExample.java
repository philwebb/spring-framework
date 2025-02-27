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

package example.httpservice.multiclient;

import java.util.Map;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ComponentScan
public class MultiClientExample {

	@Bean
	RestClient echo() {
		return RestClient.builder().baseUrl("https://echo.zuplo.io").build();
	}

	@Bean
	RestClient ecommerce() {
		return RestClient.builder().baseUrl("https://ecommerce-api.zuplo.io").build();
	}

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MultiClientExample.class);
		EchoService echoService = context.getBean(EchoService.class);
		System.out.println(echoService.echo(Map.of("hello", "world")));
		EcommerceService ecommerceService = context.getBean(EcommerceService.class);
		System.out.println(ecommerceService.users());
	}

}
