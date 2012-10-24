
package org.springframework.core.convert;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.MultiValueMap;

/**
 * Tests for {@link GenericType}.
 *
 * @author Phillip Webb
 */
public class GenericTypeTests {

	private GenericType mixedUpMapType;

	@Before
	public void setup() throws Exception {
		this.mixedUpMapType = GenericType.get(getClass().getField("mixedUpMap"));
	}

	@Test
	public void shouldGetType() throws Exception {
		assertEquals(MixedupMap.class, mixedUpMapType.getTypeClass());
	}

	@Test
	public void shouldGetSuperType() throws Exception {
		assertEquals(HashMap.class, mixedUpMapType.getSuperclass().getTypeClass());
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
		GenericType[] generics = mixedUpMapType.getSuperclass().getGenerics();
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
		assertThat(mixedUpMapType.getSuperclass().toString(), is("java.util.HashMap<java.lang.Integer, java.lang.String>"));
		assertThat(mixedUpMapType.getInterfaces()[0].toString(), is("org.springframework.core.convert.GenericTypeTests$KeyAccess<java.lang.Integer>"));
	}

	@Test
	public void shouldSupportWildcards() throws Exception {
		GenericType type = GenericType.get(getClass().getField("wildcard"));
		assertNull(type.getGenerics()[1].getTypeClass());
		assertNull(type.getSuperclass().getGenerics()[0].getTypeClass());
		assertThat(type.toString(), is("org.springframework.core.convert.GenericTypeTests$MixedupMap<java.lang.String, ?>"));
		assertThat(type.getSuperclass().toString(), is("java.util.HashMap<?, java.lang.String>"));
		assertThat(type.getInterfaces()[0].toString(), is("org.springframework.core.convert.GenericTypeTests$KeyAccess<?>"));
	}

	@Test
	public void shouldKeepNestedGenerics() throws Exception {
		GenericType type = GenericType.get(getClass().getField("nested"));
		assertThat(type.getGenerics()[0].toString(), is("java.util.List<java.util.Set<java.lang.Integer>>"));
		assertThat(type.getGenerics()[1].toString(), is("java.util.List<java.util.Set<java.lang.String>>"));
		assertThat(type.getSuperclass().getGenerics()[0].toString(), is("java.util.List<java.util.Set<java.lang.String>>"));
		assertThat(type.getSuperclass().getGenerics()[1].toString(), is("java.util.List<java.util.Set<java.lang.Integer>>"));
	}

	@Test
	public void shouldFindSuperType() throws Exception {
		GenericType type = GenericType.get(getClass().getField("multiValueMap")).find(Map.class);
		assertThat(type.toString(), is("java.util.Map<java.lang.Integer, java.util.List<java.lang.String>>"));
	}

	public MixedupMap<String, Integer> mixedUpMap;

	public MixedupMap<String, ?> wildcard;

	public MixedupMap<List<Set<Integer>>, List<Set<String>>> nested;

	public MultiValueMap<Integer, String> multiValueMap;

	//FIXME generic array List<String>[]

	//FIXME generic with array List<List<String>[]>

	public static class MixedupMap<V, K> extends HashMap<K, V> implements KeyAccess<K> {
	}

	public static interface KeyAccess<K> {
	}

}
