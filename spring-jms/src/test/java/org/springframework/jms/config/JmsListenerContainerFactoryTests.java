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

package org.springframework.jms.config;

import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.transaction.TransactionManager;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.jms.StubConnectionFactory;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.jms.listener.endpoint.JmsActivationSpecConfig;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;
import org.springframework.jms.listener.endpoint.StubJmsActivationSpecFactory;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertSame;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 */
public class JmsListenerContainerFactoryTests {

	private final ConnectionFactory connectionFactory = new StubConnectionFactory();

	private final DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private final MessageConverter messageConverter = new SimpleMessageConverter();

	private final TransactionManager transactionManager = mock(TransactionManager.class);


	@Test
	public void createSimpleContainer() {
		SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
		setDefaultJmsConfig(factory);
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();

		MessageListener messageListener = new MessageListenerAdapter();
		endpoint.setMessageListener(messageListener);
		endpoint.setDestination("myQueue");

		SimpleMessageListenerContainer container = factory.createListenerContainer(endpoint);

		assertDefaultJmsConfig(container);
		assertThat(container.getMessageListener()).isEqualTo(messageListener);
		assertThat((Object) container.getDestinationName()).isEqualTo("myQueue");
	}

	@Test
	public void createJmsContainerFullConfig() {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		setDefaultJmsConfig(factory);
		factory.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
		factory.setConcurrency("3-10");
		factory.setMaxMessagesPerTask(5);

		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		MessageListener messageListener = new MessageListenerAdapter();
		endpoint.setMessageListener(messageListener);
		endpoint.setDestination("myQueue");
		DefaultMessageListenerContainer container = factory.createListenerContainer(endpoint);

		assertDefaultJmsConfig(container);
		assertEquals(DefaultMessageListenerContainer.CACHE_CONSUMER, container.getCacheLevel());
		assertEquals(3, container.getConcurrentConsumers());
		assertEquals(10, container.getMaxConcurrentConsumers());
		assertEquals(5, container.getMaxMessagesPerTask());

		assertThat(container.getMessageListener()).isEqualTo(messageListener);
		assertThat((Object) container.getDestinationName()).isEqualTo("myQueue");
	}

	@Test
	public void createJcaContainerFullConfig() {
		DefaultJcaListenerContainerFactory factory = new DefaultJcaListenerContainerFactory();
		setDefaultJcaConfig(factory);
		factory.setConcurrency("10");

		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		MessageListener messageListener = new MessageListenerAdapter();
		endpoint.setMessageListener(messageListener);
		endpoint.setDestination("myQueue");
		JmsMessageEndpointManager container = factory.createListenerContainer(endpoint);

		assertDefaultJcaConfig(container);
		assertEquals(10, container.getActivationSpecConfig().getMaxConcurrency());
		assertThat((Object) container.getMessageListener()).isEqualTo(messageListener);
		assertThat((Object) container.getActivationSpecConfig().getDestinationName()).isEqualTo("myQueue");
	}

	@Test
	public void jcaExclusiveProperties() {
		DefaultJcaListenerContainerFactory factory = new DefaultJcaListenerContainerFactory();
		factory.setDestinationResolver(this.destinationResolver);
		factory.setActivationSpecFactory(new StubJmsActivationSpecFactory());

		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setMessageListener(new MessageListenerAdapter());
		assertThatIllegalStateException().isThrownBy(() ->
				factory.createListenerContainer(endpoint));
	}

	@Test
	public void backOffOverridesRecoveryInterval() {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		BackOff backOff = new FixedBackOff();
		factory.setBackOff(backOff);
		factory.setRecoveryInterval(2000L);

		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		MessageListener messageListener = new MessageListenerAdapter();
		endpoint.setMessageListener(messageListener);
		endpoint.setDestination("myQueue");
		DefaultMessageListenerContainer container = factory.createListenerContainer(endpoint);

		assertSame(backOff, new DirectFieldAccessor(container).getPropertyValue("backOff"));
	}

	@Test
	public void endpointConcurrencyTakesPrecedence() {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		factory.setConcurrency("2-10");

		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		MessageListener messageListener = new MessageListenerAdapter();
		endpoint.setMessageListener(messageListener);
		endpoint.setDestination("myQueue");
		endpoint.setConcurrency("4-6");
		DefaultMessageListenerContainer container = factory.createListenerContainer(endpoint);
		assertEquals(4, container.getConcurrentConsumers());
		assertEquals(6, container.getMaxConcurrentConsumers());
	}


	private void setDefaultJmsConfig(AbstractJmsListenerContainerFactory<?> factory) {
		factory.setConnectionFactory(this.connectionFactory);
		factory.setDestinationResolver(this.destinationResolver);
		factory.setMessageConverter(this.messageConverter);
		factory.setSessionTransacted(true);
		factory.setSessionAcknowledgeMode(Session.DUPS_OK_ACKNOWLEDGE);
		factory.setPubSubDomain(true);
		factory.setReplyPubSubDomain(true);
		factory.setReplyQosSettings(new QosSettings(1, 7, 5000));
		factory.setSubscriptionDurable(true);
		factory.setClientId("client-1234");
		factory.setAutoStartup(false);
	}

	private void assertDefaultJmsConfig(AbstractMessageListenerContainer container) {
		assertThat((Object) container.getConnectionFactory()).isEqualTo(this.connectionFactory);
		assertThat((Object) container.getDestinationResolver()).isEqualTo(this.destinationResolver);
		assertThat((Object) container.getMessageConverter()).isEqualTo(this.messageConverter);
		assertThat((Object) container.isSessionTransacted()).isEqualTo(true);
		assertEquals(Session.DUPS_OK_ACKNOWLEDGE, container.getSessionAcknowledgeMode());
		assertThat((Object) container.isPubSubDomain()).isEqualTo(true);
		assertThat((Object) container.isReplyPubSubDomain()).isEqualTo(true);
		assertThat((Object) container.getReplyQosSettings()).isEqualTo(new QosSettings(1, 7, 5000));
		assertThat((Object) container.isSubscriptionDurable()).isEqualTo(true);
		assertThat((Object) container.getClientId()).isEqualTo("client-1234");
		assertThat((Object) container.isAutoStartup()).isEqualTo(false);
	}

	private void setDefaultJcaConfig(DefaultJcaListenerContainerFactory factory) {
		factory.setDestinationResolver(this.destinationResolver);
		factory.setTransactionManager(this.transactionManager);
		factory.setMessageConverter(this.messageConverter);
		factory.setAcknowledgeMode(Session.DUPS_OK_ACKNOWLEDGE);
		factory.setPubSubDomain(true);
		factory.setReplyQosSettings(new QosSettings(1, 7, 5000));
		factory.setSubscriptionDurable(true);
		factory.setClientId("client-1234");
	}

	private void assertDefaultJcaConfig(JmsMessageEndpointManager container) {
		assertThat((Object) container.getMessageConverter()).isEqualTo(this.messageConverter);
		assertThat((Object) container.getDestinationResolver()).isEqualTo(this.destinationResolver);
		JmsActivationSpecConfig config = container.getActivationSpecConfig();
		assertNotNull(config);
		assertEquals(Session.DUPS_OK_ACKNOWLEDGE, config.getAcknowledgeMode());
		assertThat((Object) config.isPubSubDomain()).isEqualTo(true);
		assertThat((Object) container.getReplyQosSettings()).isEqualTo(new QosSettings(1, 7, 5000));
		assertThat((Object) config.isSubscriptionDurable()).isEqualTo(true);
		assertThat((Object) config.getClientId()).isEqualTo("client-1234");
	}

}
