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

package org.springframework.transaction.interceptor;


import java.io.IOException;

import org.junit.Test;

import org.springframework.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests to check conversion from String to TransactionAttribute.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 26.04.2003
 */
public class TransactionAttributeEditorTests {

	@Test
	public void testNull() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText(null);
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(ta == null).isTrue();
	}

	@Test
	public void testEmptyString() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText("");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(ta == null).isTrue();
	}

	@Test
	public void testValidPropagationCodeOnly() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText("PROPAGATION_REQUIRED");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(ta != null).isTrue();
		assertThat(ta.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED).isTrue();
		assertThat(ta.getIsolationLevel() == TransactionDefinition.ISOLATION_DEFAULT).isTrue();
		boolean condition = !ta.isReadOnly();
		assertThat(condition).isTrue();
	}

	@Test
	public void testInvalidPropagationCodeOnly() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		// should have failed with bogus propagation code
		assertThatIllegalArgumentException().isThrownBy(() ->
				pe.setAsText("XXPROPAGATION_REQUIRED"));
	}

	@Test
	public void testValidPropagationCodeAndIsolationCode() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText("PROPAGATION_REQUIRED, ISOLATION_READ_UNCOMMITTED");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(ta != null).isTrue();
		assertThat(ta.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED).isTrue();
		assertThat(ta.getIsolationLevel() == TransactionDefinition.ISOLATION_READ_UNCOMMITTED).isTrue();
	}

	@Test
	public void testValidPropagationAndIsolationCodesAndInvalidRollbackRule() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		// should fail with bogus rollback rule
		assertThatIllegalArgumentException().isThrownBy(() ->
				pe.setAsText("PROPAGATION_REQUIRED,ISOLATION_READ_UNCOMMITTED,XXX"));
	}

	@Test
	public void testValidPropagationCodeAndIsolationCodeAndRollbackRules1() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText("PROPAGATION_MANDATORY,ISOLATION_REPEATABLE_READ,timeout_10,-IOException,+MyRuntimeException");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(ta).isNotNull();
		assertThat(ta.getPropagationBehavior()).isEqualTo((long) TransactionDefinition.PROPAGATION_MANDATORY);
		assertThat(ta.getIsolationLevel()).isEqualTo((long) TransactionDefinition.ISOLATION_REPEATABLE_READ);
		assertThat(ta.getTimeout()).isEqualTo((long) 10);
		assertThat(ta.isReadOnly()).isFalse();
		assertThat(ta.rollbackOn(new RuntimeException())).isTrue();
		assertThat(ta.rollbackOn(new Exception())).isFalse();
		// Check for our bizarre customized rollback rules
		assertThat(ta.rollbackOn(new IOException())).isTrue();
		boolean condition = !ta.rollbackOn(new MyRuntimeException(""));
		assertThat(condition).isTrue();
	}

	@Test
	public void testValidPropagationCodeAndIsolationCodeAndRollbackRules2() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText("+IOException,readOnly,ISOLATION_READ_COMMITTED,-MyRuntimeException,PROPAGATION_SUPPORTS");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(ta).isNotNull();
		assertThat(ta.getPropagationBehavior()).isEqualTo((long) TransactionDefinition.PROPAGATION_SUPPORTS);
		assertThat(ta.getIsolationLevel()).isEqualTo((long) TransactionDefinition.ISOLATION_READ_COMMITTED);
		assertThat(ta.getTimeout()).isEqualTo((long) TransactionDefinition.TIMEOUT_DEFAULT);
		assertThat(ta.isReadOnly()).isTrue();
		assertThat(ta.rollbackOn(new RuntimeException())).isTrue();
		assertThat(ta.rollbackOn(new Exception())).isFalse();
		// Check for our bizarre customized rollback rules
		assertThat(ta.rollbackOn(new IOException())).isFalse();
		assertThat(ta.rollbackOn(new MyRuntimeException(""))).isTrue();
	}

	@Test
	public void testDefaultTransactionAttributeToString() {
		DefaultTransactionAttribute source = new DefaultTransactionAttribute();
		source.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		source.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		source.setTimeout(10);
		source.setReadOnly(true);

		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText(source.toString());
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(source).isEqualTo(ta);
		assertThat(ta.getPropagationBehavior()).isEqualTo((long) TransactionDefinition.PROPAGATION_SUPPORTS);
		assertThat(ta.getIsolationLevel()).isEqualTo((long) TransactionDefinition.ISOLATION_REPEATABLE_READ);
		assertThat(ta.getTimeout()).isEqualTo((long) 10);
		assertThat(ta.isReadOnly()).isTrue();
		assertThat(ta.rollbackOn(new RuntimeException())).isTrue();
		assertThat(ta.rollbackOn(new Exception())).isFalse();

		source.setTimeout(9);
		assertThat(source).isNotSameAs(ta);
		source.setTimeout(10);
		assertThat(source).isEqualTo(ta);
	}

	@Test
	public void testRuleBasedTransactionAttributeToString() {
		RuleBasedTransactionAttribute source = new RuleBasedTransactionAttribute();
		source.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		source.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		source.setTimeout(10);
		source.setReadOnly(true);
		source.getRollbackRules().add(new RollbackRuleAttribute("IllegalArgumentException"));
		source.getRollbackRules().add(new NoRollbackRuleAttribute("IllegalStateException"));

		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText(source.toString());
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(source).isEqualTo(ta);
		assertThat(ta.getPropagationBehavior()).isEqualTo((long) TransactionDefinition.PROPAGATION_SUPPORTS);
		assertThat(ta.getIsolationLevel()).isEqualTo((long) TransactionDefinition.ISOLATION_REPEATABLE_READ);
		assertThat(ta.getTimeout()).isEqualTo((long) 10);
		assertThat(ta.isReadOnly()).isTrue();
		assertThat(ta.rollbackOn(new IllegalArgumentException())).isTrue();
		assertThat(ta.rollbackOn(new IllegalStateException())).isFalse();

		source.getRollbackRules().clear();
		assertThat(source).isNotSameAs(ta);
		source.getRollbackRules().add(new RollbackRuleAttribute("IllegalArgumentException"));
		source.getRollbackRules().add(new NoRollbackRuleAttribute("IllegalStateException"));
		assertThat(source).isEqualTo(ta);
	}

}
