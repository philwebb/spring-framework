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

package org.springframework.beans;

import java.util.Iterator;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link MutablePropertyValues}.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class MutablePropertyValuesTests extends AbstractPropertyValuesTests {

	@Test
	public void testValid() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("forname", "Tony"));
		pvs.addPropertyValue(new PropertyValue("surname", "Blair"));
		pvs.addPropertyValue(new PropertyValue("age", "50"));
		doTestTony(pvs);

		MutablePropertyValues deepCopy = new MutablePropertyValues(pvs);
		doTestTony(deepCopy);
		deepCopy.setPropertyValueAt(new PropertyValue("name", "Gordon"), 0);
		doTestTony(pvs);
		assertEquals("Gordon", deepCopy.getPropertyValue("name").getValue());
	}

	@Test
	public void testAddOrOverride() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("forname", "Tony"));
		pvs.addPropertyValue(new PropertyValue("surname", "Blair"));
		pvs.addPropertyValue(new PropertyValue("age", "50"));
		doTestTony(pvs);
		PropertyValue addedPv = new PropertyValue("rod", "Rod");
		pvs.addPropertyValue(addedPv);
		assertTrue(pvs.getPropertyValue("rod").equals(addedPv));
		PropertyValue changedPv = new PropertyValue("forname", "Greg");
		pvs.addPropertyValue(changedPv);
		assertTrue(pvs.getPropertyValue("forname").equals(changedPv));
	}

	@Test
	public void testChangesOnEquals() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("forname", "Tony"));
		pvs.addPropertyValue(new PropertyValue("surname", "Blair"));
		pvs.addPropertyValue(new PropertyValue("age", "50"));
		MutablePropertyValues pvs2 = pvs;
		PropertyValues changes = pvs2.changesSince(pvs);
		assertTrue("changes are empty", changes.getPropertyValues().length == 0);
	}

	@Test
	public void testChangeOfOneField() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("forname", "Tony"));
		pvs.addPropertyValue(new PropertyValue("surname", "Blair"));
		pvs.addPropertyValue(new PropertyValue("age", "50"));

		MutablePropertyValues pvs2 = new MutablePropertyValues(pvs);
		PropertyValues changes = pvs2.changesSince(pvs);
		assertTrue("changes are empty, not of length " + changes.getPropertyValues().length,
				changes.getPropertyValues().length == 0);

		pvs2.addPropertyValue(new PropertyValue("forname", "Gordon"));
		changes = pvs2.changesSince(pvs);
		assertEquals("1 change", 1, changes.getPropertyValues().length);
		PropertyValue fn = changes.getPropertyValue("forname");
		assertTrue("change is forname", fn != null);
		assertTrue("new value is gordon", fn.getValue().equals("Gordon"));

		MutablePropertyValues pvs3 = new MutablePropertyValues(pvs);
		changes = pvs3.changesSince(pvs);
		assertTrue("changes are empty, not of length " + changes.getPropertyValues().length,
				changes.getPropertyValues().length == 0);

		// add new
		pvs3.addPropertyValue(new PropertyValue("foo", "bar"));
		pvs3.addPropertyValue(new PropertyValue("fi", "fum"));
		changes = pvs3.changesSince(pvs);
		assertTrue("2 change", changes.getPropertyValues().length == 2);
		fn = changes.getPropertyValue("foo");
		assertTrue("change in foo", fn != null);
		assertTrue("new value is bar", fn.getValue().equals("bar"));
	}

	@Test
	public void iteratorContainsPropertyValue() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("foo", "bar");

		Iterator<PropertyValue> it = pvs.iterator();
		assertTrue(it.hasNext());
		PropertyValue pv = it.next();
		assertEquals("foo", pv.getName());
		assertEquals("bar", pv.getValue());
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(it::remove);
		assertFalse(it.hasNext());
	}

	@Test
	public void iteratorIsEmptyForEmptyValues() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		Iterator<PropertyValue> it = pvs.iterator();
		assertFalse(it.hasNext());
	}

	@Test
	public void streamContainsPropertyValue() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("foo", "bar");

		assertThat(pvs.stream(), notNullValue());
		assertThat(pvs.stream().count()).isEqualTo(1L);
		assertThat(pvs.stream().anyMatch(pv -> "foo".equals(pv.getName()) && "bar".equals(pv.getValue()))).isTrue();
		assertThat(pvs.stream().anyMatch(pv -> "bar".equals(pv.getName()) && "foo".equals(pv.getValue()))).isFalse();
	}

	@Test
	public void streamIsEmptyForEmptyValues() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		assertThat(pvs.stream(), notNullValue());
		assertThat(pvs.stream().count()).isEqualTo(0L);
	}

}
