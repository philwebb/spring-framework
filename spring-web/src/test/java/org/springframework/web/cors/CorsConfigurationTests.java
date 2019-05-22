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

package org.springframework.web.cors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;

/**
 * Unit tests for {@link CorsConfiguration}.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
public class CorsConfigurationTests {

	@Test
	public void setNullValues() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(null);
		assertNull(config.getAllowedOrigins());
		config.setAllowedHeaders(null);
		assertNull(config.getAllowedHeaders());
		config.setAllowedMethods(null);
		assertNull(config.getAllowedMethods());
		config.setExposedHeaders(null);
		assertNull(config.getExposedHeaders());
		config.setAllowCredentials(null);
		assertNull(config.getAllowCredentials());
		config.setMaxAge((Long) null);
		assertNull(config.getMaxAge());
	}

	@Test
	public void setValues() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		assertThat((Object) config.getAllowedOrigins()).isEqualTo(Arrays.asList("*"));
		config.addAllowedHeader("*");
		assertThat((Object) config.getAllowedHeaders()).isEqualTo(Arrays.asList("*"));
		config.addAllowedMethod("*");
		assertThat((Object) config.getAllowedMethods()).isEqualTo(Arrays.asList("*"));
		config.addExposedHeader("header1");
		config.addExposedHeader("header2");
		assertThat((Object) config.getExposedHeaders()).isEqualTo(Arrays.asList("header1", "header2"));
		config.setAllowCredentials(true);
		assertThat((boolean) config.getAllowCredentials()).isTrue();
		config.setMaxAge(123L);
		assertThat((Object) config.getMaxAge()).isEqualTo(new Long(123));
	}

	@Test
	public void asteriskWildCardOnAddExposedHeader() {
		CorsConfiguration config = new CorsConfiguration();
		assertThatIllegalArgumentException().isThrownBy(() ->
				config.addExposedHeader("*"));
	}

	@Test
	public void asteriskWildCardOnSetExposedHeaders() {
		CorsConfiguration config = new CorsConfiguration();
		assertThatIllegalArgumentException().isThrownBy(() ->
				config.setExposedHeaders(Arrays.asList("*")));
	}

	@Test
	public void combineWithNull() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.asList("*"));
		config.combine(null);
		assertThat((Object) config.getAllowedOrigins()).isEqualTo(Arrays.asList("*"));
	}

	@Test
	public void combineWithNullProperties() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		config.addAllowedHeader("header1");
		config.addExposedHeader("header3");
		config.addAllowedMethod(HttpMethod.GET.name());
		config.setMaxAge(123L);
		config.setAllowCredentials(true);
		CorsConfiguration other = new CorsConfiguration();
		config = config.combine(other);
		assertThat((Object) config.getAllowedOrigins()).isEqualTo(Arrays.asList("*"));
		assertThat((Object) config.getAllowedHeaders()).isEqualTo(Arrays.asList("header1"));
		assertThat((Object) config.getExposedHeaders()).isEqualTo(Arrays.asList("header3"));
		assertThat((Object) config.getAllowedMethods()).isEqualTo(Arrays.asList(HttpMethod.GET.name()));
		assertThat((Object) config.getMaxAge()).isEqualTo(new Long(123));
		assertThat((boolean) config.getAllowCredentials()).isTrue();
	}

	@Test  // SPR-15772
	public void combineWithDefaultPermitValues() {
		CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOrigin("https://domain.com");
		other.addAllowedHeader("header1");
		other.addAllowedMethod(HttpMethod.PUT.name());

		CorsConfiguration combinedConfig = config.combine(other);
		assertThat((Object) combinedConfig.getAllowedOrigins()).isEqualTo(Arrays.asList("https://domain.com"));
		assertThat((Object) combinedConfig.getAllowedHeaders()).isEqualTo(Arrays.asList("header1"));
		assertThat((Object) combinedConfig.getAllowedMethods()).isEqualTo(Arrays.asList(HttpMethod.PUT.name()));

		combinedConfig = other.combine(config);
		assertThat((Object) combinedConfig.getAllowedOrigins()).isEqualTo(Arrays.asList("https://domain.com"));
		assertThat((Object) combinedConfig.getAllowedHeaders()).isEqualTo(Arrays.asList("header1"));
		assertThat((Object) combinedConfig.getAllowedMethods()).isEqualTo(Arrays.asList(HttpMethod.PUT.name()));

		combinedConfig = config.combine(new CorsConfiguration());
		assertThat((Object) config.getAllowedOrigins()).isEqualTo(Arrays.asList("*"));
		assertThat((Object) config.getAllowedHeaders()).isEqualTo(Arrays.asList("*"));
		assertThat((Object) combinedConfig.getAllowedMethods()).isEqualTo(Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name(),
				HttpMethod.POST.name()));

		combinedConfig = new CorsConfiguration().combine(config);
		assertThat((Object) config.getAllowedOrigins()).isEqualTo(Arrays.asList("*"));
		assertThat((Object) config.getAllowedHeaders()).isEqualTo(Arrays.asList("*"));
		assertThat((Object) combinedConfig.getAllowedMethods()).isEqualTo(Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name(),
				HttpMethod.POST.name()));
	}

	@Test
	public void combineWithAsteriskWildCard() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOrigin("https://domain.com");
		other.addAllowedHeader("header1");
		other.addExposedHeader("header2");
		other.addAllowedMethod(HttpMethod.PUT.name());
		CorsConfiguration combinedConfig = config.combine(other);
		assertThat((Object) combinedConfig.getAllowedOrigins()).isEqualTo(Arrays.asList("*"));
		assertThat((Object) combinedConfig.getAllowedHeaders()).isEqualTo(Arrays.asList("*"));
		assertThat((Object) combinedConfig.getAllowedMethods()).isEqualTo(Arrays.asList("*"));
		combinedConfig = other.combine(config);
		assertThat((Object) combinedConfig.getAllowedOrigins()).isEqualTo(Arrays.asList("*"));
		assertThat((Object) combinedConfig.getAllowedHeaders()).isEqualTo(Arrays.asList("*"));
		assertThat((Object) combinedConfig.getAllowedMethods()).isEqualTo(Arrays.asList("*"));
	}

	@Test  // SPR-14792
	public void combineWithDuplicatedElements() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("https://domain1.com");
		config.addAllowedOrigin("https://domain2.com");
		config.addAllowedHeader("header1");
		config.addAllowedHeader("header2");
		config.addExposedHeader("header3");
		config.addExposedHeader("header4");
		config.addAllowedMethod(HttpMethod.GET.name());
		config.addAllowedMethod(HttpMethod.PUT.name());
		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOrigin("https://domain1.com");
		other.addAllowedHeader("header1");
		other.addExposedHeader("header3");
		other.addAllowedMethod(HttpMethod.GET.name());
		CorsConfiguration combinedConfig = config.combine(other);
		assertThat((Object) combinedConfig.getAllowedOrigins()).isEqualTo(Arrays.asList("https://domain1.com", "https://domain2.com"));
		assertThat((Object) combinedConfig.getAllowedHeaders()).isEqualTo(Arrays.asList("header1", "header2"));
		assertThat((Object) combinedConfig.getExposedHeaders()).isEqualTo(Arrays.asList("header3", "header4"));
		assertThat((Object) combinedConfig.getAllowedMethods()).isEqualTo(Arrays.asList(HttpMethod.GET.name(), HttpMethod.PUT.name()));
	}

	@Test
	public void combine() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("https://domain1.com");
		config.addAllowedHeader("header1");
		config.addExposedHeader("header3");
		config.addAllowedMethod(HttpMethod.GET.name());
		config.setMaxAge(123L);
		config.setAllowCredentials(true);
		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOrigin("https://domain2.com");
		other.addAllowedHeader("header2");
		other.addExposedHeader("header4");
		other.addAllowedMethod(HttpMethod.PUT.name());
		other.setMaxAge(456L);
		other.setAllowCredentials(false);
		config = config.combine(other);
		assertThat((Object) config.getAllowedOrigins()).isEqualTo(Arrays.asList("https://domain1.com", "https://domain2.com"));
		assertThat((Object) config.getAllowedHeaders()).isEqualTo(Arrays.asList("header1", "header2"));
		assertThat((Object) config.getExposedHeaders()).isEqualTo(Arrays.asList("header3", "header4"));
		assertThat((Object) config.getAllowedMethods()).isEqualTo(Arrays.asList(HttpMethod.GET.name(), HttpMethod.PUT.name()));
		assertThat((Object) config.getMaxAge()).isEqualTo(new Long(456));
		assertThat((boolean) config.getAllowCredentials()).isFalse();
	}

	@Test
	public void checkOriginAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.asList("*"));
		assertThat((Object) config.checkOrigin("https://domain.com")).isEqualTo("*");
		config.setAllowCredentials(true);
		assertThat((Object) config.checkOrigin("https://domain.com")).isEqualTo("https://domain.com");
		config.setAllowedOrigins(Arrays.asList("https://domain.com"));
		assertThat((Object) config.checkOrigin("https://domain.com")).isEqualTo("https://domain.com");
		config.setAllowCredentials(false);
		assertThat((Object) config.checkOrigin("https://domain.com")).isEqualTo("https://domain.com");
	}

	@Test
	public void checkOriginNotAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertNull(config.checkOrigin(null));
		assertNull(config.checkOrigin("https://domain.com"));
		config.addAllowedOrigin("*");
		assertNull(config.checkOrigin(null));
		config.setAllowedOrigins(Arrays.asList("https://domain1.com"));
		assertNull(config.checkOrigin("https://domain2.com"));
		config.setAllowedOrigins(new ArrayList<>());
		assertNull(config.checkOrigin("https://domain.com"));
	}

	@Test
	public void checkMethodAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat((Object) config.checkHttpMethod(HttpMethod.GET)).isEqualTo(Arrays.asList(HttpMethod.GET, HttpMethod.HEAD));
		config.addAllowedMethod("GET");
		assertThat((Object) config.checkHttpMethod(HttpMethod.GET)).isEqualTo(Arrays.asList(HttpMethod.GET));
		config.addAllowedMethod("POST");
		assertThat((Object) config.checkHttpMethod(HttpMethod.GET)).isEqualTo(Arrays.asList(HttpMethod.GET, HttpMethod.POST));
		assertThat((Object) config.checkHttpMethod(HttpMethod.POST)).isEqualTo(Arrays.asList(HttpMethod.GET, HttpMethod.POST));
	}

	@Test
	public void checkMethodNotAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertNull(config.checkHttpMethod(null));
		assertNull(config.checkHttpMethod(HttpMethod.DELETE));
		config.setAllowedMethods(new ArrayList<>());
		assertNull(config.checkHttpMethod(HttpMethod.POST));
	}

	@Test
	public void checkHeadersAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat((Object) config.checkHeaders(Collections.emptyList())).isEqualTo(Collections.emptyList());
		config.addAllowedHeader("header1");
		config.addAllowedHeader("header2");
		assertThat((Object) config.checkHeaders(Arrays.asList("header1"))).isEqualTo(Arrays.asList("header1"));
		assertThat((Object) config.checkHeaders(Arrays.asList("header1", "header2"))).isEqualTo(Arrays.asList("header1", "header2"));
		assertThat((Object) config.checkHeaders(Arrays.asList("header1", "header2", "header3"))).isEqualTo(Arrays.asList("header1", "header2"));
	}

	@Test
	public void checkHeadersNotAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertNull(config.checkHeaders(null));
		assertNull(config.checkHeaders(Arrays.asList("header1")));
		config.setAllowedHeaders(Collections.emptyList());
		assertNull(config.checkHeaders(Arrays.asList("header1")));
		config.addAllowedHeader("header2");
		config.addAllowedHeader("header3");
		assertNull(config.checkHeaders(Arrays.asList("header1")));
	}

	@Test  // SPR-15772
	public void changePermitDefaultValues() {
		CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
		config.addAllowedOrigin("https://domain.com");
		config.addAllowedHeader("header1");
		config.addAllowedMethod("PATCH");
		assertThat((Object) config.getAllowedOrigins()).isEqualTo(Arrays.asList("*", "https://domain.com"));
		assertThat((Object) config.getAllowedHeaders()).isEqualTo(Arrays.asList("*", "header1"));
		assertThat((Object) config.getAllowedMethods()).isEqualTo(Arrays.asList("GET", "HEAD", "POST", "PATCH"));
	}

}
