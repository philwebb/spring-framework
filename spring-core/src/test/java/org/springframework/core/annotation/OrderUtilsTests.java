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

package org.springframework.core.annotation;

import javax.annotation.Priority;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class OrderUtilsTests {

	@Test
	public void getSimpleOrder() {
		assertThat((Object) OrderUtils.getOrder(SimpleOrder.class, null)).isEqualTo(Integer.valueOf(50));
		assertThat((Object) OrderUtils.getOrder(SimpleOrder.class, null)).isEqualTo(Integer.valueOf(50));
	}

	@Test
	public void getPriorityOrder() {
		assertThat((Object) OrderUtils.getOrder(SimplePriority.class, null)).isEqualTo(Integer.valueOf(55));
		assertThat((Object) OrderUtils.getOrder(SimplePriority.class, null)).isEqualTo(Integer.valueOf(55));
	}

	@Test
	public void getOrderWithBoth() {
		assertThat((Object) OrderUtils.getOrder(OrderAndPriority.class, null)).isEqualTo(Integer.valueOf(50));
		assertThat((Object) OrderUtils.getOrder(OrderAndPriority.class, null)).isEqualTo(Integer.valueOf(50));
	}

	@Test
	public void getDefaultOrder() {
		assertEquals(33, OrderUtils.getOrder(NoOrder.class, 33));
		assertEquals(33, OrderUtils.getOrder(NoOrder.class, 33));
	}

	@Test
	public void getPriorityValueNoAnnotation() {
		assertNull(OrderUtils.getPriority(SimpleOrder.class));
		assertNull(OrderUtils.getPriority(SimpleOrder.class));
	}

	@Test
	public void getPriorityValue() {
		assertThat((Object) OrderUtils.getPriority(OrderAndPriority.class)).isEqualTo(Integer.valueOf(55));
		assertThat((Object) OrderUtils.getPriority(OrderAndPriority.class)).isEqualTo(Integer.valueOf(55));
	}


	@Order(50)
	private static class SimpleOrder {}

	@Priority(55)
	private static class SimplePriority {}

	@Order(50)
	@Priority(55)
	private static class OrderAndPriority {}

	private static class NoOrder {}

}
