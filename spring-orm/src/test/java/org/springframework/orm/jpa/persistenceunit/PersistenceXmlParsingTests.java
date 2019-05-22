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

package org.springframework.orm.jpa.persistenceunit;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup;
import org.springframework.tests.mock.jndi.SimpleNamingContextBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;
import static temp.XAssert.assertSame;

/**
 * Unit and integration tests for the JPA XML resource parsing support.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Nicholas Williams
 */
public class PersistenceXmlParsingTests {

	@Test
	public void testMetaInfCase() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/META-INF/persistence.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertNotNull(info);
		assertEquals(1, info.length);
		assertThat((Object) info[0].getPersistenceUnitName()).isEqualTo("OrderManagement");

		assertEquals(2, info[0].getJarFileUrls().size());
		assertThat((Object) info[0].getJarFileUrls().get(0)).isEqualTo(new ClassPathResource("order.jar").getURL());
		assertThat((Object) info[0].getJarFileUrls().get(1)).isEqualTo(new ClassPathResource("order-supplemental.jar").getURL());

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();
	}

	@Test
	public void testExample1() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-example1.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertNotNull(info);
		assertEquals(1, info.length);
		assertThat((Object) info[0].getPersistenceUnitName()).isEqualTo("OrderManagement");

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();
	}

	@Test
	public void testExample2() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-example2.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertNotNull(info);
		assertEquals(1, info.length);

		assertThat((Object) info[0].getPersistenceUnitName()).isEqualTo("OrderManagement2");

		assertEquals(1, info[0].getMappingFileNames().size());
		assertThat((Object) info[0].getMappingFileNames().get(0)).isEqualTo("mappings.xml");
		assertEquals(0, info[0].getProperties().keySet().size());

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();
	}

	@Test
	public void testExample3() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-example3.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertNotNull(info);
		assertEquals(1, info.length);
		assertThat((Object) info[0].getPersistenceUnitName()).isEqualTo("OrderManagement3");

		assertEquals(2, info[0].getJarFileUrls().size());
		assertThat((Object) info[0].getJarFileUrls().get(0)).isEqualTo(new ClassPathResource("order.jar").getURL());
		assertThat((Object) info[0].getJarFileUrls().get(1)).isEqualTo(new ClassPathResource("order-supplemental.jar").getURL());

		assertEquals(0, info[0].getProperties().keySet().size());
		assertNull(info[0].getJtaDataSource());
		assertNull(info[0].getNonJtaDataSource());

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();
	}

	@Test
	public void testExample4() throws Exception {
		SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
		DataSource ds = new DriverManagerDataSource();
		builder.bind("java:comp/env/jdbc/MyDB", ds);

		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-example4.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertNotNull(info);
		assertEquals(1, info.length);
		assertThat((Object) info[0].getPersistenceUnitName()).isEqualTo("OrderManagement4");

		assertEquals(1, info[0].getMappingFileNames().size());
		assertThat((Object) info[0].getMappingFileNames().get(0)).isEqualTo("order-mappings.xml");

		assertEquals(3, info[0].getManagedClassNames().size());
		assertThat((Object) info[0].getManagedClassNames().get(0)).isEqualTo("com.acme.Order");
		assertThat((Object) info[0].getManagedClassNames().get(1)).isEqualTo("com.acme.Customer");
		assertThat((Object) info[0].getManagedClassNames().get(2)).isEqualTo("com.acme.Item");

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should be true when no value.").isTrue();

		assertSame(PersistenceUnitTransactionType.RESOURCE_LOCAL, info[0].getTransactionType());
		assertEquals(0, info[0].getProperties().keySet().size());

		builder.clear();
	}

	@Test
	public void testExample5() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-example5.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertNotNull(info);
		assertEquals(1, info.length);
		assertThat((Object) info[0].getPersistenceUnitName()).isEqualTo("OrderManagement5");

		assertEquals(2, info[0].getMappingFileNames().size());
		assertThat((Object) info[0].getMappingFileNames().get(0)).isEqualTo("order1.xml");
		assertThat((Object) info[0].getMappingFileNames().get(1)).isEqualTo("order2.xml");

		assertEquals(2, info[0].getJarFileUrls().size());
		assertThat((Object) info[0].getJarFileUrls().get(0)).isEqualTo(new ClassPathResource("order.jar").getURL());
		assertThat((Object) info[0].getJarFileUrls().get(1)).isEqualTo(new ClassPathResource("order-supplemental.jar").getURL());

		assertThat((Object) info[0].getPersistenceProviderClassName()).isEqualTo("com.acme.AcmePersistence");
		assertEquals(0, info[0].getProperties().keySet().size());

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();
	}

	@Test
	public void testExampleComplex() throws Exception {
		DataSource ds = new DriverManagerDataSource();

		String resource = "/org/springframework/orm/jpa/persistence-complex.xml";
		MapDataSourceLookup dataSourceLookup = new MapDataSourceLookup();
		Map<String, DataSource> dataSources = new HashMap<>();
		dataSources.put("jdbc/MyPartDB", ds);
		dataSources.put("jdbc/MyDB", ds);
		dataSourceLookup.setDataSources(dataSources);
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), dataSourceLookup);
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertEquals(2, info.length);

		PersistenceUnitInfo pu1 = info[0];

		assertThat((Object) pu1.getPersistenceUnitName()).isEqualTo("pu1");

		assertThat((Object) pu1.getPersistenceProviderClassName()).isEqualTo("com.acme.AcmePersistence");

		assertEquals(1, pu1.getMappingFileNames().size());
		assertThat((Object) pu1.getMappingFileNames().get(0)).isEqualTo("ormap2.xml");

		assertEquals(1, pu1.getJarFileUrls().size());
		assertThat((Object) pu1.getJarFileUrls().get(0)).isEqualTo(new ClassPathResource("order.jar").getURL());

		assertThat(pu1.excludeUnlistedClasses()).isFalse();

		assertSame(PersistenceUnitTransactionType.RESOURCE_LOCAL, pu1.getTransactionType());

		Properties props = pu1.getProperties();
		assertEquals(2, props.keySet().size());
		assertThat((Object) props.getProperty("com.acme.persistence.sql-logging")).isEqualTo("on");
		assertThat((Object) props.getProperty("foo")).isEqualTo("bar");

		assertNull(pu1.getNonJtaDataSource());

		assertSame(ds, pu1.getJtaDataSource());

		assertThat(pu1.excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();

		PersistenceUnitInfo pu2 = info[1];

		assertSame(PersistenceUnitTransactionType.JTA, pu2.getTransactionType());
		assertThat((Object) pu2.getPersistenceProviderClassName()).isEqualTo("com.acme.AcmePersistence");

		assertEquals(1, pu2.getMappingFileNames().size());
		assertThat((Object) pu2.getMappingFileNames().get(0)).isEqualTo("order2.xml");

		// the following assertions fail only during coverage runs
		// assertEquals(1, pu2.getJarFileUrls().size());
		// assertEquals(new ClassPathResource("order-supplemental.jar").getURL(), pu2.getJarFileUrls().get(0));

		assertThat(pu2.excludeUnlistedClasses()).isTrue();

		assertNull(pu2.getJtaDataSource());
		assertThat((Object) pu2.getNonJtaDataSource()).isEqualTo(ds);

		assertThat(pu2.excludeUnlistedClasses()).as("Exclude unlisted should be true when no value.").isTrue();
	}

	@Test
	public void testExample6() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-example6.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);
		assertEquals(1, info.length);
		assertThat((Object) info[0].getPersistenceUnitName()).isEqualTo("pu");
		assertEquals(0, info[0].getProperties().keySet().size());

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();
	}

	@Ignore  // not doing schema parsing anymore for JPA 2.0 compatibility
	@Test
	public void testInvalidPersistence() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-invalid.xml";
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
				reader.readPersistenceUnitInfos(resource));
	}

	@Ignore  // not doing schema parsing anymore for JPA 2.0 compatibility
	@Test
	public void testNoSchemaPersistence() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-no-schema.xml";
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
				reader.readPersistenceUnitInfos(resource));
	}

	@Test
	public void testPersistenceUnitRootUrl() throws Exception {
		URL url = PersistenceUnitReader.determinePersistenceUnitRootUrl(new ClassPathResource("/org/springframework/orm/jpa/persistence-no-schema.xml"));
		assertNull(url);

		url = PersistenceUnitReader.determinePersistenceUnitRootUrl(new ClassPathResource("/org/springframework/orm/jpa/META-INF/persistence.xml"));
		assertThat(url.toString().endsWith("/org/springframework/orm/jpa")).as("the containing folder should have been returned").isTrue();
	}

	@Test
	public void testPersistenceUnitRootUrlWithJar() throws Exception {
		ClassPathResource archive = new ClassPathResource("/org/springframework/orm/jpa/jpa-archive.jar");
		String newRoot = "jar:" + archive.getURL().toExternalForm() + "!/META-INF/persist.xml";
		Resource insideArchive = new UrlResource(newRoot);
		// make sure the location actually exists
		assertThat(insideArchive.exists()).isTrue();
		URL url = PersistenceUnitReader.determinePersistenceUnitRootUrl(insideArchive);
		assertThat(archive.getURL().sameFile(url)).as("the archive location should have been returned").isTrue();
	}

	@Test
	public void testJpa1ExcludeUnlisted() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-exclude-1.0.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertNotNull(info);
		assertEquals("The number of persistence units is incorrect.", 4, info.length);

		PersistenceUnitInfo noExclude = info[0];
		assertNotNull("noExclude should not be null.", noExclude);
		assertThat((Object) noExclude.getPersistenceUnitName()).as("noExclude name is not correct.").isEqualTo("NoExcludeElement");
		assertThat(noExclude.excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();

		PersistenceUnitInfo emptyExclude = info[1];
		assertNotNull("emptyExclude should not be null.", emptyExclude);
		assertThat((Object) emptyExclude.getPersistenceUnitName()).as("emptyExclude name is not correct.").isEqualTo("EmptyExcludeElement");
		assertThat(emptyExclude.excludeUnlistedClasses()).as("emptyExclude should be true.").isTrue();

		PersistenceUnitInfo trueExclude = info[2];
		assertNotNull("trueExclude should not be null.", trueExclude);
		assertThat((Object) trueExclude.getPersistenceUnitName()).as("trueExclude name is not correct.").isEqualTo("TrueExcludeElement");
		assertThat(trueExclude.excludeUnlistedClasses()).as("trueExclude should be true.").isTrue();

		PersistenceUnitInfo falseExclude = info[3];
		assertNotNull("falseExclude should not be null.", falseExclude);
		assertThat((Object) falseExclude.getPersistenceUnitName()).as("falseExclude name is not correct.").isEqualTo("FalseExcludeElement");
		assertThat(falseExclude.excludeUnlistedClasses()).as("falseExclude should be false.").isFalse();
	}

	@Test
	public void testJpa2ExcludeUnlisted() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-exclude-2.0.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertNotNull(info);
		assertEquals("The number of persistence units is incorrect.", 4, info.length);

		PersistenceUnitInfo noExclude = info[0];
		assertNotNull("noExclude should not be null.", noExclude);
		assertThat((Object) noExclude.getPersistenceUnitName()).as("noExclude name is not correct.").isEqualTo("NoExcludeElement");
		assertThat(noExclude.excludeUnlistedClasses()).as("Exclude unlisted still defaults to false in 2.0.").isFalse();

		PersistenceUnitInfo emptyExclude = info[1];
		assertNotNull("emptyExclude should not be null.", emptyExclude);
		assertThat((Object) emptyExclude.getPersistenceUnitName()).as("emptyExclude name is not correct.").isEqualTo("EmptyExcludeElement");
		assertThat(emptyExclude.excludeUnlistedClasses()).as("emptyExclude should be true.").isTrue();

		PersistenceUnitInfo trueExclude = info[2];
		assertNotNull("trueExclude should not be null.", trueExclude);
		assertThat((Object) trueExclude.getPersistenceUnitName()).as("trueExclude name is not correct.").isEqualTo("TrueExcludeElement");
		assertThat(trueExclude.excludeUnlistedClasses()).as("trueExclude should be true.").isTrue();

		PersistenceUnitInfo falseExclude = info[3];
		assertNotNull("falseExclude should not be null.", falseExclude);
		assertThat((Object) falseExclude.getPersistenceUnitName()).as("falseExclude name is not correct.").isEqualTo("FalseExcludeElement");
		assertThat(falseExclude.excludeUnlistedClasses()).as("falseExclude should be false.").isFalse();
	}

}
