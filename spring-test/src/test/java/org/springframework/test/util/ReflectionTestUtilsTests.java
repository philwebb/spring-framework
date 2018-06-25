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

package org.springframework.test.util;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.test.util.subpackage.Component;
import org.springframework.test.util.subpackage.LegacyEntity;
import org.springframework.test.util.subpackage.Person;
import org.springframework.test.util.subpackage.PersonEntity;
import org.springframework.test.util.subpackage.StaticFields;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.invokeGetterMethod;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;
import static org.springframework.test.util.ReflectionTestUtils.invokeSetterMethod;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * Unit tests for {@link ReflectionTestUtils}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 */
public class ReflectionTestUtilsTests {

	private static final Float PI = Float.valueOf((float) 22 / 7);

	private final Person person = new PersonEntity();

	private final Component component = new Component();

	private final LegacyEntity entity = new LegacyEntity();

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Before
	public void resetStaticFields() {
		StaticFields.reset();
	}

	@Test
	public void setFieldWithNullTargetObject() throws Exception {
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage(startsWith("Either targetObject or targetClass"));
		setField((Object) null, "id", Long.valueOf(99));
	}

	@Test
	public void getFieldWithNullTargetObject() throws Exception {
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage(startsWith("Either targetObject or targetClass"));
		getField((Object) null, "id");
	}

	@Test
	public void setFieldWithNullTargetClass() throws Exception {
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage(startsWith("Either targetObject or targetClass"));
		setField((Class<?>) null, "id", Long.valueOf(99));
	}

	@Test
	public void getFieldWithNullTargetClass() throws Exception {
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage(startsWith("Either targetObject or targetClass"));
		getField((Class<?>) null, "id");
	}

	@Test
	public void setFieldWithNullNameAndNullType() throws Exception {
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage(startsWith("Either name or type"));
		setField(this.person, null, Long.valueOf(99), null);
	}

	@Test
	public void setFieldWithBogusName() throws Exception {
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage(startsWith("Could not find field 'bogus'"));
		setField(this.person, "bogus", Long.valueOf(99), long.class);
	}

	@Test
	public void setFieldWithWrongType() throws Exception {
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage(startsWith("Could not find field"));
		setField(this.person, "id", Long.valueOf(99), String.class);
	}

	@Test
	public void setFieldAndGetFieldForStandardUseCases() throws Exception {
		assertSetFieldAndGetFieldBehavior(this.person);
	}

	@Test
	public void setFieldAndGetFieldViaJdkDynamicProxy() throws Exception {
		ProxyFactory pf = new ProxyFactory(this.person);
		pf.addInterface(Person.class);
		Person proxy = (Person) pf.getProxy();
		assertTrue("Proxy is a JDK dynamic proxy", AopUtils.isJdkDynamicProxy(proxy));
		assertSetFieldAndGetFieldBehaviorForProxy(proxy, this.person);
	}

	@Test
	public void setFieldAndGetFieldViaCglibProxy() throws Exception {
		ProxyFactory pf = new ProxyFactory(this.person);
		pf.setProxyTargetClass(true);
		Person proxy = (Person) pf.getProxy();
		assertTrue("Proxy is a CGLIB proxy", AopUtils.isCglibProxy(proxy));
		assertSetFieldAndGetFieldBehaviorForProxy(proxy, this.person);
	}

	private static void assertSetFieldAndGetFieldBehavior(Person person) {
		// Set reflectively
		setField(person, "id", Long.valueOf(99), long.class);
		setField(person, "name", "Tom");
		setField(person, "age", Integer.valueOf(42));
		setField(person, "eyeColor", "blue", String.class);
		setField(person, "likesPets", Boolean.TRUE);
		setField(person, "favoriteNumber", PI, Number.class);

		// Get reflectively
		assertEquals(Long.valueOf(99), getField(person, "id"));
		assertEquals("Tom", getField(person, "name"));
		assertEquals(Integer.valueOf(42), getField(person, "age"));
		assertEquals("blue", getField(person, "eyeColor"));
		assertEquals(Boolean.TRUE, getField(person, "likesPets"));
		assertEquals(PI, getField(person, "favoriteNumber"));

		// Get directly
		assertEquals("ID (private field in a superclass)", 99, person.getId());
		assertEquals("name (protected field)", "Tom", person.getName());
		assertEquals("age (private field)", 42, person.getAge());
		assertEquals("eye color (package private field)", "blue", person.getEyeColor());
		assertEquals("'likes pets' flag (package private boolean field)", true, person.likesPets());
		assertEquals("'favorite number' (package field)", PI, person.getFavoriteNumber());
	}

	private static void assertSetFieldAndGetFieldBehaviorForProxy(Person proxy, Person target) {
		assertSetFieldAndGetFieldBehavior(proxy);

		// Get directly from Target
		assertEquals("ID (private field in a superclass)", 99, target.getId());
		assertEquals("name (protected field)", "Tom", target.getName());
		assertEquals("age (private field)", 42, target.getAge());
		assertEquals("eye color (package private field)", "blue", target.getEyeColor());
		assertEquals("'likes pets' flag (package private boolean field)", true, target.likesPets());
		assertEquals("'favorite number' (package field)", PI, target.getFavoriteNumber());
	}

	@Test
	public void setFieldWithNullValuesForNonPrimitives() throws Exception {
		// Fields must be non-null to start with
		setField(this.person, "name", "Tom");
		setField(this.person, "eyeColor", "blue", String.class);
		setField(this.person, "favoriteNumber", PI, Number.class);
		assertNotNull(this.person.getName());
		assertNotNull(this.person.getEyeColor());
		assertNotNull(this.person.getFavoriteNumber());

		// Set to null
		setField(this.person, "name", null, String.class);
		setField(this.person, "eyeColor", null, String.class);
		setField(this.person, "favoriteNumber", null, Number.class);

		assertNull("name (protected field)", this.person.getName());
		assertNull("eye color (package private field)", this.person.getEyeColor());
		assertNull("'favorite number' (package field)", this.person.getFavoriteNumber());
	}

	@Test(expected = IllegalArgumentException.class)
	public void setFieldWithNullValueForPrimitiveLong() throws Exception {
		setField(this.person, "id", null, long.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setFieldWithNullValueForPrimitiveInt() throws Exception {
		setField(this.person, "age", null, int.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setFieldWithNullValueForPrimitiveBoolean() throws Exception {
		setField(this.person, "likesPets", null, boolean.class);
	}

	@Test
	public void setStaticFieldViaClass() throws Exception {
		setField(StaticFields.class, "publicField", "xxx");
		setField(StaticFields.class, "privateField", "yyy");

		assertEquals("public static field", "xxx", StaticFields.publicField);
		assertEquals("private static field", "yyy", StaticFields.getPrivateField());
	}

	@Test
	public void setStaticFieldViaClassWithExplicitType() throws Exception {
		setField(StaticFields.class, "publicField", "xxx", String.class);
		setField(StaticFields.class, "privateField", "yyy", String.class);

		assertEquals("public static field", "xxx", StaticFields.publicField);
		assertEquals("private static field", "yyy", StaticFields.getPrivateField());
	}

	@Test
	public void setStaticFieldViaInstance() throws Exception {
		StaticFields staticFields = new StaticFields();
		setField(staticFields, null, "publicField", "xxx", null);
		setField(staticFields, null, "privateField", "yyy", null);

		assertEquals("public static field", "xxx", StaticFields.publicField);
		assertEquals("private static field", "yyy", StaticFields.getPrivateField());
	}

	@Test
	public void getStaticFieldViaClass() throws Exception {
		assertEquals("public static field", "public", getField(StaticFields.class, "publicField"));
		assertEquals("private static field", "private", getField(StaticFields.class, "privateField"));
	}

	@Test
	public void getStaticFieldViaInstance() throws Exception {
		StaticFields staticFields = new StaticFields();
		assertEquals("public static field", "public", getField(staticFields, "publicField"));
		assertEquals("private static field", "private", getField(staticFields, "privateField"));
	}

	@Test
	public void invokeSetterMethodAndInvokeGetterMethodWithExplicitMethodNames() throws Exception {
		invokeSetterMethod(this.person, "setId", Long.valueOf(1), long.class);
		invokeSetterMethod(this.person, "setName", "Jerry", String.class);
		invokeSetterMethod(this.person, "setAge", Integer.valueOf(33), int.class);
		invokeSetterMethod(this.person, "setEyeColor", "green", String.class);
		invokeSetterMethod(this.person, "setLikesPets", Boolean.FALSE, boolean.class);
		invokeSetterMethod(this.person, "setFavoriteNumber", Integer.valueOf(42), Number.class);

		assertEquals("ID (protected method in a superclass)", 1, this.person.getId());
		assertEquals("name (private method)", "Jerry", this.person.getName());
		assertEquals("age (protected method)", 33, this.person.getAge());
		assertEquals("eye color (package private method)", "green", this.person.getEyeColor());
		assertEquals("'likes pets' flag (protected method for a boolean)", false, this.person.likesPets());
		assertEquals("'favorite number' (protected method for a Number)", Integer.valueOf(42), this.person.getFavoriteNumber());

		assertEquals(Long.valueOf(1), invokeGetterMethod(this.person, "getId"));
		assertEquals("Jerry", invokeGetterMethod(this.person, "getName"));
		assertEquals(Integer.valueOf(33), invokeGetterMethod(this.person, "getAge"));
		assertEquals("green", invokeGetterMethod(this.person, "getEyeColor"));
		assertEquals(Boolean.FALSE, invokeGetterMethod(this.person, "likesPets"));
		assertEquals(Integer.valueOf(42), invokeGetterMethod(this.person, "getFavoriteNumber"));
	}

	@Test
	public void invokeSetterMethodAndInvokeGetterMethodWithJavaBeanPropertyNames() throws Exception {
		invokeSetterMethod(this.person, "id", Long.valueOf(99), long.class);
		invokeSetterMethod(this.person, "name", "Tom");
		invokeSetterMethod(this.person, "age", Integer.valueOf(42));
		invokeSetterMethod(this.person, "eyeColor", "blue", String.class);
		invokeSetterMethod(this.person, "likesPets", Boolean.TRUE);
		invokeSetterMethod(this.person, "favoriteNumber", PI, Number.class);

		assertEquals("ID (protected method in a superclass)", 99, this.person.getId());
		assertEquals("name (private method)", "Tom", this.person.getName());
		assertEquals("age (protected method)", 42, this.person.getAge());
		assertEquals("eye color (package private method)", "blue", this.person.getEyeColor());
		assertEquals("'likes pets' flag (protected method for a boolean)", true, this.person.likesPets());
		assertEquals("'favorite number' (protected method for a Number)", PI, this.person.getFavoriteNumber());

		assertEquals(Long.valueOf(99), invokeGetterMethod(this.person, "id"));
		assertEquals("Tom", invokeGetterMethod(this.person, "name"));
		assertEquals(Integer.valueOf(42), invokeGetterMethod(this.person, "age"));
		assertEquals("blue", invokeGetterMethod(this.person, "eyeColor"));
		assertEquals(Boolean.TRUE, invokeGetterMethod(this.person, "likesPets"));
		assertEquals(PI, invokeGetterMethod(this.person, "favoriteNumber"));
	}

	@Test
	public void invokeSetterMethodWithNullValuesForNonPrimitives() throws Exception {
		invokeSetterMethod(this.person, "name", null, String.class);
		invokeSetterMethod(this.person, "eyeColor", null, String.class);
		invokeSetterMethod(this.person, "favoriteNumber", null, Number.class);

		assertNull("name (private method)", this.person.getName());
		assertNull("eye color (package private method)", this.person.getEyeColor());
		assertNull("'favorite number' (protected method for a Number)", this.person.getFavoriteNumber());
	}

	@Test(expected = IllegalArgumentException.class)
	public void invokeSetterMethodWithNullValueForPrimitiveLong() throws Exception {
		invokeSetterMethod(this.person, "id", null, long.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invokeSetterMethodWithNullValueForPrimitiveInt() throws Exception {
		invokeSetterMethod(this.person, "age", null, int.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invokeSetterMethodWithNullValueForPrimitiveBoolean() throws Exception {
		invokeSetterMethod(this.person, "likesPets", null, boolean.class);
	}

	@Test
	public void invokeMethodWithAutoboxingAndUnboxing() {
		// IntelliJ IDEA 11 won't accept int assignment here
		Integer difference = invokeMethod(this.component, "subtract", 5, 2);
		assertEquals("subtract(5, 2)", 3, difference.intValue());
	}

	@Test
	@Ignore("[SPR-8644] findMethod() does not currently support var-args")
	public void invokeMethodWithPrimitiveVarArgs() {
		// IntelliJ IDEA 11 won't accept int assignment here
		Integer sum = invokeMethod(this.component, "add", 1, 2, 3, 4);
		assertEquals("add(1,2,3,4)", 10, sum.intValue());
	}

	@Test
	public void invokeMethodWithPrimitiveVarArgsAsSingleArgument() {
		// IntelliJ IDEA 11 won't accept int assignment here
		Integer sum = invokeMethod(this.component, "add", new int[] { 1, 2, 3, 4 });
		assertEquals("add(1,2,3,4)", 10, sum.intValue());
	}

	@Test
	public void invokeMethodSimulatingLifecycleEvents() {
		assertNull("number", this.component.getNumber());
		assertNull("text", this.component.getText());

		// Simulate autowiring a configuration method
		invokeMethod(this.component, "configure", Integer.valueOf(42), "enigma");
		assertEquals("number should have been configured", Integer.valueOf(42), this.component.getNumber());
		assertEquals("text should have been configured", "enigma", this.component.getText());

		// Simulate @PostConstruct life-cycle event
		invokeMethod(this.component, "init");
		// assertions in init() should succeed

		// Simulate @PreDestroy life-cycle event
		invokeMethod(this.component, "destroy");
		assertNull("number", this.component.getNumber());
		assertNull("text", this.component.getText());
	}

	@Test
	public void invokeInitMethodBeforeAutowiring() {
		this.exception.expect(IllegalStateException.class);
		this.exception.expectMessage(equalTo("number must not be null"));
		invokeMethod(this.component, "init");
	}

	@Test
	public void invokeMethodWithIncompatibleArgumentTypes() {
		this.exception.expect(IllegalStateException.class);
		this.exception.expectMessage(startsWith("Method not found"));
		invokeMethod(this.component, "subtract", "foo", 2.0);
	}

	@Test
	public void invokeMethodWithTooFewArguments() {
		this.exception.expect(IllegalStateException.class);
		this.exception.expectMessage(startsWith("Method not found"));
		invokeMethod(this.component, "configure", Integer.valueOf(42));
	}

	@Test
	public void invokeMethodWithTooManyArguments() {
		this.exception.expect(IllegalStateException.class);
		this.exception.expectMessage(startsWith("Method not found"));
		invokeMethod(this.component, "configure", Integer.valueOf(42), "enigma", "baz", "quux");
	}

	@Test // SPR-14363
	public void getFieldOnLegacyEntityWithSideEffectsInToString() {
		Object collaborator = getField(this.entity, "collaborator");
		assertNotNull(collaborator);
	}

	@Test // SPR-9571 and SPR-14363
	public void setFieldOnLegacyEntityWithSideEffectsInToString() {
		String testCollaborator = "test collaborator";
		setField(this.entity, "collaborator", testCollaborator, Object.class);
		assertTrue(this.entity.toString().contains(testCollaborator));
	}

	@Test // SPR-14363
	public void invokeMethodOnLegacyEntityWithSideEffectsInToString() {
		invokeMethod(this.entity, "configure", Integer.valueOf(42), "enigma");
		assertEquals("number should have been configured", Integer.valueOf(42), this.entity.getNumber());
		assertEquals("text should have been configured", "enigma", this.entity.getText());
	}

	@Test // SPR-14363
	public void invokeGetterMethodOnLegacyEntityWithSideEffectsInToString() {
		Object collaborator = invokeGetterMethod(this.entity, "collaborator");
		assertNotNull(collaborator);
	}

	@Test // SPR-14363
	public void invokeSetterMethodOnLegacyEntityWithSideEffectsInToString() {
		String testCollaborator = "test collaborator";
		invokeSetterMethod(this.entity, "collaborator", testCollaborator);
		assertTrue(this.entity.toString().contains(testCollaborator));
	}

}
