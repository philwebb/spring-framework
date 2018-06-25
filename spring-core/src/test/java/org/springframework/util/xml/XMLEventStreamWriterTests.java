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

package org.springframework.util.xml;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;
import org.xmlunit.util.Predicate;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import java.io.StringWriter;

import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

public class XMLEventStreamWriterTests {

	private static final String XML =
			"<?pi content?><root xmlns='namespace'><prefix:child xmlns:prefix='namespace2'><!--comment-->content</prefix:child></root>";

	private XMLEventStreamWriter streamWriter;

	private StringWriter stringWriter;

	@Before
	public void createStreamReader() throws Exception {
		this.stringWriter = new StringWriter();
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(this.stringWriter);
		this.streamWriter = new XMLEventStreamWriter(eventWriter, XMLEventFactory.newInstance());
	}

	@Test
	public void write() throws Exception {
		this.streamWriter.writeStartDocument();
		this.streamWriter.writeProcessingInstruction("pi", "content");
		this.streamWriter.writeStartElement("namespace", "root");
		this.streamWriter.writeDefaultNamespace("namespace");
		this.streamWriter.writeStartElement("prefix", "child", "namespace2");
		this.streamWriter.writeNamespace("prefix", "namespace2");
		this.streamWriter.writeComment("comment");
		this.streamWriter.writeCharacters("content");
		this.streamWriter.writeEndElement();
		this.streamWriter.writeEndElement();
		this.streamWriter.writeEndDocument();

		Predicate<Node> nodeFilter = n -> n.getNodeType() != Node.DOCUMENT_TYPE_NODE && n.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE;
		assertThat(this.stringWriter.toString(), isSimilarTo(XML).withNodeFilter(nodeFilter));
	}


}
