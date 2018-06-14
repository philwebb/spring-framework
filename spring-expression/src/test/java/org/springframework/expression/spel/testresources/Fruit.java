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

package org.springframework.expression.spel.testresources;

import java.awt.Color;

///CLOVER:OFF
public class Fruit {
	public String name; // accessible as property field
	public Color color; // accessible as property through getter/setter
	public String colorName; // accessible as property through getter/setter
	public int stringscount = -1;

	public Fruit(String name, Color color, String colorName) {
		this.name = name;
		this.color = color;
		this.colorName = colorName;
	}

	public Color getColor() {
		return this.color;
	}

	public Fruit(String... strings) {
		this.stringscount = strings.length;
	}

	public Fruit(int i, String... strings) {
		this.stringscount = i + strings.length;
	}

	public int stringscount() {
		return this.stringscount;
	}

	@Override
	public String toString() {
		return "A" + (this.colorName != null && this.colorName.startsWith("o") ? "n " : " ") + this.colorName + " " + this.name;
	}
}
