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

package org.springframework.expression.spel.standard;

import java.util.function.Consumer;

import org.junit.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;

/**
 * @author Andy Clement
 * @author Juergen Hoeller
 */
public class SpelParserTests {

	@Test
	public void theMostBasic() {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("2");
		assertNotNull(expr);
		assertNotNull(expr.getAST());
		assertThat(expr.getValue()).isEqualTo(2);
		assertThat((Object) expr.getValueType()).isEqualTo(Integer.class);
		assertThat(expr.getAST().getValue(null)).isEqualTo(2);
	}

	@Test
	public void valueType() {
		SpelExpressionParser parser = new SpelExpressionParser();
		EvaluationContext ctx = new StandardEvaluationContext();
		Class<?> c = parser.parseRaw("2").getValueType();
		assertThat((Object) c).isEqualTo(Integer.class);
		c = parser.parseRaw("12").getValueType(ctx);
		assertThat((Object) c).isEqualTo(Integer.class);
		c = parser.parseRaw("null").getValueType();
		assertNull(c);
		c = parser.parseRaw("null").getValueType(ctx);
		assertNull(c);
		Object o = parser.parseRaw("null").getValue(ctx, Integer.class);
		assertNull(o);
	}

	@Test
	public void whitespace() {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("2      +    3");
		assertThat(expr.getValue()).isEqualTo(5);
		expr = parser.parseRaw("2	+	3");
		assertThat(expr.getValue()).isEqualTo(5);
		expr = parser.parseRaw("2\n+\t3");
		assertThat(expr.getValue()).isEqualTo(5);
		expr = parser.parseRaw("2\r\n+\t3");
		assertThat(expr.getValue()).isEqualTo(5);
	}

	@Test
	public void arithmeticPlus1() {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("2+2");
		assertNotNull(expr);
		assertNotNull(expr.getAST());
		assertThat(expr.getValue()).isEqualTo(4);
	}

	@Test
	public void arithmeticPlus2() {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("37+41");
		assertThat(expr.getValue()).isEqualTo(78);
	}

	@Test
	public void arithmeticMultiply1() {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("2*3");
		assertNotNull(expr);
		assertNotNull(expr.getAST());
		// printAst(expr.getAST(),0);
		assertThat(expr.getValue()).isEqualTo(6);
	}

	@Test
	public void arithmeticPrecedence1() {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("2*3+5");
		assertThat(expr.getValue()).isEqualTo(11);
	}

	@Test
	public void generalExpressions() {
		assertThatExceptionOfType(SpelParseException.class).isThrownBy(() -> {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw("new String");
		})
		.satisfies(ex -> parseExceptionRequirements(SpelMessage.MISSING_CONSTRUCTOR_ARGS, 10));

		assertThatExceptionOfType(SpelParseException.class).isThrownBy(() -> {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw("new String(3,");
		})
		.satisfies(ex -> parseExceptionRequirements(SpelMessage.RUN_OUT_OF_ARGUMENTS, 10));

		assertThatExceptionOfType(SpelParseException.class).isThrownBy(() -> {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw("new String(3");
		})
		.satisfies(ex -> parseExceptionRequirements(SpelMessage.RUN_OUT_OF_ARGUMENTS, 10));

		assertThatExceptionOfType(SpelParseException.class).isThrownBy(() -> {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw("new String(");
		})
		.satisfies(ex -> parseExceptionRequirements(SpelMessage.RUN_OUT_OF_ARGUMENTS, 10));

		assertThatExceptionOfType(SpelParseException.class).isThrownBy(() -> {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw("\"abc");
		})
		.satisfies(ex -> parseExceptionRequirements(SpelMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING, 0));

		assertThatExceptionOfType(SpelParseException.class).isThrownBy(() -> {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw("'abc");
		})
		.satisfies(ex -> parseExceptionRequirements(SpelMessage.NON_TERMINATING_QUOTED_STRING, 0));

	}

	private <E extends SpelParseException> Consumer<E> parseExceptionRequirements(
			SpelMessage expectedMessage, int expectedPosition) {
		return ex -> {
			assertThat(ex.getMessageCode()).isEqualTo(expectedMessage);
			assertThat(ex.getPosition()).isEqualTo(expectedPosition);
			assertThat(ex.getMessage()).contains(ex.getExpressionString());
		};
	}

	@Test
	public void arithmeticPrecedence2() {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("2+3*5");
		assertThat(expr.getValue()).isEqualTo(17);
	}

	@Test
	public void arithmeticPrecedence3() {
		SpelExpression expr = new SpelExpressionParser().parseRaw("3+10/2");
		assertThat(expr.getValue()).isEqualTo(8);
	}

	@Test
	public void arithmeticPrecedence4() {
		SpelExpression expr = new SpelExpressionParser().parseRaw("10/2+3");
		assertThat(expr.getValue()).isEqualTo(8);
	}

	@Test
	public void arithmeticPrecedence5() {
		SpelExpression expr = new SpelExpressionParser().parseRaw("(4+10)/2");
		assertThat(expr.getValue()).isEqualTo(7);
	}

	@Test
	public void arithmeticPrecedence6() {
		SpelExpression expr = new SpelExpressionParser().parseRaw("(3+2)*2");
		assertThat(expr.getValue()).isEqualTo(10);
	}

	@Test
	public void booleanOperators() {
		SpelExpression expr = new SpelExpressionParser().parseRaw("true");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.TRUE);
		expr = new SpelExpressionParser().parseRaw("false");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.FALSE);
		expr = new SpelExpressionParser().parseRaw("false and false");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.FALSE);
		expr = new SpelExpressionParser().parseRaw("true and (true or false)");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.TRUE);
		expr = new SpelExpressionParser().parseRaw("true and true or false");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.TRUE);
		expr = new SpelExpressionParser().parseRaw("!true");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.FALSE);
		expr = new SpelExpressionParser().parseRaw("!(false or true)");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void booleanOperators_symbolic_spr9614() {
		SpelExpression expr = new SpelExpressionParser().parseRaw("true");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.TRUE);
		expr = new SpelExpressionParser().parseRaw("false");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.FALSE);
		expr = new SpelExpressionParser().parseRaw("false && false");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.FALSE);
		expr = new SpelExpressionParser().parseRaw("true && (true || false)");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.TRUE);
		expr = new SpelExpressionParser().parseRaw("true && true || false");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.TRUE);
		expr = new SpelExpressionParser().parseRaw("!true");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.FALSE);
		expr = new SpelExpressionParser().parseRaw("!(false || true)");
		assertThat(expr.getValue(Boolean.class)).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void stringLiterals() {
		SpelExpression expr = new SpelExpressionParser().parseRaw("'howdy'");
		assertThat(expr.getValue()).isEqualTo("howdy");
		expr = new SpelExpressionParser().parseRaw("'hello '' world'");
		assertThat(expr.getValue()).isEqualTo("hello ' world");
	}

	@Test
	public void stringLiterals2() {
		SpelExpression expr = new SpelExpressionParser().parseRaw("'howdy'.substring(0,2)");
		assertThat(expr.getValue()).isEqualTo("ho");
	}

	@Test
	public void testStringLiterals_DoubleQuotes_spr9620() {
		SpelExpression expr = new SpelExpressionParser().parseRaw("\"double quote: \"\".\"");
		assertThat(expr.getValue()).isEqualTo("double quote: \".");
		expr = new SpelExpressionParser().parseRaw("\"hello \"\" world\"");
		assertThat(expr.getValue()).isEqualTo("hello \" world");
	}

	@Test
	public void testStringLiterals_DoubleQuotes_spr9620_2() {
		assertThatExceptionOfType(SpelParseException.class).isThrownBy(() ->
				new SpelExpressionParser().parseRaw("\"double quote: \\\"\\\".\""))
			.satisfies(ex -> {
				assertThat(ex.getPosition()).isEqualTo(17);
				assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.UNEXPECTED_ESCAPE_CHAR);
			});
	}

	@Test
	public void positionalInformation() {
		SpelExpression expr = new SpelExpressionParser().parseRaw("true and true or false");
		SpelNode rootAst = expr.getAST();
		OpOr operatorOr = (OpOr) rootAst;
		OpAnd operatorAnd = (OpAnd) operatorOr.getLeftOperand();
		SpelNode rightOrOperand = operatorOr.getRightOperand();

		// check position for final 'false'
		assertEquals(17, rightOrOperand.getStartPosition());
		assertEquals(22, rightOrOperand.getEndPosition());

		// check position for first 'true'
		assertEquals(0, operatorAnd.getLeftOperand().getStartPosition());
		assertEquals(4, operatorAnd.getLeftOperand().getEndPosition());

		// check position for second 'true'
		assertEquals(9, operatorAnd.getRightOperand().getStartPosition());
		assertEquals(13, operatorAnd.getRightOperand().getEndPosition());

		// check position for OperatorAnd
		assertEquals(5, operatorAnd.getStartPosition());
		assertEquals(8, operatorAnd.getEndPosition());

		// check position for OperatorOr
		assertEquals(14, operatorOr.getStartPosition());
		assertEquals(16, operatorOr.getEndPosition());
	}

	@Test
	public void tokenKind() {
		TokenKind tk = TokenKind.NOT;
		assertThat(tk.hasPayload()).isFalse();
		assertThat((Object) tk.toString()).isEqualTo("NOT(!)");

		tk = TokenKind.MINUS;
		assertThat(tk.hasPayload()).isFalse();
		assertThat((Object) tk.toString()).isEqualTo("MINUS(-)");

		tk = TokenKind.LITERAL_STRING;
		assertThat((Object) tk.toString()).isEqualTo("LITERAL_STRING");
		assertThat(tk.hasPayload()).isTrue();
	}

	@Test
	public void token() {
		Token token = new Token(TokenKind.NOT, 0, 3);
		assertThat((Object) token.kind).isEqualTo(TokenKind.NOT);
		assertEquals(0, token.startPos);
		assertEquals(3, token.endPos);
		assertThat((Object) token.toString()).isEqualTo("[NOT(!)](0,3)");

		token = new Token(TokenKind.LITERAL_STRING, "abc".toCharArray(), 0, 3);
		assertThat((Object) token.kind).isEqualTo(TokenKind.LITERAL_STRING);
		assertEquals(0, token.startPos);
		assertEquals(3, token.endPos);
		assertThat((Object) token.toString()).isEqualTo("[LITERAL_STRING:abc](0,3)");
	}

	@Test
	public void exceptions() {
		ExpressionException exprEx = new ExpressionException("test");
		assertThat((Object) exprEx.getSimpleMessage()).isEqualTo("test");
		assertThat((Object) exprEx.toDetailedString()).isEqualTo("test");
		assertThat((Object) exprEx.getMessage()).isEqualTo("test");

		exprEx = new ExpressionException("wibble", "test");
		assertThat((Object) exprEx.getSimpleMessage()).isEqualTo("test");
		assertThat((Object) exprEx.toDetailedString()).isEqualTo("Expression [wibble]: test");
		assertThat((Object) exprEx.getMessage()).isEqualTo("Expression [wibble]: test");

		exprEx = new ExpressionException("wibble", 3, "test");
		assertThat((Object) exprEx.getSimpleMessage()).isEqualTo("test");
		assertThat((Object) exprEx.toDetailedString()).isEqualTo("Expression [wibble] @3: test");
		assertThat((Object) exprEx.getMessage()).isEqualTo("Expression [wibble] @3: test");
	}

	@Test
	public void parseMethodsOnNumbers() {
		checkNumber("3.14.toString()", "3.14", String.class);
		checkNumber("3.toString()", "3", String.class);
	}

	@Test
	public void numerics() {
		checkNumber("2", 2, Integer.class);
		checkNumber("22", 22, Integer.class);
		checkNumber("+22", 22, Integer.class);
		checkNumber("-22", -22, Integer.class);
		checkNumber("2L", 2L, Long.class);
		checkNumber("22l", 22L, Long.class);

		checkNumber("0x1", 1, Integer.class);
		checkNumber("0x1L", 1L, Long.class);
		checkNumber("0xa", 10, Integer.class);
		checkNumber("0xAL", 10L, Long.class);

		checkNumberError("0x", SpelMessage.NOT_AN_INTEGER);
		checkNumberError("0xL", SpelMessage.NOT_A_LONG);
		checkNumberError(".324", SpelMessage.UNEXPECTED_DATA_AFTER_DOT);
		checkNumberError("3.4L", SpelMessage.REAL_CANNOT_BE_LONG);

		checkNumber("3.5f", 3.5f, Float.class);
		checkNumber("1.2e3", 1.2e3d, Double.class);
		checkNumber("1.2e+3", 1.2e3d, Double.class);
		checkNumber("1.2e-3", 1.2e-3d, Double.class);
		checkNumber("1.2e3", 1.2e3d, Double.class);
		checkNumber("1e+3", 1e3d, Double.class);
	}


	private void checkNumber(String expression, Object value, Class<?> type) {
		try {
			SpelExpressionParser parser = new SpelExpressionParser();
			SpelExpression expr = parser.parseRaw(expression);
			Object exprVal = expr.getValue();
			assertThat(exprVal).isEqualTo(value);
			assertThat((Object) exprVal.getClass()).isEqualTo(type);
		}
		catch (Exception ex) {
			throw new AssertionError(ex.getMessage(), ex);
		}
	}

	private void checkNumberError(String expression, SpelMessage expectedMessage) {
		SpelExpressionParser parser = new SpelExpressionParser();
		assertThatExceptionOfType(SpelParseException.class).isThrownBy(() ->
				parser.parseRaw(expression))
			.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(expectedMessage));
	}

}
