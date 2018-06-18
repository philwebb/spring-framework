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

package org.springframework.expression.spel;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.junit.Test;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.OpLT;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.ast.Ternary;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testdata.PersonInOtherPackage;

import static org.junit.Assert.*;

/**
 * Checks SpelCompiler behavior. This should cover compilation all compiled node types.
 *
 * @author Andy Clement
 * @since 4.1
 */
public class SpelCompilationCoverageTests extends AbstractExpressionTests {

	/*
	 * Further TODOs for compilation:
	 *
	 * - OpMinus with a single literal operand could be treated as a negative literal. Will save a
	 *   pointless loading of 0 and then a subtract instruction in code gen.
	 * - allow other accessors/resolvers to participate in compilation and create their own code
	 * - A TypeReference followed by (what ends up as) a static method invocation can really skip
	 *   code gen for the TypeReference since once that is used to locate the method it is not
	 *   used again.
	 * - The opEq implementation is quite basic. It will compare numbers of the same type (allowing
	 *   them to be their boxed or unboxed variants) or compare object references. It does not
	 *   compile expressions where numbers are of different types or when objects implement
	 *   Comparable.
     *
	 * Compiled nodes:
	 *
	 * TypeReference
	 * OperatorInstanceOf
	 * StringLiteral
	 * NullLiteral
	 * RealLiteral
	 * IntLiteral
	 * LongLiteral
	 * BooleanLiteral
	 * FloatLiteral
	 * OpOr
	 * OpAnd
	 * OperatorNot
	 * Ternary
	 * Elvis
	 * VariableReference
	 * OpLt
	 * OpLe
	 * OpGt
	 * OpGe
	 * OpEq
	 * OpNe
	 * OpPlus
	 * OpMinus
	 * OpMultiply
	 * OpDivide
	 * MethodReference
	 * PropertyOrFieldReference
	 * Indexer
	 * CompoundExpression
	 * ConstructorReference
	 * FunctionReference
	 * InlineList
	 * OpModulus
	 *
	 * Not yet compiled (some may never need to be):
	 * Assign
	 * BeanReference
	 * Identifier
	 * OpDec
	 * OpBetween
	 * OpMatches
	 * OpPower
	 * OpInc
	 * Projection
	 * QualifiedId
	 * Selection
	 */


	private Expression expression;

	private SpelNodeImpl ast;


	@Test
	public void typeReference() throws Exception {
		this.expression = parse("T(String)");
		assertEquals(String.class, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(String.class, this.expression.getValue());

		this.expression = parse("T(java.io.IOException)");
		assertEquals(IOException.class, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(IOException.class, this.expression.getValue());

		this.expression = parse("T(java.io.IOException[])");
		assertEquals(IOException[].class, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(IOException[].class, this.expression.getValue());

		this.expression = parse("T(int[][])");
		assertEquals(int[][].class, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(int[][].class, this.expression.getValue());

		this.expression = parse("T(int)");
		assertEquals(Integer.TYPE, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(Integer.TYPE, this.expression.getValue());

		this.expression = parse("T(byte)");
		assertEquals(Byte.TYPE, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(Byte.TYPE, this.expression.getValue());

		this.expression = parse("T(char)");
		assertEquals(Character.TYPE, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(Character.TYPE, this.expression.getValue());

		this.expression = parse("T(short)");
		assertEquals(Short.TYPE, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(Short.TYPE, this.expression.getValue());

		this.expression = parse("T(long)");
		assertEquals(Long.TYPE, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(Long.TYPE, this.expression.getValue());

		this.expression = parse("T(float)");
		assertEquals(Float.TYPE, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(Float.TYPE, this.expression.getValue());

		this.expression = parse("T(double)");
		assertEquals(Double.TYPE, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(Double.TYPE, this.expression.getValue());

		this.expression = parse("T(boolean)");
		assertEquals(Boolean.TYPE, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(Boolean.TYPE, this.expression.getValue());

		this.expression = parse("T(Missing)");
		assertGetValueFail(this.expression);
		assertCantCompile(this.expression);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void operatorInstanceOf() throws Exception {
		this.expression = parse("'xyz' instanceof T(String)");
		assertEquals(true, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(true, this.expression.getValue());

		this.expression = parse("'xyz' instanceof T(Integer)");
		assertEquals(false, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(false, this.expression.getValue());

		List<String> list = new ArrayList<>();
		this.expression = parse("#root instanceof T(java.util.List)");
		assertEquals(true, this.expression.getValue(list));
		assertCanCompile(this.expression);
		assertEquals(true, this.expression.getValue(list));

		List<String>[] arrayOfLists = new List[] {new ArrayList<String>()};
		this.expression = parse("#root instanceof T(java.util.List[])");
		assertEquals(true, this.expression.getValue(arrayOfLists));
		assertCanCompile(this.expression);
		assertEquals(true, this.expression.getValue(arrayOfLists));

		int[] intArray = new int[] {1,2,3};
		this.expression = parse("#root instanceof T(int[])");
		assertEquals(true, this.expression.getValue(intArray));
		assertCanCompile(this.expression);
		assertEquals(true, this.expression.getValue(intArray));

		String root = null;
		this.expression = parse("#root instanceof T(Integer)");
		assertEquals(false, this.expression.getValue(root));
		assertCanCompile(this.expression);
		assertEquals(false, this.expression.getValue(root));

		// root still null
		this.expression = parse("#root instanceof T(java.lang.Object)");
		assertEquals(false, this.expression.getValue(root));
		assertCanCompile(this.expression);
		assertEquals(false, this.expression.getValue(root));

		root = "howdy!";
		this.expression = parse("#root instanceof T(java.lang.Object)");
		assertEquals(true, this.expression.getValue(root));
		assertCanCompile(this.expression);
		assertEquals(true, this.expression.getValue(root));
	}

	@Test
	public void operatorInstanceOf_SPR14250() throws Exception {
		// primitive left operand - should get boxed, return true
		this.expression = parse("3 instanceof T(Integer)");
		assertEquals(true, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(true, this.expression.getValue());

		// primitive left operand - should get boxed, return false
		this.expression = parse("3 instanceof T(String)");
		assertEquals(false, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(false, this.expression.getValue());

		// double slot left operand - should get boxed, return false
		this.expression = parse("3.0d instanceof T(Integer)");
		assertEquals(false, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(false, this.expression.getValue());

		// double slot left operand - should get boxed, return true
		this.expression = parse("3.0d instanceof T(Double)");
		assertEquals(true, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(true, this.expression.getValue());

		// Only when the right hand operand is a direct type reference
		// will it be compilable.
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("foo", String.class);
		this.expression = parse("3 instanceof #foo");
		assertEquals(false, this.expression.getValue(ctx));
		assertCantCompile(this.expression);

		// use of primitive as type for instanceof check - compilable
		// but always false
		this.expression = parse("3 instanceof T(int)");
		assertEquals(false, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(false, this.expression.getValue());

		this.expression = parse("3 instanceof T(long)");
		assertEquals(false, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(false, this.expression.getValue());
	}

	@Test
	public void stringLiteral() throws Exception {
		this.expression = this.parser.parseExpression("'abcde'");
		assertEquals("abcde", this.expression.getValue(new TestClass1(), String.class));
		assertCanCompile(this.expression);
		String resultC = this.expression.getValue(new TestClass1(), String.class);
		assertEquals("abcde", resultC);
		assertEquals("abcde", this.expression.getValue(String.class));
		assertEquals("abcde", this.expression.getValue());
		assertEquals("abcde", this.expression.getValue(new StandardEvaluationContext()));
		this.expression = this.parser.parseExpression("\"abcde\"");
		assertCanCompile(this.expression);
		assertEquals("abcde", this.expression.getValue(String.class));
	}

	@Test
	public void nullLiteral() throws Exception {
		this.expression = this.parser.parseExpression("null");
		Object resultI = this.expression.getValue(new TestClass1(), Object.class);
		assertCanCompile(this.expression);
		Object resultC = this.expression.getValue(new TestClass1(), Object.class);
		assertEquals(null, resultI);
		assertEquals(null, resultC);
		assertEquals(null, resultC);
	}

	@Test
	public void realLiteral() throws Exception {
		this.expression = this.parser.parseExpression("3.4d");
		double resultI = this.expression.getValue(new TestClass1(), Double.TYPE);
		assertCanCompile(this.expression);
		double resultC = this.expression.getValue(new TestClass1(), Double.TYPE);
		assertEquals(3.4d, resultI, 0.1d);
		assertEquals(3.4d, resultC, 0.1d);
		assertEquals(3.4d, this.expression.getValue());
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void inlineList() throws Exception {
		this.expression = this.parser.parseExpression("'abcde'.substring({1,3,4}[0])");
		Object o = this.expression.getValue();
		assertEquals("bcde",o);
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals("bcde", o);

		this.expression = this.parser.parseExpression("{'abc','def'}");
		List<?> l = (List) this.expression.getValue();
		assertEquals("[abc, def]", l.toString());
		assertCanCompile(this.expression);
		l = (List) this.expression.getValue();
		assertEquals("[abc, def]", l.toString());

		this.expression = this.parser.parseExpression("{'abc','def'}[0]");
		o = this.expression.getValue();
		assertEquals("abc",o);
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals("abc", o);

		this.expression = this.parser.parseExpression("{'abcde','ijklm'}[0].substring({1,3,4}[0])");
		o = this.expression.getValue();
		assertEquals("bcde",o);
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals("bcde", o);

		this.expression = this.parser.parseExpression("{'abcde','ijklm'}[0].substring({1,3,4}[0],{1,3,4}[1])");
		o = this.expression.getValue();
		assertEquals("bc",o);
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals("bc", o);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void nestedInlineLists() throws Exception {
		Object o = null;

		this.expression = this.parser.parseExpression("{{1,2,3},{4,5,6},{7,8,9}}");
		o = this.expression.getValue();
		assertEquals("[[1, 2, 3], [4, 5, 6], [7, 8, 9]]",o.toString());
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals("[[1, 2, 3], [4, 5, 6], [7, 8, 9]]",o.toString());

		this.expression = this.parser.parseExpression("{{1,2,3},{4,5,6},{7,8,9}}.toString()");
		o = this.expression.getValue();
		assertEquals("[[1, 2, 3], [4, 5, 6], [7, 8, 9]]",o);
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals("[[1, 2, 3], [4, 5, 6], [7, 8, 9]]",o);

		this.expression = this.parser.parseExpression("{{1,2,3},{4,5,6},{7,8,9}}[1][0]");
		o = this.expression.getValue();
		assertEquals(4,o);
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals(4,o);

		this.expression = this.parser.parseExpression("{{1,2,3},'abc',{7,8,9}}[1]");
		o = this.expression.getValue();
		assertEquals("abc",o);
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals("abc",o);

		this.expression = this.parser.parseExpression("'abcde'.substring({{1,3},1,3,4}[0][1])");
		o = this.expression.getValue();
		assertEquals("de",o);
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals("de", o);

		this.expression = this.parser.parseExpression("'abcde'.substring({{1,3},1,3,4}[1])");
		o = this.expression.getValue();
		assertEquals("bcde",o);
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals("bcde", o);

		this.expression = this.parser.parseExpression("{'abc',{'def','ghi'}}");
		List<?> l = (List) this.expression.getValue();
		assertEquals("[abc, [def, ghi]]", l.toString());
		assertCanCompile(this.expression);
		l = (List) this.expression.getValue();
		assertEquals("[abc, [def, ghi]]", l.toString());

		this.expression = this.parser.parseExpression("{'abcde',{'ijklm','nopqr'}}[0].substring({1,3,4}[0])");
		o = this.expression.getValue();
		assertEquals("bcde",o);
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals("bcde", o);

		this.expression = this.parser.parseExpression("{'abcde',{'ijklm','nopqr'}}[1][0].substring({1,3,4}[0])");
		o = this.expression.getValue();
		assertEquals("jklm",o);
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals("jklm", o);

		this.expression = this.parser.parseExpression("{'abcde',{'ijklm','nopqr'}}[1][1].substring({1,3,4}[0],{1,3,4}[1])");
		o = this.expression.getValue();
		assertEquals("op",o);
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals("op", o);
	}

	@Test
	public void intLiteral() throws Exception {
		this.expression = this.parser.parseExpression("42");
		int resultI = this.expression.getValue(new TestClass1(), Integer.TYPE);
		assertCanCompile(this.expression);
		int resultC = this.expression.getValue(new TestClass1(), Integer.TYPE);
		assertEquals(42, resultI);
		assertEquals(42, resultC);

		this.expression = this.parser.parseExpression("T(Integer).valueOf(42)");
		this.expression.getValue(Integer.class);
		assertCanCompile(this.expression);
		assertEquals(new Integer(42), this.expression.getValue(Integer.class));

		// Code gen is different for -1 .. 6 because there are bytecode instructions specifically for those values

		// Not an int literal but an opminus with one operand:
		// expression = parser.parseExpression("-1");
		// assertCanCompile(expression);
		// assertEquals(-1, expression.getValue());
		this.expression = this.parser.parseExpression("0");
		assertCanCompile(this.expression);
		assertEquals(0, this.expression.getValue());
		this.expression = this.parser.parseExpression("2");
		assertCanCompile(this.expression);
		assertEquals(2, this.expression.getValue());
		this.expression = this.parser.parseExpression("7");
		assertCanCompile(this.expression);
		assertEquals(7, this.expression.getValue());
	}

	@Test
	public void longLiteral() throws Exception {
		this.expression = this.parser.parseExpression("99L");
		long resultI = this.expression.getValue(new TestClass1(), Long.TYPE);
		assertCanCompile(this.expression);
		long resultC = this.expression.getValue(new TestClass1(), Long.TYPE);
		assertEquals(99L, resultI);
		assertEquals(99L, resultC);
	}

	@Test
	public void booleanLiteral() throws Exception {
		this.expression = this.parser.parseExpression("true");
		boolean resultI = this.expression.getValue(1, Boolean.TYPE);
		assertEquals(true, resultI);
		assertTrue(SpelCompiler.compile(this.expression));
		boolean resultC = this.expression.getValue(1, Boolean.TYPE);
		assertEquals(true, resultC);

		this.expression = this.parser.parseExpression("false");
		resultI = this.expression.getValue(1, Boolean.TYPE);
		assertEquals(false, resultI);
		assertTrue(SpelCompiler.compile(this.expression));
		resultC = this.expression.getValue(1, Boolean.TYPE);
		assertEquals(false, resultC);
	}

	@Test
	public void floatLiteral() throws Exception {
		this.expression = this.parser.parseExpression("3.4f");
		float resultI = this.expression.getValue(new TestClass1(), Float.TYPE);
		assertCanCompile(this.expression);
		float resultC = this.expression.getValue(new TestClass1(), Float.TYPE);
		assertEquals(3.4f, resultI, 0.1f);
		assertEquals(3.4f, resultC, 0.1f);

		assertEquals(3.4f, this.expression.getValue());
	}

	@Test
	public void opOr() throws Exception {
		Expression expression = this.parser.parseExpression("false or false");
		boolean resultI = expression.getValue(1, Boolean.TYPE);
		SpelCompiler.compile(expression);
		boolean resultC = expression.getValue(1, Boolean.TYPE);
		assertEquals(false, resultI);
		assertEquals(false, resultC);

		expression = this.parser.parseExpression("false or true");
		resultI = expression.getValue(1, Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(1, Boolean.TYPE);
		assertEquals(true, resultI);
		assertEquals(true, resultC);

		expression = this.parser.parseExpression("true or false");
		resultI = expression.getValue(1, Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(1, Boolean.TYPE);
		assertEquals(true, resultI);
		assertEquals(true, resultC);

		expression = this.parser.parseExpression("true or true");
		resultI = expression.getValue(1, Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(1, Boolean.TYPE);
		assertEquals(true, resultI);
		assertEquals(true, resultC);

		TestClass4 tc = new TestClass4();
		expression = this.parser.parseExpression("getfalse() or gettrue()");
		resultI = expression.getValue(tc, Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(tc, Boolean.TYPE);
		assertEquals(true, resultI);
		assertEquals(true, resultC);

		// Can't compile this as we aren't going down the getfalse() branch in our evaluation
		expression = this.parser.parseExpression("gettrue() or getfalse()");
		resultI = expression.getValue(tc, Boolean.TYPE);
		assertCantCompile(expression);

		expression = this.parser.parseExpression("getA() or getB()");
		tc.a = true;
		tc.b = true;
		resultI = expression.getValue(tc, Boolean.TYPE);
		assertCantCompile(expression); // Haven't yet been into second branch
		tc.a = false;
		tc.b = true;
		resultI = expression.getValue(tc, Boolean.TYPE);
		assertCanCompile(expression); // Now been down both
		assertTrue(resultI);

		boolean b = false;
		expression = parse("#root or #root");
		Object resultI2 = expression.getValue(b);
		assertCanCompile(expression);
		assertFalse((Boolean) resultI2);
		assertFalse((Boolean) expression.getValue(b));
	}

	@Test
	public void opAnd() throws Exception {
		Expression expression = this.parser.parseExpression("false and false");
		boolean resultI = expression.getValue(1, Boolean.TYPE);
		SpelCompiler.compile(expression);
		boolean resultC = expression.getValue(1, Boolean.TYPE);
		assertEquals(false, resultI);
		assertEquals(false, resultC);

		expression = this.parser.parseExpression("false and true");
		resultI = expression.getValue(1, Boolean.TYPE);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1, Boolean.TYPE);
		assertEquals(false, resultI);
		assertEquals(false, resultC);

		expression = this.parser.parseExpression("true and false");
		resultI = expression.getValue(1, Boolean.TYPE);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1, Boolean.TYPE);
		assertEquals(false, resultI);
		assertEquals(false, resultC);

		expression = this.parser.parseExpression("true and true");
		resultI = expression.getValue(1, Boolean.TYPE);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1, Boolean.TYPE);
		assertEquals(true, resultI);
		assertEquals(true, resultC);

		TestClass4 tc = new TestClass4();

		// Can't compile this as we aren't going down the gettrue() branch in our evaluation
		expression = this.parser.parseExpression("getfalse() and gettrue()");
		resultI = expression.getValue(tc, Boolean.TYPE);
		assertCantCompile(expression);

		expression = this.parser.parseExpression("getA() and getB()");
		tc.a = false;
		tc.b = false;
		resultI = expression.getValue(tc, Boolean.TYPE);
		assertCantCompile(expression); // Haven't yet been into second branch
		tc.a = true;
		tc.b = false;
		resultI = expression.getValue(tc, Boolean.TYPE);
		assertCanCompile(expression); // Now been down both
		assertFalse(resultI);
		tc.a = true;
		tc.b = true;
		resultI = expression.getValue(tc, Boolean.TYPE);
		assertTrue(resultI);

		boolean b = true;
		expression = parse("#root and #root");
		Object resultI2 = expression.getValue(b);
		assertCanCompile(expression);
		assertTrue((Boolean) resultI2);
		assertTrue((Boolean) expression.getValue(b));
	}

	@Test
	public void operatorNot() throws Exception {
		this.expression = parse("!true");
		assertEquals(false, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(false, this.expression.getValue());

		this.expression = parse("!false");
		assertEquals(true, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(true, this.expression.getValue());

		boolean b = true;
		this.expression = parse("!#root");
		assertEquals(false, this.expression.getValue(b));
		assertCanCompile(this.expression);
		assertEquals(false, this.expression.getValue(b));

		b = false;
		this.expression = parse("!#root");
		assertEquals(true, this.expression.getValue(b));
		assertCanCompile(this.expression);
		assertEquals(true, this.expression.getValue(b));
	}

	@Test
	public void ternary() throws Exception {
		Expression expression = this.parser.parseExpression("true?'a':'b'");
		String resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(String.class);
		assertEquals("a", resultI);
		assertEquals("a", resultC);

		expression = this.parser.parseExpression("false?'a':'b'");
		resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		resultC = expression.getValue(String.class);
		assertEquals("b", resultI);
		assertEquals("b", resultC);

		expression = this.parser.parseExpression("false?1:'b'");
		// All literals so we can do this straight away
		assertCanCompile(expression);
		assertEquals("b", expression.getValue());

		boolean root = true;
		expression = this.parser.parseExpression("(#root and true)?T(Integer).valueOf(1):T(Long).valueOf(3L)");
		assertEquals(1, expression.getValue(root));
		assertCantCompile(expression); // Have not gone down false branch
		root = false;
		assertEquals(3L, expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(3L, expression.getValue(root));
		root = true;
		assertEquals(1, expression.getValue(root));
	}

	@Test
	public void ternaryWithBooleanReturn() { // SPR-12271
		this.expression = this.parser.parseExpression("T(Boolean).TRUE?'abc':'def'");
		assertEquals("abc", this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals("abc", this.expression.getValue());

		this.expression = this.parser.parseExpression("T(Boolean).FALSE?'abc':'def'");
		assertEquals("def", this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals("def", this.expression.getValue());
	}

	@Test
	public void nullsafeFieldPropertyDereferencing_SPR16489() throws Exception {
		FooObjectHolder foh = new FooObjectHolder();
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setRootObject(foh);

		// First non compiled:
		SpelExpression expression = (SpelExpression) this.parser.parseExpression("foo?.object");
		assertEquals("hello",expression.getValue(context));
		foh.foo = null;
		assertNull(expression.getValue(context));

		// Now revert state of foh and try compiling it:
		foh.foo = new FooObject();
		assertEquals("hello",expression.getValue(context));
		assertCanCompile(expression);
		assertEquals("hello",expression.getValue(context));
		foh.foo = null;
		assertNull(expression.getValue(context));

		// Static references
		expression = (SpelExpression)this.parser.parseExpression("#var?.propertya");
		context.setVariable("var", StaticsHelper.class);
		assertEquals("sh",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));
		assertCanCompile(expression);
		context.setVariable("var", StaticsHelper.class);
		assertEquals("sh",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));

		// Single size primitive (boolean)
		expression = (SpelExpression)this.parser.parseExpression("#var?.a");
		context.setVariable("var", new TestClass4());
		assertFalse((Boolean)expression.getValue(context));
		context.setVariable("var", null);
		assertNull(expression.getValue(context));
		assertCanCompile(expression);
		context.setVariable("var", new TestClass4());
		assertFalse((Boolean)expression.getValue(context));
		context.setVariable("var", null);
		assertNull(expression.getValue(context));

		// Double slot primitives
		expression = (SpelExpression)this.parser.parseExpression("#var?.four");
		context.setVariable("var", new Three());
		assertEquals("0.04",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));
		assertCanCompile(expression);
		context.setVariable("var", new Three());
		assertEquals("0.04",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));
	}

	@Test
	public void nullsafeMethodChaining_SPR16489() throws Exception {
		FooObjectHolder foh = new FooObjectHolder();
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setRootObject(foh);

		// First non compiled:
		SpelExpression expression = (SpelExpression) this.parser.parseExpression("getFoo()?.getObject()");
		assertEquals("hello",expression.getValue(context));
		foh.foo = null;
		assertNull(expression.getValue(context));
		assertCanCompile(expression);
		foh.foo = new FooObject();
		assertEquals("hello",expression.getValue(context));
		foh.foo = null;
		assertNull(expression.getValue(context));

		// Static method references
		expression = (SpelExpression)this.parser.parseExpression("#var?.methoda()");
		context.setVariable("var", StaticsHelper.class);
		assertEquals("sh",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));
		assertCanCompile(expression);
		context.setVariable("var", StaticsHelper.class);
		assertEquals("sh",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));

		// Nullsafe guard on expression element evaluating to primitive/null
		expression = (SpelExpression)this.parser.parseExpression("#var?.intValue()");
		context.setVariable("var", 4);
		assertEquals("4",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));
		assertCanCompile(expression);
		context.setVariable("var", 4);
		assertEquals("4",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));


		// Nullsafe guard on expression element evaluating to primitive/null
		expression = (SpelExpression)this.parser.parseExpression("#var?.booleanValue()");
		context.setVariable("var", false);
		assertEquals("false",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));
		assertCanCompile(expression);
		context.setVariable("var", false);
		assertEquals("false",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));

		// Nullsafe guard on expression element evaluating to primitive/null
		expression = (SpelExpression)this.parser.parseExpression("#var?.booleanValue()");
		context.setVariable("var", true);
		assertEquals("true",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));
		assertCanCompile(expression);
		context.setVariable("var", true);
		assertEquals("true",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));

		// Nullsafe guard on expression element evaluating to primitive/null
		expression = (SpelExpression)this.parser.parseExpression("#var?.longValue()");
		context.setVariable("var", 5L);
		assertEquals("5",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));
		assertCanCompile(expression);
		context.setVariable("var", 5L);
		assertEquals("5",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));

		// Nullsafe guard on expression element evaluating to primitive/null
		expression = (SpelExpression)this.parser.parseExpression("#var?.floatValue()");
		context.setVariable("var", 3f);
		assertEquals("3.0",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));
		assertCanCompile(expression);
		context.setVariable("var", 3f);
		assertEquals("3.0",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));

		// Nullsafe guard on expression element evaluating to primitive/null
		expression = (SpelExpression)this.parser.parseExpression("#var?.shortValue()");
		context.setVariable("var", (short)8);
		assertEquals("8",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));
		assertCanCompile(expression);
		context.setVariable("var", (short)8);
		assertEquals("8",expression.getValue(context).toString());
		context.setVariable("var", null);
		assertNull(expression.getValue(context));
	}

	@Test
	public void elvis() throws Exception {
		Expression expression = this.parser.parseExpression("'a'?:'b'");
		String resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(String.class);
		assertEquals("a", resultI);
		assertEquals("a", resultC);

		expression = this.parser.parseExpression("null?:'a'");
		resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		resultC = expression.getValue(String.class);
		assertEquals("a", resultI);
		assertEquals("a", resultC);

		String s = "abc";
		expression = this.parser.parseExpression("#root?:'b'");
		assertCantCompile(expression);
		resultI = expression.getValue(s, String.class);
		assertEquals("abc", resultI);
		assertCanCompile(expression);
	}

	@Test
	public void variableReference_root() throws Exception {
		String s = "hello";
		Expression expression = this.parser.parseExpression("#root");
		String resultI = expression.getValue(s, String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(s, String.class);
		assertEquals(s, resultI);
		assertEquals(s, resultC);

		expression = this.parser.parseExpression("#root");
		int i = (Integer) expression.getValue(42);
		assertEquals(42,i);
		assertCanCompile(expression);
		i = (Integer) expression.getValue(42);
		assertEquals(42,i);
	}

	public static String concat(String a, String b) {
		return a+b;
	}

	public static String join(String...strings) {
		StringBuilder buf = new StringBuilder();
		for (String string: strings) {
			buf.append(string);
		}
		return buf.toString();
	}

	@Test
	public void compiledExpressionShouldWorkWhenUsingCustomFunctionWithVarargs() throws Exception {
		StandardEvaluationContext context = null;

		// Here the target method takes Object... and we are passing a string
		this.expression = this.parser.parseExpression("#doFormat('hey %s', 'there')");
		context = new StandardEvaluationContext();
		context.registerFunction("doFormat",
				DelegatingStringFormat.class.getDeclaredMethod("format", String.class, Object[].class));
		((SpelExpression) this.expression).setEvaluationContext(context);

		assertEquals("hey there", this.expression.getValue(String.class));
		assertTrue(((SpelNodeImpl) ((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("hey there", this.expression.getValue(String.class));

		this.expression = this.parser.parseExpression("#doFormat([0], 'there')");
		context = new StandardEvaluationContext(new Object[] {"hey %s"});
		context.registerFunction("doFormat",
				DelegatingStringFormat.class.getDeclaredMethod("format", String.class, Object[].class));
		((SpelExpression) this.expression).setEvaluationContext(context);

		assertEquals("hey there", this.expression.getValue(String.class));
		assertTrue(((SpelNodeImpl) ((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("hey there", this.expression.getValue(String.class));

		this.expression = this.parser.parseExpression("#doFormat([0], #arg)");
		context = new StandardEvaluationContext(new Object[] {"hey %s"});
		context.registerFunction("doFormat",
				DelegatingStringFormat.class.getDeclaredMethod("format", String.class, Object[].class));
		context.setVariable("arg", "there");
		((SpelExpression) this.expression).setEvaluationContext(context);

		assertEquals("hey there", this.expression.getValue(String.class));
		assertTrue(((SpelNodeImpl) ((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("hey there", this.expression.getValue(String.class));
	}

	@Test
	public void functionReference() throws Exception {
		EvaluationContext ctx = new StandardEvaluationContext();
		Method m = getClass().getDeclaredMethod("concat", String.class, String.class);
		ctx.setVariable("concat",m);

		this.expression = this.parser.parseExpression("#concat('a','b')");
		assertEquals("ab", this.expression.getValue(ctx));
		assertCanCompile(this.expression);
		assertEquals("ab", this.expression.getValue(ctx));

		this.expression = this.parser.parseExpression("#concat(#concat('a','b'),'c').charAt(1)");
		assertEquals('b', this.expression.getValue(ctx));
		assertCanCompile(this.expression);
		assertEquals('b', this.expression.getValue(ctx));

		this.expression = this.parser.parseExpression("#concat(#a,#b)");
		ctx.setVariable("a", "foo");
		ctx.setVariable("b", "bar");
		assertEquals("foobar", this.expression.getValue(ctx));
		assertCanCompile(this.expression);
		assertEquals("foobar", this.expression.getValue(ctx));
		ctx.setVariable("b", "boo");
		assertEquals("fooboo", this.expression.getValue(ctx));

		m = Math.class.getDeclaredMethod("pow", Double.TYPE, Double.TYPE);
		ctx.setVariable("kapow",m);
		this.expression = this.parser.parseExpression("#kapow(2.0d,2.0d)");
		assertEquals("4.0", this.expression.getValue(ctx).toString());
		assertCanCompile(this.expression);
		assertEquals("4.0", this.expression.getValue(ctx).toString());
	}

	@Test
	public void functionReferenceVisibility_SPR12359() throws Exception {
		// Confirms visibility of what is being called.
		StandardEvaluationContext context = new StandardEvaluationContext(new Object[] {"1"});
		context.registerFunction("doCompare", SomeCompareMethod.class.getDeclaredMethod(
				"compare", Object.class, Object.class));
		context.setVariable("arg", "2");
		// type nor method are public
		this.expression = this.parser.parseExpression("#doCompare([0],#arg)");
		assertEquals("-1", this.expression.getValue(context, Integer.class).toString());
		assertCantCompile(this.expression);

		// type not public but method is
		context = new StandardEvaluationContext(new Object[] {"1"});
		context.registerFunction("doCompare", SomeCompareMethod.class.getDeclaredMethod(
				"compare2", Object.class, Object.class));
		context.setVariable("arg", "2");
		this.expression = this.parser.parseExpression("#doCompare([0],#arg)");
		assertEquals("-1", this.expression.getValue(context, Integer.class).toString());
		assertCantCompile(this.expression);
	}

	@Test
	public void functionReferenceNonCompilableArguments_SPR12359() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext(new Object[] {"1"});
		context.registerFunction("negate", SomeCompareMethod2.class.getDeclaredMethod(
				"negate", Integer.TYPE));
		context.setVariable("arg", "2");
		int[] ints = new int[] {1,2,3};
		context.setVariable("ints",ints);

		this.expression = this.parser.parseExpression("#negate(#ints.?[#this<2][0])");
		assertEquals("-1", this.expression.getValue(context, Integer.class).toString());
		// Selection isn't compilable.
		assertFalse(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
	}

	@Test
	public void functionReferenceVarargs_SPR12359() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.registerFunction("append",
				SomeCompareMethod2.class.getDeclaredMethod("append", String[].class));
		context.registerFunction("append2",
				SomeCompareMethod2.class.getDeclaredMethod("append2", Object[].class));
		context.registerFunction("append3",
				SomeCompareMethod2.class.getDeclaredMethod("append3", String[].class));
		context.registerFunction("append4",
				SomeCompareMethod2.class.getDeclaredMethod("append4", String.class, String[].class));
		context.registerFunction("appendChar",
				SomeCompareMethod2.class.getDeclaredMethod("appendChar", char[].class));
		context.registerFunction("sum",
				SomeCompareMethod2.class.getDeclaredMethod("sum", int[].class));
		context.registerFunction("sumDouble",
				SomeCompareMethod2.class.getDeclaredMethod("sumDouble", double[].class));
		context.registerFunction("sumFloat",
				SomeCompareMethod2.class.getDeclaredMethod("sumFloat", float[].class));
		context.setVariable("stringArray", new String[] {"x","y","z"});
		context.setVariable("intArray", new int[] {5,6,9});
		context.setVariable("doubleArray", new double[] {5.0d,6.0d,9.0d});
		context.setVariable("floatArray", new float[] {5.0f,6.0f,9.0f});

		this.expression = this.parser.parseExpression("#append('a','b','c')");
		assertEquals("abc", this.expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("abc", this.expression.getValue(context).toString());

		this.expression = this.parser.parseExpression("#append('a')");
		assertEquals("a", this.expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("a", this.expression.getValue(context).toString());

		this.expression = this.parser.parseExpression("#append()");
		assertEquals("", this.expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("", this.expression.getValue(context).toString());

		this.expression = this.parser.parseExpression("#append(#stringArray)");
		assertEquals("xyz", this.expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("xyz", this.expression.getValue(context).toString());

		// This is a methodreference invocation, to compare with functionreference
		this.expression = this.parser.parseExpression("append(#stringArray)");
		assertEquals("xyz", this.expression.getValue(context,new SomeCompareMethod2()).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("xyz", this.expression.getValue(context,new SomeCompareMethod2()).toString());

		this.expression = this.parser.parseExpression("#append2('a','b','c')");
		assertEquals("abc", this.expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("abc", this.expression.getValue(context).toString());

		this.expression = this.parser.parseExpression("append2('a','b')");
		assertEquals("ab", this.expression.getValue(context, new SomeCompareMethod2()).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("ab", this.expression.getValue(context, new SomeCompareMethod2()).toString());

		this.expression = this.parser.parseExpression("#append2('a','b')");
		assertEquals("ab", this.expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("ab", this.expression.getValue(context).toString());

		this.expression = this.parser.parseExpression("#append2()");
		assertEquals("", this.expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("", this.expression.getValue(context).toString());

		this.expression = this.parser.parseExpression("#append3(#stringArray)");
		assertEquals("xyz", this.expression.getValue(context, new SomeCompareMethod2()).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("xyz", this.expression.getValue(context, new SomeCompareMethod2()).toString());

		// TODO fails due to conversionservice handling of String[] to Object...
		//	expression = parser.parseExpression("#append2(#stringArray)");
		//	assertEquals("xyz", expression.getValue(context).toString());
		//	assertTrue(((SpelNodeImpl)((SpelExpression) expression).getAST()).isCompilable());
		//	assertCanCompile(expression);
		//	assertEquals("xyz", expression.getValue(context).toString());

		this.expression = this.parser.parseExpression("#sum(1,2,3)");
		assertEquals(6, this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals(6, this.expression.getValue(context));

		this.expression = this.parser.parseExpression("#sum(2)");
		assertEquals(2, this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals(2, this.expression.getValue(context));

		this.expression = this.parser.parseExpression("#sum()");
		assertEquals(0, this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals(0, this.expression.getValue(context));

		this.expression = this.parser.parseExpression("#sum(#intArray)");
		assertEquals(20, this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals(20, this.expression.getValue(context));

		this.expression = this.parser.parseExpression("#sumDouble(1.0d,2.0d,3.0d)");
		assertEquals(6, this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals(6, this.expression.getValue(context));

		this.expression = this.parser.parseExpression("#sumDouble(2.0d)");
		assertEquals(2, this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals(2, this.expression.getValue(context));

		this.expression = this.parser.parseExpression("#sumDouble()");
		assertEquals(0, this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals(0, this.expression.getValue(context));

		this.expression = this.parser.parseExpression("#sumDouble(#doubleArray)");
		assertEquals(20, this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals(20, this.expression.getValue(context));

		this.expression = this.parser.parseExpression("#sumFloat(1.0f,2.0f,3.0f)");
		assertEquals(6, this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals(6, this.expression.getValue(context));

		this.expression = this.parser.parseExpression("#sumFloat(2.0f)");
		assertEquals(2, this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals(2, this.expression.getValue(context));

		this.expression = this.parser.parseExpression("#sumFloat()");
		assertEquals(0, this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals(0, this.expression.getValue(context));

		this.expression = this.parser.parseExpression("#sumFloat(#floatArray)");
		assertEquals(20, this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals(20, this.expression.getValue(context));


		this.expression = this.parser.parseExpression("#appendChar('abc'.charAt(0),'abc'.charAt(1))");
		assertEquals("ab", this.expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("ab", this.expression.getValue(context));


		this.expression = this.parser.parseExpression("#append4('a','b','c')");
		assertEquals("a::bc", this.expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("a::bc", this.expression.getValue(context).toString());

		this.expression = this.parser.parseExpression("#append4('a','b')");
		assertEquals("a::b", this.expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("a::b", this.expression.getValue(context).toString());

		this.expression = this.parser.parseExpression("#append4('a')");
		assertEquals("a::", this.expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("a::", this.expression.getValue(context).toString());

		this.expression = this.parser.parseExpression("#append4('a',#stringArray)");
		assertEquals("a::xyz", this.expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression) this.expression).getAST()).isCompilable());
		assertCanCompile(this.expression);
		assertEquals("a::xyz", this.expression.getValue(context).toString());
	}

	@Test
	public void functionReferenceVarargs() throws Exception {
		EvaluationContext ctx = new StandardEvaluationContext();
		Method m = getClass().getDeclaredMethod("join", String[].class);
		ctx.setVariable("join", m);
		this.expression = this.parser.parseExpression("#join('a','b','c')");
		assertEquals("abc", this.expression.getValue(ctx));
		assertCanCompile(this.expression);
		assertEquals("abc", this.expression.getValue(ctx));
	}

	@Test
	public void variableReference_userDefined() throws Exception {
		EvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("target", "abc");
		this.expression = this.parser.parseExpression("#target");
		assertEquals("abc", this.expression.getValue(ctx));
		assertCanCompile(this.expression);
		assertEquals("abc", this.expression.getValue(ctx));
		ctx.setVariable("target", "123");
		assertEquals("123", this.expression.getValue(ctx));
		ctx.setVariable("target", 42);
		try {
			assertEquals(42, this.expression.getValue(ctx));
			fail();
		}
		catch (SpelEvaluationException see) {
			assertTrue(see.getCause() instanceof ClassCastException);
		}

		ctx.setVariable("target", "abc");
		this.expression = this.parser.parseExpression("#target.charAt(0)");
		assertEquals('a', this.expression.getValue(ctx));
		assertCanCompile(this.expression);
		assertEquals('a', this.expression.getValue(ctx));
		ctx.setVariable("target", "1");
		assertEquals('1', this.expression.getValue(ctx));
		ctx.setVariable("target", 42);
		try {
			assertEquals('4', this.expression.getValue(ctx));
			fail();
		}
		catch (SpelEvaluationException see) {
			assertTrue(see.getCause() instanceof ClassCastException);
		}
	}

	@Test
	public void opLt() throws Exception {
		this.expression = parse("3.0d < 4.0d");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("3446.0d < 1123.0d");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("3 < 1");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("2 < 4");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("3.0f < 1.0f");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("1.0f < 5.0f");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("30L < 30L");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("15L < 20L");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		// Differing types of number, not yet supported
		this.expression = parse("1 < 3.0d");
		assertCantCompile(this.expression);

		this.expression = parse("T(Integer).valueOf(3) < 4");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("T(Integer).valueOf(3) < T(Integer).valueOf(3)");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("5 < T(Integer).valueOf(3)");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
	}

	@Test
	public void opLe() throws Exception {
		this.expression = parse("3.0d <= 4.0d");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("3446.0d <= 1123.0d");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("3446.0d <= 3446.0d");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("3 <= 1");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("2 <= 4");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("3 <= 3");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("3.0f <= 1.0f");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("1.0f <= 5.0f");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("2.0f <= 2.0f");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("30L <= 30L");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("15L <= 20L");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		// Differing types of number, not yet supported
		this.expression = parse("1 <= 3.0d");
		assertCantCompile(this.expression);

		this.expression = parse("T(Integer).valueOf(3) <= 4");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("T(Integer).valueOf(3) <= T(Integer).valueOf(3)");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("5 <= T(Integer).valueOf(3)");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
	}

	@Test
	public void opGt() throws Exception {
		this.expression = parse("3.0d > 4.0d");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("3446.0d > 1123.0d");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("3 > 1");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("2 > 4");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("3.0f > 1.0f");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("1.0f > 5.0f");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("30L > 30L");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("15L > 20L");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		// Differing types of number, not yet supported
		this.expression = parse("1 > 3.0d");
		assertCantCompile(this.expression);

		this.expression = parse("T(Integer).valueOf(3) > 4");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("T(Integer).valueOf(3) > T(Integer).valueOf(3)");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("5 > T(Integer).valueOf(3)");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
	}

	@Test
	public void opGe() throws Exception {
		this.expression = parse("3.0d >= 4.0d");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("3446.0d >= 1123.0d");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("3446.0d >= 3446.0d");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("3 >= 1");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("2 >= 4");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("3 >= 3");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("3.0f >= 1.0f");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("1.0f >= 5.0f");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("3.0f >= 3.0f");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("40L >= 30L");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("15L >= 20L");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("30L >= 30L");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		// Differing types of number, not yet supported
		this.expression = parse("1 >= 3.0d");
		assertCantCompile(this.expression);

		this.expression = parse("T(Integer).valueOf(3) >= 4");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("T(Integer).valueOf(3) >= T(Integer).valueOf(3)");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("5 >= T(Integer).valueOf(3)");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
	}

	@Test
	public void opEq() throws Exception {
		String tvar = "35";
		this.expression = parse("#root == 35");
		assertFalse((Boolean) this.expression.getValue(tvar));
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue(tvar));

		this.expression = parse("35 == #root");
		this.expression.getValue(tvar);
		assertFalse((Boolean) this.expression.getValue(tvar));
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue(tvar));

		TestClass7 tc7 = new TestClass7();
		this.expression = parse("property == 'UK'");
		assertTrue((Boolean) this.expression.getValue(tc7));
		TestClass7.property = null;
		assertFalse((Boolean) this.expression.getValue(tc7));
		assertCanCompile(this.expression);
		TestClass7.reset();
		assertTrue((Boolean) this.expression.getValue(tc7));
		TestClass7.property = "UK";
		assertTrue((Boolean) this.expression.getValue(tc7));
		TestClass7.reset();
		TestClass7.property = null;
		assertFalse((Boolean) this.expression.getValue(tc7));
		this.expression = parse("property == null");
		assertTrue((Boolean) this.expression.getValue(tc7));
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue(tc7));

		this.expression = parse("3.0d == 4.0d");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("3446.0d == 3446.0d");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("3 == 1");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("3 == 3");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("3.0f == 1.0f");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("2.0f == 2.0f");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("30L == 30L");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("15L == 20L");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		// number types are not the same
		this.expression = parse("1 == 3.0d");
		assertCantCompile(this.expression);

		Double d = 3.0d;
		this.expression = parse("#root==3.0d");
		assertTrue((Boolean) this.expression.getValue(d));
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue(d));

		Integer i = 3;
		this.expression = parse("#root==3");
		assertTrue((Boolean) this.expression.getValue(i));
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue(i));

		Float f = 3.0f;
		this.expression = parse("#root==3.0f");
		assertTrue((Boolean) this.expression.getValue(f));
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue(f));

		long l = 300L;
		this.expression = parse("#root==300l");
		assertTrue((Boolean) this.expression.getValue(l));
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue(l));

		boolean b = true;
		this.expression = parse("#root==true");
		assertTrue((Boolean) this.expression.getValue(b));
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue(b));

		this.expression = parse("T(Integer).valueOf(3) == 4");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("T(Integer).valueOf(3) == T(Integer).valueOf(3)");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("5 == T(Integer).valueOf(3)");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("T(Float).valueOf(3.0f) == 4.0f");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("T(Float).valueOf(3.0f) == T(Float).valueOf(3.0f)");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("5.0f == T(Float).valueOf(3.0f)");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("T(Long).valueOf(3L) == 4L");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("T(Long).valueOf(3L) == T(Long).valueOf(3L)");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("5L == T(Long).valueOf(3L)");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("T(Double).valueOf(3.0d) == 4.0d");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("T(Double).valueOf(3.0d) == T(Double).valueOf(3.0d)");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("5.0d == T(Double).valueOf(3.0d)");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("false == true");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("T(Boolean).valueOf('true') == T(Boolean).valueOf('true')");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("T(Boolean).valueOf('true') == true");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("false == T(Boolean).valueOf('false')");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
	}

	@Test
	public void opNe() throws Exception {
		this.expression = parse("3.0d != 4.0d");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("3446.0d != 3446.0d");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("3 != 1");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("3 != 3");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("3.0f != 1.0f");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
		this.expression = parse("2.0f != 2.0f");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("30L != 30L");
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());
		this.expression = parse("15L != 20L");
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		// not compatible number types
		this.expression = parse("1 != 3.0d");
		assertCantCompile(this.expression);

		this.expression = parse("T(Integer).valueOf(3) != 4");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("T(Integer).valueOf(3) != T(Integer).valueOf(3)");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("5 != T(Integer).valueOf(3)");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("T(Float).valueOf(3.0f) != 4.0f");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("T(Float).valueOf(3.0f) != T(Float).valueOf(3.0f)");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("5.0f != T(Float).valueOf(3.0f)");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("T(Long).valueOf(3L) != 4L");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("T(Long).valueOf(3L) != T(Long).valueOf(3L)");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("5L != T(Long).valueOf(3L)");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("T(Double).valueOf(3.0d) == 4.0d");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("T(Double).valueOf(3.0d) == T(Double).valueOf(3.0d)");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("5.0d == T(Double).valueOf(3.0d)");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("false == true");
		assertFalse((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertFalse((Boolean) this.expression.getValue());

		this.expression = parse("T(Boolean).valueOf('true') == T(Boolean).valueOf('true')");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("T(Boolean).valueOf('true') == true");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());

		this.expression = parse("false == T(Boolean).valueOf('false')");
		assertTrue((Boolean) this.expression.getValue());
		assertCanCompile(this.expression);
		assertTrue((Boolean) this.expression.getValue());
	}

	@Test
	public void opNe_SPR14863() throws Exception {
		SpelParserConfiguration configuration =
				new SpelParserConfiguration(SpelCompilerMode.MIXED, ClassLoader.getSystemClassLoader());
		SpelExpressionParser parser = new SpelExpressionParser(configuration);
		Expression expression = parser.parseExpression("data['my-key'] != 'my-value'");

		Map<String, String> data = new HashMap<>();
		data.put("my-key", new String("my-value"));
		StandardEvaluationContext context = new StandardEvaluationContext(new MyContext(data));
		assertFalse(expression.getValue(context, Boolean.class));
		assertCanCompile(expression);
		((SpelExpression) expression).compileExpression();
		assertFalse(expression.getValue(context, Boolean.class));

		List<String> ls = new ArrayList<String>();
		ls.add(new String("foo"));
		context = new StandardEvaluationContext(ls);
		expression = parse("get(0) != 'foo'");
		assertFalse(expression.getValue(context, Boolean.class));
		assertCanCompile(expression);
		assertFalse(expression.getValue(context, Boolean.class));

		ls.remove(0);
		ls.add("goo");
		assertTrue(expression.getValue(context, Boolean.class));
	}

	@Test
	public void opEq_SPR14863() throws Exception {
		// Exercise the comparator invocation code that runs in
		// equalityCheck() (called from interpreted and compiled code)
		this.expression = this.parser.parseExpression("#aa==#bb");
		StandardEvaluationContext sec = new StandardEvaluationContext();
		Apple aa = new Apple(1);
		Apple bb = new Apple(2);
		sec.setVariable("aa",aa);
		sec.setVariable("bb",bb);
		boolean b = this.expression.getValue(sec, Boolean.class);
		// Verify what the expression caused aa to be compared to
		assertEquals(bb,aa.gotComparedTo);
		assertFalse(b);
		bb.setValue(1);
		b = this.expression.getValue(sec, Boolean.class);
		assertEquals(bb,aa.gotComparedTo);
		assertTrue(b);

		assertCanCompile(this.expression);

		// Similar test with compiled expression
		aa = new Apple(99);
		bb = new Apple(100);
		sec.setVariable("aa",aa);
		sec.setVariable("bb",bb);
		b = this.expression.getValue(sec, Boolean.class);
		assertFalse(b);
		assertEquals(bb,aa.gotComparedTo);
		bb.setValue(99);
		b = this.expression.getValue(sec, Boolean.class);
		assertTrue(b);
		assertEquals(bb,aa.gotComparedTo);


		List<String> ls = new ArrayList<String>();
		ls.add(new String("foo"));
		StandardEvaluationContext context = new StandardEvaluationContext(ls);
		this.expression = parse("get(0) == 'foo'");
		assertTrue(this.expression.getValue(context, Boolean.class));
		assertCanCompile(this.expression);
		assertTrue(this.expression.getValue(context, Boolean.class));

		ls.remove(0);
		ls.add("goo");
		assertFalse(this.expression.getValue(context, Boolean.class));
	}

	@Test
	public void opPlus() throws Exception {
		this.expression = parse("2+2");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(4, this.expression.getValue());

		this.expression = parse("2L+2L");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(4L, this.expression.getValue());

		this.expression = parse("2.0f+2.0f");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(4.0f, this.expression.getValue());

		this.expression = parse("3.0d+4.0d");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(7.0d, this.expression.getValue());

		this.expression = parse("+1");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(1, this.expression.getValue());

		this.expression = parse("+1L");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(1L, this.expression.getValue());

		this.expression = parse("+1.5f");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(1.5f, this.expression.getValue());

		this.expression = parse("+2.5d");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(2.5d, this.expression.getValue());

		this.expression = parse("+T(Double).valueOf(2.5d)");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(2.5d, this.expression.getValue());

		this.expression = parse("T(Integer).valueOf(2)+6");
		assertEquals(8, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(8, this.expression.getValue());

		this.expression = parse("T(Integer).valueOf(1)+T(Integer).valueOf(3)");
		assertEquals(4, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(4, this.expression.getValue());

		this.expression = parse("1+T(Integer).valueOf(3)");
		assertEquals(4, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(4, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(2.0f)+6");
		assertEquals(8.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(8.0f, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(2.0f)+T(Float).valueOf(3.0f)");
		assertEquals(5.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(5.0f, this.expression.getValue());

		this.expression = parse("3L+T(Long).valueOf(4L)");
		assertEquals(7L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(7L, this.expression.getValue());

		this.expression = parse("T(Long).valueOf(2L)+6");
		assertEquals(8L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(8L, this.expression.getValue());

		this.expression = parse("T(Long).valueOf(2L)+T(Long).valueOf(3L)");
		assertEquals(5L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(5L, this.expression.getValue());

		this.expression = parse("1L+T(Long).valueOf(2L)");
		assertEquals(3L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(3L, this.expression.getValue());
	}

	@Test
	public void opDivide_mixedNumberTypes() throws Exception {
		PayloadX p = new PayloadX();

		// This is what you had to do before the changes in order for it to compile:
		//	expression = parse("(T(java.lang.Double).parseDouble(payload.valueI.toString()))/60D");

		// right is a double
		checkCalc(p,"payload.valueSB/60D",2d);
		checkCalc(p,"payload.valueBB/60D",2d);
		checkCalc(p,"payload.valueFB/60D",2d);
		checkCalc(p,"payload.valueDB/60D",2d);
		checkCalc(p,"payload.valueJB/60D",2d);
		checkCalc(p,"payload.valueIB/60D",2d);

		checkCalc(p,"payload.valueS/60D",2d);
		checkCalc(p,"payload.valueB/60D",2d);
		checkCalc(p,"payload.valueF/60D",2d);
		checkCalc(p,"payload.valueD/60D",2d);
		checkCalc(p,"payload.valueJ/60D",2d);
		checkCalc(p,"payload.valueI/60D",2d);

		checkCalc(p,"payload.valueSB/payload.valueDB60",2d);
		checkCalc(p,"payload.valueBB/payload.valueDB60",2d);
		checkCalc(p,"payload.valueFB/payload.valueDB60",2d);
		checkCalc(p,"payload.valueDB/payload.valueDB60",2d);
		checkCalc(p,"payload.valueJB/payload.valueDB60",2d);
		checkCalc(p,"payload.valueIB/payload.valueDB60",2d);

		checkCalc(p,"payload.valueS/payload.valueDB60",2d);
		checkCalc(p,"payload.valueB/payload.valueDB60",2d);
		checkCalc(p,"payload.valueF/payload.valueDB60",2d);
		checkCalc(p,"payload.valueD/payload.valueDB60",2d);
		checkCalc(p,"payload.valueJ/payload.valueDB60",2d);
		checkCalc(p,"payload.valueI/payload.valueDB60",2d);

		// right is a float
		checkCalc(p,"payload.valueSB/60F",2F);
		checkCalc(p,"payload.valueBB/60F",2F);
		checkCalc(p,"payload.valueFB/60F",2f);
		checkCalc(p,"payload.valueDB/60F",2d);
		checkCalc(p,"payload.valueJB/60F",2F);
		checkCalc(p,"payload.valueIB/60F",2F);

		checkCalc(p,"payload.valueS/60F",2F);
		checkCalc(p,"payload.valueB/60F",2F);
		checkCalc(p,"payload.valueF/60F",2f);
		checkCalc(p,"payload.valueD/60F",2d);
		checkCalc(p,"payload.valueJ/60F",2F);
		checkCalc(p,"payload.valueI/60F",2F);

		checkCalc(p,"payload.valueSB/payload.valueFB60",2F);
		checkCalc(p,"payload.valueBB/payload.valueFB60",2F);
		checkCalc(p,"payload.valueFB/payload.valueFB60",2f);
		checkCalc(p,"payload.valueDB/payload.valueFB60",2d);
		checkCalc(p,"payload.valueJB/payload.valueFB60",2F);
		checkCalc(p,"payload.valueIB/payload.valueFB60",2F);

		checkCalc(p,"payload.valueS/payload.valueFB60",2F);
		checkCalc(p,"payload.valueB/payload.valueFB60",2F);
		checkCalc(p,"payload.valueF/payload.valueFB60",2f);
		checkCalc(p,"payload.valueD/payload.valueFB60",2d);
		checkCalc(p,"payload.valueJ/payload.valueFB60",2F);
		checkCalc(p,"payload.valueI/payload.valueFB60",2F);

		// right is a long
		checkCalc(p,"payload.valueSB/60L",2L);
		checkCalc(p,"payload.valueBB/60L",2L);
		checkCalc(p,"payload.valueFB/60L",2f);
		checkCalc(p,"payload.valueDB/60L",2d);
		checkCalc(p,"payload.valueJB/60L",2L);
		checkCalc(p,"payload.valueIB/60L",2L);

		checkCalc(p,"payload.valueS/60L",2L);
		checkCalc(p,"payload.valueB/60L",2L);
		checkCalc(p,"payload.valueF/60L",2f);
		checkCalc(p,"payload.valueD/60L",2d);
		checkCalc(p,"payload.valueJ/60L",2L);
		checkCalc(p,"payload.valueI/60L",2L);

		checkCalc(p,"payload.valueSB/payload.valueJB60",2L);
		checkCalc(p,"payload.valueBB/payload.valueJB60",2L);
		checkCalc(p,"payload.valueFB/payload.valueJB60",2f);
		checkCalc(p,"payload.valueDB/payload.valueJB60",2d);
		checkCalc(p,"payload.valueJB/payload.valueJB60",2L);
		checkCalc(p,"payload.valueIB/payload.valueJB60",2L);

		checkCalc(p,"payload.valueS/payload.valueJB60",2L);
		checkCalc(p,"payload.valueB/payload.valueJB60",2L);
		checkCalc(p,"payload.valueF/payload.valueJB60",2f);
		checkCalc(p,"payload.valueD/payload.valueJB60",2d);
		checkCalc(p,"payload.valueJ/payload.valueJB60",2L);
		checkCalc(p,"payload.valueI/payload.valueJB60",2L);

		// right is an int
		checkCalc(p,"payload.valueSB/60",2);
		checkCalc(p,"payload.valueBB/60",2);
		checkCalc(p,"payload.valueFB/60",2f);
		checkCalc(p,"payload.valueDB/60",2d);
		checkCalc(p,"payload.valueJB/60",2L);
		checkCalc(p,"payload.valueIB/60",2);

		checkCalc(p,"payload.valueS/60",2);
		checkCalc(p,"payload.valueB/60",2);
		checkCalc(p,"payload.valueF/60",2f);
		checkCalc(p,"payload.valueD/60",2d);
		checkCalc(p,"payload.valueJ/60",2L);
		checkCalc(p,"payload.valueI/60",2);

		checkCalc(p,"payload.valueSB/payload.valueIB60",2);
		checkCalc(p,"payload.valueBB/payload.valueIB60",2);
		checkCalc(p,"payload.valueFB/payload.valueIB60",2f);
		checkCalc(p,"payload.valueDB/payload.valueIB60",2d);
		checkCalc(p,"payload.valueJB/payload.valueIB60",2L);
		checkCalc(p,"payload.valueIB/payload.valueIB60",2);

		checkCalc(p,"payload.valueS/payload.valueIB60",2);
		checkCalc(p,"payload.valueB/payload.valueIB60",2);
		checkCalc(p,"payload.valueF/payload.valueIB60",2f);
		checkCalc(p,"payload.valueD/payload.valueIB60",2d);
		checkCalc(p,"payload.valueJ/payload.valueIB60",2L);
		checkCalc(p,"payload.valueI/payload.valueIB60",2);

		// right is a short
		checkCalc(p,"payload.valueSB/payload.valueS",1);
		checkCalc(p,"payload.valueBB/payload.valueS",1);
		checkCalc(p,"payload.valueFB/payload.valueS",1f);
		checkCalc(p,"payload.valueDB/payload.valueS",1d);
		checkCalc(p,"payload.valueJB/payload.valueS",1L);
		checkCalc(p,"payload.valueIB/payload.valueS",1);

		checkCalc(p,"payload.valueS/payload.valueS",1);
		checkCalc(p,"payload.valueB/payload.valueS",1);
		checkCalc(p,"payload.valueF/payload.valueS",1f);
		checkCalc(p,"payload.valueD/payload.valueS",1d);
		checkCalc(p,"payload.valueJ/payload.valueS",1L);
		checkCalc(p,"payload.valueI/payload.valueS",1);

		checkCalc(p,"payload.valueSB/payload.valueSB",1);
		checkCalc(p,"payload.valueBB/payload.valueSB",1);
		checkCalc(p,"payload.valueFB/payload.valueSB",1f);
		checkCalc(p,"payload.valueDB/payload.valueSB",1d);
		checkCalc(p,"payload.valueJB/payload.valueSB",1L);
		checkCalc(p,"payload.valueIB/payload.valueSB",1);

		checkCalc(p,"payload.valueS/payload.valueSB",1);
		checkCalc(p,"payload.valueB/payload.valueSB",1);
		checkCalc(p,"payload.valueF/payload.valueSB",1f);
		checkCalc(p,"payload.valueD/payload.valueSB",1d);
		checkCalc(p,"payload.valueJ/payload.valueSB",1L);
		checkCalc(p,"payload.valueI/payload.valueSB",1);

		// right is a byte
		checkCalc(p,"payload.valueSB/payload.valueB",1);
		checkCalc(p,"payload.valueBB/payload.valueB",1);
		checkCalc(p,"payload.valueFB/payload.valueB",1f);
		checkCalc(p,"payload.valueDB/payload.valueB",1d);
		checkCalc(p,"payload.valueJB/payload.valueB",1L);
		checkCalc(p,"payload.valueIB/payload.valueB",1);

		checkCalc(p,"payload.valueS/payload.valueB",1);
		checkCalc(p,"payload.valueB/payload.valueB",1);
		checkCalc(p,"payload.valueF/payload.valueB",1f);
		checkCalc(p,"payload.valueD/payload.valueB",1d);
		checkCalc(p,"payload.valueJ/payload.valueB",1L);
		checkCalc(p,"payload.valueI/payload.valueB",1);

		checkCalc(p,"payload.valueSB/payload.valueBB",1);
		checkCalc(p,"payload.valueBB/payload.valueBB",1);
		checkCalc(p,"payload.valueFB/payload.valueBB",1f);
		checkCalc(p,"payload.valueDB/payload.valueBB",1d);
		checkCalc(p,"payload.valueJB/payload.valueBB",1L);
		checkCalc(p,"payload.valueIB/payload.valueBB",1);

		checkCalc(p,"payload.valueS/payload.valueBB",1);
		checkCalc(p,"payload.valueB/payload.valueBB",1);
		checkCalc(p,"payload.valueF/payload.valueBB",1f);
		checkCalc(p,"payload.valueD/payload.valueBB",1d);
		checkCalc(p,"payload.valueJ/payload.valueBB",1L);
		checkCalc(p,"payload.valueI/payload.valueBB",1);
	}

	@Test
	public void opPlus_mixedNumberTypes() throws Exception {
		PayloadX p = new PayloadX();

		// This is what you had to do before the changes in order for it to compile:
		//	expression = parse("(T(java.lang.Double).parseDouble(payload.valueI.toString()))/60D");

		// right is a double
		checkCalc(p,"payload.valueSB+60D",180d);
		checkCalc(p,"payload.valueBB+60D",180d);
		checkCalc(p,"payload.valueFB+60D",180d);
		checkCalc(p,"payload.valueDB+60D",180d);
		checkCalc(p,"payload.valueJB+60D",180d);
		checkCalc(p,"payload.valueIB+60D",180d);

		checkCalc(p,"payload.valueS+60D",180d);
		checkCalc(p,"payload.valueB+60D",180d);
		checkCalc(p,"payload.valueF+60D",180d);
		checkCalc(p,"payload.valueD+60D",180d);
		checkCalc(p,"payload.valueJ+60D",180d);
		checkCalc(p,"payload.valueI+60D",180d);

		checkCalc(p,"payload.valueSB+payload.valueDB60",180d);
		checkCalc(p,"payload.valueBB+payload.valueDB60",180d);
		checkCalc(p,"payload.valueFB+payload.valueDB60",180d);
		checkCalc(p,"payload.valueDB+payload.valueDB60",180d);
		checkCalc(p,"payload.valueJB+payload.valueDB60",180d);
		checkCalc(p,"payload.valueIB+payload.valueDB60",180d);

		checkCalc(p,"payload.valueS+payload.valueDB60",180d);
		checkCalc(p,"payload.valueB+payload.valueDB60",180d);
		checkCalc(p,"payload.valueF+payload.valueDB60",180d);
		checkCalc(p,"payload.valueD+payload.valueDB60",180d);
		checkCalc(p,"payload.valueJ+payload.valueDB60",180d);
		checkCalc(p,"payload.valueI+payload.valueDB60",180d);

		// right is a float
		checkCalc(p,"payload.valueSB+60F",180F);
		checkCalc(p,"payload.valueBB+60F",180F);
		checkCalc(p,"payload.valueFB+60F",180f);
		checkCalc(p,"payload.valueDB+60F",180d);
		checkCalc(p,"payload.valueJB+60F",180F);
		checkCalc(p,"payload.valueIB+60F",180F);

		checkCalc(p,"payload.valueS+60F",180F);
		checkCalc(p,"payload.valueB+60F",180F);
		checkCalc(p,"payload.valueF+60F",180f);
		checkCalc(p,"payload.valueD+60F",180d);
		checkCalc(p,"payload.valueJ+60F",180F);
		checkCalc(p,"payload.valueI+60F",180F);

		checkCalc(p,"payload.valueSB+payload.valueFB60",180F);
		checkCalc(p,"payload.valueBB+payload.valueFB60",180F);
		checkCalc(p,"payload.valueFB+payload.valueFB60",180f);
		checkCalc(p,"payload.valueDB+payload.valueFB60",180d);
		checkCalc(p,"payload.valueJB+payload.valueFB60",180F);
		checkCalc(p,"payload.valueIB+payload.valueFB60",180F);

		checkCalc(p,"payload.valueS+payload.valueFB60",180F);
		checkCalc(p,"payload.valueB+payload.valueFB60",180F);
		checkCalc(p,"payload.valueF+payload.valueFB60",180f);
		checkCalc(p,"payload.valueD+payload.valueFB60",180d);
		checkCalc(p,"payload.valueJ+payload.valueFB60",180F);
		checkCalc(p,"payload.valueI+payload.valueFB60",180F);

		// right is a long
		checkCalc(p,"payload.valueSB+60L",180L);
		checkCalc(p,"payload.valueBB+60L",180L);
		checkCalc(p,"payload.valueFB+60L",180f);
		checkCalc(p,"payload.valueDB+60L",180d);
		checkCalc(p,"payload.valueJB+60L",180L);
		checkCalc(p,"payload.valueIB+60L",180L);

		checkCalc(p,"payload.valueS+60L",180L);
		checkCalc(p,"payload.valueB+60L",180L);
		checkCalc(p,"payload.valueF+60L",180f);
		checkCalc(p,"payload.valueD+60L",180d);
		checkCalc(p,"payload.valueJ+60L",180L);
		checkCalc(p,"payload.valueI+60L",180L);

		checkCalc(p,"payload.valueSB+payload.valueJB60",180L);
		checkCalc(p,"payload.valueBB+payload.valueJB60",180L);
		checkCalc(p,"payload.valueFB+payload.valueJB60",180f);
		checkCalc(p,"payload.valueDB+payload.valueJB60",180d);
		checkCalc(p,"payload.valueJB+payload.valueJB60",180L);
		checkCalc(p,"payload.valueIB+payload.valueJB60",180L);

		checkCalc(p,"payload.valueS+payload.valueJB60",180L);
		checkCalc(p,"payload.valueB+payload.valueJB60",180L);
		checkCalc(p,"payload.valueF+payload.valueJB60",180f);
		checkCalc(p,"payload.valueD+payload.valueJB60",180d);
		checkCalc(p,"payload.valueJ+payload.valueJB60",180L);
		checkCalc(p,"payload.valueI+payload.valueJB60",180L);

		// right is an int
		checkCalc(p,"payload.valueSB+60",180);
		checkCalc(p,"payload.valueBB+60",180);
		checkCalc(p,"payload.valueFB+60",180f);
		checkCalc(p,"payload.valueDB+60",180d);
		checkCalc(p,"payload.valueJB+60",180L);
		checkCalc(p,"payload.valueIB+60",180);

		checkCalc(p,"payload.valueS+60",180);
		checkCalc(p,"payload.valueB+60",180);
		checkCalc(p,"payload.valueF+60",180f);
		checkCalc(p,"payload.valueD+60",180d);
		checkCalc(p,"payload.valueJ+60",180L);
		checkCalc(p,"payload.valueI+60",180);

		checkCalc(p,"payload.valueSB+payload.valueIB60",180);
		checkCalc(p,"payload.valueBB+payload.valueIB60",180);
		checkCalc(p,"payload.valueFB+payload.valueIB60",180f);
		checkCalc(p,"payload.valueDB+payload.valueIB60",180d);
		checkCalc(p,"payload.valueJB+payload.valueIB60",180L);
		checkCalc(p,"payload.valueIB+payload.valueIB60",180);

		checkCalc(p,"payload.valueS+payload.valueIB60",180);
		checkCalc(p,"payload.valueB+payload.valueIB60",180);
		checkCalc(p,"payload.valueF+payload.valueIB60",180f);
		checkCalc(p,"payload.valueD+payload.valueIB60",180d);
		checkCalc(p,"payload.valueJ+payload.valueIB60",180L);
		checkCalc(p,"payload.valueI+payload.valueIB60",180);

		// right is a short
		checkCalc(p,"payload.valueSB+payload.valueS",240);
		checkCalc(p,"payload.valueBB+payload.valueS",240);
		checkCalc(p,"payload.valueFB+payload.valueS",240f);
		checkCalc(p,"payload.valueDB+payload.valueS",240d);
		checkCalc(p,"payload.valueJB+payload.valueS",240L);
		checkCalc(p,"payload.valueIB+payload.valueS",240);

		checkCalc(p,"payload.valueS+payload.valueS",240);
		checkCalc(p,"payload.valueB+payload.valueS",240);
		checkCalc(p,"payload.valueF+payload.valueS",240f);
		checkCalc(p,"payload.valueD+payload.valueS",240d);
		checkCalc(p,"payload.valueJ+payload.valueS",240L);
		checkCalc(p,"payload.valueI+payload.valueS",240);

		checkCalc(p,"payload.valueSB+payload.valueSB",240);
		checkCalc(p,"payload.valueBB+payload.valueSB",240);
		checkCalc(p,"payload.valueFB+payload.valueSB",240f);
		checkCalc(p,"payload.valueDB+payload.valueSB",240d);
		checkCalc(p,"payload.valueJB+payload.valueSB",240L);
		checkCalc(p,"payload.valueIB+payload.valueSB",240);

		checkCalc(p,"payload.valueS+payload.valueSB",240);
		checkCalc(p,"payload.valueB+payload.valueSB",240);
		checkCalc(p,"payload.valueF+payload.valueSB",240f);
		checkCalc(p,"payload.valueD+payload.valueSB",240d);
		checkCalc(p,"payload.valueJ+payload.valueSB",240L);
		checkCalc(p,"payload.valueI+payload.valueSB",240);

		// right is a byte
		checkCalc(p,"payload.valueSB+payload.valueB",240);
		checkCalc(p,"payload.valueBB+payload.valueB",240);
		checkCalc(p,"payload.valueFB+payload.valueB",240f);
		checkCalc(p,"payload.valueDB+payload.valueB",240d);
		checkCalc(p,"payload.valueJB+payload.valueB",240L);
		checkCalc(p,"payload.valueIB+payload.valueB",240);

		checkCalc(p,"payload.valueS+payload.valueB",240);
		checkCalc(p,"payload.valueB+payload.valueB",240);
		checkCalc(p,"payload.valueF+payload.valueB",240f);
		checkCalc(p,"payload.valueD+payload.valueB",240d);
		checkCalc(p,"payload.valueJ+payload.valueB",240L);
		checkCalc(p,"payload.valueI+payload.valueB",240);

		checkCalc(p,"payload.valueSB+payload.valueBB",240);
		checkCalc(p,"payload.valueBB+payload.valueBB",240);
		checkCalc(p,"payload.valueFB+payload.valueBB",240f);
		checkCalc(p,"payload.valueDB+payload.valueBB",240d);
		checkCalc(p,"payload.valueJB+payload.valueBB",240L);
		checkCalc(p,"payload.valueIB+payload.valueBB",240);

		checkCalc(p,"payload.valueS+payload.valueBB",240);
		checkCalc(p,"payload.valueB+payload.valueBB",240);
		checkCalc(p,"payload.valueF+payload.valueBB",240f);
		checkCalc(p,"payload.valueD+payload.valueBB",240d);
		checkCalc(p,"payload.valueJ+payload.valueBB",240L);
		checkCalc(p,"payload.valueI+payload.valueBB",240);
	}

	private void checkCalc(PayloadX p, String expression, int expectedResult) {
		Expression expr = parse(expression);
		assertEquals(expectedResult, expr.getValue(p));
		assertCanCompile(expr);
		assertEquals(expectedResult, expr.getValue(p));
	}

	private void checkCalc(PayloadX p, String expression, float expectedResult) {
		Expression expr = parse(expression);
		assertEquals(expectedResult, expr.getValue(p));
		assertCanCompile(expr);
		assertEquals(expectedResult, expr.getValue(p));
	}

	private void checkCalc(PayloadX p, String expression, long expectedResult) {
		Expression expr = parse(expression);
		assertEquals(expectedResult, expr.getValue(p));
		assertCanCompile(expr);
		assertEquals(expectedResult, expr.getValue(p));
	}

	private void checkCalc(PayloadX p, String expression, double expectedResult) {
		Expression expr = parse(expression);
		assertEquals(expectedResult, expr.getValue(p));
		assertCanCompile(expr);
		assertEquals(expectedResult, expr.getValue(p));
	}

	@Test
	public void opPlusString() throws Exception {
		this.expression = parse("'hello' + 'world'");
		assertEquals("helloworld", this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals("helloworld", this.expression.getValue());

		// Method with string return
		this.expression = parse("'hello' + getWorld()");
		assertEquals("helloworld", this.expression.getValue(new Greeter()));
		assertCanCompile(this.expression);
		assertEquals("helloworld", this.expression.getValue(new Greeter()));

		// Method with string return
		this.expression = parse("getWorld() + 'hello'");
		assertEquals("worldhello", this.expression.getValue(new Greeter()));
		assertCanCompile(this.expression);
		assertEquals("worldhello", this.expression.getValue(new Greeter()));

		// Three strings, optimal bytecode would only use one StringBuilder
		this.expression = parse("'hello' + getWorld() + ' spring'");
		assertEquals("helloworld spring", this.expression.getValue(new Greeter()));
		assertCanCompile(this.expression);
		assertEquals("helloworld spring", this.expression.getValue(new Greeter()));

		// Three strings, optimal bytecode would only use one StringBuilder
		this.expression = parse("'hello' + 3 + ' spring'");
		assertEquals("hello3 spring", this.expression.getValue(new Greeter()));
		assertCantCompile(this.expression);

		this.expression = parse("object + 'a'");
		assertEquals("objecta", this.expression.getValue(new Greeter()));
		assertCanCompile(this.expression);
		assertEquals("objecta", this.expression.getValue(new Greeter()));

		this.expression = parse("'a'+object");
		assertEquals("aobject", this.expression.getValue(new Greeter()));
		assertCanCompile(this.expression);
		assertEquals("aobject", this.expression.getValue(new Greeter()));

		this.expression = parse("'a'+object+'a'");
		assertEquals("aobjecta", this.expression.getValue(new Greeter()));
		assertCanCompile(this.expression);
		assertEquals("aobjecta", this.expression.getValue(new Greeter()));

		this.expression = parse("object+'a'+object");
		assertEquals("objectaobject", this.expression.getValue(new Greeter()));
		assertCanCompile(this.expression);
		assertEquals("objectaobject", this.expression.getValue(new Greeter()));

		this.expression = parse("object+object");
		assertEquals("objectobject", this.expression.getValue(new Greeter()));
		assertCanCompile(this.expression);
		assertEquals("objectobject", this.expression.getValue(new Greeter()));
	}

	@Test
	public void opMinus() throws Exception {
		this.expression = parse("2-2");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(0, this.expression.getValue());

		this.expression = parse("4L-2L");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(2L, this.expression.getValue());

		this.expression = parse("4.0f-2.0f");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(2.0f, this.expression.getValue());

		this.expression = parse("3.0d-4.0d");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(-1.0d, this.expression.getValue());

		this.expression = parse("-1");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(-1, this.expression.getValue());

		this.expression = parse("-1L");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(-1L, this.expression.getValue());

		this.expression = parse("-1.5f");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(-1.5f, this.expression.getValue());

		this.expression = parse("-2.5d");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(-2.5d, this.expression.getValue());

		this.expression = parse("T(Integer).valueOf(2)-6");
		assertEquals(-4, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(-4, this.expression.getValue());

		this.expression = parse("T(Integer).valueOf(1)-T(Integer).valueOf(3)");
		assertEquals(-2, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(-2, this.expression.getValue());

		this.expression = parse("4-T(Integer).valueOf(3)");
		assertEquals(1, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(1, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(2.0f)-6");
		assertEquals(-4.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(-4.0f, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(8.0f)-T(Float).valueOf(3.0f)");
		assertEquals(5.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(5.0f, this.expression.getValue());

		this.expression = parse("11L-T(Long).valueOf(4L)");
		assertEquals(7L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(7L, this.expression.getValue());

		this.expression = parse("T(Long).valueOf(9L)-6");
		assertEquals(3L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(3L, this.expression.getValue());

		this.expression = parse("T(Long).valueOf(4L)-T(Long).valueOf(3L)");
		assertEquals(1L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(1L, this.expression.getValue());

		this.expression = parse("8L-T(Long).valueOf(2L)");
		assertEquals(6L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(6L, this.expression.getValue());
	}

	@Test
	public void opMinus_mixedNumberTypes() throws Exception {
		PayloadX p = new PayloadX();

		// This is what you had to do before the changes in order for it to compile:
		//	expression = parse("(T(java.lang.Double).parseDouble(payload.valueI.toString()))/60D");

		// right is a double
		checkCalc(p,"payload.valueSB-60D",60d);
		checkCalc(p,"payload.valueBB-60D",60d);
		checkCalc(p,"payload.valueFB-60D",60d);
		checkCalc(p,"payload.valueDB-60D",60d);
		checkCalc(p,"payload.valueJB-60D",60d);
		checkCalc(p,"payload.valueIB-60D",60d);

		checkCalc(p,"payload.valueS-60D",60d);
		checkCalc(p,"payload.valueB-60D",60d);
		checkCalc(p,"payload.valueF-60D",60d);
		checkCalc(p,"payload.valueD-60D",60d);
		checkCalc(p,"payload.valueJ-60D",60d);
		checkCalc(p,"payload.valueI-60D",60d);

		checkCalc(p,"payload.valueSB-payload.valueDB60",60d);
		checkCalc(p,"payload.valueBB-payload.valueDB60",60d);
		checkCalc(p,"payload.valueFB-payload.valueDB60",60d);
		checkCalc(p,"payload.valueDB-payload.valueDB60",60d);
		checkCalc(p,"payload.valueJB-payload.valueDB60",60d);
		checkCalc(p,"payload.valueIB-payload.valueDB60",60d);

		checkCalc(p,"payload.valueS-payload.valueDB60",60d);
		checkCalc(p,"payload.valueB-payload.valueDB60",60d);
		checkCalc(p,"payload.valueF-payload.valueDB60",60d);
		checkCalc(p,"payload.valueD-payload.valueDB60",60d);
		checkCalc(p,"payload.valueJ-payload.valueDB60",60d);
		checkCalc(p,"payload.valueI-payload.valueDB60",60d);

		// right is a float
		checkCalc(p,"payload.valueSB-60F",60F);
		checkCalc(p,"payload.valueBB-60F",60F);
		checkCalc(p,"payload.valueFB-60F",60f);
		checkCalc(p,"payload.valueDB-60F",60d);
		checkCalc(p,"payload.valueJB-60F",60F);
		checkCalc(p,"payload.valueIB-60F",60F);

		checkCalc(p,"payload.valueS-60F",60F);
		checkCalc(p,"payload.valueB-60F",60F);
		checkCalc(p,"payload.valueF-60F",60f);
		checkCalc(p,"payload.valueD-60F",60d);
		checkCalc(p,"payload.valueJ-60F",60F);
		checkCalc(p,"payload.valueI-60F",60F);

		checkCalc(p,"payload.valueSB-payload.valueFB60",60F);
		checkCalc(p,"payload.valueBB-payload.valueFB60",60F);
		checkCalc(p,"payload.valueFB-payload.valueFB60",60f);
		checkCalc(p,"payload.valueDB-payload.valueFB60",60d);
		checkCalc(p,"payload.valueJB-payload.valueFB60",60F);
		checkCalc(p,"payload.valueIB-payload.valueFB60",60F);

		checkCalc(p,"payload.valueS-payload.valueFB60",60F);
		checkCalc(p,"payload.valueB-payload.valueFB60",60F);
		checkCalc(p,"payload.valueF-payload.valueFB60",60f);
		checkCalc(p,"payload.valueD-payload.valueFB60",60d);
		checkCalc(p,"payload.valueJ-payload.valueFB60",60F);
		checkCalc(p,"payload.valueI-payload.valueFB60",60F);

		// right is a long
		checkCalc(p,"payload.valueSB-60L",60L);
		checkCalc(p,"payload.valueBB-60L",60L);
		checkCalc(p,"payload.valueFB-60L",60f);
		checkCalc(p,"payload.valueDB-60L",60d);
		checkCalc(p,"payload.valueJB-60L",60L);
		checkCalc(p,"payload.valueIB-60L",60L);

		checkCalc(p,"payload.valueS-60L",60L);
		checkCalc(p,"payload.valueB-60L",60L);
		checkCalc(p,"payload.valueF-60L",60f);
		checkCalc(p,"payload.valueD-60L",60d);
		checkCalc(p,"payload.valueJ-60L",60L);
		checkCalc(p,"payload.valueI-60L",60L);

		checkCalc(p,"payload.valueSB-payload.valueJB60",60L);
		checkCalc(p,"payload.valueBB-payload.valueJB60",60L);
		checkCalc(p,"payload.valueFB-payload.valueJB60",60f);
		checkCalc(p,"payload.valueDB-payload.valueJB60",60d);
		checkCalc(p,"payload.valueJB-payload.valueJB60",60L);
		checkCalc(p,"payload.valueIB-payload.valueJB60",60L);

		checkCalc(p,"payload.valueS-payload.valueJB60",60L);
		checkCalc(p,"payload.valueB-payload.valueJB60",60L);
		checkCalc(p,"payload.valueF-payload.valueJB60",60f);
		checkCalc(p,"payload.valueD-payload.valueJB60",60d);
		checkCalc(p,"payload.valueJ-payload.valueJB60",60L);
		checkCalc(p,"payload.valueI-payload.valueJB60",60L);

		// right is an int
		checkCalc(p,"payload.valueSB-60",60);
		checkCalc(p,"payload.valueBB-60",60);
		checkCalc(p,"payload.valueFB-60",60f);
		checkCalc(p,"payload.valueDB-60",60d);
		checkCalc(p,"payload.valueJB-60",60L);
		checkCalc(p,"payload.valueIB-60",60);

		checkCalc(p,"payload.valueS-60",60);
		checkCalc(p,"payload.valueB-60",60);
		checkCalc(p,"payload.valueF-60",60f);
		checkCalc(p,"payload.valueD-60",60d);
		checkCalc(p,"payload.valueJ-60",60L);
		checkCalc(p,"payload.valueI-60",60);

		checkCalc(p,"payload.valueSB-payload.valueIB60",60);
		checkCalc(p,"payload.valueBB-payload.valueIB60",60);
		checkCalc(p,"payload.valueFB-payload.valueIB60",60f);
		checkCalc(p,"payload.valueDB-payload.valueIB60",60d);
		checkCalc(p,"payload.valueJB-payload.valueIB60",60L);
		checkCalc(p,"payload.valueIB-payload.valueIB60",60);

		checkCalc(p,"payload.valueS-payload.valueIB60",60);
		checkCalc(p,"payload.valueB-payload.valueIB60",60);
		checkCalc(p,"payload.valueF-payload.valueIB60",60f);
		checkCalc(p,"payload.valueD-payload.valueIB60",60d);
		checkCalc(p,"payload.valueJ-payload.valueIB60",60L);
		checkCalc(p,"payload.valueI-payload.valueIB60",60);

		// right is a short
		checkCalc(p,"payload.valueSB-payload.valueS20",100);
		checkCalc(p,"payload.valueBB-payload.valueS20",100);
		checkCalc(p,"payload.valueFB-payload.valueS20",100f);
		checkCalc(p,"payload.valueDB-payload.valueS20",100d);
		checkCalc(p,"payload.valueJB-payload.valueS20",100L);
		checkCalc(p,"payload.valueIB-payload.valueS20",100);

		checkCalc(p,"payload.valueS-payload.valueS20",100);
		checkCalc(p,"payload.valueB-payload.valueS20",100);
		checkCalc(p,"payload.valueF-payload.valueS20",100f);
		checkCalc(p,"payload.valueD-payload.valueS20",100d);
		checkCalc(p,"payload.valueJ-payload.valueS20",100L);
		checkCalc(p,"payload.valueI-payload.valueS20",100);

		checkCalc(p,"payload.valueSB-payload.valueSB20",100);
		checkCalc(p,"payload.valueBB-payload.valueSB20",100);
		checkCalc(p,"payload.valueFB-payload.valueSB20",100f);
		checkCalc(p,"payload.valueDB-payload.valueSB20",100d);
		checkCalc(p,"payload.valueJB-payload.valueSB20",100L);
		checkCalc(p,"payload.valueIB-payload.valueSB20",100);

		checkCalc(p,"payload.valueS-payload.valueSB20",100);
		checkCalc(p,"payload.valueB-payload.valueSB20",100);
		checkCalc(p,"payload.valueF-payload.valueSB20",100f);
		checkCalc(p,"payload.valueD-payload.valueSB20",100d);
		checkCalc(p,"payload.valueJ-payload.valueSB20",100L);
		checkCalc(p,"payload.valueI-payload.valueSB20",100);

		// right is a byte
		checkCalc(p,"payload.valueSB-payload.valueB20",100);
		checkCalc(p,"payload.valueBB-payload.valueB20",100);
		checkCalc(p,"payload.valueFB-payload.valueB20",100f);
		checkCalc(p,"payload.valueDB-payload.valueB20",100d);
		checkCalc(p,"payload.valueJB-payload.valueB20",100L);
		checkCalc(p,"payload.valueIB-payload.valueB20",100);

		checkCalc(p,"payload.valueS-payload.valueB20",100);
		checkCalc(p,"payload.valueB-payload.valueB20",100);
		checkCalc(p,"payload.valueF-payload.valueB20",100f);
		checkCalc(p,"payload.valueD-payload.valueB20",100d);
		checkCalc(p,"payload.valueJ-payload.valueB20",100L);
		checkCalc(p,"payload.valueI-payload.valueB20",100);

		checkCalc(p,"payload.valueSB-payload.valueBB20",100);
		checkCalc(p,"payload.valueBB-payload.valueBB20",100);
		checkCalc(p,"payload.valueFB-payload.valueBB20",100f);
		checkCalc(p,"payload.valueDB-payload.valueBB20",100d);
		checkCalc(p,"payload.valueJB-payload.valueBB20",100L);
		checkCalc(p,"payload.valueIB-payload.valueBB20",100);

		checkCalc(p,"payload.valueS-payload.valueBB20",100);
		checkCalc(p,"payload.valueB-payload.valueBB20",100);
		checkCalc(p,"payload.valueF-payload.valueBB20",100f);
		checkCalc(p,"payload.valueD-payload.valueBB20",100d);
		checkCalc(p,"payload.valueJ-payload.valueBB20",100L);
		checkCalc(p,"payload.valueI-payload.valueBB20",100);
	}

	@Test
	public void opMultiply_mixedNumberTypes() throws Exception {
		PayloadX p = new PayloadX();

		// This is what you had to do before the changes in order for it to compile:
		//	expression = parse("(T(java.lang.Double).parseDouble(payload.valueI.toString()))/60D");

		// right is a double
		checkCalc(p,"payload.valueSB*60D",7200d);
		checkCalc(p,"payload.valueBB*60D",7200d);
		checkCalc(p,"payload.valueFB*60D",7200d);
		checkCalc(p,"payload.valueDB*60D",7200d);
		checkCalc(p,"payload.valueJB*60D",7200d);
		checkCalc(p,"payload.valueIB*60D",7200d);

		checkCalc(p,"payload.valueS*60D",7200d);
		checkCalc(p,"payload.valueB*60D",7200d);
		checkCalc(p,"payload.valueF*60D",7200d);
		checkCalc(p,"payload.valueD*60D",7200d);
		checkCalc(p,"payload.valueJ*60D",7200d);
		checkCalc(p,"payload.valueI*60D",7200d);

		checkCalc(p,"payload.valueSB*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueBB*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueFB*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueDB*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueJB*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueIB*payload.valueDB60",7200d);

		checkCalc(p,"payload.valueS*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueB*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueF*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueD*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueJ*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueI*payload.valueDB60",7200d);

		// right is a float
		checkCalc(p,"payload.valueSB*60F",7200F);
		checkCalc(p,"payload.valueBB*60F",7200F);
		checkCalc(p,"payload.valueFB*60F",7200f);
		checkCalc(p,"payload.valueDB*60F",7200d);
		checkCalc(p,"payload.valueJB*60F",7200F);
		checkCalc(p,"payload.valueIB*60F",7200F);

		checkCalc(p,"payload.valueS*60F",7200F);
		checkCalc(p,"payload.valueB*60F",7200F);
		checkCalc(p,"payload.valueF*60F",7200f);
		checkCalc(p,"payload.valueD*60F",7200d);
		checkCalc(p,"payload.valueJ*60F",7200F);
		checkCalc(p,"payload.valueI*60F",7200F);

		checkCalc(p,"payload.valueSB*payload.valueFB60",7200F);
		checkCalc(p,"payload.valueBB*payload.valueFB60",7200F);
		checkCalc(p,"payload.valueFB*payload.valueFB60",7200f);
		checkCalc(p,"payload.valueDB*payload.valueFB60",7200d);
		checkCalc(p,"payload.valueJB*payload.valueFB60",7200F);
		checkCalc(p,"payload.valueIB*payload.valueFB60",7200F);

		checkCalc(p,"payload.valueS*payload.valueFB60",7200F);
		checkCalc(p,"payload.valueB*payload.valueFB60",7200F);
		checkCalc(p,"payload.valueF*payload.valueFB60",7200f);
		checkCalc(p,"payload.valueD*payload.valueFB60",7200d);
		checkCalc(p,"payload.valueJ*payload.valueFB60",7200F);
		checkCalc(p,"payload.valueI*payload.valueFB60",7200F);

		// right is a long
		checkCalc(p,"payload.valueSB*60L",7200L);
		checkCalc(p,"payload.valueBB*60L",7200L);
		checkCalc(p,"payload.valueFB*60L",7200f);
		checkCalc(p,"payload.valueDB*60L",7200d);
		checkCalc(p,"payload.valueJB*60L",7200L);
		checkCalc(p,"payload.valueIB*60L",7200L);

		checkCalc(p,"payload.valueS*60L",7200L);
		checkCalc(p,"payload.valueB*60L",7200L);
		checkCalc(p,"payload.valueF*60L",7200f);
		checkCalc(p,"payload.valueD*60L",7200d);
		checkCalc(p,"payload.valueJ*60L",7200L);
		checkCalc(p,"payload.valueI*60L",7200L);

		checkCalc(p,"payload.valueSB*payload.valueJB60",7200L);
		checkCalc(p,"payload.valueBB*payload.valueJB60",7200L);
		checkCalc(p,"payload.valueFB*payload.valueJB60",7200f);
		checkCalc(p,"payload.valueDB*payload.valueJB60",7200d);
		checkCalc(p,"payload.valueJB*payload.valueJB60",7200L);
		checkCalc(p,"payload.valueIB*payload.valueJB60",7200L);

		checkCalc(p,"payload.valueS*payload.valueJB60",7200L);
		checkCalc(p,"payload.valueB*payload.valueJB60",7200L);
		checkCalc(p,"payload.valueF*payload.valueJB60",7200f);
		checkCalc(p,"payload.valueD*payload.valueJB60",7200d);
		checkCalc(p,"payload.valueJ*payload.valueJB60",7200L);
		checkCalc(p,"payload.valueI*payload.valueJB60",7200L);

		// right is an int
		checkCalc(p,"payload.valueSB*60",7200);
		checkCalc(p,"payload.valueBB*60",7200);
		checkCalc(p,"payload.valueFB*60",7200f);
		checkCalc(p,"payload.valueDB*60",7200d);
		checkCalc(p,"payload.valueJB*60",7200L);
		checkCalc(p,"payload.valueIB*60",7200);

		checkCalc(p,"payload.valueS*60",7200);
		checkCalc(p,"payload.valueB*60",7200);
		checkCalc(p,"payload.valueF*60",7200f);
		checkCalc(p,"payload.valueD*60",7200d);
		checkCalc(p,"payload.valueJ*60",7200L);
		checkCalc(p,"payload.valueI*60",7200);

		checkCalc(p,"payload.valueSB*payload.valueIB60",7200);
		checkCalc(p,"payload.valueBB*payload.valueIB60",7200);
		checkCalc(p,"payload.valueFB*payload.valueIB60",7200f);
		checkCalc(p,"payload.valueDB*payload.valueIB60",7200d);
		checkCalc(p,"payload.valueJB*payload.valueIB60",7200L);
		checkCalc(p,"payload.valueIB*payload.valueIB60",7200);

		checkCalc(p,"payload.valueS*payload.valueIB60",7200);
		checkCalc(p,"payload.valueB*payload.valueIB60",7200);
		checkCalc(p,"payload.valueF*payload.valueIB60",7200f);
		checkCalc(p,"payload.valueD*payload.valueIB60",7200d);
		checkCalc(p,"payload.valueJ*payload.valueIB60",7200L);
		checkCalc(p,"payload.valueI*payload.valueIB60",7200);

		// right is a short
		checkCalc(p,"payload.valueSB*payload.valueS20",2400);
		checkCalc(p,"payload.valueBB*payload.valueS20",2400);
		checkCalc(p,"payload.valueFB*payload.valueS20",2400f);
		checkCalc(p,"payload.valueDB*payload.valueS20",2400d);
		checkCalc(p,"payload.valueJB*payload.valueS20",2400L);
		checkCalc(p,"payload.valueIB*payload.valueS20",2400);

		checkCalc(p,"payload.valueS*payload.valueS20",2400);
		checkCalc(p,"payload.valueB*payload.valueS20",2400);
		checkCalc(p,"payload.valueF*payload.valueS20",2400f);
		checkCalc(p,"payload.valueD*payload.valueS20",2400d);
		checkCalc(p,"payload.valueJ*payload.valueS20",2400L);
		checkCalc(p,"payload.valueI*payload.valueS20",2400);

		checkCalc(p,"payload.valueSB*payload.valueSB20",2400);
		checkCalc(p,"payload.valueBB*payload.valueSB20",2400);
		checkCalc(p,"payload.valueFB*payload.valueSB20",2400f);
		checkCalc(p,"payload.valueDB*payload.valueSB20",2400d);
		checkCalc(p,"payload.valueJB*payload.valueSB20",2400L);
		checkCalc(p,"payload.valueIB*payload.valueSB20",2400);

		checkCalc(p,"payload.valueS*payload.valueSB20",2400);
		checkCalc(p,"payload.valueB*payload.valueSB20",2400);
		checkCalc(p,"payload.valueF*payload.valueSB20",2400f);
		checkCalc(p,"payload.valueD*payload.valueSB20",2400d);
		checkCalc(p,"payload.valueJ*payload.valueSB20",2400L);
		checkCalc(p,"payload.valueI*payload.valueSB20",2400);

		// right is a byte
		checkCalc(p,"payload.valueSB*payload.valueB20",2400);
		checkCalc(p,"payload.valueBB*payload.valueB20",2400);
		checkCalc(p,"payload.valueFB*payload.valueB20",2400f);
		checkCalc(p,"payload.valueDB*payload.valueB20",2400d);
		checkCalc(p,"payload.valueJB*payload.valueB20",2400L);
		checkCalc(p,"payload.valueIB*payload.valueB20",2400);

		checkCalc(p,"payload.valueS*payload.valueB20",2400);
		checkCalc(p,"payload.valueB*payload.valueB20",2400);
		checkCalc(p,"payload.valueF*payload.valueB20",2400f);
		checkCalc(p,"payload.valueD*payload.valueB20",2400d);
		checkCalc(p,"payload.valueJ*payload.valueB20",2400L);
		checkCalc(p,"payload.valueI*payload.valueB20",2400);

		checkCalc(p,"payload.valueSB*payload.valueBB20",2400);
		checkCalc(p,"payload.valueBB*payload.valueBB20",2400);
		checkCalc(p,"payload.valueFB*payload.valueBB20",2400f);
		checkCalc(p,"payload.valueDB*payload.valueBB20",2400d);
		checkCalc(p,"payload.valueJB*payload.valueBB20",2400L);
		checkCalc(p,"payload.valueIB*payload.valueBB20",2400);

		checkCalc(p,"payload.valueS*payload.valueBB20",2400);
		checkCalc(p,"payload.valueB*payload.valueBB20",2400);
		checkCalc(p,"payload.valueF*payload.valueBB20",2400f);
		checkCalc(p,"payload.valueD*payload.valueBB20",2400d);
		checkCalc(p,"payload.valueJ*payload.valueBB20",2400L);
		checkCalc(p,"payload.valueI*payload.valueBB20",2400);
	}

	@Test
	public void opModulus_mixedNumberTypes() throws Exception {
		PayloadX p = new PayloadX();

		// This is what you had to do before the changes in order for it to compile:
		//	expression = parse("(T(java.lang.Double).parseDouble(payload.valueI.toString()))/60D");

		// right is a double
		checkCalc(p,"payload.valueSB%58D",4d);
		checkCalc(p,"payload.valueBB%58D",4d);
		checkCalc(p,"payload.valueFB%58D",4d);
		checkCalc(p,"payload.valueDB%58D",4d);
		checkCalc(p,"payload.valueJB%58D",4d);
		checkCalc(p,"payload.valueIB%58D",4d);

		checkCalc(p,"payload.valueS%58D",4d);
		checkCalc(p,"payload.valueB%58D",4d);
		checkCalc(p,"payload.valueF%58D",4d);
		checkCalc(p,"payload.valueD%58D",4d);
		checkCalc(p,"payload.valueJ%58D",4d);
		checkCalc(p,"payload.valueI%58D",4d);

		checkCalc(p,"payload.valueSB%payload.valueDB58",4d);
		checkCalc(p,"payload.valueBB%payload.valueDB58",4d);
		checkCalc(p,"payload.valueFB%payload.valueDB58",4d);
		checkCalc(p,"payload.valueDB%payload.valueDB58",4d);
		checkCalc(p,"payload.valueJB%payload.valueDB58",4d);
		checkCalc(p,"payload.valueIB%payload.valueDB58",4d);

		checkCalc(p,"payload.valueS%payload.valueDB58",4d);
		checkCalc(p,"payload.valueB%payload.valueDB58",4d);
		checkCalc(p,"payload.valueF%payload.valueDB58",4d);
		checkCalc(p,"payload.valueD%payload.valueDB58",4d);
		checkCalc(p,"payload.valueJ%payload.valueDB58",4d);
		checkCalc(p,"payload.valueI%payload.valueDB58",4d);

		// right is a float
		checkCalc(p,"payload.valueSB%58F",4F);
		checkCalc(p,"payload.valueBB%58F",4F);
		checkCalc(p,"payload.valueFB%58F",4f);
		checkCalc(p,"payload.valueDB%58F",4d);
		checkCalc(p,"payload.valueJB%58F",4F);
		checkCalc(p,"payload.valueIB%58F",4F);

		checkCalc(p,"payload.valueS%58F",4F);
		checkCalc(p,"payload.valueB%58F",4F);
		checkCalc(p,"payload.valueF%58F",4f);
		checkCalc(p,"payload.valueD%58F",4d);
		checkCalc(p,"payload.valueJ%58F",4F);
		checkCalc(p,"payload.valueI%58F",4F);

		checkCalc(p,"payload.valueSB%payload.valueFB58",4F);
		checkCalc(p,"payload.valueBB%payload.valueFB58",4F);
		checkCalc(p,"payload.valueFB%payload.valueFB58",4f);
		checkCalc(p,"payload.valueDB%payload.valueFB58",4d);
		checkCalc(p,"payload.valueJB%payload.valueFB58",4F);
		checkCalc(p,"payload.valueIB%payload.valueFB58",4F);

		checkCalc(p,"payload.valueS%payload.valueFB58",4F);
		checkCalc(p,"payload.valueB%payload.valueFB58",4F);
		checkCalc(p,"payload.valueF%payload.valueFB58",4f);
		checkCalc(p,"payload.valueD%payload.valueFB58",4d);
		checkCalc(p,"payload.valueJ%payload.valueFB58",4F);
		checkCalc(p,"payload.valueI%payload.valueFB58",4F);

		// right is a long
		checkCalc(p,"payload.valueSB%58L",4L);
		checkCalc(p,"payload.valueBB%58L",4L);
		checkCalc(p,"payload.valueFB%58L",4f);
		checkCalc(p,"payload.valueDB%58L",4d);
		checkCalc(p,"payload.valueJB%58L",4L);
		checkCalc(p,"payload.valueIB%58L",4L);

		checkCalc(p,"payload.valueS%58L",4L);
		checkCalc(p,"payload.valueB%58L",4L);
		checkCalc(p,"payload.valueF%58L",4f);
		checkCalc(p,"payload.valueD%58L",4d);
		checkCalc(p,"payload.valueJ%58L",4L);
		checkCalc(p,"payload.valueI%58L",4L);

		checkCalc(p,"payload.valueSB%payload.valueJB58",4L);
		checkCalc(p,"payload.valueBB%payload.valueJB58",4L);
		checkCalc(p,"payload.valueFB%payload.valueJB58",4f);
		checkCalc(p,"payload.valueDB%payload.valueJB58",4d);
		checkCalc(p,"payload.valueJB%payload.valueJB58",4L);
		checkCalc(p,"payload.valueIB%payload.valueJB58",4L);

		checkCalc(p,"payload.valueS%payload.valueJB58",4L);
		checkCalc(p,"payload.valueB%payload.valueJB58",4L);
		checkCalc(p,"payload.valueF%payload.valueJB58",4f);
		checkCalc(p,"payload.valueD%payload.valueJB58",4d);
		checkCalc(p,"payload.valueJ%payload.valueJB58",4L);
		checkCalc(p,"payload.valueI%payload.valueJB58",4L);

		// right is an int
		checkCalc(p,"payload.valueSB%58",4);
		checkCalc(p,"payload.valueBB%58",4);
		checkCalc(p,"payload.valueFB%58",4f);
		checkCalc(p,"payload.valueDB%58",4d);
		checkCalc(p,"payload.valueJB%58",4L);
		checkCalc(p,"payload.valueIB%58",4);

		checkCalc(p,"payload.valueS%58",4);
		checkCalc(p,"payload.valueB%58",4);
		checkCalc(p,"payload.valueF%58",4f);
		checkCalc(p,"payload.valueD%58",4d);
		checkCalc(p,"payload.valueJ%58",4L);
		checkCalc(p,"payload.valueI%58",4);

		checkCalc(p,"payload.valueSB%payload.valueIB58",4);
		checkCalc(p,"payload.valueBB%payload.valueIB58",4);
		checkCalc(p,"payload.valueFB%payload.valueIB58",4f);
		checkCalc(p,"payload.valueDB%payload.valueIB58",4d);
		checkCalc(p,"payload.valueJB%payload.valueIB58",4L);
		checkCalc(p,"payload.valueIB%payload.valueIB58",4);

		checkCalc(p,"payload.valueS%payload.valueIB58",4);
		checkCalc(p,"payload.valueB%payload.valueIB58",4);
		checkCalc(p,"payload.valueF%payload.valueIB58",4f);
		checkCalc(p,"payload.valueD%payload.valueIB58",4d);
		checkCalc(p,"payload.valueJ%payload.valueIB58",4L);
		checkCalc(p,"payload.valueI%payload.valueIB58",4);

		// right is a short
		checkCalc(p,"payload.valueSB%payload.valueS18",12);
		checkCalc(p,"payload.valueBB%payload.valueS18",12);
		checkCalc(p,"payload.valueFB%payload.valueS18",12f);
		checkCalc(p,"payload.valueDB%payload.valueS18",12d);
		checkCalc(p,"payload.valueJB%payload.valueS18",12L);
		checkCalc(p,"payload.valueIB%payload.valueS18",12);

		checkCalc(p,"payload.valueS%payload.valueS18",12);
		checkCalc(p,"payload.valueB%payload.valueS18",12);
		checkCalc(p,"payload.valueF%payload.valueS18",12f);
		checkCalc(p,"payload.valueD%payload.valueS18",12d);
		checkCalc(p,"payload.valueJ%payload.valueS18",12L);
		checkCalc(p,"payload.valueI%payload.valueS18",12);

		checkCalc(p,"payload.valueSB%payload.valueSB18",12);
		checkCalc(p,"payload.valueBB%payload.valueSB18",12);
		checkCalc(p,"payload.valueFB%payload.valueSB18",12f);
		checkCalc(p,"payload.valueDB%payload.valueSB18",12d);
		checkCalc(p,"payload.valueJB%payload.valueSB18",12L);
		checkCalc(p,"payload.valueIB%payload.valueSB18",12);

		checkCalc(p,"payload.valueS%payload.valueSB18",12);
		checkCalc(p,"payload.valueB%payload.valueSB18",12);
		checkCalc(p,"payload.valueF%payload.valueSB18",12f);
		checkCalc(p,"payload.valueD%payload.valueSB18",12d);
		checkCalc(p,"payload.valueJ%payload.valueSB18",12L);
		checkCalc(p,"payload.valueI%payload.valueSB18",12);

		// right is a byte
		checkCalc(p,"payload.valueSB%payload.valueB18",12);
		checkCalc(p,"payload.valueBB%payload.valueB18",12);
		checkCalc(p,"payload.valueFB%payload.valueB18",12f);
		checkCalc(p,"payload.valueDB%payload.valueB18",12d);
		checkCalc(p,"payload.valueJB%payload.valueB18",12L);
		checkCalc(p,"payload.valueIB%payload.valueB18",12);

		checkCalc(p,"payload.valueS%payload.valueB18",12);
		checkCalc(p,"payload.valueB%payload.valueB18",12);
		checkCalc(p,"payload.valueF%payload.valueB18",12f);
		checkCalc(p,"payload.valueD%payload.valueB18",12d);
		checkCalc(p,"payload.valueJ%payload.valueB18",12L);
		checkCalc(p,"payload.valueI%payload.valueB18",12);

		checkCalc(p,"payload.valueSB%payload.valueBB18",12);
		checkCalc(p,"payload.valueBB%payload.valueBB18",12);
		checkCalc(p,"payload.valueFB%payload.valueBB18",12f);
		checkCalc(p,"payload.valueDB%payload.valueBB18",12d);
		checkCalc(p,"payload.valueJB%payload.valueBB18",12L);
		checkCalc(p,"payload.valueIB%payload.valueBB18",12);

		checkCalc(p,"payload.valueS%payload.valueBB18",12);
		checkCalc(p,"payload.valueB%payload.valueBB18",12);
		checkCalc(p,"payload.valueF%payload.valueBB18",12f);
		checkCalc(p,"payload.valueD%payload.valueBB18",12d);
		checkCalc(p,"payload.valueJ%payload.valueBB18",12L);
		checkCalc(p,"payload.valueI%payload.valueBB18",12);
	}

	@Test
	public void opMultiply() throws Exception {
		this.expression = parse("2*2");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(4, this.expression.getValue());

		this.expression = parse("2L*2L");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(4L, this.expression.getValue());

		this.expression = parse("2.0f*2.0f");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(4.0f, this.expression.getValue());

		this.expression = parse("3.0d*4.0d");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(12.0d, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(2.0f)*6");
		assertEquals(12.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(12.0f, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(8.0f)*T(Float).valueOf(3.0f)");
		assertEquals(24.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(24.0f, this.expression.getValue());

		this.expression = parse("11L*T(Long).valueOf(4L)");
		assertEquals(44L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(44L, this.expression.getValue());

		this.expression = parse("T(Long).valueOf(9L)*6");
		assertEquals(54L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(54L, this.expression.getValue());

		this.expression = parse("T(Long).valueOf(4L)*T(Long).valueOf(3L)");
		assertEquals(12L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(12L, this.expression.getValue());

		this.expression = parse("8L*T(Long).valueOf(2L)");
		assertEquals(16L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(16L, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(8.0f)*-T(Float).valueOf(3.0f)");
		assertEquals(-24.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(-24.0f, this.expression.getValue());
	}

	@Test
	public void opDivide() throws Exception {
		this.expression = parse("2/2");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(1, this.expression.getValue());

		this.expression = parse("2L/2L");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(1L, this.expression.getValue());

		this.expression = parse("2.0f/2.0f");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(1.0f, this.expression.getValue());

		this.expression = parse("3.0d/4.0d");
		this.expression.getValue();
		assertCanCompile(this.expression);
		assertEquals(0.75d, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(6.0f)/2");
		assertEquals(3.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(3.0f, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(8.0f)/T(Float).valueOf(2.0f)");
		assertEquals(4.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(4.0f, this.expression.getValue());

		this.expression = parse("12L/T(Long).valueOf(4L)");
		assertEquals(3L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(3L, this.expression.getValue());

		this.expression = parse("T(Long).valueOf(44L)/11");
		assertEquals(4L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(4L, this.expression.getValue());

		this.expression = parse("T(Long).valueOf(4L)/T(Long).valueOf(2L)");
		assertEquals(2L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(2L, this.expression.getValue());

		this.expression = parse("8L/T(Long).valueOf(2L)");
		assertEquals(4L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(4L, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(8.0f)/-T(Float).valueOf(4.0f)");
		assertEquals(-2.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(-2.0f, this.expression.getValue());
	}

	@Test
	public void opModulus_12041() throws Exception {
		this.expression = parse("2%2");
		assertEquals(0, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(0, this.expression.getValue());

		this.expression = parse("payload%2==0");
		assertTrue(this.expression.getValue(new GenericMessageTestHelper<>(4), Boolean.TYPE));
		assertFalse(this.expression.getValue(new GenericMessageTestHelper<>(5), Boolean.TYPE));
		assertCanCompile(this.expression);
		assertTrue(this.expression.getValue(new GenericMessageTestHelper<>(4), Boolean.TYPE));
		assertFalse(this.expression.getValue(new GenericMessageTestHelper<>(5), Boolean.TYPE));

		this.expression = parse("8%3");
		assertEquals(2, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(2, this.expression.getValue());

		this.expression = parse("17L%5L");
		assertEquals(2L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(2L, this.expression.getValue());

		this.expression = parse("3.0f%2.0f");
		assertEquals(1.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(1.0f, this.expression.getValue());

		this.expression = parse("3.0d%4.0d");
		assertEquals(3.0d, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(3.0d, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(6.0f)%2");
		assertEquals(0.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(0.0f, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(6.0f)%4");
		assertEquals(2.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(2.0f, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(8.0f)%T(Float).valueOf(3.0f)");
		assertEquals(2.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(2.0f, this.expression.getValue());

		this.expression = parse("13L%T(Long).valueOf(4L)");
		assertEquals(1L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(1L, this.expression.getValue());

		this.expression = parse("T(Long).valueOf(44L)%12");
		assertEquals(8L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(8L, this.expression.getValue());

		this.expression = parse("T(Long).valueOf(9L)%T(Long).valueOf(2L)");
		assertEquals(1L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(1L, this.expression.getValue());

		this.expression = parse("7L%T(Long).valueOf(2L)");
		assertEquals(1L, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(1L, this.expression.getValue());

		this.expression = parse("T(Float).valueOf(9.0f)%-T(Float).valueOf(4.0f)");
		assertEquals(1.0f, this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals(1.0f, this.expression.getValue());
	}

	@Test
	public void compilationOfBasicNullSafeMethodReference() {
		SpelExpressionParser parser = new SpelExpressionParser(
				new SpelParserConfiguration(SpelCompilerMode.OFF, getClass().getClassLoader()));
		SpelExpression expression = parser.parseRaw("#it?.equals(3)");
		StandardEvaluationContext context = new StandardEvaluationContext(new Object[] {1});
		context.setVariable("it", 3);
		expression.setEvaluationContext(context);
		assertTrue(expression.getValue(Boolean.class));
		context.setVariable("it", null);
		assertNull(expression.getValue(Boolean.class));

		assertCanCompile(expression);

		context.setVariable("it", 3);
		assertTrue(expression.getValue(Boolean.class));
		context.setVariable("it", null);
		assertNull(expression.getValue(Boolean.class));
	}

	@Test
	public void failsWhenSettingContextForExpression_SPR12326() {
		SpelExpressionParser parser = new SpelExpressionParser(
				new SpelParserConfiguration(SpelCompilerMode.OFF, getClass().getClassLoader()));
		Person3 person = new Person3("foo", 1);
		SpelExpression expression = parser.parseRaw("#it?.age?.equals([0])");
		StandardEvaluationContext context = new StandardEvaluationContext(new Object[] {1});
		context.setVariable("it", person);
		expression.setEvaluationContext(context);
		assertTrue(expression.getValue(Boolean.class));
		// This will trigger compilation (second usage)
		assertTrue(expression.getValue(Boolean.class));
		context.setVariable("it", null);
		assertNull(expression.getValue(Boolean.class));

		assertCanCompile(expression);

		context.setVariable("it", person);
		assertTrue(expression.getValue(Boolean.class));
		context.setVariable("it", null);
		assertNull(expression.getValue(Boolean.class));
	}


	/**
	 * Test variants of using T(...) and static/non-static method/property/field references.
	 */
	@Test
	public void constructorReference_SPR13781() {
		// Static field access on a T() referenced type
		this.expression = this.parser.parseExpression("T(java.util.Locale).ENGLISH");
		assertEquals("en", this.expression.getValue().toString());
		assertCanCompile(this.expression);
		assertEquals("en", this.expression.getValue().toString());

		// The actual expression from the bug report. It fails if the ENGLISH reference fails
		// to pop the type reference for Locale off the stack (if it isn't popped then
		// toLowerCase() will be called with a Locale parameter). In this situation the
		// code generation for ENGLISH should notice there is something on the stack that
		// is not required and pop it off.
		this.expression = this.parser.parseExpression("#userId.toString().toLowerCase(T(java.util.Locale).ENGLISH)");
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("userId", "RoDnEy");
		assertEquals("rodney", this.expression.getValue(context));
		assertCanCompile(this.expression);
		assertEquals("rodney", this.expression.getValue(context));

		// Property access on a class object
		this.expression = this.parser.parseExpression("T(String).name");
		assertEquals("java.lang.String", this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals("java.lang.String", this.expression.getValue());

		// Now the type reference isn't on the stack, and needs loading
		context = new StandardEvaluationContext(String.class);
		this.expression = this.parser.parseExpression("name");
		assertEquals("java.lang.String", this.expression.getValue(context));
		assertCanCompile(this.expression);
		assertEquals("java.lang.String", this.expression.getValue(context));

		this.expression = this.parser.parseExpression("T(String).getName()");
		assertEquals("java.lang.String", this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals("java.lang.String", this.expression.getValue());

		// These tests below verify that the chain of static accesses (either method/property or field)
		// leave the right thing on top of the stack for processing by any outer consuming code.
		// Here the consuming code is the String.valueOf() function.  If the wrong thing were on
		// the stack (for example if the compiled code for static methods wasn't popping the
		// previous thing off the stack) the valueOf() would operate on the wrong value.

		String shclass = StaticsHelper.class.getName();
		// Basic chain: property access then method access
		this.expression = this.parser.parseExpression("T(String).valueOf(T(String).name.valueOf(1))");
		assertEquals("1", this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals("1", this.expression.getValue());

		// chain of statics ending with static method
		this.expression = this.parser.parseExpression("T(String).valueOf(T(" + shclass + ").methoda().methoda().methodb())");
		assertEquals("mb", this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals("mb", this.expression.getValue());

		// chain of statics ending with static field
		this.expression = this.parser.parseExpression("T(String).valueOf(T(" + shclass + ").fielda.fielda.fieldb)");
		assertEquals("fb", this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals("fb", this.expression.getValue());

		// chain of statics ending with static property access
		this.expression = this.parser.parseExpression("T(String).valueOf(T(" + shclass + ").propertya.propertya.propertyb)");
		assertEquals("pb", this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals("pb", this.expression.getValue());

		// variety chain
		this.expression = this.parser.parseExpression("T(String).valueOf(T(" + shclass + ").fielda.methoda().propertya.fieldb)");
		assertEquals("fb", this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals("fb", this.expression.getValue());

		this.expression = this.parser.parseExpression("T(String).valueOf(fielda.fieldb)");
		assertEquals("fb", this.expression.getValue(StaticsHelper.sh));
		assertCanCompile(this.expression);
		assertEquals("fb", this.expression.getValue(StaticsHelper.sh));

		this.expression = this.parser.parseExpression("T(String).valueOf(propertya.propertyb)");
		assertEquals("pb", this.expression.getValue(StaticsHelper.sh));
		assertCanCompile(this.expression);
		assertEquals("pb", this.expression.getValue(StaticsHelper.sh));

		this.expression = this.parser.parseExpression("T(String).valueOf(methoda().methodb())");
		assertEquals("mb", this.expression.getValue(StaticsHelper.sh));
		assertCanCompile(this.expression);
		assertEquals("mb", this.expression.getValue(StaticsHelper.sh));

	}

	@Test
	public void constructorReference_SPR12326() {
		String type = getClass().getName();
		String prefix = "new " + type + ".Obj";

		this.expression = this.parser.parseExpression(prefix + "([0])");
		assertEquals("test", ((Obj) this.expression.getValue(new Object[] {"test"})).param1);
		assertCanCompile(this.expression);
		assertEquals("test", ((Obj) this.expression.getValue(new Object[] {"test"})).param1);

		this.expression = this.parser.parseExpression(prefix + "2('foo','bar').output");
		assertEquals("foobar", this.expression.getValue(String.class));
		assertCanCompile(this.expression);
		assertEquals("foobar", this.expression.getValue(String.class));

		this.expression = this.parser.parseExpression(prefix + "2('foo').output");
		assertEquals("foo", this.expression.getValue(String.class));
		assertCanCompile(this.expression);
		assertEquals("foo", this.expression.getValue(String.class));

		this.expression = this.parser.parseExpression(prefix + "2().output");
		assertEquals("", this.expression.getValue(String.class));
		assertCanCompile(this.expression);
		assertEquals("", this.expression.getValue(String.class));

		this.expression = this.parser.parseExpression(prefix + "3(1,2,3).output");
		assertEquals("123", this.expression.getValue(String.class));
		assertCanCompile(this.expression);
		assertEquals("123", this.expression.getValue(String.class));

		this.expression = this.parser.parseExpression(prefix + "3(1).output");
		assertEquals("1", this.expression.getValue(String.class));
		assertCanCompile(this.expression);
		assertEquals("1", this.expression.getValue(String.class));

		this.expression = this.parser.parseExpression(prefix + "3().output");
		assertEquals("", this.expression.getValue(String.class));
		assertCanCompile(this.expression);
		assertEquals("", this.expression.getValue(String.class));

		this.expression = this.parser.parseExpression(prefix + "3('abc',5.0f,1,2,3).output");
		assertEquals("abc:5.0:123", this.expression.getValue(String.class));
		assertCanCompile(this.expression);
		assertEquals("abc:5.0:123", this.expression.getValue(String.class));

		this.expression = this.parser.parseExpression(prefix + "3('abc',5.0f,1).output");
		assertEquals("abc:5.0:1", this.expression.getValue(String.class));
		assertCanCompile(this.expression);
		assertEquals("abc:5.0:1", this.expression.getValue(String.class));

		this.expression = this.parser.parseExpression(prefix + "3('abc',5.0f).output");
		assertEquals("abc:5.0:", this.expression.getValue(String.class));
		assertCanCompile(this.expression);
		assertEquals("abc:5.0:", this.expression.getValue(String.class));

		this.expression = this.parser.parseExpression(prefix + "4(#root).output");
		assertEquals("123", this.expression.getValue(new int[] {1,2,3}, String.class));
		assertCanCompile(this.expression);
		assertEquals("123", this.expression.getValue(new int[] {1,2,3}, String.class));
	}

	@Test
	public void methodReferenceMissingCastAndRootObjectAccessing_SPR12326() {
		// Need boxing code on the 1 so that toString() can be called
		this.expression = this.parser.parseExpression("1.toString()");
		assertEquals("1", this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals("1", this.expression.getValue());

		this.expression = this.parser.parseExpression("#it?.age.equals([0])");
		Person person = new Person(1);
		StandardEvaluationContext context = new StandardEvaluationContext(new Object[] {person.getAge()});
		context.setVariable("it", person);
		assertTrue(this.expression.getValue(context, Boolean.class));
		assertCanCompile(this.expression);
		assertTrue(this.expression.getValue(context, Boolean.class));

		// Variant of above more like what was in the bug report:
		SpelExpressionParser parser = new SpelExpressionParser(
				new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, getClass().getClassLoader()));

		SpelExpression ex = parser.parseRaw("#it?.age.equals([0])");
		context = new StandardEvaluationContext(new Object[] {person.getAge()});
		context.setVariable("it", person);
		assertTrue(ex.getValue(context, Boolean.class));
		assertTrue(ex.getValue(context, Boolean.class));

		PersonInOtherPackage person2 = new PersonInOtherPackage(1);
		ex = parser.parseRaw("#it?.age.equals([0])");
		context = new StandardEvaluationContext(new Object[] {person2.getAge()});
		context.setVariable("it", person2);
		assertTrue(ex.getValue(context, Boolean.class));
		assertTrue(ex.getValue(context, Boolean.class));

		ex = parser.parseRaw("#it?.age.equals([0])");
		context = new StandardEvaluationContext(new Object[] {person2.getAge()});
		context.setVariable("it", person2);
		assertTrue((Boolean) ex.getValue(context));
		assertTrue((Boolean) ex.getValue(context));
	}

	@Test
	public void constructorReference() throws Exception {
		// simple ctor
		this.expression = this.parser.parseExpression("new String('123')");
		assertEquals("123", this.expression.getValue());
		assertCanCompile(this.expression);
		assertEquals("123", this.expression.getValue());

		String testclass8 = "org.springframework.expression.spel.SpelCompilationCoverageTests$TestClass8";
		// multi arg ctor that includes primitives
		this.expression = this.parser.parseExpression("new " + testclass8 + "(42,'123',4.0d,true)");
		assertEquals(testclass8, this.expression.getValue().getClass().getName());
		assertCanCompile(this.expression);
		Object o = this.expression.getValue();
		assertEquals(testclass8,o.getClass().getName());
		TestClass8 tc8 = (TestClass8) o;
		assertEquals(42, tc8.i);
		assertEquals("123", tc8.s);
		assertEquals(4.0d, tc8.d, 0.5d);
		assertEquals(true, tc8.z);

		// no-arg ctor
		this.expression = this.parser.parseExpression("new " + testclass8 + "()");
		assertEquals(testclass8, this.expression.getValue().getClass().getName());
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals(testclass8,o.getClass().getName());

		// pass primitive to reference type ctor
		this.expression = this.parser.parseExpression("new " + testclass8 + "(42)");
		assertEquals(testclass8, this.expression.getValue().getClass().getName());
		assertCanCompile(this.expression);
		o = this.expression.getValue();
		assertEquals(testclass8,o.getClass().getName());
		tc8 = (TestClass8) o;
		assertEquals(42, tc8.i);

		// private class, can't compile it
		String testclass9 = "org.springframework.expression.spel.SpelCompilationCoverageTests$TestClass9";
		this.expression = this.parser.parseExpression("new " + testclass9 + "(42)");
		assertEquals(testclass9, this.expression.getValue().getClass().getName());
		assertCantCompile(this.expression);
	}

	@Test
	public void methodReferenceReflectiveMethodSelectionWithVarargs() throws Exception {
		TestClass10 tc = new TestClass10();

		// Should call the non varargs version of concat
		// (which causes the '::' prefix in test output)
		this.expression = this.parser.parseExpression("concat('test')");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("::test", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("::test", tc.s);
		tc.reset();

		// This will call the varargs concat with an empty array
		this.expression = this.parser.parseExpression("concat()");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("", tc.s);
		tc.reset();

		// Should call the non varargs version of concat
		// (which causes the '::' prefix in test output)
		this.expression = this.parser.parseExpression("concat2('test')");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("::test", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("::test", tc.s);
		tc.reset();

		// This will call the varargs concat with an empty array
		this.expression = this.parser.parseExpression("concat2()");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("", tc.s);
		tc.reset();
	}

	@Test
	public void methodReferenceVarargs() throws Exception {
		TestClass5 tc = new TestClass5();

		// varargs string
		this.expression = this.parser.parseExpression("eleven()");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("", tc.s);
		tc.reset();

		// varargs string
		this.expression = this.parser.parseExpression("eleven('aaa')");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("aaa", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("aaa", tc.s);
		tc.reset();

		// varargs string
		this.expression = this.parser.parseExpression("eleven(stringArray)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("aaabbbccc", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("aaabbbccc", tc.s);
		tc.reset();

		// varargs string
		this.expression = this.parser.parseExpression("eleven('aaa','bbb','ccc')");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("aaabbbccc", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("aaabbbccc", tc.s);
		tc.reset();

		this.expression = this.parser.parseExpression("sixteen('aaa','bbb','ccc')");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("aaabbbccc", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("aaabbbccc", tc.s);
		tc.reset();

		// TODO Fails related to conversion service converting a String[] to satisfy Object...
//		expression = parser.parseExpression("sixteen(stringArray)");
//		assertCantCompile(expression);
//		expression.getValue(tc);
//		assertEquals("aaabbbccc", tc.s);
//		assertCanCompile(expression);
//		tc.reset();
//		expression.getValue(tc);
//		assertEquals("aaabbbccc", tc.s);
//		tc.reset();

		// varargs int
		this.expression = this.parser.parseExpression("twelve(1,2,3)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals(6, tc.i);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals(6, tc.i);
		tc.reset();

		this.expression = this.parser.parseExpression("twelve(1)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals(1, tc.i);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals(1, tc.i);
		tc.reset();

		// one string then varargs string
		this.expression = this.parser.parseExpression("thirteen('aaa','bbb','ccc')");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("aaa::bbbccc", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("aaa::bbbccc", tc.s);
		tc.reset();

		// nothing passed to varargs parameter
		this.expression = this.parser.parseExpression("thirteen('aaa')");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("aaa::", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("aaa::", tc.s);
		tc.reset();

		// nested arrays
		this.expression = this.parser.parseExpression("fourteen('aaa',stringArray,stringArray)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("aaa::{aaabbbccc}{aaabbbccc}", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("aaa::{aaabbbccc}{aaabbbccc}", tc.s);
		tc.reset();

		// nested primitive array
		this.expression = this.parser.parseExpression("fifteen('aaa',intArray,intArray)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("aaa::{112233}{112233}", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("aaa::{112233}{112233}", tc.s);
		tc.reset();

		// varargs boolean
		this.expression = this.parser.parseExpression("arrayz(true,true,false)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("truetruefalse", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("truetruefalse", tc.s);
		tc.reset();

		this.expression = this.parser.parseExpression("arrayz(true)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("true", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("true", tc.s);
		tc.reset();

		// varargs short
		this.expression = this.parser.parseExpression("arrays(s1,s2,s3)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("123", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("123", tc.s);
		tc.reset();

		this.expression = this.parser.parseExpression("arrays(s1)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("1", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("1", tc.s);
		tc.reset();

		// varargs double
		this.expression = this.parser.parseExpression("arrayd(1.0d,2.0d,3.0d)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("1.02.03.0", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("1.02.03.0", tc.s);
		tc.reset();

		this.expression = this.parser.parseExpression("arrayd(1.0d)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("1.0", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("1.0", tc.s);
		tc.reset();

		// varargs long
		this.expression = this.parser.parseExpression("arrayj(l1,l2,l3)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("123", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("123", tc.s);
		tc.reset();

		this.expression = this.parser.parseExpression("arrayj(l1)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("1", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("1", tc.s);
		tc.reset();

		// varargs char
		this.expression = this.parser.parseExpression("arrayc(c1,c2,c3)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("abc", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("abc", tc.s);
		tc.reset();

		this.expression = this.parser.parseExpression("arrayc(c1)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("a", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("a", tc.s);
		tc.reset();

		// varargs byte
		this.expression = this.parser.parseExpression("arrayb(b1,b2,b3)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("656667", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("656667", tc.s);
		tc.reset();

		this.expression = this.parser.parseExpression("arrayb(b1)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("65", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("65", tc.s);
		tc.reset();

		// varargs float
		this.expression = this.parser.parseExpression("arrayf(f1,f2,f3)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("1.02.03.0", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("1.02.03.0", tc.s);
		tc.reset();

		this.expression = this.parser.parseExpression("arrayf(f1)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("1.0", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("1.0", tc.s);
		tc.reset();
	}

	@Test
	public void methodReference() throws Exception {
		TestClass5 tc = new TestClass5();

		// non-static method, no args, void return
		this.expression = this.parser.parseExpression("one()");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals(1, tc.i);
		tc.reset();

		// static method, no args, void return
		this.expression = this.parser.parseExpression("two()");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals(1, TestClass5._i);
		tc.reset();

		// non-static method, reference type return
		this.expression = this.parser.parseExpression("three()");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertCanCompile(this.expression);
		tc.reset();
		assertEquals("hello", this.expression.getValue(tc));
		tc.reset();

		// non-static method, primitive type return
		this.expression = this.parser.parseExpression("four()");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertCanCompile(this.expression);
		tc.reset();
		assertEquals(3277700L, this.expression.getValue(tc));
		tc.reset();

		// static method, reference type return
		this.expression = this.parser.parseExpression("five()");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertCanCompile(this.expression);
		tc.reset();
		assertEquals("hello", this.expression.getValue(tc));
		tc.reset();

		// static method, primitive type return
		this.expression = this.parser.parseExpression("six()");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertCanCompile(this.expression);
		tc.reset();
		assertEquals(3277700L, this.expression.getValue(tc));
		tc.reset();

		// non-static method, one parameter of reference type
		this.expression = this.parser.parseExpression("seven(\"foo\")");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("foo", tc.s);
		tc.reset();

		// static method, one parameter of reference type
		this.expression = this.parser.parseExpression("eight(\"bar\")");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals("bar", TestClass5._s);
		tc.reset();

		// non-static method, one parameter of primitive type
		this.expression = this.parser.parseExpression("nine(231)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals(231, tc.i);
		tc.reset();

		// static method, one parameter of primitive type
		this.expression = this.parser.parseExpression("ten(111)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertCanCompile(this.expression);
		tc.reset();
		this.expression.getValue(tc);
		assertEquals(111, TestClass5._i);
		tc.reset();

		// method that gets type converted parameters

		// Converting from an int to a string
		this.expression = this.parser.parseExpression("seven(123)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("123", tc.s);
		assertCantCompile(this.expression); // Uncompilable as argument conversion is occurring

		Expression expression = this.parser.parseExpression("'abcd'.substring(index1,index2)");
		String resultI = expression.getValue(new TestClass1(), String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(), String.class);
		assertEquals("bc", resultI);
		assertEquals("bc", resultC);

		// Converting from an int to a Number
		expression = this.parser.parseExpression("takeNumber(123)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("123", tc.s);
		tc.reset();
		assertCanCompile(expression); // The generated code should include boxing of the int to a Number
		expression.getValue(tc);
		assertEquals("123", tc.s);

		// Passing a subtype
		expression = this.parser.parseExpression("takeNumber(T(Integer).valueOf(42))");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("42", tc.s);
		tc.reset();
		assertCanCompile(expression); // The generated code should include boxing of the int to a Number
		expression.getValue(tc);
		assertEquals("42", tc.s);

		// Passing a subtype
		expression = this.parser.parseExpression("takeString(T(Integer).valueOf(42))");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("42", tc.s);
		tc.reset();
		assertCantCompile(expression); // method takes a string and we are passing an Integer
	}

	@Test
	public void errorHandling() throws Exception {
		TestClass5 tc = new TestClass5();

		// changing target

		// from primitive array to reference type array
		int[] is = new int[] {1,2,3};
		String[] strings = new String[] {"a","b","c"};
		this.expression = this.parser.parseExpression("[1]");
		assertEquals(2, this.expression.getValue(is));
		assertCanCompile(this.expression);
		assertEquals(2, this.expression.getValue(is));

		try {
			assertEquals(2, this.expression.getValue(strings));
			fail();
		}
		catch (SpelEvaluationException see) {
			assertTrue(see.getCause() instanceof ClassCastException);
		}
		SpelCompiler.revertToInterpreted(this.expression);
		assertEquals("b", this.expression.getValue(strings));
		assertCanCompile(this.expression);
		assertEquals("b", this.expression.getValue(strings));


		tc.field = "foo";
		this.expression = this.parser.parseExpression("seven(field)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("foo", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		tc.field="bar";
		this.expression.getValue(tc);

		// method with changing parameter types (change reference type)
		tc.obj = "foo";
		this.expression = this.parser.parseExpression("seven(obj)");
		assertCantCompile(this.expression);
		this.expression.getValue(tc);
		assertEquals("foo", tc.s);
		assertCanCompile(this.expression);
		tc.reset();
		tc.obj=new Integer(42);
		try {
			this.expression.getValue(tc);
			fail();
		}
		catch (SpelEvaluationException see) {
			assertTrue(see.getCause() instanceof ClassCastException);
		}

		// method with changing target
		this.expression = this.parser.parseExpression("#root.charAt(0)");
		assertEquals('a', this.expression.getValue("abc"));
		assertCanCompile(this.expression);
		try {
			this.expression.getValue(new Integer(42));
			fail();
		}
		catch (SpelEvaluationException see) {
			// java.lang.Integer cannot be cast to java.lang.String
			assertTrue(see.getCause() instanceof ClassCastException);
		}
	}

	@Test
	public void methodReference_staticMethod() throws Exception {
		Expression expression = this.parser.parseExpression("T(Integer).valueOf(42)");
		int resultI = expression.getValue(new TestClass1(), Integer.TYPE);
		assertCanCompile(expression);
		int resultC = expression.getValue(new TestClass1(), Integer.TYPE);
		assertEquals(42, resultI);
		assertEquals(42, resultC);
	}

	@Test
	public void methodReference_literalArguments_int() throws Exception {
		Expression expression = this.parser.parseExpression("'abcd'.substring(1,3)");
		String resultI = expression.getValue(new TestClass1(), String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(), String.class);
		assertEquals("bc", resultI);
		assertEquals("bc", resultC);
	}

	@Test
	public void methodReference_simpleInstanceMethodNoArg() throws Exception {
		Expression expression = this.parser.parseExpression("toString()");
		String resultI = expression.getValue(42, String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(42, String.class);
		assertEquals("42", resultI);
		assertEquals("42", resultC);
	}

	@Test
	public void methodReference_simpleInstanceMethodNoArgReturnPrimitive() throws Exception {
		this.expression = this.parser.parseExpression("intValue()");
		int resultI = this.expression.getValue(new Integer(42), Integer.TYPE);
		assertEquals(42, resultI);
		assertCanCompile(this.expression);
		int resultC = this.expression.getValue(new Integer(42), Integer.TYPE);
		assertEquals(42, resultC);
	}

	@Test
	public void methodReference_simpleInstanceMethodOneArgReturnPrimitive1() throws Exception {
		Expression expression = this.parser.parseExpression("indexOf('b')");
		int resultI = expression.getValue("abc", Integer.TYPE);
		assertCanCompile(expression);
		int resultC = expression.getValue("abc", Integer.TYPE);
		assertEquals(1, resultI);
		assertEquals(1, resultC);
	}

	@Test
	public void methodReference_simpleInstanceMethodOneArgReturnPrimitive2() throws Exception {
		this.expression = this.parser.parseExpression("charAt(2)");
		char resultI = this.expression.getValue("abc", Character.TYPE);
		assertEquals('c', resultI);
		assertCanCompile(this.expression);
		char resultC = this.expression.getValue("abc", Character.TYPE);
		assertEquals('c', resultC);
	}

	@Test
	public void compoundExpression() throws Exception {
		Payload payload = new Payload();
		this.expression = this.parser.parseExpression("DR[0]");
		assertEquals("instanceof Two", this.expression.getValue(payload).toString());
		assertCanCompile(this.expression);
		assertEquals("instanceof Two", this.expression.getValue(payload).toString());
		this.ast = getAst();
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Two", this.ast.getExitDescriptor());

		this.expression = this.parser.parseExpression("holder.three");
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Three", this.expression.getValue(payload).getClass().getName());
		assertCanCompile(this.expression);
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Three", this.expression.getValue(payload).getClass().getName());
		this.ast = getAst();
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Three", this.ast.getExitDescriptor());

		this.expression = this.parser.parseExpression("DR[0]");
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Two", this.expression.getValue(payload).getClass().getName());
		assertCanCompile(this.expression);
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Two", this.expression.getValue(payload).getClass().getName());
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Two", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("DR[0].three");
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Three", this.expression.getValue(payload).getClass().getName());
		assertCanCompile(this.expression);
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Three", this.expression.getValue(payload).getClass().getName());
		this.ast = getAst();
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Three", this.ast.getExitDescriptor());

		this.expression = this.parser.parseExpression("DR[0].three.four");
		assertEquals(0.04d, this.expression.getValue(payload));
		assertCanCompile(this.expression);
		assertEquals(0.04d, this.expression.getValue(payload));
		assertEquals("D", getAst().getExitDescriptor());
	}

	@Test
	public void mixingItUp_indexerOpEqTernary() throws Exception {
		Map<String, String> m = new HashMap<>();
		m.put("andy","778");

		this.expression = parse("['andy']==null?1:2");
		assertEquals(2, this.expression.getValue(m));
		assertCanCompile(this.expression);
		assertEquals(2, this.expression.getValue(m));
		m.remove("andy");
		assertEquals(1, this.expression.getValue(m));
	}

	@Test
	public void propertyReference() throws Exception {
		TestClass6 tc = new TestClass6();

		// non static field
		this.expression = this.parser.parseExpression("orange");
		assertCantCompile(this.expression);
		assertEquals("value1", this.expression.getValue(tc));
		assertCanCompile(this.expression);
		assertEquals("value1", this.expression.getValue(tc));

		// static field
		this.expression = this.parser.parseExpression("apple");
		assertCantCompile(this.expression);
		assertEquals("value2", this.expression.getValue(tc));
		assertCanCompile(this.expression);
		assertEquals("value2", this.expression.getValue(tc));

		// non static getter
		this.expression = this.parser.parseExpression("banana");
		assertCantCompile(this.expression);
		assertEquals("value3", this.expression.getValue(tc));
		assertCanCompile(this.expression);
		assertEquals("value3", this.expression.getValue(tc));

		// static getter
		this.expression = this.parser.parseExpression("plum");
		assertCantCompile(this.expression);
		assertEquals("value4", this.expression.getValue(tc));
		assertCanCompile(this.expression);
		assertEquals("value4", this.expression.getValue(tc));
	}

	@Test
	public void propertyReferenceVisibility() { // SPR-12771
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("httpServletRequest", HttpServlet3RequestFactory.getOne());
		// Without a fix compilation was inserting a checkcast to a private type
		this.expression = this.parser.parseExpression("#httpServletRequest.servletPath");
		assertEquals("wibble", this.expression.getValue(ctx));
		assertCanCompile(this.expression);
		assertEquals("wibble", this.expression.getValue(ctx));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void indexer() throws Exception {
		String[] sss = new String[] {"a","b","c"};
		Number[] ns = new Number[] {2,8,9};
		int[] is = new int[] {8,9,10};
		double[] ds = new double[] {3.0d,4.0d,5.0d};
		long[] ls = new long[] {2L,3L,4L};
		short[] ss = new short[] {(short)33,(short)44,(short)55};
		float[] fs = new float[] {6.0f,7.0f,8.0f};
		byte[] bs = new byte[] {(byte)2,(byte)3,(byte)4};
		char[] cs = new char[] {'a','b','c'};

		// Access String (reference type) array
		this.expression = this.parser.parseExpression("[0]");
		assertEquals("a", this.expression.getValue(sss));
		assertCanCompile(this.expression);
		assertEquals("a", this.expression.getValue(sss));
		assertEquals("Ljava/lang/String", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("[1]");
		assertEquals(8, this.expression.getValue(ns));
		assertCanCompile(this.expression);
		assertEquals(8, this.expression.getValue(ns));
		assertEquals("Ljava/lang/Number", getAst().getExitDescriptor());

		// Access int array
		this.expression = this.parser.parseExpression("[2]");
		assertEquals(10, this.expression.getValue(is));
		assertCanCompile(this.expression);
		assertEquals(10, this.expression.getValue(is));
		assertEquals("I", getAst().getExitDescriptor());

		// Access double array
		this.expression = this.parser.parseExpression("[1]");
		assertEquals(4.0d, this.expression.getValue(ds));
		assertCanCompile(this.expression);
		assertEquals(4.0d, this.expression.getValue(ds));
		assertEquals("D", getAst().getExitDescriptor());

		// Access long array
		this.expression = this.parser.parseExpression("[0]");
		assertEquals(2L, this.expression.getValue(ls));
		assertCanCompile(this.expression);
		assertEquals(2L, this.expression.getValue(ls));
		assertEquals("J", getAst().getExitDescriptor());

		// Access short array
		this.expression = this.parser.parseExpression("[2]");
		assertEquals((short)55, this.expression.getValue(ss));
		assertCanCompile(this.expression);
		assertEquals((short)55, this.expression.getValue(ss));
		assertEquals("S", getAst().getExitDescriptor());

		// Access float array
		this.expression = this.parser.parseExpression("[0]");
		assertEquals(6.0f, this.expression.getValue(fs));
		assertCanCompile(this.expression);
		assertEquals(6.0f, this.expression.getValue(fs));
		assertEquals("F", getAst().getExitDescriptor());

		// Access byte array
		this.expression = this.parser.parseExpression("[2]");
		assertEquals((byte)4, this.expression.getValue(bs));
		assertCanCompile(this.expression);
		assertEquals((byte)4, this.expression.getValue(bs));
		assertEquals("B", getAst().getExitDescriptor());

		// Access char array
		this.expression = this.parser.parseExpression("[1]");
		assertEquals('b', this.expression.getValue(cs));
		assertCanCompile(this.expression);
		assertEquals('b', this.expression.getValue(cs));
		assertEquals("C", getAst().getExitDescriptor());

		// Collections
		List<String> strings = new ArrayList<>();
		strings.add("aaa");
		strings.add("bbb");
		strings.add("ccc");
		this.expression = this.parser.parseExpression("[1]");
		assertEquals("bbb", this.expression.getValue(strings));
		assertCanCompile(this.expression);
		assertEquals("bbb", this.expression.getValue(strings));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());

		List<Integer> ints = new ArrayList<>();
		ints.add(123);
		ints.add(456);
		ints.add(789);
		this.expression = this.parser.parseExpression("[2]");
		assertEquals(789, this.expression.getValue(ints));
		assertCanCompile(this.expression);
		assertEquals(789, this.expression.getValue(ints));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());

		// Maps
		Map<String, Integer> map1 = new HashMap<>();
		map1.put("aaa", 111);
		map1.put("bbb", 222);
		map1.put("ccc", 333);
		this.expression = this.parser.parseExpression("['aaa']");
		assertEquals(111, this.expression.getValue(map1));
		assertCanCompile(this.expression);
		assertEquals(111, this.expression.getValue(map1));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());

		// Object
		TestClass6 tc = new TestClass6();
		this.expression = this.parser.parseExpression("['orange']");
		assertEquals("value1", this.expression.getValue(tc));
		assertCanCompile(this.expression);
		assertEquals("value1", this.expression.getValue(tc));
		assertEquals("Ljava/lang/String", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("['peach']");
		assertEquals(34L, this.expression.getValue(tc));
		assertCanCompile(this.expression);
		assertEquals(34L, this.expression.getValue(tc));
		assertEquals("J", getAst().getExitDescriptor());

		// getter
		this.expression = this.parser.parseExpression("['banana']");
		assertEquals("value3", this.expression.getValue(tc));
		assertCanCompile(this.expression);
		assertEquals("value3", this.expression.getValue(tc));
		assertEquals("Ljava/lang/String", getAst().getExitDescriptor());

		// list of arrays

		List<String[]> listOfStringArrays = new ArrayList<>();
		listOfStringArrays.add(new String[] {"a","b","c"});
		listOfStringArrays.add(new String[] {"d","e","f"});
		this.expression = this.parser.parseExpression("[1]");
		assertEquals("d e f", stringify(this.expression.getValue(listOfStringArrays)));
		assertCanCompile(this.expression);
		assertEquals("d e f", stringify(this.expression.getValue(listOfStringArrays)));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("[1][0]");
		assertEquals("d", stringify(this.expression.getValue(listOfStringArrays)));
		assertCanCompile(this.expression);
		assertEquals("d", stringify(this.expression.getValue(listOfStringArrays)));
		assertEquals("Ljava/lang/String", getAst().getExitDescriptor());

		List<Integer[]> listOfIntegerArrays = new ArrayList<>();
		listOfIntegerArrays.add(new Integer[] {1,2,3});
		listOfIntegerArrays.add(new Integer[] {4,5,6});
		this.expression = this.parser.parseExpression("[0]");
		assertEquals("1 2 3", stringify(this.expression.getValue(listOfIntegerArrays)));
		assertCanCompile(this.expression);
		assertEquals("1 2 3", stringify(this.expression.getValue(listOfIntegerArrays)));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("[0][1]");
		assertEquals(2, this.expression.getValue(listOfIntegerArrays));
		assertCanCompile(this.expression);
		assertEquals(2, this.expression.getValue(listOfIntegerArrays));
		assertEquals("Ljava/lang/Integer", getAst().getExitDescriptor());

		// array of lists
		List<String>[] stringArrayOfLists = new ArrayList[2];
		stringArrayOfLists[0] = new ArrayList<>();
		stringArrayOfLists[0].add("a");
		stringArrayOfLists[0].add("b");
		stringArrayOfLists[0].add("c");
		stringArrayOfLists[1] = new ArrayList<>();
		stringArrayOfLists[1].add("d");
		stringArrayOfLists[1].add("e");
		stringArrayOfLists[1].add("f");
		this.expression = this.parser.parseExpression("[1]");
		assertEquals("d e f", stringify(this.expression.getValue(stringArrayOfLists)));
		assertCanCompile(this.expression);
		assertEquals("d e f", stringify(this.expression.getValue(stringArrayOfLists)));
		assertEquals("Ljava/util/ArrayList", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("[1][2]");
		assertEquals("f", stringify(this.expression.getValue(stringArrayOfLists)));
		assertCanCompile(this.expression);
		assertEquals("f", stringify(this.expression.getValue(stringArrayOfLists)));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());

		// array of arrays
		String[][] referenceTypeArrayOfArrays = new String[][] {new String[] {"a","b","c"},new String[] {"d","e","f"}};
		this.expression = this.parser.parseExpression("[1]");
		assertEquals("d e f", stringify(this.expression.getValue(referenceTypeArrayOfArrays)));
		assertCanCompile(this.expression);
		assertEquals("[Ljava/lang/String", getAst().getExitDescriptor());
		assertEquals("d e f", stringify(this.expression.getValue(referenceTypeArrayOfArrays)));
		assertEquals("[Ljava/lang/String", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("[1][2]");
		assertEquals("f", stringify(this.expression.getValue(referenceTypeArrayOfArrays)));
		assertCanCompile(this.expression);
		assertEquals("f", stringify(this.expression.getValue(referenceTypeArrayOfArrays)));
		assertEquals("Ljava/lang/String", getAst().getExitDescriptor());

		int[][] primitiveTypeArrayOfArrays = new int[][] {new int[] {1,2,3},new int[] {4,5,6}};
		this.expression = this.parser.parseExpression("[1]");
		assertEquals("4 5 6", stringify(this.expression.getValue(primitiveTypeArrayOfArrays)));
		assertCanCompile(this.expression);
		assertEquals("4 5 6", stringify(this.expression.getValue(primitiveTypeArrayOfArrays)));
		assertEquals("[I", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("[1][2]");
		assertEquals("6", stringify(this.expression.getValue(primitiveTypeArrayOfArrays)));
		assertCanCompile(this.expression);
		assertEquals("6", stringify(this.expression.getValue(primitiveTypeArrayOfArrays)));
		assertEquals("I", getAst().getExitDescriptor());

		// list of lists of reference types
		List<List<String>> listOfListOfStrings = new ArrayList<>();
		List<String> list = new ArrayList<>();
		list.add("a");
		list.add("b");
		list.add("c");
		listOfListOfStrings.add(list);
		list = new ArrayList<>();
		list.add("d");
		list.add("e");
		list.add("f");
		listOfListOfStrings.add(list);

		this.expression = this.parser.parseExpression("[1]");
		assertEquals("d e f", stringify(this.expression.getValue(listOfListOfStrings)));
		assertCanCompile(this.expression);
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());
		assertEquals("d e f", stringify(this.expression.getValue(listOfListOfStrings)));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("[1][2]");
		assertEquals("f", stringify(this.expression.getValue(listOfListOfStrings)));
		assertCanCompile(this.expression);
		assertEquals("f", stringify(this.expression.getValue(listOfListOfStrings)));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());

		// Map of lists
		Map<String,List<String>> mapToLists = new HashMap<>();
		list = new ArrayList<>();
		list.add("a");
		list.add("b");
		list.add("c");
		mapToLists.put("foo", list);
		this.expression = this.parser.parseExpression("['foo']");
		assertEquals("a b c", stringify(this.expression.getValue(mapToLists)));
		assertCanCompile(this.expression);
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());
		assertEquals("a b c", stringify(this.expression.getValue(mapToLists)));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("['foo'][2]");
		assertEquals("c", stringify(this.expression.getValue(mapToLists)));
		assertCanCompile(this.expression);
		assertEquals("c", stringify(this.expression.getValue(mapToLists)));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());

		// Map to array
		Map<String,int[]> mapToIntArray = new HashMap<>();
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.addPropertyAccessor(new CompilableMapAccessor());
		mapToIntArray.put("foo",new int[] {1,2,3});
		this.expression = this.parser.parseExpression("['foo']");
		assertEquals("1 2 3", stringify(this.expression.getValue(mapToIntArray)));
		assertCanCompile(this.expression);
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());
		assertEquals("1 2 3", stringify(this.expression.getValue(mapToIntArray)));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("['foo'][1]");
		assertEquals(2, this.expression.getValue(mapToIntArray));
		assertCanCompile(this.expression);
		assertEquals(2, this.expression.getValue(mapToIntArray));

		this.expression = this.parser.parseExpression("foo");
		assertEquals("1 2 3", stringify(this.expression.getValue(ctx, mapToIntArray)));
		assertCanCompile(this.expression);
		assertEquals("1 2 3", stringify(this.expression.getValue(ctx, mapToIntArray)));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("foo[1]");
		assertEquals(2, this.expression.getValue(ctx, mapToIntArray));
		assertCanCompile(this.expression);
		assertEquals(2, this.expression.getValue(ctx, mapToIntArray));

		this.expression = this.parser.parseExpression("['foo'][2]");
		assertEquals("3", stringify(this.expression.getValue(ctx, mapToIntArray)));
		assertCanCompile(this.expression);
		assertEquals("3", stringify(this.expression.getValue(ctx, mapToIntArray)));
		assertEquals("I", getAst().getExitDescriptor());

		// Map array
		Map<String, String>[] mapArray = new Map[1];
		mapArray[0] = new HashMap<>();
		mapArray[0].put("key", "value1");
		this.expression = this.parser.parseExpression("[0]");
		assertEquals("{key=value1}", stringify(this.expression.getValue(mapArray)));
		assertCanCompile(this.expression);
		assertEquals("Ljava/util/Map", getAst().getExitDescriptor());
		assertEquals("{key=value1}", stringify(this.expression.getValue(mapArray)));
		assertEquals("Ljava/util/Map", getAst().getExitDescriptor());

		this.expression = this.parser.parseExpression("[0]['key']");
		assertEquals("value1", stringify(this.expression.getValue(mapArray)));
		assertCanCompile(this.expression);
		assertEquals("value1", stringify(this.expression.getValue(mapArray)));
		assertEquals("Ljava/lang/Object", getAst().getExitDescriptor());
	}

	@Test
	public void plusNeedingCheckcast_SPR12426() {
		this.expression = this.parser.parseExpression("object + ' world'");
		Object v = this.expression.getValue(new FooObject());
		assertEquals("hello world", v);
		assertCanCompile(this.expression);
		assertEquals("hello world", v);

		this.expression = this.parser.parseExpression("object + ' world'");
		v = this.expression.getValue(new FooString());
		assertEquals("hello world", v);
		assertCanCompile(this.expression);
		assertEquals("hello world", v);
	}

	@Test
	public void mixingItUp_propertyAccessIndexerOpLtTernaryRootNull() throws Exception {
		Payload payload = new Payload();

		this.expression = this.parser.parseExpression("DR[0].three");
		Object v = this.expression.getValue(payload);
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Three",
				getAst().getExitDescriptor());

		Expression expression = this.parser.parseExpression("DR[0].three.four lt 0.1d?#root:null");
		v = expression.getValue(payload);

		SpelExpression sExpr = (SpelExpression) expression;
		Ternary ternary = (Ternary) sExpr.getAST();
		OpLT oplt = (OpLT) ternary.getChild(0);
		CompoundExpression cExpr = (CompoundExpression) oplt.getLeftOperand();
		String cExprExitDescriptor = cExpr.getExitDescriptor();
		assertEquals("D", cExprExitDescriptor);
		assertEquals("Z", oplt.getExitDescriptor());

		assertCanCompile(expression);
		Object vc = expression.getValue(payload);
		assertEquals(payload, v);
		assertEquals(payload,vc);
		payload.DR[0].three.four = 0.13d;
		vc = expression.getValue(payload);
		assertNull(vc);
	}

	@Test
	public void variantGetter() throws Exception {
		Payload2Holder holder = new Payload2Holder();
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.addPropertyAccessor(new MyAccessor());
		this.expression = this.parser.parseExpression("payload2.var1");
		Object v = this.expression.getValue(ctx,holder);
		assertEquals("abc", v);

		//	// time it interpreted
		//	long stime = System.currentTimeMillis();
		//	for (int i = 0; i < 100000; i++) {
		//		v = expression.getValue(ctx,holder);
		//	}
		//	System.out.println((System.currentTimeMillis() - stime));

		assertCanCompile(this.expression);
		v = this.expression.getValue(ctx,holder);
		assertEquals("abc", v);

		//	// time it compiled
		//	stime = System.currentTimeMillis();
		//	for (int i = 0; i < 100000; i++) {
		//		v = expression.getValue(ctx,holder);
		//	}
		//	System.out.println((System.currentTimeMillis() - stime));
	}

	@Test
	public void compilerWithGenerics_12040() {
		this.expression = this.parser.parseExpression("payload!=2");
		assertTrue(this.expression.getValue(new GenericMessageTestHelper<>(4), Boolean.class));
		assertCanCompile(this.expression);
		assertFalse(this.expression.getValue(new GenericMessageTestHelper<>(2), Boolean.class));

		this.expression = this.parser.parseExpression("2!=payload");
		assertTrue(this.expression.getValue(new GenericMessageTestHelper<>(4), Boolean.class));
		assertCanCompile(this.expression);
		assertFalse(this.expression.getValue(new GenericMessageTestHelper<>(2), Boolean.class));

		this.expression = this.parser.parseExpression("payload!=6L");
		assertTrue(this.expression.getValue(new GenericMessageTestHelper<>(4L), Boolean.class));
		assertCanCompile(this.expression);
		assertFalse(this.expression.getValue(new GenericMessageTestHelper<>(6L), Boolean.class));

		this.expression = this.parser.parseExpression("payload==2");
		assertFalse(this.expression.getValue(new GenericMessageTestHelper<>(4), Boolean.class));
		assertCanCompile(this.expression);
		assertTrue(this.expression.getValue(new GenericMessageTestHelper<>(2), Boolean.class));

		this.expression = this.parser.parseExpression("2==payload");
		assertFalse(this.expression.getValue(new GenericMessageTestHelper<>(4), Boolean.class));
		assertCanCompile(this.expression);
		assertTrue(this.expression.getValue(new GenericMessageTestHelper<>(2), Boolean.class));

		this.expression = this.parser.parseExpression("payload==6L");
		assertFalse(this.expression.getValue(new GenericMessageTestHelper<>(4L), Boolean.class));
		assertCanCompile(this.expression);
		assertTrue(this.expression.getValue(new GenericMessageTestHelper<>(6L), Boolean.class));

		this.expression = this.parser.parseExpression("2==payload");
		assertFalse(this.expression.getValue(new GenericMessageTestHelper<>(4), Boolean.class));
		assertCanCompile(this.expression);
		assertTrue(this.expression.getValue(new GenericMessageTestHelper<>(2), Boolean.class));

		this.expression = this.parser.parseExpression("payload/2");
		assertEquals(2, this.expression.getValue(new GenericMessageTestHelper<>(4)));
		assertCanCompile(this.expression);
		assertEquals(3, this.expression.getValue(new GenericMessageTestHelper<>(6)));

		this.expression = this.parser.parseExpression("100/payload");
		assertEquals(25, this.expression.getValue(new GenericMessageTestHelper<>(4)));
		assertCanCompile(this.expression);
		assertEquals(10, this.expression.getValue(new GenericMessageTestHelper<>(10)));

		this.expression = this.parser.parseExpression("payload+2");
		assertEquals(6, this.expression.getValue(new GenericMessageTestHelper<>(4)));
		assertCanCompile(this.expression);
		assertEquals(8, this.expression.getValue(new GenericMessageTestHelper<>(6)));

		this.expression = this.parser.parseExpression("100+payload");
		assertEquals(104, this.expression.getValue(new GenericMessageTestHelper<>(4)));
		assertCanCompile(this.expression);
		assertEquals(110, this.expression.getValue(new GenericMessageTestHelper<>(10)));

		this.expression = this.parser.parseExpression("payload-2");
		assertEquals(2, this.expression.getValue(new GenericMessageTestHelper<>(4)));
		assertCanCompile(this.expression);
		assertEquals(4, this.expression.getValue(new GenericMessageTestHelper<>(6)));

		this.expression = this.parser.parseExpression("100-payload");
		assertEquals(96, this.expression.getValue(new GenericMessageTestHelper<>(4)));
		assertCanCompile(this.expression);
		assertEquals(90, this.expression.getValue(new GenericMessageTestHelper<>(10)));

		this.expression = this.parser.parseExpression("payload*2");
		assertEquals(8, this.expression.getValue(new GenericMessageTestHelper<>(4)));
		assertCanCompile(this.expression);
		assertEquals(12, this.expression.getValue(new GenericMessageTestHelper<>(6)));

		this.expression = this.parser.parseExpression("100*payload");
		assertEquals(400, this.expression.getValue(new GenericMessageTestHelper<>(4)));
		assertCanCompile(this.expression);
		assertEquals(1000, this.expression.getValue(new GenericMessageTestHelper<>(10)));

		this.expression = this.parser.parseExpression("payload/2L");
		assertEquals(2L, this.expression.getValue(new GenericMessageTestHelper<>(4L)));
		assertCanCompile(this.expression);
		assertEquals(3L, this.expression.getValue(new GenericMessageTestHelper<>(6L)));

		this.expression = this.parser.parseExpression("100L/payload");
		assertEquals(25L, this.expression.getValue(new GenericMessageTestHelper<>(4L)));
		assertCanCompile(this.expression);
		assertEquals(10L, this.expression.getValue(new GenericMessageTestHelper<>(10L)));

		this.expression = this.parser.parseExpression("payload/2f");
		assertEquals(2f, this.expression.getValue(new GenericMessageTestHelper<>(4f)));
		assertCanCompile(this.expression);
		assertEquals(3f, this.expression.getValue(new GenericMessageTestHelper<>(6f)));

		this.expression = this.parser.parseExpression("100f/payload");
		assertEquals(25f, this.expression.getValue(new GenericMessageTestHelper<>(4f)));
		assertCanCompile(this.expression);
		assertEquals(10f, this.expression.getValue(new GenericMessageTestHelper<>(10f)));

		this.expression = this.parser.parseExpression("payload/2d");
		assertEquals(2d, this.expression.getValue(new GenericMessageTestHelper<>(4d)));
		assertCanCompile(this.expression);
		assertEquals(3d, this.expression.getValue(new GenericMessageTestHelper<>(6d)));

		this.expression = this.parser.parseExpression("100d/payload");
		assertEquals(25d, this.expression.getValue(new GenericMessageTestHelper<>(4d)));
		assertCanCompile(this.expression);
		assertEquals(10d, this.expression.getValue(new GenericMessageTestHelper<>(10d)));
	}

	// The new helper class here uses an upper bound on the generic
	@Test
	public void compilerWithGenerics_12040_2() {
		this.expression = this.parser.parseExpression("payload/2");
		assertEquals(2, this.expression.getValue(new GenericMessageTestHelper2<>(4)));
		assertCanCompile(this.expression);
		assertEquals(3, this.expression.getValue(new GenericMessageTestHelper2<>(6)));

		this.expression = this.parser.parseExpression("9/payload");
		assertEquals(1, this.expression.getValue(new GenericMessageTestHelper2<>(9)));
		assertCanCompile(this.expression);
		assertEquals(3, this.expression.getValue(new GenericMessageTestHelper2<>(3)));

		this.expression = this.parser.parseExpression("payload+2");
		assertEquals(6, this.expression.getValue(new GenericMessageTestHelper2<>(4)));
		assertCanCompile(this.expression);
		assertEquals(8, this.expression.getValue(new GenericMessageTestHelper2<>(6)));

		this.expression = this.parser.parseExpression("100+payload");
		assertEquals(104, this.expression.getValue(new GenericMessageTestHelper2<>(4)));
		assertCanCompile(this.expression);
		assertEquals(110, this.expression.getValue(new GenericMessageTestHelper2<>(10)));

		this.expression = this.parser.parseExpression("payload-2");
		assertEquals(2, this.expression.getValue(new GenericMessageTestHelper2<>(4)));
		assertCanCompile(this.expression);
		assertEquals(4, this.expression.getValue(new GenericMessageTestHelper2<>(6)));

		this.expression = this.parser.parseExpression("100-payload");
		assertEquals(96, this.expression.getValue(new GenericMessageTestHelper2<>(4)));
		assertCanCompile(this.expression);
		assertEquals(90, this.expression.getValue(new GenericMessageTestHelper2<>(10)));

		this.expression = this.parser.parseExpression("payload*2");
		assertEquals(8, this.expression.getValue(new GenericMessageTestHelper2<>(4)));
		assertCanCompile(this.expression);
		assertEquals(12, this.expression.getValue(new GenericMessageTestHelper2<>(6)));

		this.expression = this.parser.parseExpression("100*payload");
		assertEquals(400, this.expression.getValue(new GenericMessageTestHelper2<>(4)));
		assertCanCompile(this.expression);
		assertEquals(1000, this.expression.getValue(new GenericMessageTestHelper2<>(10)));
	}

	// The other numeric operators
	@Test
	public void compilerWithGenerics_12040_3() {
		this.expression = this.parser.parseExpression("payload >= 2");
		assertTrue(this.expression.getValue(new GenericMessageTestHelper2<>(4), Boolean.TYPE));
		assertCanCompile(this.expression);
		assertFalse(this.expression.getValue(new GenericMessageTestHelper2<>(1), Boolean.TYPE));

		this.expression = this.parser.parseExpression("2 >= payload");
		assertFalse(this.expression.getValue(new GenericMessageTestHelper2<>(5), Boolean.TYPE));
		assertCanCompile(this.expression);
		assertTrue(this.expression.getValue(new GenericMessageTestHelper2<>(1), Boolean.TYPE));

		this.expression = this.parser.parseExpression("payload > 2");
		assertTrue(this.expression.getValue(new GenericMessageTestHelper2<>(4), Boolean.TYPE));
		assertCanCompile(this.expression);
		assertFalse(this.expression.getValue(new GenericMessageTestHelper2<>(1), Boolean.TYPE));

		this.expression = this.parser.parseExpression("2 > payload");
		assertFalse(this.expression.getValue(new GenericMessageTestHelper2<>(5), Boolean.TYPE));
		assertCanCompile(this.expression);
		assertTrue(this.expression.getValue(new GenericMessageTestHelper2<>(1), Boolean.TYPE));

		this.expression = this.parser.parseExpression("payload <=2");
		assertTrue(this.expression.getValue(new GenericMessageTestHelper2<>(1), Boolean.TYPE));
		assertCanCompile(this.expression);
		assertFalse(this.expression.getValue(new GenericMessageTestHelper2<>(6), Boolean.TYPE));

		this.expression = this.parser.parseExpression("2 <= payload");
		assertFalse(this.expression.getValue(new GenericMessageTestHelper2<>(1), Boolean.TYPE));
		assertCanCompile(this.expression);
		assertTrue(this.expression.getValue(new GenericMessageTestHelper2<>(6), Boolean.TYPE));

		this.expression = this.parser.parseExpression("payload < 2");
		assertTrue(this.expression.getValue(new GenericMessageTestHelper2<>(1), Boolean.TYPE));
		assertCanCompile(this.expression);
		assertFalse(this.expression.getValue(new GenericMessageTestHelper2<>(6), Boolean.TYPE));

		this.expression = this.parser.parseExpression("2 < payload");
		assertFalse(this.expression.getValue(new GenericMessageTestHelper2<>(1), Boolean.TYPE));
		assertCanCompile(this.expression);
		assertTrue(this.expression.getValue(new GenericMessageTestHelper2<>(6), Boolean.TYPE));
	}

	@Test
	public void indexerMapAccessor_12045() throws Exception {
		SpelParserConfiguration spc = new SpelParserConfiguration(
				SpelCompilerMode.IMMEDIATE,getClass().getClassLoader());
		SpelExpressionParser sep = new SpelExpressionParser(spc);
		this.expression=sep.parseExpression("headers[command]");
		MyMessage root = new MyMessage();
		assertEquals("wibble", this.expression.getValue(root));
		// This next call was failing because the isCompilable check in Indexer
		// did not check on the key being compilable (and also generateCode in the
		// Indexer was missing the optimization that it didn't need necessarily
		// need to call generateCode for that accessor)
		assertEquals("wibble", this.expression.getValue(root));
		assertCanCompile(this.expression);

		// What about a map key that is an expression - ensure the getKey() is evaluated in the right scope
		this.expression=sep.parseExpression("headers[getKey()]");
		assertEquals("wobble", this.expression.getValue(root));
		assertEquals("wobble", this.expression.getValue(root));

		this.expression=sep.parseExpression("list[getKey2()]");
		assertEquals("wobble", this.expression.getValue(root));
		assertEquals("wobble", this.expression.getValue(root));

		this.expression = sep.parseExpression("ia[getKey2()]");
		assertEquals(3, this.expression.getValue(root));
		assertEquals(3, this.expression.getValue(root));
	}

	@Test
	public void elvisOperator_SPR15192() {
		SpelParserConfiguration configuration = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null);
		Expression exp;

		exp = new SpelExpressionParser(configuration).parseExpression("bar()");
		assertEquals("BAR", exp.getValue(new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("BAR", exp.getValue(new Foo(), String.class));
		assertIsCompiled(exp);

		exp = new SpelExpressionParser(configuration).parseExpression("bar('baz')");
		assertEquals("BAZ", exp.getValue(new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("BAZ", exp.getValue(new Foo(), String.class));
		assertIsCompiled(exp);

		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("map", Collections.singletonMap("foo", "qux"));

		exp = new SpelExpressionParser(configuration).parseExpression("bar(#map['foo'])");
		assertEquals("QUX", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("QUX", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		exp = new SpelExpressionParser(configuration).parseExpression("bar(#map['foo'] ?: 'qux')");
		assertEquals("QUX", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("QUX", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		// When the condition is a primitive
		exp = new SpelExpressionParser(configuration).parseExpression("3?:'foo'");
		assertEquals("3", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("3", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		// When the condition is a double slot primitive
		exp = new SpelExpressionParser(configuration).parseExpression("3L?:'foo'");
		assertEquals("3", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("3", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		// When the condition is an empty string
		exp = new SpelExpressionParser(configuration).parseExpression("''?:4L");
		assertEquals("4", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("4", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		// null condition
		exp = new SpelExpressionParser(configuration).parseExpression("null?:4L");
		assertEquals("4", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("4", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		// variable access returning primitive
		exp = new SpelExpressionParser(configuration).parseExpression("#x?:'foo'");
		context.setVariable("x",50);
		assertEquals("50", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("50", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		exp = new SpelExpressionParser(configuration).parseExpression("#x?:'foo'");
		context.setVariable("x",null);
		assertEquals("foo", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("foo", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		// variable access returning array
		exp = new SpelExpressionParser(configuration).parseExpression("#x?:'foo'");
		context.setVariable("x",new int[]{1,2,3});
		assertEquals("1,2,3", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("1,2,3", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);
	}

	@Test
	public void ternaryOperator_SPR15192() {
		SpelParserConfiguration configuration = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null);
		Expression exp;
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("map", Collections.singletonMap("foo", "qux"));

		exp = new SpelExpressionParser(configuration).parseExpression("bar(#map['foo'] != null ? #map['foo'] : 'qux')");
		assertEquals("QUX", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("QUX", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		exp = new SpelExpressionParser(configuration).parseExpression("3==3?3:'foo'");
		assertEquals("3", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("3", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);
		exp = new SpelExpressionParser(configuration).parseExpression("3!=3?3:'foo'");
		assertEquals("foo", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("foo", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		// When the condition is a double slot primitive
		exp = new SpelExpressionParser(configuration).parseExpression("3==3?3L:'foo'");
		assertEquals("3", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("3", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);
		exp = new SpelExpressionParser(configuration).parseExpression("3!=3?3L:'foo'");
		assertEquals("foo", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("foo", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		// When the condition is an empty string
		exp = new SpelExpressionParser(configuration).parseExpression("''==''?'abc':4L");
		assertEquals("abc", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("abc", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		// null condition
		exp = new SpelExpressionParser(configuration).parseExpression("3==3?null:4L");
		assertEquals(null, exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals(null, exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		// variable access returning primitive
		exp = new SpelExpressionParser(configuration).parseExpression("#x==#x?50:'foo'");
		context.setVariable("x",50);
		assertEquals("50", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("50", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		exp = new SpelExpressionParser(configuration).parseExpression("#x!=#x?50:'foo'");
		context.setVariable("x",null);
		assertEquals("foo", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("foo", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);

		// variable access returning array
		exp = new SpelExpressionParser(configuration).parseExpression("#x==#x?'1,2,3':'foo'");
		context.setVariable("x",new int[]{1,2,3});
		assertEquals("1,2,3", exp.getValue(context, new Foo(), String.class));
		assertCanCompile(exp);
		assertEquals("1,2,3", exp.getValue(context, new Foo(), String.class));
		assertIsCompiled(exp);
	}

	@Test
	public void repeatedCompilation() throws Exception {
		// Verifying that after a number of compilations, the classloaders
		// used to load the compiled expressions are discarded/replaced.
		// See SpelCompiler.loadClass()
		Field f = SpelExpression.class.getDeclaredField("compiledAst");
		Set<Object> classloadersUsed = new HashSet<>();
		for (int i = 0; i < 1500; i++) {  // 1500 is greater than SpelCompiler.CLASSES_DEFINED_LIMIT
			this.expression = this.parser.parseExpression("4 + 5");
			assertEquals(9, (int) this.expression.getValue(Integer.class));
			assertCanCompile(this.expression);
			f.setAccessible(true);
			CompiledExpression cEx = (CompiledExpression) f.get(this.expression);
			classloadersUsed.add(cEx.getClass().getClassLoader());
			assertEquals(9, (int) this.expression.getValue(Integer.class));
		}
		assertTrue(classloadersUsed.size() > 1);
	}


	// helper methods

	private SpelNodeImpl getAst() {
		SpelExpression spelExpression = (SpelExpression) this.expression;
		SpelNode ast = spelExpression.getAST();
		return (SpelNodeImpl)ast;
	}

	private String stringify(Object object) {
		StringBuilder s = new StringBuilder();
		if (object instanceof List) {
			List<?> ls = (List<?>) object;
			for (Object l: ls) {
				s.append(l);
				s.append(" ");
			}
		}
		else if (object instanceof Object[]) {
			Object[] os = (Object[]) object;
			for (Object o: os) {
				s.append(o);
				s.append(" ");
			}
		}
		else if (object instanceof int[]) {
			int[] is = (int[]) object;
			for (int i: is) {
				s.append(i);
				s.append(" ");
			}
		}
		else {
			s.append(object.toString());
		}
		return s.toString().trim();
	}

	private void assertCanCompile(Expression expression) {
		assertTrue(SpelCompiler.compile(expression));
	}

	private void assertCantCompile(Expression expression) {
		assertFalse(SpelCompiler.compile(expression));
	}

	private Expression parse(String expression) {
		return this.parser.parseExpression(expression);
	}

	private void assertGetValueFail(Expression expression) {
		try {
			Object o = expression.getValue();
			fail("Calling getValue on the expression should have failed but returned "+o);
		}
		catch (Exception ex) {
			// success!
		}
	}

	private void assertIsCompiled(Expression expression) {
		try {
			Field field = SpelExpression.class.getDeclaredField("compiledAst");
			field.setAccessible(true);
			Object object = field.get(expression);
			assertNotNull(object);
		}
		catch (Exception ex) {
			fail(ex.toString());
		}
	}


	// nested types

	public interface Message<T> {

		MessageHeaders getHeaders();

		@SuppressWarnings("rawtypes")
		List getList();

		int[] getIa();
	}


	public static class MyMessage implements Message<String> {

		public MessageHeaders getHeaders() {
			MessageHeaders mh = new MessageHeaders();
			mh.put("command", "wibble");
			mh.put("command2", "wobble");
			return mh;
		}

		public int[] getIa() {
			return new int[] { 5, 3 };
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		public List getList() {
			List l = new ArrayList();
			l.add("wibble");
			l.add("wobble");
			return l;
		}

		public String getKey() {
			return "command2";
		}

		public int getKey2() {
			return 1;
		}
	}


	@SuppressWarnings("serial")
	public static class MessageHeaders extends HashMap<String, Object> {
	}


	public static class GenericMessageTestHelper<T> {

		private T payload;

		GenericMessageTestHelper(T value) {
			this.payload = value;
		}

		public T getPayload() {
			return this.payload;
		}
	}


	// This test helper has a bound on the type variable
	public static class GenericMessageTestHelper2<T extends Number> {

		private T payload;

		GenericMessageTestHelper2(T value) {
			this.payload = value;
		}

		public T getPayload() {
			return this.payload;
		}
	}


	static class MyAccessor implements CompilablePropertyAccessor {

		private Method method;

		public Class<?>[] getSpecificTargetClasses() {
			return new Class<?>[] {Payload2.class};
		}

		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			// target is a Payload2 instance
			return true;
		}

		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			Payload2 payload2 = (Payload2)target;
			return new TypedValue(payload2.getField(name));
		}

		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return false;
		}

		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		}

		@Override
		public boolean isCompilable() {
			return true;
		}

		@Override
		public Class<?> getPropertyType() {
			return Object.class;
		}

		@Override
		public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
			if (this.method == null) {
				try {
					this.method = Payload2.class.getDeclaredMethod("getField", String.class);
				}
				catch (Exception ex) {
				}
			}
			String descriptor = cf.lastDescriptor();
			String memberDeclaringClassSlashedDescriptor = this.method.getDeclaringClass().getName().replace('.','/');
			if (descriptor == null) {
				cf.loadTarget(mv);
			}
			if (descriptor == null || !memberDeclaringClassSlashedDescriptor.equals(descriptor.substring(1))) {
				mv.visitTypeInsn(CHECKCAST, memberDeclaringClassSlashedDescriptor);
			}
			mv.visitLdcInsn(propertyName);
			mv.visitMethodInsn(INVOKEVIRTUAL, memberDeclaringClassSlashedDescriptor, this.method.getName(),
					CodeFlow.createSignatureDescriptor(this.method), false);
		}
	}


	static class CompilableMapAccessor implements CompilablePropertyAccessor {

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			Map<?,?> map = (Map<?,?>) target;
			return map.containsKey(name);
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			Map<?,?> map = (Map<?,?>) target;
			Object value = map.get(name);
			if (value == null && !map.containsKey(name)) {
				throw new MapAccessException(name);
			}
			return new TypedValue(value);
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
			Map<String, Object> map = (Map<String, Object>) target;
			map.put(name, newValue);
		}

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class<?>[] {Map.class};
		}

		@Override
		public boolean isCompilable() {
			return true;
		}

		@Override
		public Class<?> getPropertyType() {
			return Object.class;
		}

		@Override
		public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
			String descriptor = cf.lastDescriptor();
			if (descriptor == null) {
				cf.loadTarget(mv);
			}
			mv.visitLdcInsn(propertyName);
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get","(Ljava/lang/Object;)Ljava/lang/Object;",true);
		}
	}


	/**
	 * Exception thrown from {@code read} in order to reset a cached
	 * PropertyAccessor, allowing other accessors to have a try.
	 */
	@SuppressWarnings("serial")
	private static class MapAccessException extends AccessException {

		private final String key;

		public MapAccessException(String key) {
			super(null);
			this.key = key;
		}

		@Override
		public String getMessage() {
			return "Map does not contain a value for key '" + this.key + "'";
		}
	}


	public static class Greeter {

		public String getWorld() {
			return "world";
		}

		public Object getObject() {
			return "object";
		}
	}

	public static class FooObjectHolder {

		private FooObject foo = new FooObject();

		public FooObject getFoo() {
			return this.foo;
		}
	}

	public static class FooObject {

		public Object getObject() {
			return "hello";
		}
	}


	public static class FooString {

		public String getObject() {
			return "hello";
		}
	}


	public static class Payload {

		Two[] DR = new Two[] {new Two()};

		public Two holder = new Two();

		public Two[] getDR() {
			return this.DR;
		}
	}


	public static class Payload2 {

		String var1 = "abc";
		String var2 = "def";

		public Object getField(String name) {
			if (name.equals("var1")) {
				return this.var1;
			}
			else if (name.equals("var2")) {
				return this.var2;
			}
			return null;
		}
	}


	public static class Payload2Holder {

		public Payload2 payload2 = new Payload2();
	}


	public class Person {

		private int age;

		public Person(int age) {
			this.age = age;
		}

		public int getAge() {
			return this.age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}


	public class Person3 {

		private int age;

		public Person3(String name, int age) {
			this.age = age;
		}

		public int getAge() {
			return this.age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}


	public static class Two {

		Three three = new Three();

		public Three getThree() {
			return this.three;
		}
		public String toString() {
			return "instanceof Two";
		}
	}


	public static class Three {

		double four = 0.04d;

		public double getFour() {
			return this.four;
		}
	}


	public class PayloadX {

		public int valueI = 120;
		public Integer valueIB = 120;
		public Integer valueIB58 = 58;
		public Integer valueIB60 = 60;
		public long valueJ = 120L;
		public Long valueJB = 120L;
		public Long valueJB58 = 58L;
		public Long valueJB60 = 60L;
		public double valueD = 120D;
		public Double valueDB = 120D;
		public Double valueDB58 = 58D;
		public Double valueDB60 = 60D;
		public float valueF = 120F;
		public Float valueFB = 120F;
		public Float valueFB58 = 58F;
		public Float valueFB60 = 60F;
		public byte valueB = (byte)120;
		public byte valueB18 = (byte)18;
		public byte valueB20 = (byte)20;
		public Byte valueBB = (byte)120;
		public Byte valueBB18 = (byte)18;
		public Byte valueBB20 = (byte)20;
		public char valueC = (char)120;
		public Character valueCB = (char)120;
		public short valueS = (short)120;
		public short valueS18 = (short)18;
		public short valueS20 = (short)20;
		public Short valueSB = (short)120;
		public Short valueSB18 = (short)18;
		public Short valueSB20 = (short)20;

		public PayloadX payload;

		public PayloadX() {
			this.payload = this;
		}
	}


	public static class TestClass1 {

		public int index1 = 1;
		public int index2 = 3;
		public String word = "abcd";
	}


	public static class TestClass4 {

		public boolean a;

		public boolean b;

		public boolean gettrue() {
			return true;
		}

		public boolean getfalse() {
			return false;
		}

		public boolean getA() {
			return this.a;
		}

		public boolean getB() {
			return this.b;
		}
	}


	public static class TestClass10 {

		public String s = null;

		public void reset() {
			this.s = null;
		}

		public void concat(String arg) {
			this.s = "::"+arg;
		}

		public void concat(String... vargs) {
			if (vargs == null) {
				this.s = "";
			}
			else {
				this.s = "";
				for (String varg : vargs) {
					this.s += varg;
				}
			}
		}

		public void concat2(Object arg) {
			this.s = "::"+arg;
		}

		public void concat2(Object... vargs) {
			if (vargs == null) {
				this.s = "";
			}
			else {
				this.s = "";
				for (Object varg : vargs) {
					this.s += varg;
				}
			}
		}
	}


	public static class TestClass5 {

		public int i = 0;
		public String s = null;
		public static int _i = 0;
		public static String _s = null;

		public static short s1 = (short)1;
		public static short s2 = (short)2;
		public static short s3 = (short)3;

		public static long l1 = 1L;
		public static long l2 = 2L;
		public static long l3 = 3L;

		public static float f1 = 1f;
		public static float f2 = 2f;
		public static float f3 = 3f;

		public static char c1 = 'a';
		public static char c2 = 'b';
		public static char c3 = 'c';

		public static byte b1 = (byte)65;
		public static byte b2 = (byte)66;
		public static byte b3 = (byte)67;

		public static String[] stringArray = new String[] {"aaa","bbb","ccc"};
		public static int[] intArray = new int[] {11,22,33};

		public Object obj = null;

		public String field = null;

		public void reset() {
			this.i = 0;
			_i = 0;
			this.s = null;
			_s = null;
			this.field = null;
		}

		public void one() {
			this.i = 1;
		}

		public static void two() {
			_i = 1;
		}

		public String three() {
			return "hello";
		}

		public long four() {
			return 3277700L;
		}

		public static String five() {
			return "hello";
		}

		public static long six() {
			return 3277700L;
		}

		public void seven(String toset) {
			this.s = toset;
		}
		// public void seven(Number n) { s = n.toString(); }

		public void takeNumber(Number n) {
			this.s = n.toString();
		}

		public void takeString(String s) {
			this.s = s;
		}

		public static void eight(String toset) {
			_s = toset;
		}

		public void nine(int toset) {
			this.i = toset;
		}

		public static void ten(int toset) {
			_i = toset;
		}

		public void eleven(String... vargs) {
			if (vargs == null) {
				this.s = "";
			}
			else {
				this.s = "";
				for (String varg: vargs) {
					this.s += varg;
				}
			}
		}

		public void twelve(int... vargs) {
			if (vargs == null) {
				this.i = 0;
			}
			else {
				this.i = 0;
				for (int varg: vargs) {
					this.i += varg;
				}
			}
		}

		public void thirteen(String a, String... vargs) {
			if (vargs == null) {
				this.s = a + "::";
			}
			else {
				this.s = a+"::";
				for (String varg: vargs) {
					this.s += varg;
				}
			}
		}

		public void arrayz(boolean... bs) {
			this.s = "";
			if (bs != null) {
				this.s = "";
				for (boolean b: bs) {
					this.s += Boolean.toString(b);
				}
			}
		}

		public void arrays(short... ss) {
			this.s = "";
			if (ss != null) {
				this.s = "";
				for (short s: ss) {
					this.s += Short.toString(s);
				}
			}
		}

		public void arrayd(double... vargs) {
			this.s = "";
			if (vargs != null) {
				this.s = "";
				for (double v: vargs) {
					this.s += Double.toString(v);
				}
			}
		}

		public void arrayf(float... vargs) {
			this.s = "";
			if (vargs != null) {
				this.s = "";
				for (float v: vargs) {
					this.s += Float.toString(v);
				}
			}
		}

		public void arrayj(long... vargs) {
			this.s = "";
			if (vargs != null) {
				this.s = "";
				for (long v: vargs) {
					this.s += Long.toString(v);
				}
			}
		}

		public void arrayb(byte... vargs) {
			this.s = "";
			if (vargs != null) {
				this.s = "";
				for (Byte v: vargs) {
					this.s += Byte.toString(v);
				}
			}
		}

		public void arrayc(char... vargs) {
			this.s = "";
			if (vargs != null) {
				this.s = "";
				for (char v: vargs) {
					this.s += Character.toString(v);
				}
			}
		}

		public void fourteen(String a, String[]... vargs) {
			if (vargs == null) {
				this.s = a+"::";
			}
			else {
				this.s = a+"::";
				for (String[] varg: vargs) {
					this.s += "{";
					for (String v: varg) {
						this.s += v;
					}
					this.s += "}";
				}
			}
		}

		public void fifteen(String a, int[]... vargs) {
			if (vargs == null) {
				this.s = a+"::";
			}
			else {
				this.s = a+"::";
				for (int[] varg: vargs) {
					this.s += "{";
					for (int v: varg) {
						this.s += Integer.toString(v);
					}
					this.s += "}";
				}
			}
		}

		public void sixteen(Object... vargs) {
			if (vargs == null) {
				this.s = "";
			}
			else {
				this.s = "";
				for (Object varg: vargs) {
					this.s += varg;
				}
			}
		}
	}


	public static class TestClass6 {

		public String orange = "value1";
		public static String apple = "value2";

		public long peach = 34L;

		public String getBanana() {
			return "value3";
		}

		public static String getPlum() {
			return "value4";
		}
	}


	public static class TestClass7 {

		public static String property;

		static {
			String s = "UK 123";
			StringTokenizer st = new StringTokenizer(s);
			property = st.nextToken();
		}

		public static void reset() {
			String s = "UK 123";
			StringTokenizer st = new StringTokenizer(s);
			property = st.nextToken();
		}
	}


	public static class TestClass8 {

		public int i;
		public String s;
		public double d;
		public boolean z;

		public TestClass8(int i, String s, double d, boolean z) {
			this.i = i;
			this.s = s;
			this.d = d;
			this.z = z;
		}

		public TestClass8() {
		}

		public TestClass8(Integer i) {
			this.i = i;
		}

		@SuppressWarnings("unused")
		private TestClass8(String a, String b) {
			this.s = a+b;
		}
	}


	public static class Obj {

		private final String param1;

		public Obj(String param1){
			this.param1 = param1;
		}
	}


	public static class Obj2 {

		public final String output;

		public Obj2(String... params){
			StringBuilder b = new StringBuilder();
			for (String param: params) {
				b.append(param);
			}
			this.output = b.toString();
		}
	}


	public static class Obj3 {

		public final String output;

		public Obj3(int... params) {
			StringBuilder b = new StringBuilder();
			for (int param: params) {
				b.append(Integer.toString(param));
			}
			this.output = b.toString();
		}

		public Obj3(String s, Float f, int... ints) {
			StringBuilder b = new StringBuilder();
			b.append(s);
			b.append(":");
			b.append(Float.toString(f));
			b.append(":");
			for (int param: ints) {
				b.append(Integer.toString(param));
			}
			this.output = b.toString();
		}
	}


	public static class Obj4 {

		public final String output;

		public Obj4(int[] params) {
			StringBuilder b = new StringBuilder();
			for (int param: params) {
				b.append(Integer.toString(param));
			}
			this.output = b.toString();
		}
	}


	@SuppressWarnings("unused")
	private static class TestClass9 {

		public TestClass9(int i) {
		}
	}


	// These test classes simulate a pattern of public/private classes seen in Spring Security

	// final class HttpServlet3RequestFactory implements HttpServletRequestFactory
	static class HttpServlet3RequestFactory {

		static Servlet3SecurityContextHolderAwareRequestWrapper getOne() {
			HttpServlet3RequestFactory outer = new HttpServlet3RequestFactory();
			return outer.new Servlet3SecurityContextHolderAwareRequestWrapper();
		}

		// private class Servlet3SecurityContextHolderAwareRequestWrapper extends SecurityContextHolderAwareRequestWrapper
		private class Servlet3SecurityContextHolderAwareRequestWrapper extends SecurityContextHolderAwareRequestWrapper {
		}
	}


	// public class SecurityContextHolderAwareRequestWrapper extends HttpServletRequestWrapper
	static class SecurityContextHolderAwareRequestWrapper extends HttpServletRequestWrapper {
	}


	public static class HttpServletRequestWrapper {

		public String getServletPath() {
			return "wibble";
		}
	}


	// Here the declaring class is not public
	static class SomeCompareMethod {

		// method not public
		static int compare(Object o1, Object o2) {
			return -1;
		}

		// public
		public static int compare2(Object o1, Object o2) {
			return -1;
		}
	}


	public static class SomeCompareMethod2 {

		public static int negate(int i1) {
			return -i1;
		}

		public static String append(String... strings) {
			StringBuilder b = new StringBuilder();
			for (String string : strings) {
				b.append(string);
			}
			return b.toString();
		}

		public static String append2(Object... objects) {
			StringBuilder b = new StringBuilder();
			for (Object object : objects) {
				b.append(object.toString());
			}
			return b.toString();
		}

		public static String append3(String[] strings) {
			StringBuilder b = new StringBuilder();
			for (String string : strings) {
				b.append(string);
			}
			return b.toString();
		}

		public static String append4(String s, String... strings) {
			StringBuilder b = new StringBuilder();
			b.append(s).append("::");
			for (String string : strings) {
				b.append(string);
			}
			return b.toString();
		}

		public static String appendChar(char... values) {
			StringBuilder b = new StringBuilder();
			for (char ch : values) {
				b.append(ch);
			}
			return b.toString();
		}

		public static int sum(int... ints) {
			int total = 0;
			for (int i : ints) {
				total += i;
			}
			return total;
		}

		public static int sumDouble(double... values) {
			int total = 0;
			for (double i : values) {
				total += i;
			}
			return total;
		}

		public static int sumFloat(float... values) {
			int total = 0;
			for (float i : values) {
				total += i;
			}
			return total;
		}
	}


	public static class DelegatingStringFormat {

		public static String format(String s, Object... args) {
			return String.format(s, args);
		}
	}


	public static class StaticsHelper {

		static StaticsHelper sh = new StaticsHelper();
		public static StaticsHelper fielda = sh;
		public static String fieldb = "fb";

		public static StaticsHelper methoda() {
			return sh;
		}
		public static String methodb() {
			return "mb";
		}

		public static StaticsHelper getPropertya() {
			return sh;
		}

		public static String getPropertyb() {
			return "pb";
		}

		public String toString() {
			return "sh";
		}
	}


	public static class Apple implements Comparable<Apple> {

		public Object gotComparedTo = null;
		public int i;

		public Apple(int i) {
			this.i = i;
		}

		public void setValue(int i) {
			this.i = i;
		}

		@Override
		public int compareTo(Apple that) {
			this.gotComparedTo = that;
			if (this.i < that.i) {
				return -1;
			}
			else if (this.i > that.i) {
				return +1;
			}
			else {
				return 0;
			}
		}
	}


	// For opNe_SPR14863
	public static class MyContext {

		private final Map<String, String> data;

		public MyContext(Map<String, String> data) {
			this.data = data;
		}

		public Map<String, String> getData() {
			return this.data;
		}
	}


	public static class Foo {

		public String bar() {
			return "BAR";
		}

		public String bar(String arg) {
			return arg.toUpperCase();
		}
	}

}
