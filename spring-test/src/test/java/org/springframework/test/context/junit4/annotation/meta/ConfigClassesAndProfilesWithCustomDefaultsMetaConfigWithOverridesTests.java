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

package org.springframework.test.context.junit4.annotation.meta;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.annotation.PojoAndStringConfig;
import org.springframework.tests.sample.beans.Employee;
import org.springframework.tests.sample.beans.Pet;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;

/**
 * Integration tests for meta-annotation attribute override support, overriding
 * default attribute values defined in {@link ConfigClassesAndProfilesWithCustomDefaultsMetaConfig}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ConfigClassesAndProfilesWithCustomDefaultsMetaConfig(classes = { PojoAndStringConfig.class,
	ConfigClassesAndProfilesWithCustomDefaultsMetaConfig.ProductionConfig.class }, profiles = "prod")
public class ConfigClassesAndProfilesWithCustomDefaultsMetaConfigWithOverridesTests {

	@Autowired
	private String foo;

	@Autowired
	private Pet pet;

	@Autowired
	protected Employee employee;


	@Test
	public void verifyEmployee() {
		assertNotNull("The employee should have been autowired.", this.employee);
		assertThat((Object) this.employee.getName()).isEqualTo("John Smith");
	}

	@Test
	public void verifyPet() {
		assertNotNull("The pet should have been autowired.", this.pet);
		assertThat((Object) this.pet.getName()).isEqualTo("Fido");
	}

	@Test
	public void verifyFoo() {
		assertThat((Object) this.foo).isEqualTo("Production Foo");
	}
}
