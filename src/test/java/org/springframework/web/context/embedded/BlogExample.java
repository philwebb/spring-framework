/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.embedded;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

// FIXME delete this
public class BlogExample {

	public static void main(String[] args) {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		context.getEnvironment().getPropertySources().addFirst(new SimpleCommandLinePropertySource(args));
		context.register(PropertySourcesPlaceholderConfigurer.class, MyConfiguration.class);
		context.refresh();
	}

	@Configuration
	@EnableWebMvc
	@Import(MyController.class)
	public static class MyConfiguration {

		@Value("${port:8080}")
		private int port;

		@Bean
		public EmbeddedServletContainerFactory servletContainerFactory() {
			return new TomcatEmbeddedServletContainerFactory(this.port);
		}

		@Bean
		public DispatcherServlet dispatcher() {
			return new DispatcherServlet();
		}
	}

	@Controller
	public static class MyController {

		@RequestMapping("/hello")
		@ResponseBody
		public String sayHello() {
			return "Hello World!";
		}
	}

}
