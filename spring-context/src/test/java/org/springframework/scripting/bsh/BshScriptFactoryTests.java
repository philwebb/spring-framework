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

package org.springframework.scripting.bsh;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.dynamic.Refreshable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.NestedRuntimeException;
import org.springframework.scripting.Calculator;
import org.springframework.scripting.ConfigurableMessenger;
import org.springframework.scripting.Messenger;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.TestBeanAwareMessenger;
import org.springframework.scripting.support.ScriptFactoryPostProcessor;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.*;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertFalse;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNotSame;
import static temp.XAssert.assertNull;
import static temp.XAssert.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class BshScriptFactoryTests {

	@Test
	public void staticScript() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());

		assertThat(Arrays.asList(ctx.getBeanNamesForType(Calculator.class)).contains("calculator")).isTrue();
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messenger")).isTrue();

		Calculator calc = (Calculator) ctx.getBean("calculator");
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertFalse("Scripted object should not be instance of Refreshable", calc instanceof Refreshable);
		assertFalse("Scripted object should not be instance of Refreshable", messenger instanceof Refreshable);

		assertEquals(calc, calc);
		assertEquals(messenger, messenger);
		boolean condition1 = !messenger.equals(calc);
		assertThat(condition1).isTrue();
		assertThat(messenger.hashCode() != calc.hashCode()).isTrue();
		boolean condition = !messenger.toString().equals(calc.toString());
		assertThat(condition).isTrue();

		assertEquals(5, calc.add(2, 3));

		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());

		assertThat(ctx.getBeansOfType(Calculator.class).values().contains(calc)).isTrue();
		assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();
	}

	@Test
	public void staticScriptWithNullReturnValue() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerWithConfig")).isTrue();

		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerWithConfig");
		messenger.setMessage(null);
		assertNull(messenger.getMessage());
		assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();
	}

	@Test
	public void staticScriptWithTwoInterfacesSpecified() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerWithConfigExtra")).isTrue();

		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerWithConfigExtra");
		messenger.setMessage(null);
		assertNull(messenger.getMessage());
		assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();

		ctx.close();
		assertNull(messenger.getMessage());
	}

	@Test
	public void staticWithScriptReturningInstance() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerInstance")).isTrue();

		Messenger messenger = (Messenger) ctx.getBean("messengerInstance");
		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());
		assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();

		ctx.close();
		assertNull(messenger.getMessage());
	}

	@Test
	public void staticScriptImplementingInterface() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerImpl")).isTrue();

		Messenger messenger = (Messenger) ctx.getBean("messengerImpl");
		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());
		assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();

		ctx.close();
		assertNull(messenger.getMessage());
	}

	@Test
	public void staticPrototypeScript() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertFalse("Shouldn't get proxy when refresh is disabled", AopUtils.isAopProxy(messenger));
		assertFalse("Scripted object should not be instance of Refreshable", messenger instanceof Refreshable);

		assertNotSame(messenger, messenger2);
		assertSame(messenger.getClass(), messenger2.getClass());
		assertEquals("Hello World!", messenger.getMessage());
		assertEquals("Hello World!", messenger2.getMessage());
		messenger.setMessage("Bye World!");
		messenger2.setMessage("Byebye World!");
		assertEquals("Bye World!", messenger.getMessage());
		assertEquals("Byebye World!", messenger2.getMessage());
	}

	@Test
	public void nonStaticScript() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshRefreshableContext.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertThat(AopUtils.isAopProxy(messenger)).as("Should be a proxy for refreshable scripts").isTrue();
		boolean condition = messenger instanceof Refreshable;
		assertThat(condition).as("Should be an instance of Refreshable").isTrue();

		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());

		Refreshable refreshable = (Refreshable) messenger;
		refreshable.refresh();

		assertEquals("Message is incorrect after refresh", desiredMessage, messenger.getMessage());
		assertEquals("Incorrect refresh count", 2, refreshable.getRefreshCount());
	}

	@Test
	public void nonStaticPrototypeScript() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshRefreshableContext.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertThat(AopUtils.isAopProxy(messenger)).as("Should be a proxy for refreshable scripts").isTrue();
		boolean condition = messenger instanceof Refreshable;
		assertThat(condition).as("Should be an instance of Refreshable").isTrue();

		assertEquals("Hello World!", messenger.getMessage());
		assertEquals("Hello World!", messenger2.getMessage());
		messenger.setMessage("Bye World!");
		messenger2.setMessage("Byebye World!");
		assertEquals("Bye World!", messenger.getMessage());
		assertEquals("Byebye World!", messenger2.getMessage());

		Refreshable refreshable = (Refreshable) messenger;
		refreshable.refresh();

		assertEquals("Hello World!", messenger.getMessage());
		assertEquals("Byebye World!", messenger2.getMessage());
		assertEquals("Incorrect refresh count", 2, refreshable.getRefreshCount());
	}

	@Test
	public void scriptCompilationException() {
		assertThatExceptionOfType(NestedRuntimeException.class).isThrownBy(() ->
				new ClassPathXmlApplicationContext("org/springframework/scripting/bsh/bshBrokenContext.xml"))
			.matches(ex -> ex.contains(ScriptCompilationException.class));
	}

	@Test
	public void scriptThatCompilesButIsJustPlainBad() throws IOException {
		ScriptSource script = mock(ScriptSource.class);
		final String badScript = "String getMessage() { throw new IllegalArgumentException(); }";
		given(script.getScriptAsString()).willReturn(badScript);
		given(script.isModified()).willReturn(true);
		BshScriptFactory factory = new BshScriptFactory(
				ScriptFactoryPostProcessor.INLINE_SCRIPT_PREFIX + badScript, Messenger.class);
		assertThatExceptionOfType(BshScriptUtils.BshExecutionException.class).isThrownBy(() -> {
			Messenger messenger = (Messenger) factory.getScriptedObject(script, Messenger.class);
			messenger.getMessage();
		});
	}

	@Test
	public void ctorWithNullScriptSourceLocator() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BshScriptFactory(null, Messenger.class));
	}

	@Test
	public void ctorWithEmptyScriptSourceLocator() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BshScriptFactory("", Messenger.class));
	}

	@Test
	public void ctorWithWhitespacedScriptSourceLocator() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BshScriptFactory("\n   ", Messenger.class));
	}

	@Test
	public void resourceScriptFromTag() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		TestBean testBean = (TestBean) ctx.getBean("testBean");

		Collection<String> beanNames = Arrays.asList(ctx.getBeanNamesForType(Messenger.class));
		assertThat(beanNames.contains("messenger")).isTrue();
		assertThat(beanNames.contains("messengerImpl")).isTrue();
		assertThat(beanNames.contains("messengerInstance")).isTrue();

		Messenger messenger = (Messenger) ctx.getBean("messenger");
		assertEquals("Hello World!", messenger.getMessage());
		assertFalse(messenger instanceof Refreshable);

		Messenger messengerImpl = (Messenger) ctx.getBean("messengerImpl");
		assertEquals("Hello World!", messengerImpl.getMessage());

		Messenger messengerInstance = (Messenger) ctx.getBean("messengerInstance");
		assertEquals("Hello World!", messengerInstance.getMessage());

		TestBeanAwareMessenger messengerByType = (TestBeanAwareMessenger) ctx.getBean("messengerByType");
		assertEquals(testBean, messengerByType.getTestBean());

		TestBeanAwareMessenger messengerByName = (TestBeanAwareMessenger) ctx.getBean("messengerByName");
		assertEquals(testBean, messengerByName.getTestBean());

		Collection<Messenger> beans = ctx.getBeansOfType(Messenger.class).values();
		assertThat(beans.contains(messenger)).isTrue();
		assertThat(beans.contains(messengerImpl)).isTrue();
		assertThat(beans.contains(messengerInstance)).isTrue();
		assertThat(beans.contains(messengerByType)).isTrue();
		assertThat(beans.contains(messengerByName)).isTrue();

		ctx.close();
		assertNull(messenger.getMessage());
		assertNull(messengerImpl.getMessage());
		assertNull(messengerInstance.getMessage());
	}

	@Test
	public void prototypeScriptFromTag() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertNotSame(messenger, messenger2);
		assertSame(messenger.getClass(), messenger2.getClass());
		assertEquals("Hello World!", messenger.getMessage());
		assertEquals("Hello World!", messenger2.getMessage());
		messenger.setMessage("Bye World!");
		messenger2.setMessage("Byebye World!");
		assertEquals("Bye World!", messenger.getMessage());
		assertEquals("Byebye World!", messenger2.getMessage());
	}

	@Test
	public void inlineScriptFromTag() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		Calculator calculator = (Calculator) ctx.getBean("calculator");
		assertNotNull(calculator);
		assertFalse(calculator instanceof Refreshable);
	}

	@Test
	public void refreshableFromTag() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("refreshableMessenger");
		assertEquals("Hello World!", messenger.getMessage());
		boolean condition = messenger instanceof Refreshable;
		assertThat(condition).as("Messenger should be Refreshable").isTrue();
	}

	@Test
	public void applicationEventListener() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		Messenger eventListener = (Messenger) ctx.getBean("eventListener");
		ctx.publishEvent(new MyEvent(ctx));
		assertEquals("count=2", eventListener.getMessage());
	}


	@SuppressWarnings("serial")
	private static class MyEvent extends ApplicationEvent {

		public MyEvent(Object source) {
			super(source);
		}
	}

}
