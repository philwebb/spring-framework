/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Factory to create a client proxy from an HTTP service interface with
 * {@link HttpExchange @HttpExchange} methods.
 *
 * <p>To create an instance, use static methods to obtain a
 * {@link Builder Builder}.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 6.0
 * @see org.springframework.web.client.support.RestClientAdapter
 * @see org.springframework.web.reactive.function.client.support.WebClientAdapter
 * @see org.springframework.web.client.support.RestTemplateAdapter
 */
public final class HttpServiceProxyFactory {

	private final HttpExchangeAdapter exchangeAdapter;

	private final List<HttpServiceArgumentResolver> argumentResolvers;

	@Nullable
	private final StringValueResolver embeddedValueResolver;


	private HttpServiceProxyFactory(
			HttpExchangeAdapter exchangeAdapter, List<HttpServiceArgumentResolver> argumentResolvers,
			@Nullable StringValueResolver embeddedValueResolver) {

		this.exchangeAdapter = exchangeAdapter;
		this.argumentResolvers = argumentResolvers;
		this.embeddedValueResolver = embeddedValueResolver;
	}

	/**
	 * Return a proxy that implements the given HTTP service interface to perform
	 * HTTP requests and retrieve responses through an HTTP client.
	 * @param serviceType the HTTP service to create a proxy for
	 * @param <S> the HTTP service type
	 * @return the created proxy
	 */
	@Deprecated
	public <S> S createClient(Class<S> serviceType) {
		return serviceProxy(serviceType);
	}

	/**
	 * Return a proxy that implements the given HTTP service interface to perform
	 * HTTP requests and retrieve responses through an HTTP client.
	 * @param type the HTTP service to create a proxy for
	 * @param <S> the HTTP service type
	 * @return the created proxy
	 */
	public <S> S serviceProxy(Class<S> type) {

		List<HttpServiceMethod> httpServiceMethods =
				MethodIntrospector.selectMethods(type, this::isExchangeMethod).stream()
						.map(method -> createHttpServiceMethod(type, method))
						.toList();

		return ProxyFactory.getProxy(type, new HttpServiceMethodInterceptor(httpServiceMethods));
	}


	private boolean isExchangeMethod(Method method) {
		return AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class);
	}

	private <S> HttpServiceMethod createHttpServiceMethod(Class<S> serviceType, Method method) {
		Assert.notNull(this.argumentResolvers,
				"No argument resolvers: afterPropertiesSet was not called");

		return new HttpServiceMethod(
				method, serviceType, this.argumentResolvers, this.exchangeAdapter, this.embeddedValueResolver);
	}


	/**
	 * Return a builder that's initialized with the given client.
	 * @since 6.1
	 * @deprecated since 7.0 in favor of {@link #builder(HttpExchangeAdapter)}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public static Builder builderFor(HttpExchangeAdapter exchangeAdapter) {
		return builder(exchangeAdapter);
	}

	/**
	 * Factory method that can be used to create a new {@link HttpServiceProxyFactory}
	 * backed by the given {@link HttpExchangeAdapter}.
	 * @param exchangeAdapter the exchange adapter
	 * @return a new {@link HttpServiceProxyFactory}
	 * @since 7.0
	 */
	public static HttpServiceProxyFactory of(HttpExchangeAdapter exchangeAdapter) {
		return of(exchangeAdapter, (builder) -> {
		});
	}

	/**
	 * Factory method that can be used to create a new {@link HttpServiceProxyFactory}
	 * backed by the given {@link HttpExchangeAdapter}.
	 * @param exchangeAdapter the exchange adapter
	 * @param builderCustomizer callback used to customize the {@link Builder}
	 * @return a new {@link HttpServiceProxyFactory}
	 * @since 7.0
	 */
	public static HttpServiceProxyFactory of(HttpExchangeAdapter exchangeAdapter, Consumer<Builder> builderCustomizer) {
		// FIXME might be nice if the customizer took something that can't build
		Builder builder = HttpServiceProxyFactory.builder(exchangeAdapter);
		builderCustomizer.accept(builder);
		return builder.build();
	}

	/**
	 * Return an empty builder, with the client to be provided to builder.
	 * @since 7.0.0
	 */
	public static Builder builder(HttpExchangeAdapter exchangeAdapter) {
		return new Builder(exchangeAdapter);
	}

	/**
	 * Return an empty builder, with the client to be provided to builder.
	 * @deprecated since 7.0 in favor of {@link #builder(HttpExchangeAdapter)} with a
	 * mandatory {@link HttpExchangeAdapter}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public static Builder builder() {
		return new Builder();
	}


	/**
	 * Builder to create an {@link HttpServiceProxyFactory}.
	 */
	public static final class Builder {

		@Nullable
		private HttpExchangeAdapter exchangeAdapter;

		private final List<HttpServiceArgumentResolver> customArgumentResolvers = new ArrayList<>();

		@Nullable
		private ConversionService conversionService;

		@Nullable
		private StringValueResolver embeddedValueResolver;

		@Deprecated(since = "7.0", forRemoval = true)
		private Builder() {
		}

		private Builder(HttpExchangeAdapter exchangeAdapter) {
			this.exchangeAdapter = exchangeAdapter;
		}

		/**
		 * Provide the HTTP client to perform requests through.
		 * @param adapter a client adapted to {@link HttpExchangeAdapter}
		 * @return this same builder instance
		 * @since 6.1
		 * @deprecated since 7.0 in favor of {@link HttpServiceProxyFactory#builder(HttpExchangeAdapter)}
		 */
		@Deprecated(since = "7.0", forRemoval = true)
		public Builder exchangeAdapter(HttpExchangeAdapter adapter) {
			this.exchangeAdapter = adapter;
			return this;
		}

		/**
		 * Register a custom argument resolver, invoked ahead of default resolvers.
		 * @param resolver the resolver to add
		 * @return this same builder instance
		 */
		public Builder customArgumentResolver(HttpServiceArgumentResolver resolver) {
			this.customArgumentResolvers.add(resolver);
			return this;
		}

		/**
		 * Set the {@link ConversionService} to use where input values need to
		 * be formatted as Strings.
		 * <p>By default this is {@link DefaultFormattingConversionService}.
		 * @return this same builder instance
		 */
		public Builder conversionService(ConversionService conversionService) {
			this.conversionService = conversionService;
			return this;
		}

		/**
		 * Set the {@link StringValueResolver} to use for resolving placeholders
		 * and expressions embedded in {@link HttpExchange#url()}.
		 * @param embeddedValueResolver the resolver to use
		 * @return this same builder instance
		 */
		public Builder embeddedValueResolver(StringValueResolver embeddedValueResolver) {
			this.embeddedValueResolver = embeddedValueResolver;
			return this;
		}

		/**
		 * Build the {@link HttpServiceProxyFactory} instance.
		 */
		public HttpServiceProxyFactory build() {
			Assert.notNull(this.exchangeAdapter, "HttpClientAdapter is required");

			return new HttpServiceProxyFactory(
					this.exchangeAdapter, initArgumentResolvers(), this.embeddedValueResolver);
		}

		@SuppressWarnings({"DataFlowIssue", "NullAway"})
		private List<HttpServiceArgumentResolver> initArgumentResolvers() {

			// Custom
			List<HttpServiceArgumentResolver> resolvers = new ArrayList<>(this.customArgumentResolvers);

			ConversionService service = (this.conversionService != null ?
					this.conversionService : new DefaultFormattingConversionService());

			// Annotation-based
			resolvers.add(new RequestHeaderArgumentResolver(service));
			resolvers.add(new RequestBodyArgumentResolver(this.exchangeAdapter));
			resolvers.add(new PathVariableArgumentResolver(service));
			resolvers.add(new RequestParamArgumentResolver(service));
			resolvers.add(new RequestPartArgumentResolver(this.exchangeAdapter));
			resolvers.add(new CookieValueArgumentResolver(service));
			if (this.exchangeAdapter.supportsRequestAttributes()) {
				resolvers.add(new RequestAttributeArgumentResolver());
			}

			// Specific type
			resolvers.add(new UrlArgumentResolver());
			resolvers.add(new UriBuilderFactoryArgumentResolver());
			resolvers.add(new HttpMethodArgumentResolver());

			return resolvers;
		}
	}


	/**
	 * {@link MethodInterceptor} that invokes an {@link HttpServiceMethod}.
	 */
	private static final class HttpServiceMethodInterceptor implements MethodInterceptor {

		private final Map<Method, HttpServiceMethod> httpServiceMethods;

		private HttpServiceMethodInterceptor(List<HttpServiceMethod> methods) {
			this.httpServiceMethods = methods.stream()
					.collect(Collectors.toMap(HttpServiceMethod::getMethod, Function.identity()));
		}

		@Override
		@Nullable
		public Object invoke(MethodInvocation invocation) throws Throwable {
			Method method = invocation.getMethod();
			HttpServiceMethod httpServiceMethod = this.httpServiceMethods.get(method);
			if (httpServiceMethod != null) {
				Object[] arguments = KotlinDetector.isSuspendingFunction(method) ?
						resolveCoroutinesArguments(invocation.getArguments()) : invocation.getArguments();
				return httpServiceMethod.invoke(arguments);
			}
			if (method.isDefault()) {
				if (invocation instanceof ReflectiveMethodInvocation reflectiveMethodInvocation) {
					Object proxy = reflectiveMethodInvocation.getProxy();
					return InvocationHandler.invokeDefault(proxy, method, invocation.getArguments());
				}
			}
			throw new IllegalStateException("Unexpected method invocation: " + method);
		}

		private static Object[] resolveCoroutinesArguments(Object[] args) {
			Object[] functionArgs = new Object[args.length - 1];
			System.arraycopy(args, 0, functionArgs, 0, args.length - 1);
			return functionArgs;
		}

	}

}
