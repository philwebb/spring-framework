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

package org.springframework.oxm.jaxb;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xmlunit.diff.DifferenceEvaluator;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.AbstractMarshallerTests;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.jaxb.test.FlightType;
import org.springframework.oxm.jaxb.test.Flights;
import org.springframework.oxm.jaxb.test.ObjectFactory;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.xmlunit.diff.ComparisonType.XML_STANDALONE;
import static org.xmlunit.diff.DifferenceEvaluators.Default;
import static org.xmlunit.diff.DifferenceEvaluators.chain;
import static org.xmlunit.diff.DifferenceEvaluators.downgradeDifferencesToEqual;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

/**
 * @author Arjen Poutsma
 * @author Biju Kunjummen
 * @author Sam Brannen
 */
public class Jaxb2MarshallerTests extends AbstractMarshallerTests<Jaxb2Marshaller> {

	private static final String CONTEXT_PATH = "org.springframework.oxm.jaxb.test";

	private Flights flights;


	@Override
	protected Jaxb2Marshaller createMarshaller() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath(CONTEXT_PATH);
		marshaller.afterPropertiesSet();
		return marshaller;
	}

	@Override
	protected Object createFlights() {
		FlightType flight = new FlightType();
		flight.setNumber(42L);
		this.flights = new Flights();
		this.flights.getFlight().add(flight);
		return this.flights;
	}


	@Test
	public void marshalSAXResult() throws Exception {
		ContentHandler contentHandler = mock(ContentHandler.class);
		SAXResult result = new SAXResult(contentHandler);
		this.marshaller.marshal(this.flights, result);
		InOrder ordered = inOrder(contentHandler);
		ordered.verify(contentHandler).setDocumentLocator(isA(Locator.class));
		ordered.verify(contentHandler).startDocument();
		ordered.verify(contentHandler).startPrefixMapping("", "http://samples.springframework.org/flight");
		ordered.verify(contentHandler).startElement(eq("http://samples.springframework.org/flight"), eq("flights"), eq("flights"), isA(Attributes.class));
		ordered.verify(contentHandler).startElement(eq("http://samples.springframework.org/flight"), eq("flight"), eq("flight"), isA(Attributes.class));
		ordered.verify(contentHandler).startElement(eq("http://samples.springframework.org/flight"), eq("number"), eq("number"), isA(Attributes.class));
		ordered.verify(contentHandler).characters(isA(char[].class), eq(0), eq(2));
		ordered.verify(contentHandler).endElement("http://samples.springframework.org/flight", "number", "number");
		ordered.verify(contentHandler).endElement("http://samples.springframework.org/flight", "flight", "flight");
		ordered.verify(contentHandler).endElement("http://samples.springframework.org/flight", "flights", "flights");
		ordered.verify(contentHandler).endPrefixMapping("");
		ordered.verify(contentHandler).endDocument();
	}

	@Test
	public void lazyInit() throws Exception {
		this.marshaller = new Jaxb2Marshaller();
		this.marshaller.setContextPath(CONTEXT_PATH);
		this.marshaller.setLazyInit(true);
		this.marshaller.afterPropertiesSet();
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		this.marshaller.marshal(this.flights, result);
		DifferenceEvaluator ev = chain(Default, downgradeDifferencesToEqual(XML_STANDALONE));
		assertThat("Marshaller writes invalid StreamResult", writer.toString(),
				isSimilarTo(EXPECTED_STRING).withDifferenceEvaluator(ev));
	}

	@Test
	public void properties() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath(CONTEXT_PATH);
		marshaller.setMarshallerProperties(
				Collections.<String, Object>singletonMap(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT,
						Boolean.TRUE));
		marshaller.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noContextPathOrClassesToBeBound() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.afterPropertiesSet();
	}

	@Test(expected = UncategorizedMappingException.class)
	public void testInvalidContextPath() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath("ab");
		marshaller.afterPropertiesSet();
	}

	@Test(expected = XmlMappingException.class)
	public void marshalInvalidClass() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(FlightType.class);
		marshaller.afterPropertiesSet();
		Result result = new StreamResult(new StringWriter());
		Flights flights = new Flights();
		marshaller.marshal(flights, result);
	}

	@Test
	public void supportsContextPath() throws Exception {
		testSupports();
	}

	@Test
	public void supportsClassesToBeBound() throws Exception {
		this.marshaller = new Jaxb2Marshaller();
		this.marshaller.setClassesToBeBound(Flights.class, FlightType.class);
		this.marshaller.afterPropertiesSet();
		testSupports();
	}

	@Test
	public void supportsPackagesToScan() throws Exception {
		this.marshaller = new Jaxb2Marshaller();
		this.marshaller.setPackagesToScan(CONTEXT_PATH);
		this.marshaller.afterPropertiesSet();
		testSupports();
	}

	private void testSupports() throws Exception {
		assertTrue("Jaxb2Marshaller does not support Flights class", this.marshaller.supports(Flights.class));
		assertTrue("Jaxb2Marshaller does not support Flights generic type", this.marshaller.supports((Type)Flights.class));

		assertFalse("Jaxb2Marshaller supports FlightType class", this.marshaller.supports(FlightType.class));
		assertFalse("Jaxb2Marshaller supports FlightType type", this.marshaller.supports((Type)FlightType.class));

		Method method = ObjectFactory.class.getDeclaredMethod("createFlight", FlightType.class);
		assertTrue("Jaxb2Marshaller does not support JAXBElement<FlightsType>",
				this.marshaller.supports(method.getGenericReturnType()));

		this.marshaller.setSupportJaxbElementClass(true);
		JAXBElement<FlightType> flightTypeJAXBElement = new JAXBElement<>(new QName("http://springframework.org", "flight"), FlightType.class,
				new FlightType());
		assertTrue("Jaxb2Marshaller does not support JAXBElement<FlightsType>", this.marshaller.supports(flightTypeJAXBElement.getClass()));

		assertFalse("Jaxb2Marshaller supports class not in context path", this.marshaller.supports(DummyRootElement.class));
		assertFalse("Jaxb2Marshaller supports type not in context path", this.marshaller.supports((Type)DummyRootElement.class));
		method = getClass().getDeclaredMethod("createDummyRootElement");
		assertFalse("Jaxb2Marshaller supports JAXBElement not in context path",
				this.marshaller.supports(method.getGenericReturnType()));

		assertFalse("Jaxb2Marshaller supports class not in context path", this.marshaller.supports(DummyType.class));
		assertFalse("Jaxb2Marshaller supports type not in context path", this.marshaller.supports((Type)DummyType.class));
		method = getClass().getDeclaredMethod("createDummyType");
		assertFalse("Jaxb2Marshaller supports JAXBElement not in context path",
				this.marshaller.supports(method.getGenericReturnType()));

		testSupportsPrimitives();
		testSupportsStandardClasses();
	}

	private void testSupportsPrimitives() {
		final Primitives primitives = new Primitives();
		ReflectionUtils.doWithMethods(Primitives.class, new ReflectionUtils.MethodCallback() {
			@Override
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Type returnType = method.getGenericReturnType();
				assertTrue("Jaxb2Marshaller does not support JAXBElement<" + method.getName().substring(9) + ">",
						marshaller.supports(returnType));
				try {
					// make sure the marshalling does not result in errors
					Object returnValue = method.invoke(primitives);
					marshaller.marshal(returnValue, new StreamResult(new ByteArrayOutputStream()));
				}
				catch (InvocationTargetException ex) {
					fail(ex.getMessage());
				}
			}
		}, new ReflectionUtils.MethodFilter() {
			@Override
			public boolean matches(Method method) {
				return method.getName().startsWith("primitive");
			}
		});
	}

	private void testSupportsStandardClasses() throws Exception {
		final StandardClasses standardClasses = new StandardClasses();
		ReflectionUtils.doWithMethods(StandardClasses.class, new ReflectionUtils.MethodCallback() {
			@Override
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Type returnType = method.getGenericReturnType();
				assertTrue("Jaxb2Marshaller does not support JAXBElement<" + method.getName().substring(13) + ">",
						marshaller.supports(returnType));
				try {
					// make sure the marshalling does not result in errors
					Object returnValue = method.invoke(standardClasses);
					marshaller.marshal(returnValue, new StreamResult(new ByteArrayOutputStream()));
				}
				catch (InvocationTargetException ex) {
					fail(ex.getMessage());
				}
			}
		}, new ReflectionUtils.MethodFilter() {
			@Override
			public boolean matches(Method method) {
				return method.getName().startsWith("standardClass");
			}
		});
	}

	@Test
	public void supportsXmlRootElement() throws Exception {
		this.marshaller = new Jaxb2Marshaller();
		this.marshaller.setClassesToBeBound(DummyRootElement.class, DummyType.class);
		this.marshaller.afterPropertiesSet();
		assertTrue("Jaxb2Marshaller does not support XmlRootElement class", this.marshaller.supports(DummyRootElement.class));
		assertTrue("Jaxb2Marshaller does not support XmlRootElement generic type", this.marshaller.supports((Type)DummyRootElement.class));

		assertFalse("Jaxb2Marshaller supports DummyType class", this.marshaller.supports(DummyType.class));
		assertFalse("Jaxb2Marshaller supports DummyType type", this.marshaller.supports((Type)DummyType.class));
	}


	@Test
	public void marshalAttachments() throws Exception {
		this.marshaller = new Jaxb2Marshaller();
		this.marshaller.setClassesToBeBound(BinaryObject.class);
		this.marshaller.setMtomEnabled(true);
		this.marshaller.afterPropertiesSet();
		MimeContainer mimeContainer = mock(MimeContainer.class);

		Resource logo = new ClassPathResource("spring-ws.png", getClass());
		DataHandler dataHandler = new DataHandler(new FileDataSource(logo.getFile()));

		given(mimeContainer.convertToXopPackage()).willReturn(true);
		byte[] bytes = FileCopyUtils.copyToByteArray(logo.getInputStream());
		BinaryObject object = new BinaryObject(bytes, dataHandler);
		StringWriter writer = new StringWriter();
		this.marshaller.marshal(object, new StreamResult(writer), mimeContainer);
		assertTrue("No XML written", writer.toString().length() > 0);
		verify(mimeContainer, times(3)).addAttachment(isA(String.class), isA(DataHandler.class));
	}

	@Test  // SPR-10714
	public void marshalAWrappedObjectHoldingAnXmlElementDeclElement() throws Exception {
		this.marshaller = new Jaxb2Marshaller();
		this.marshaller.setPackagesToScan("org.springframework.oxm.jaxb");
		this.marshaller.afterPropertiesSet();
		Airplane airplane = new Airplane();
		airplane.setName("test");
		StringWriter writer = new StringWriter();
		Result result = new StreamResult(writer);
		this.marshaller.marshal(airplane, result);
		DifferenceEvaluator ev = chain(Default, downgradeDifferencesToEqual(XML_STANDALONE));
		assertThat("Marshalling should use root Element",
				writer.toString(),
				isSimilarTo("<airplane><name>test</name></airplane>").withDifferenceEvaluator(ev));
	}

	@Test  // SPR-10806
	public void unmarshalStreamSourceWithXmlOptions() throws Exception {
		final javax.xml.bind.Unmarshaller unmarshaller = mock(javax.xml.bind.Unmarshaller.class);
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller() {
			@Override
			protected javax.xml.bind.Unmarshaller createUnmarshaller() {
				return unmarshaller;
			}
		};

		// 1. external-general-entities and dtd support disabled (default)

		marshaller.unmarshal(new StreamSource("1"));
		ArgumentCaptor<SAXSource> sourceCaptor = ArgumentCaptor.forClass(SAXSource.class);
		verify(unmarshaller).unmarshal(sourceCaptor.capture());

		SAXSource result = sourceCaptor.getValue();
		assertEquals(true, result.getXMLReader().getFeature("http://apache.org/xml/features/disallow-doctype-decl"));
		assertEquals(false, result.getXMLReader().getFeature("http://xml.org/sax/features/external-general-entities"));

		// 2. external-general-entities and dtd support enabled

		reset(unmarshaller);
		marshaller.setProcessExternalEntities(true);
		marshaller.setSupportDtd(true);

		marshaller.unmarshal(new StreamSource("1"));
		verify(unmarshaller).unmarshal(sourceCaptor.capture());

		result = sourceCaptor.getValue();
		assertEquals(false, result.getXMLReader().getFeature("http://apache.org/xml/features/disallow-doctype-decl"));
		assertEquals(true, result.getXMLReader().getFeature("http://xml.org/sax/features/external-general-entities"));
	}

	@Test  // SPR-10806
	public void unmarshalSaxSourceWithXmlOptions() throws Exception {
		final javax.xml.bind.Unmarshaller unmarshaller = mock(javax.xml.bind.Unmarshaller.class);
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller() {
			@Override
			protected javax.xml.bind.Unmarshaller createUnmarshaller() {
				return unmarshaller;
			}
		};

		// 1. external-general-entities and dtd support disabled (default)

		marshaller.unmarshal(new SAXSource(new InputSource("1")));
		ArgumentCaptor<SAXSource> sourceCaptor = ArgumentCaptor.forClass(SAXSource.class);
		verify(unmarshaller).unmarshal(sourceCaptor.capture());

		SAXSource result = sourceCaptor.getValue();
		assertEquals(true, result.getXMLReader().getFeature("http://apache.org/xml/features/disallow-doctype-decl"));
		assertEquals(false, result.getXMLReader().getFeature("http://xml.org/sax/features/external-general-entities"));

		// 2. external-general-entities and dtd support enabled

		reset(unmarshaller);
		marshaller.setProcessExternalEntities(true);
		marshaller.setSupportDtd(true);

		marshaller.unmarshal(new SAXSource(new InputSource("1")));
		verify(unmarshaller).unmarshal(sourceCaptor.capture());

		result = sourceCaptor.getValue();
		assertEquals(false, result.getXMLReader().getFeature("http://apache.org/xml/features/disallow-doctype-decl"));
		assertEquals(true, result.getXMLReader().getFeature("http://xml.org/sax/features/external-general-entities"));
	}


	@XmlRootElement
	@SuppressWarnings("unused")
	public static class DummyRootElement {

		private DummyType t = new DummyType();
	}

	@XmlType
	@SuppressWarnings("unused")
	public static class DummyType {

		private String s = "Hello";
	}

	@SuppressWarnings("unused")
	private JAXBElement<DummyRootElement> createDummyRootElement() {
		return null;
	}

	@SuppressWarnings("unused")
	private JAXBElement<DummyType> createDummyType() {
		return null;
	}

}
