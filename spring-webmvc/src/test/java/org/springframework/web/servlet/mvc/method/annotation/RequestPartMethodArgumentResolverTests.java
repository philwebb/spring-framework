/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.Part;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockMultipartFile;
import org.springframework.mock.web.test.MockMultipartHttpServletRequest;
import org.springframework.mock.web.test.MockPart;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Test fixture with {@link RequestPartMethodArgumentResolver} and mock {@link HttpMessageConverter}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class RequestPartMethodArgumentResolverTests {

	private HttpMessageConverter<SimpleBean> messageConverter;

	private RequestPartMethodArgumentResolver resolver;

	private MultipartFile multipartFile1;

	private MultipartFile multipartFile2;

	private MockMultipartHttpServletRequest multipartRequest;

	private NativeWebRequest webRequest;

	private MethodParameter paramRequestPart;
	private MethodParameter paramNamedRequestPart;
	private MethodParameter paramValidRequestPart;
	private MethodParameter paramMultipartFile;
	private MethodParameter paramMultipartFileList;
	private MethodParameter paramMultipartFileArray;
	private MethodParameter paramInt;
	private MethodParameter paramMultipartFileNotAnnot;
	private MethodParameter paramPart;
	private MethodParameter paramPartList;
	private MethodParameter paramPartArray;
	private MethodParameter paramRequestParamAnnot;
	private MethodParameter optionalMultipartFile;
	private MethodParameter optionalMultipartFileList;
	private MethodParameter optionalPart;
	private MethodParameter optionalPartList;
	private MethodParameter optionalRequestPart;


	@Before
	@SuppressWarnings("unchecked")
	public void setup() throws Exception {
		this.messageConverter = mock(HttpMessageConverter.class);
		given(this.messageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));

		this.resolver = new RequestPartMethodArgumentResolver(Collections.singletonList(this.messageConverter));
		reset(this.messageConverter);

		byte[] content = "doesn't matter as long as not empty".getBytes(StandardCharsets.UTF_8);
		this.multipartFile1 = new MockMultipartFile("requestPart", "", "text/plain", content);
		this.multipartFile2 = new MockMultipartFile("requestPart", "", "text/plain", content);
		this.multipartRequest = new MockMultipartHttpServletRequest();
		this.multipartRequest.addFile(this.multipartFile1);
		this.multipartRequest.addFile(this.multipartFile2);
		this.multipartRequest.addFile(new MockMultipartFile("otherPart", "", "text/plain", content));
		this.webRequest = new ServletWebRequest(this.multipartRequest, new MockHttpServletResponse());

		Method method = ReflectionUtils.findMethod(getClass(), "handle", (Class<?>[]) null);
		this.paramRequestPart = new SynthesizingMethodParameter(method, 0);
		this.paramRequestPart.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		this.paramNamedRequestPart = new SynthesizingMethodParameter(method, 1);
		this.paramValidRequestPart = new SynthesizingMethodParameter(method, 2);
		this.paramMultipartFile = new SynthesizingMethodParameter(method, 3);
		this.paramMultipartFileList = new SynthesizingMethodParameter(method, 4);
		this.paramMultipartFileArray = new SynthesizingMethodParameter(method, 5);
		this.paramInt = new SynthesizingMethodParameter(method, 6);
		this.paramMultipartFileNotAnnot = new SynthesizingMethodParameter(method, 7);
		this.paramMultipartFileNotAnnot.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		this.paramPart = new SynthesizingMethodParameter(method, 8);
		this.paramPart.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		this.paramPartList = new SynthesizingMethodParameter(method, 9);
		this.paramPartArray = new SynthesizingMethodParameter(method, 10);
		this.paramRequestParamAnnot = new SynthesizingMethodParameter(method, 11);
		this.optionalMultipartFile = new SynthesizingMethodParameter(method, 12);
		this.optionalMultipartFile.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		this.optionalMultipartFileList = new SynthesizingMethodParameter(method, 13);
		this.optionalMultipartFileList.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		this.optionalPart = new SynthesizingMethodParameter(method, 14);
		this.optionalPart.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		this.optionalPartList = new SynthesizingMethodParameter(method, 15);
		this.optionalPartList.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		this.optionalRequestPart = new SynthesizingMethodParameter(method, 16);
	}


	@Test
	public void supportsParameter() {
		assertTrue(this.resolver.supportsParameter(this.paramRequestPart));
		assertTrue(this.resolver.supportsParameter(this.paramNamedRequestPart));
		assertTrue(this.resolver.supportsParameter(this.paramValidRequestPart));
		assertTrue(this.resolver.supportsParameter(this.paramMultipartFile));
		assertTrue(this.resolver.supportsParameter(this.paramMultipartFileList));
		assertTrue(this.resolver.supportsParameter(this.paramMultipartFileArray));
		assertFalse(this.resolver.supportsParameter(this.paramInt));
		assertTrue(this.resolver.supportsParameter(this.paramMultipartFileNotAnnot));
		assertTrue(this.resolver.supportsParameter(this.paramPart));
		assertTrue(this.resolver.supportsParameter(this.paramPartList));
		assertTrue(this.resolver.supportsParameter(this.paramPartArray));
		assertFalse(this.resolver.supportsParameter(this.paramRequestParamAnnot));
		assertTrue(this.resolver.supportsParameter(this.optionalMultipartFile));
		assertTrue(this.resolver.supportsParameter(this.optionalMultipartFileList));
		assertTrue(this.resolver.supportsParameter(this.optionalPart));
		assertTrue(this.resolver.supportsParameter(this.optionalPartList));
		assertTrue(this.resolver.supportsParameter(this.optionalRequestPart));
	}

	@Test
	public void resolveMultipartFile() throws Exception {
		Object actual = this.resolver.resolveArgument(this.paramMultipartFile, null, this.webRequest, null);
		assertSame(this.multipartFile1, actual);
	}

	@Test
	public void resolveMultipartFileList() throws Exception {
		Object actual = this.resolver.resolveArgument(this.paramMultipartFileList, null, this.webRequest, null);
		assertTrue(actual instanceof List);
		assertEquals(Arrays.asList(this.multipartFile1, this.multipartFile2), actual);
	}

	@Test
	public void resolveMultipartFileArray() throws Exception {
		Object actual = this.resolver.resolveArgument(this.paramMultipartFileArray, null, this.webRequest, null);
		assertNotNull(actual);
		assertTrue(actual instanceof MultipartFile[]);
		MultipartFile[] parts = (MultipartFile[]) actual;
		assertEquals(2, parts.length);
		assertEquals(parts[0], this.multipartFile1);
		assertEquals(parts[1], this.multipartFile2);
	}

	@Test
	public void resolveMultipartFileNotAnnotArgument() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("multipartFileNotAnnot", "Hello World".getBytes());
		request.addFile(expected);
		request.addFile(new MockMultipartFile("otherPart", "", "text/plain", "Hello World".getBytes()));
		this.webRequest = new ServletWebRequest(request);

		Object result = this.resolver.resolveArgument(this.paramMultipartFileNotAnnot, null, this.webRequest, null);

		assertTrue(result instanceof MultipartFile);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolvePartArgument() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		MockPart expected = new MockPart("part", "Hello World".getBytes());
		request.addPart(expected);
		request.addPart(new MockPart("otherPart", "Hello World".getBytes()));
		this.webRequest = new ServletWebRequest(request);

		Object result = this.resolver.resolveArgument(this.paramPart, null, this.webRequest, null);
		assertTrue(result instanceof Part);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolvePartListArgument() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		MockPart part1 = new MockPart("requestPart", "Hello World 1".getBytes());
		MockPart part2 = new MockPart("requestPart", "Hello World 2".getBytes());
		request.addPart(part1);
		request.addPart(part2);
		request.addPart(new MockPart("otherPart", "Hello World".getBytes()));
		this.webRequest = new ServletWebRequest(request);

		Object result = this.resolver.resolveArgument(this.paramPartList, null, this.webRequest, null);
		assertTrue(result instanceof List);
		assertEquals(Arrays.asList(part1, part2), result);
	}

	@Test
	public void resolvePartArrayArgument() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		MockPart part1 = new MockPart("requestPart", "Hello World 1".getBytes());
		MockPart part2 = new MockPart("requestPart", "Hello World 2".getBytes());
		request.addPart(part1);
		request.addPart(part2);
		request.addPart(new MockPart("otherPart", "Hello World".getBytes()));
		this.webRequest = new ServletWebRequest(request);

		Object result = this.resolver.resolveArgument(this.paramPartArray, null, this.webRequest, null);
		assertTrue(result instanceof Part[]);
		Part[] parts = (Part[]) result;
		assertEquals(2, parts.length);
		assertEquals(parts[0], part1);
		assertEquals(parts[1], part2);
	}

	@Test
	public void resolveRequestPart() throws Exception {
		testResolveArgument(new SimpleBean("foo"), this.paramRequestPart);
	}

	@Test
	public void resolveNamedRequestPart() throws Exception {
		testResolveArgument(new SimpleBean("foo"), this.paramNamedRequestPart);
	}

	@Test
	public void resolveNamedRequestPartNotPresent() throws Exception {
		testResolveArgument(null, this.paramNamedRequestPart);
	}

	@Test
	public void resolveRequestPartNotValid() throws Exception {
		try {
			testResolveArgument(new SimpleBean(null), this.paramValidRequestPart);
			fail("Expected exception");
		}
		catch (MethodArgumentNotValidException ex) {
			assertEquals("requestPart", ex.getBindingResult().getObjectName());
			assertEquals(1, ex.getBindingResult().getErrorCount());
			assertNotNull(ex.getBindingResult().getFieldError("name"));
		}
	}

	@Test
	public void resolveRequestPartValid() throws Exception {
		testResolveArgument(new SimpleBean("foo"), this.paramValidRequestPart);
	}

	@Test
	public void resolveRequestPartRequired() throws Exception {
		try {
			testResolveArgument(null, this.paramValidRequestPart);
			fail("Expected exception");
		}
		catch (MissingServletRequestPartException ex) {
			assertEquals("requestPart", ex.getRequestPartName());
		}
	}

	@Test
	public void resolveRequestPartNotRequired() throws Exception {
		testResolveArgument(new SimpleBean("foo"), this.paramValidRequestPart);
	}

	@Test(expected = MultipartException.class)
	public void isMultipartRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		this.resolver.resolveArgument(this.paramMultipartFile, new ModelAndViewContainer(), new ServletWebRequest(request), null);
	}

	@Test  // SPR-9079
	public void isMultipartRequestPut() throws Exception {
		this.multipartRequest.setMethod("PUT");
		Object actualValue = this.resolver.resolveArgument(this.paramMultipartFile, null, this.webRequest, null);
		assertSame(this.multipartFile1, actualValue);
	}

	@Test
	public void resolveOptionalMultipartFileArgument() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("optionalMultipartFile", "Hello World".getBytes());
		request.addFile(expected);
		request.addFile(new MockMultipartFile("otherPart", "", "text/plain", "Hello World".getBytes()));
		this.webRequest = new ServletWebRequest(request);

		Object actualValue = this.resolver.resolveArgument(this.optionalMultipartFile, null, this.webRequest, null);
		assertTrue(actualValue instanceof Optional);
		assertEquals("Invalid result", expected, ((Optional<?>) actualValue).get());

		actualValue = this.resolver.resolveArgument(this.optionalMultipartFile, null, this.webRequest, null);
		assertTrue(actualValue instanceof Optional);
		assertEquals("Invalid result", expected, ((Optional<?>) actualValue).get());
	}

	@Test
	public void resolveOptionalMultipartFileArgumentNotPresent() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		this.webRequest = new ServletWebRequest(request);

		Object actualValue = this.resolver.resolveArgument(this.optionalMultipartFile, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);

		actualValue = this.resolver.resolveArgument(this.optionalMultipartFile, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);
	}

	@Test
	public void resolveOptionalMultipartFileArgumentWithoutMultipartRequest() throws Exception {
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());

		Object actualValue = this.resolver.resolveArgument(this.optionalMultipartFile, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);

		actualValue = this.resolver.resolveArgument(this.optionalMultipartFile, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);
	}

	@Test
	public void resolveOptionalMultipartFileList() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("requestPart", "Hello World".getBytes());
		request.addFile(expected);
		request.addFile(new MockMultipartFile("otherPart", "", "text/plain", "Hello World".getBytes()));
		this.webRequest = new ServletWebRequest(request);

		Object actualValue = this.resolver.resolveArgument(this.optionalMultipartFileList, null, this.webRequest, null);
		assertTrue(actualValue instanceof Optional);
		assertEquals("Invalid result", Collections.singletonList(expected), ((Optional<?>) actualValue).get());

		actualValue = this.resolver.resolveArgument(this.optionalMultipartFileList, null, this.webRequest, null);
		assertTrue(actualValue instanceof Optional);
		assertEquals("Invalid result", Collections.singletonList(expected), ((Optional<?>) actualValue).get());
	}

	@Test
	public void resolveOptionalMultipartFileListNotPresent() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		this.webRequest = new ServletWebRequest(request);

		Object actualValue = this.resolver.resolveArgument(this.optionalMultipartFileList, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);

		actualValue = this.resolver.resolveArgument(this.optionalMultipartFileList, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);
	}

	@Test
	public void resolveOptionalMultipartFileListWithoutMultipartRequest() throws Exception {
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());

		Object actualValue = this.resolver.resolveArgument(this.optionalMultipartFileList, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);

		actualValue = this.resolver.resolveArgument(this.optionalMultipartFileList, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);
	}

	@Test
	public void resolveOptionalPartArgument() throws Exception {
		MockPart expected = new MockPart("optionalPart", "Hello World".getBytes());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected);
		request.addPart(new MockPart("otherPart", "Hello World".getBytes()));
		this.webRequest = new ServletWebRequest(request);

		Object actualValue = this.resolver.resolveArgument(this.optionalPart, null, this.webRequest, null);
		assertTrue(actualValue instanceof Optional);
		assertEquals("Invalid result", expected, ((Optional<?>) actualValue).get());

		actualValue = this.resolver.resolveArgument(this.optionalPart, null, this.webRequest, null);
		assertTrue(actualValue instanceof Optional);
		assertEquals("Invalid result", expected, ((Optional<?>) actualValue).get());
	}

	@Test
	public void resolveOptionalPartArgumentNotPresent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		this.webRequest = new ServletWebRequest(request);

		Object actualValue = this.resolver.resolveArgument(this.optionalPart, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);

		actualValue = this.resolver.resolveArgument(this.optionalPart, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);
	}

	@Test
	public void resolveOptionalPartArgumentWithoutMultipartRequest() throws Exception {
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());

		Object actualValue = this.resolver.resolveArgument(this.optionalPart, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);

		actualValue = this.resolver.resolveArgument(this.optionalPart, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);
	}

	@Test
	public void resolveOptionalPartList() throws Exception {
		MockPart expected = new MockPart("requestPart", "Hello World".getBytes());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected);
		request.addPart(new MockPart("otherPart", "Hello World".getBytes()));
		this.webRequest = new ServletWebRequest(request);

		Object actualValue = this.resolver.resolveArgument(this.optionalPartList, null, this.webRequest, null);
		assertTrue(actualValue instanceof Optional);
		assertEquals("Invalid result", Collections.singletonList(expected), ((Optional<?>) actualValue).get());

		actualValue = this.resolver.resolveArgument(this.optionalPartList, null, this.webRequest, null);
		assertTrue(actualValue instanceof Optional);
		assertEquals("Invalid result", Collections.singletonList(expected), ((Optional<?>) actualValue).get());
	}

	@Test
	public void resolveOptionalPartListNotPresent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		this.webRequest = new ServletWebRequest(request);

		Object actualValue = this.resolver.resolveArgument(this.optionalPartList, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);

		actualValue = this.resolver.resolveArgument(this.optionalPartList, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);
	}

	@Test
	public void resolveOptionalPartListWithoutMultipartRequest() throws Exception {
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());

		Object actualValue = this.resolver.resolveArgument(this.optionalPartList, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);

		actualValue = this.resolver.resolveArgument(this.optionalPartList, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);
	}

	@Test
	public void resolveOptionalRequestPart() throws Exception {
		SimpleBean simpleBean = new SimpleBean("foo");
		given(this.messageConverter.canRead(SimpleBean.class, MediaType.TEXT_PLAIN)).willReturn(true);
		given(this.messageConverter.read(eq(SimpleBean.class), isA(HttpInputMessage.class))).willReturn(simpleBean);

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();

		Object actualValue = this.resolver.resolveArgument(
				this.optionalRequestPart, mavContainer, this.webRequest, new ValidatingBinderFactory());
		assertEquals("Invalid argument value", Optional.of(simpleBean), actualValue);
		assertFalse("The requestHandled flag shouldn't change", mavContainer.isRequestHandled());

		actualValue = this.resolver.resolveArgument(this.optionalRequestPart, mavContainer, this.webRequest, new ValidatingBinderFactory());
		assertEquals("Invalid argument value", Optional.of(simpleBean), actualValue);
		assertFalse("The requestHandled flag shouldn't change", mavContainer.isRequestHandled());
	}

	@Test
	public void resolveOptionalRequestPartNotPresent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		this.webRequest = new ServletWebRequest(request);

		Object actualValue = this.resolver.resolveArgument(this.optionalRequestPart, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);

		actualValue = this.resolver.resolveArgument(this.optionalRequestPart, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);
	}

	@Test
	public void resolveOptionalRequestPartWithoutMultipartRequest() throws Exception {
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());

		Object actualValue = this.resolver.resolveArgument(this.optionalRequestPart, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);

		actualValue = this.resolver.resolveArgument(this.optionalRequestPart, null, this.webRequest, null);
		assertEquals("Invalid argument value", Optional.empty(), actualValue);
	}


	private void testResolveArgument(SimpleBean argValue, MethodParameter parameter) throws Exception {
		given(this.messageConverter.canRead(SimpleBean.class, MediaType.TEXT_PLAIN)).willReturn(true);
		given(this.messageConverter.read(eq(SimpleBean.class), isA(HttpInputMessage.class))).willReturn(argValue);

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();

		Object actualValue = this.resolver.resolveArgument(parameter, mavContainer, this.webRequest, new ValidatingBinderFactory());
		assertEquals("Invalid argument value", argValue, actualValue);
		assertFalse("The requestHandled flag shouldn't change", mavContainer.isRequestHandled());
	}


	private static class SimpleBean {

		@NotNull
		private final String name;

		public SimpleBean(String name) {
			this.name = name;
		}

		@SuppressWarnings("unused")
		public String getName() {
			return this.name;
		}
	}


	private final class ValidatingBinderFactory implements WebDataBinderFactory {

		@Override
		public WebDataBinder createBinder(NativeWebRequest webRequest, @Nullable Object target,
				String objectName) throws Exception {

			LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
			validator.afterPropertiesSet();
			WebDataBinder dataBinder = new WebDataBinder(target, objectName);
			dataBinder.setValidator(validator);
			return dataBinder;
		}
	}


	@SuppressWarnings("unused")
	public void handle(
			@RequestPart SimpleBean requestPart,
			@RequestPart(value="requestPart", required=false) SimpleBean namedRequestPart,
			@Valid @RequestPart("requestPart") SimpleBean validRequestPart,
			@RequestPart("requestPart") MultipartFile multipartFile,
			@RequestPart("requestPart") List<MultipartFile> multipartFileList,
			@RequestPart("requestPart") MultipartFile[] multipartFileArray,
			int i,
			MultipartFile multipartFileNotAnnot,
			Part part,
			@RequestPart("requestPart") List<Part> partList,
			@RequestPart("requestPart") Part[] partArray,
			@RequestParam MultipartFile requestParamAnnot,
			Optional<MultipartFile> optionalMultipartFile,
			@RequestPart("requestPart") Optional<List<MultipartFile>> optionalMultipartFileList,
			Optional<Part> optionalPart,
			@RequestPart("requestPart") Optional<List<Part>> optionalPartList,
			@RequestPart("requestPart") Optional<SimpleBean> optionalRequestPart) {
	}

}
