/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import java.time.ZoneId;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Nicholas Williams
 */
public class ZoneIdEditorTests {

	private final ZoneIdEditor editor = new ZoneIdEditor();

	@Test
	public void americaChicago() {
		this.editor.setAsText("America/Chicago");

		ZoneId zoneId = (ZoneId) this.editor.getValue();
		assertNotNull("The zone ID should not be null.", zoneId);
		assertEquals("The zone ID is not correct.", ZoneId.of("America/Chicago"), zoneId);

		assertEquals("The text version is not correct.", "America/Chicago", this.editor.getAsText());
	}

	@Test
	public void americaLosAngeles() {
		this.editor.setAsText("America/Los_Angeles");

		ZoneId zoneId = (ZoneId) this.editor.getValue();
		assertNotNull("The zone ID should not be null.", zoneId);
		assertEquals("The zone ID is not correct.", ZoneId.of("America/Los_Angeles"), zoneId);

		assertEquals("The text version is not correct.", "America/Los_Angeles", this.editor.getAsText());
	}

	@Test
	public void getNullAsText() {
		assertEquals("The returned value is not correct.", "", this.editor.getAsText());
	}

	@Test
	public void getValueAsText() {
		this.editor.setValue(ZoneId.of("America/New_York"));
		assertEquals("The text version is not correct.", "America/New_York", this.editor.getAsText());
	}

}
