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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.xml.XMLConstants;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Arjen Poutsma
 * @author Leo Arnold
 */
public class SimpleNamespaceContextTests {

	private final String unboundPrefix = "unbound";
	private final String prefix = "prefix";
	private final String namespaceUri = "http://Namespace-name-URI";
	private final String additionalNamespaceUri = "http://Additional-namespace-name-URI";
	private final String unboundNamespaceUri = "http://Unbound-namespace-name-URI";
	private final String defaultNamespaceUri = "http://Default-namespace-name-URI";

	private final SimpleNamespaceContext context = new SimpleNamespaceContext();


	@Test(expected = IllegalArgumentException.class)
	public void getNamespaceURI_withNull() throws Exception {
		this.context.getNamespaceURI(null);
	}

	@Test
	public void getNamespaceURI() {
		this.context.bindNamespaceUri(XMLConstants.XMLNS_ATTRIBUTE, this.additionalNamespaceUri);
		assertThat("Always returns \"http://www.w3.org/2000/xmlns/\" for \"xmlns\"",
				this.context.getNamespaceURI(XMLConstants.XMLNS_ATTRIBUTE), is(XMLConstants.XMLNS_ATTRIBUTE_NS_URI));
		this.context.bindNamespaceUri(XMLConstants.XML_NS_PREFIX, this.additionalNamespaceUri);
		assertThat("Always returns \"http://www.w3.org/XML/1998/namespace\" for \"xml\"",
				this.context.getNamespaceURI(XMLConstants.XML_NS_PREFIX), is(XMLConstants.XML_NS_URI));

		assertThat("Returns \"\" for an unbound prefix", this.context.getNamespaceURI(this.unboundPrefix),
				is(XMLConstants.NULL_NS_URI));
		this.context.bindNamespaceUri(this.prefix, this.namespaceUri);
		assertThat("Returns the bound namespace URI for a bound prefix", this.context.getNamespaceURI(this.prefix),
				is(this.namespaceUri));

		assertThat("By default returns URI \"\" for the default namespace prefix",
				this.context.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX), is(XMLConstants.NULL_NS_URI));
		this.context.bindDefaultNamespaceUri(this.defaultNamespaceUri);
		assertThat("Returns the set URI for the default namespace prefix",
				this.context.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX), is(this.defaultNamespaceUri));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPrefix_withNull() throws Exception {
		this.context.getPrefix(null);
	}

	@Test
	public void getPrefix() {
		assertThat("Always returns \"xmlns\" for \"http://www.w3.org/2000/xmlns/\"",
				this.context.getPrefix(XMLConstants.XMLNS_ATTRIBUTE_NS_URI), is(XMLConstants.XMLNS_ATTRIBUTE));
		assertThat("Always returns \"xml\" for \"http://www.w3.org/XML/1998/namespace\"",
				this.context.getPrefix(XMLConstants.XML_NS_URI), is(XMLConstants.XML_NS_PREFIX));

		assertThat("Returns null for an unbound namespace URI", this.context.getPrefix(this.unboundNamespaceUri),
				is(nullValue()));
		this.context.bindNamespaceUri("prefix1", this.namespaceUri);
		this.context.bindNamespaceUri("prefix2", this.namespaceUri);
		assertThat("Returns a prefix for a bound namespace URI", this.context.getPrefix(this.namespaceUri),
				anyOf(is("prefix1"), is("prefix2")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPrefixes_withNull() throws Exception {
		this.context.getPrefixes(null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getPrefixes_IteratorIsNotModifiable() throws Exception {
		this.context.bindNamespaceUri(this.prefix, this.namespaceUri);
		Iterator<String> iterator = this.context.getPrefixes(this.namespaceUri);
		iterator.remove();
	}

	@Test
	public void getPrefixes() {
		assertThat("Returns only \"xmlns\" for \"http://www.w3.org/2000/xmlns/\"",
				getItemSet(this.context.getPrefixes(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)),
				is(makeSet(XMLConstants.XMLNS_ATTRIBUTE)));
		assertThat("Returns only \"xml\" for \"http://www.w3.org/XML/1998/namespace\"",
				getItemSet(this.context.getPrefixes(XMLConstants.XML_NS_URI)), is(makeSet(XMLConstants.XML_NS_PREFIX)));

		assertThat("Returns empty iterator for unbound prefix", this.context.getPrefixes("unbound Namespace URI").hasNext(),
				is(false));
		this.context.bindNamespaceUri("prefix1", this.namespaceUri);
		this.context.bindNamespaceUri("prefix2", this.namespaceUri);
		assertThat("Returns all prefixes (and only those) bound to the namespace URI",
				getItemSet(this.context.getPrefixes(this.namespaceUri)), is(makeSet("prefix1", "prefix2")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void bindNamespaceUri_withNullNamespaceUri() {
		this.context.bindNamespaceUri("prefix", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void bindNamespaceUri_withNullPrefix() {
		this.context.bindNamespaceUri(null, this.namespaceUri);
	}

	@Test
	public void bindNamespaceUri() {
		this.context.bindNamespaceUri(this.prefix, this.namespaceUri);
		assertThat("The Namespace URI was bound to the prefix", this.context.getNamespaceURI(this.prefix), is(this.namespaceUri));
		assertThat("The prefix was bound to the namespace URI", getItemSet(this.context.getPrefixes(this.namespaceUri)),
				hasItem(this.prefix));
	}

	@Test
	public void getBoundPrefixes() {
		this.context.bindNamespaceUri("prefix1", this.namespaceUri);
		this.context.bindNamespaceUri("prefix2", this.namespaceUri);
		this.context.bindNamespaceUri("prefix3", this.additionalNamespaceUri);
		assertThat("Returns all bound prefixes", getItemSet(this.context.getBoundPrefixes()),
				is(makeSet("prefix1", "prefix2", "prefix3")));
	}

	@Test
	public void clear() {
		this.context.bindNamespaceUri("prefix1", this.namespaceUri);
		this.context.bindNamespaceUri("prefix2", this.namespaceUri);
		this.context.bindNamespaceUri("prefix3", this.additionalNamespaceUri);
		this.context.clear();
		assertThat("All bound prefixes were removed", this.context.getBoundPrefixes().hasNext(), is(false));
		assertThat("All bound namespace URIs were removed", this.context.getPrefixes(this.namespaceUri).hasNext(), is(false));
	}

	@Test
	public void removeBinding() {
		this.context.removeBinding(this.unboundPrefix);

		this.context.bindNamespaceUri(this.prefix, this.namespaceUri);
		this.context.removeBinding(this.prefix);
		assertThat("Returns default namespace URI for removed prefix", this.context.getNamespaceURI(this.prefix),
				is(XMLConstants.NULL_NS_URI));
		assertThat("#getPrefix returns null when all prefixes for a namespace URI were removed",
				this.context.getPrefix(this.namespaceUri), is(nullValue()));
		assertThat("#getPrefixes returns an empty iterator when all prefixes for a namespace URI were removed",
				this.context.getPrefixes(this.namespaceUri).hasNext(), is(false));

		this.context.bindNamespaceUri("prefix1", this.additionalNamespaceUri);
		this.context.bindNamespaceUri("prefix2", this.additionalNamespaceUri);
		this.context.removeBinding("prefix1");
		assertThat("Prefix was unbound", this.context.getNamespaceURI("prefix1"), is(XMLConstants.NULL_NS_URI));
		assertThat("#getPrefix returns a bound prefix after removal of another prefix for the same namespace URI",
				this.context.getPrefix(this.additionalNamespaceUri), is("prefix2"));
		assertThat("Prefix was removed from namespace URI", getItemSet(this.context.getPrefixes(this.additionalNamespaceUri)),
				is(makeSet("prefix2")));
	}


	private Set<String> getItemSet(Iterator<String> iterator) {
		Set<String> itemSet = new HashSet<>();
		while (iterator.hasNext()) {
			itemSet.add(iterator.next());
		}
		return itemSet;
	}

	private Set<String> makeSet(String... items) {
		Set<String> itemSet = new HashSet<>();
		for (String item : items) {
			itemSet.add(item);
		}
		return itemSet;
	}

}
