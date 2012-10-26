
package org.springframework.core.convert;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.GenericBean;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * Tests for {@link GenericType}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class GenericTypeTests {

	private GenericType mixedUpMapType;

	@Before
	public void setup() throws Exception {
		this.mixedUpMapType = GenericType.fromField(getClass().getField("mixedUpMap"));
	}

	@Test
	public void shouldGetType() throws Exception {
		assertEquals(MixedupMap.class, mixedUpMapType.getTypeClass());
	}

	@Test
	public void shouldGetSuperType() throws Exception {
		assertEquals(HashMap.class, mixedUpMapType.getSuper().getTypeClass());
	}

	@Test
	public void shouldGetInterfaces() throws Exception {
		assertEquals(KeyAccess.class, mixedUpMapType.getInterfaces()[0].getTypeClass());
	}

	@Test
	public void shouldGetGenerics() throws Exception {
		GenericType[] generics = mixedUpMapType.getGenerics();
		assertThat(generics.length, is(2));
		assertEquals(String.class, generics[0].getType());
		assertEquals(Integer.class, generics[1].getType());
	}

	@Test
	public void shouldGetGenericsOnSuperclass() throws Exception {
		GenericType[] generics = mixedUpMapType.getSuper().getGenerics();
		assertThat(generics.length, is(2));
		assertThat(generics[0].getType().toString(), is("K"));
		assertThat(generics[1].getType().toString(), is("V"));
		assertEquals(Integer.class, generics[0].getTypeClass());
		assertEquals(String.class, generics[1].getTypeClass());
	}

	@Test
	public void shouldGetGenericsOnInterface() throws Exception {
		GenericType[] generics = mixedUpMapType.getInterfaces()[0].getGenerics();
		assertThat(generics.length, is(1));
		assertThat(generics[0].getType().toString(), is("K"));
		assertEquals(Integer.class, generics[0].getTypeClass());
	}

	@Test
	public void shouldSupportToString() throws Exception {
		assertThat(mixedUpMapType.toString(), is("org.springframework.core.convert.GenericTypeTests$MixedupMap<java.lang.String, java.lang.Integer>"));
	}

	@Test
	public void shouldSupportVariablesForToString() throws Exception {
		assertThat(mixedUpMapType.getSuper().toString(), is("java.util.HashMap<java.lang.Integer, java.lang.String>"));
		assertThat(mixedUpMapType.getInterfaces()[0].toString(), is("org.springframework.core.convert.GenericTypeTests$KeyAccess<java.lang.Integer>"));
	}

	@Test
	public void shouldSupportWildcards() throws Exception {
		GenericType type = GenericType.fromField(getClass().getField("wildcard"));
		assertNull(type.getGenericTypeClass(1));
		assertNull(type.getSuper().getGenericTypeClass(0));
		assertThat(type.toString(), is("org.springframework.core.convert.GenericTypeTests$MixedupMap<java.lang.String, ?>"));
		assertThat(type.getSuper().toString(), is("java.util.HashMap<?, java.lang.String>"));
		assertThat(type.getInterfaces()[0].toString(), is("org.springframework.core.convert.GenericTypeTests$KeyAccess<?>"));
	}

	@Test
	public void shouldSupportBoundedWildcards() throws Exception {
		GenericType type = GenericType.fromField(getClass().getField("boundedWildcard"));
		assertThat(type.get(Map.class).getGeneric(1).toString(), is("java.lang.Number"));
	}

	@Test
	public void shouldSupportBoundedVariable() throws Exception {
		Method method = ReflectionUtils.findMethod(getClass(), "boundedVariable");
		GenericType type = GenericType.fromMethodReturn(method);
		assertEquals(Number.class, type.getTypeClass());
	}

	@Test
	public void shouldKeepNestedGenerics() throws Exception {
		GenericType type = GenericType.fromField(getClass().getField("nested"));
		assertThat(type.getGeneric(0).toString(), is("java.util.List<java.util.Set<java.lang.Integer>>"));
		assertThat(type.getGeneric(1).toString(), is("java.util.List<java.util.Set<java.lang.String>>"));
		assertThat(type.getSuper().getGeneric(0).toString(), is("java.util.List<java.util.Set<java.lang.String>>"));
		assertThat(type.getSuper().getGeneric(1).toString(), is("java.util.List<java.util.Set<java.lang.Integer>>"));
	}

	@Test
	public void shouldFindSuperType() throws Exception {
		GenericType type = GenericType.fromField(getClass().getField("multiValueMap")).get(Map.class);
		assertThat(type.toString(), is("java.util.Map<java.lang.Integer, java.util.List<java.lang.String>>"));
		assertThat(type.getGeneric(1).toString(), is("java.util.List<java.lang.String>"));
		assertThat(type.getGeneric(1).getGeneric(0).toString(), is("java.lang.String"));
	}

	@Test
	public void shouldSupportGenericArrays() throws Exception {
		GenericType type = GenericType.fromField(getClass().getField("genericArray"));
		assertThat(type.toString(), is("java.util.ArrayList<java.util.Set<java.lang.Integer>>[]"));
		assertThat(type.isArray(), is(true));
		assertEquals(ArrayList[].class,type.getTypeClass());
		assertThat(type.getGeneric(0).toString(), is("java.util.Set<java.lang.Integer>"));
	}

	@Test
	public void shouldSupportArraySuperClass() throws Exception {
		GenericType type = GenericType.fromField(getClass().getField("genericArray")).getSuper();
		assertThat(type.toString(), is("java.util.AbstractList<java.util.Set<java.lang.Integer>>[]"));
		assertThat(type.isArray(), is(true));
		assertEquals(AbstractList[].class,type.getTypeClass());
		assertThat(type.getGeneric(0).toString(), is("java.util.Set<java.lang.Integer>"));
	}

	@Test
	public void shouldFindGenericOnArray() throws Exception {
		GenericType type = GenericType.fromField(getClass().getField("genericArray")).get(List.class);
		assertThat(type.toString(), is("java.util.List<java.util.Set<java.lang.Integer>>[]"));
		assertThat(type.isArray(), is(true));
		assertEquals(List[].class,type.getTypeClass());
		assertThat(type.getGeneric(0).toString(), is("java.util.Set<java.lang.Integer>"));
	}

	@Test
	public void shouldSupportVariablesOnArray() throws Exception {
		GenericType type = GenericType.fromField(getClass().getField("mixedUpMapArray"));
		assertThat(type.toString(), is("org.springframework.core.convert.GenericTypeTests$MixedupMap<java.lang.String, java.lang.Integer>[]"));
		assertThat(type.getSuper().toString(), is("java.util.HashMap<java.lang.Integer, java.lang.String>[]"));
		assertThat(type.getInterfaces()[0].toString(), is("org.springframework.core.convert.GenericTypeTests$KeyAccess<java.lang.Integer>[]"));
	}

	@Test
	public void shouldSupportArrayInGeneric() throws Exception {
		GenericType type = GenericType.fromField(getClass().getField("arrayInGeneric"));
		assertThat(type.toString(), is("java.util.List<java.util.Set<java.lang.Integer>[]>"));
		assertThat(type.getGeneric(0).toString(), is("java.util.Set<java.lang.Integer>[]"));;
		assertThat(type.getGeneric(0).isArray(), is(true));;
	}

	@Test
	public void shouldSupportComplexNestedArrays() throws Exception {
		GenericType type = GenericType.fromField(getClass().getField("complex"));
		assertThat(type.toString(), is("org.springframework.core.convert.GenericTypeTests$MixedupMap<org.springframework.core.convert.GenericTypeTests$MixedupMap<java.lang.String[], java.lang.Integer>, java.lang.Integer>[]"));
		GenericType nestedInnerGeneric = type.get(Map.class).getGeneric(1).get(Map.class).getGeneric(1);
		assertThat(nestedInnerGeneric.toString(), is("java.lang.String[]"));
		assertThat(nestedInnerGeneric.isArray(), is(true));
		assertEquals(String[].class, nestedInnerGeneric.getTypeClass());
	}

	@Test
	public void shouldSupportNoSuperclass() throws Exception {
		assertThat(GenericType.fromClass(Object.class).getSuper(), is(nullValue()));
	}

	@Test
	public void shouldSupportNoInterfaces() throws Exception {
		assertThat(GenericType.fromClass(Object.class).getInterfaces().length, is(0));
	}

	@Test
	public void shouldSupportNoGenerics() throws Exception {
		assertThat(GenericType.fromClass(Object.class).getGenerics().length, is(0));
	}

	@Test
	public void shouldResolveVariableFromSpecificOwner() throws Exception {
		Method method = ReflectionUtils.findMethod(BoundedType.class, "getNumber");
		assertEquals(Number.class, GenericType.fromMethodReturn(method).getTypeClass());
		assertEquals(Integer.class, GenericType.fromMethodReturn(method, DeclaredBoundedType.class).getTypeClass());
	}

	// Can we replicate GenericTypeResolverTests

	@Test
	public void shouldResolveSimpleInterface() throws Exception {
		assertEquals(String.class, GenericType.fromClass(MySimpleInterfaceType.class).get(MyInterfaceType.class).getGenericTypeClass());
	}

	@Test
	public void shouldResolveCollectionInterface() throws Exception {
		assertEquals(Collection.class, GenericType.fromClass(MyCollectionInterfaceType.class).get(MyInterfaceType.class).getGenericTypeClass());
	}

	@Test
	public void shouldResolveSimpleSuperclass() throws Exception {
		assertEquals(String.class, GenericType.fromClass(MySimpleSuperclassType.class).get(MySuperclassType.class).getGenericTypeClass());
	}

	@Test
	public void shouldResolveSimpleCollectionSuperclass() throws Exception {
		assertEquals(Collection.class, GenericType.fromClass(MyCollectionSuperclassType.class).get(MySuperclassType.class).getGenericTypeClass());
	}

	@Test
	public void shouldReturnNullIfNotResolvable() throws Exception {
		GenericClass<String> obj = new GenericClass<String>();
		assertNull(GenericType.fromClass(obj.getClass()).get(GenericClass.class).getGenericTypeClass());
	}

	@Test
	public void shouldResolveReturnTypes() throws Exception {
		assertEquals(Integer.class, GenericType.fromMethodReturn(ReflectionUtils.findMethod(MyTypeWithMethods.class, "integer")).get(MyInterfaceType.class).getGenericTypeClass());
		assertEquals(String.class, GenericType.fromMethodReturn(ReflectionUtils.findMethod(MyTypeWithMethods.class, "string")).get(MyInterfaceType.class).getGenericTypeClass());
		assertNull(GenericType.fromMethodReturn(ReflectionUtils.findMethod(MyTypeWithMethods.class, "raw")).get(MyInterfaceType.class).getGenericTypeClass());
		assertNull(GenericType.fromMethodReturn(ReflectionUtils.findMethod(MyTypeWithMethods.class, "object")).get(MyInterfaceType.class));
	}

	// Can we replicate GenericCollectionTypeResolverTests

	@Test
	public void shouldGetMapValueGenerics() throws Exception {
		assertEquals(Integer.class, GenericType.fromMethodReturn(ReflectionUtils.findMethod(Foo.class, "a")).get(Map.class).getGenericTypeClass(1));
		assertNull(GenericType.fromMethodReturn(ReflectionUtils.findMethod(Foo.class, "b")).get(Map.class).getGenericTypeClass(1));
		assertEquals(Set.class, GenericType.fromMethodReturn(ReflectionUtils.findMethod(Foo.class, "b2")).get(Map.class).getGenericTypeClass(1));
		assertEquals(Set.class, GenericType.fromMethodReturn(ReflectionUtils.findMethod(Foo.class, "b3")).get(Map.class).getGenericTypeClass(1));
		assertNull(GenericType.fromMethodReturn(ReflectionUtils.findMethod(Foo.class, "c")).get(Map.class).getGenericTypeClass(1));
		assertEquals(Integer.class, GenericType.fromMethodReturn(ReflectionUtils.findMethod(Foo.class, "d")).get(Map.class).getGenericTypeClass(1));
		assertEquals(Integer.class, GenericType.fromMethodReturn(ReflectionUtils.findMethod(Foo.class, "d2")).get(Map.class).getGenericTypeClass(1));
		assertEquals(Integer.class, GenericType.fromMethodReturn(ReflectionUtils.findMethod(Foo.class, "d3")).get(Map.class).getGenericTypeClass(1));
		assertEquals(Integer.class, GenericType.fromMethodReturn(ReflectionUtils.findMethod(Foo.class, "e")).get(Map.class).getGenericTypeClass(1));
		assertEquals(Integer.class, GenericType.fromMethodReturn(ReflectionUtils.findMethod(Foo.class, "e2")).get(Map.class).getGenericTypeClass(1));
		assertEquals(Integer.class, GenericType.fromMethodReturn(ReflectionUtils.findMethod(Foo.class, "e3")).get(Map.class).getGenericTypeClass(1));
	}


	@Test
	public void testProgrammaticListIntrospection() throws Exception {
		Method setter = GenericBean.class.getMethod("setResourceList", List.class);
		Assert.assertEquals(Resource.class, GenericType.fromMethodParameter(new MethodParameter(setter, 0)).get(Collection.class).getGenericTypeClass());
		Method getter = GenericBean.class.getMethod("getResourceList");
		Assert.assertEquals(Resource.class, GenericType.fromMethodReturn(getter).get(Collection.class).getGenericTypeClass());
	}

	@Test
	public void testClassResolution() {
		assertEquals(String.class, GenericType.fromClass(CustomSet.class).get(Collection.class).getGenericTypeClass());
		assertEquals(String.class, GenericType.fromClass(CustomMap.class).get(Map.class).getGenericTypeClass(0));
		assertEquals(Integer.class, GenericType.fromClass(CustomMap.class).get(Map.class).getGenericTypeClass(1));
	}

	// Public fields used for testing

	public MixedupMap<String, Integer> mixedUpMap;

	public MixedupMap<String, Integer>[] mixedUpMapArray;

	public MixedupMap<String, ?> wildcard;

	public MixedupMap<? extends Number, Integer> boundedWildcard;

	public MixedupMap<List<Set<Integer>>, List<Set<String>>> nested;

	public MultiValueMap<Integer, String> multiValueMap;

	public ArrayList<Set<Integer>>[] genericArray;

	public List<Set<Integer>[]> arrayInGeneric;

	public MixedupMap<MixedupMap<String[], Integer>, Integer>[] complex;

	// Public methods used for testing

	public <V extends Number> V boundedVariable() {
		return null;
	}

	// Test classes

	public static class MixedupMap<V, K> extends HashMap<K, V> implements KeyAccess<K> {
	}

	public static interface KeyAccess<K> {
	}

	public static interface BoundedType<V extends Number> {
		public V getNumber();
	}

	public static interface DeclaredBoundedType extends BoundedType<Integer> {
	}

	// From GenericTypeResolverTests

	public interface MyInterfaceType<T> {
	}

	public class MySimpleInterfaceType implements MyInterfaceType<String> {
	}

	public class MyCollectionInterfaceType implements MyInterfaceType<Collection<String>> {
	}

	public abstract class MySuperclassType<T> {
	}

	public class MySimpleSuperclassType extends MySuperclassType<String> {
	}

	public class MyCollectionSuperclassType extends MySuperclassType<Collection<String>> {
	}

	public static class MyTypeWithMethods {

		public MyInterfaceType<Integer> integer() {
			return null;
		}

		public MySimpleInterfaceType string() {
			return null;
		}

		public Object object() {
			return null;
		}

		@SuppressWarnings("rawtypes")
		public MyInterfaceType raw() {
			return null;
		}
	}

	static class GenericClass<T> {
	}

	// From GenericCollectionTypeResolverTests

	private abstract class CustomSet<T> extends AbstractSet<String> {
	}


	private abstract class CustomMap<T> extends AbstractMap<String, Integer> {
	}


	private abstract class OtherCustomMap<T> implements Map<String, Integer> {
	}


	@SuppressWarnings("rawtypes")
	private interface Foo {

		Map<String, Integer> a();

		Map<?, ?> b();

		Map<?, ? extends Set> b2();

		Map<?, ? super Set> b3();

		Map c();

		CustomMap<Date> d();

		CustomMap<?> d2();

		CustomMap d3();

		OtherCustomMap<Date> e();

		OtherCustomMap<?> e2();

		OtherCustomMap e3();
	}
}
