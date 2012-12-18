
package org.springframework.http.converter.json;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.support.DefaultFormattingConversionService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Jackson2ConversionModuleTests {

	// FIXME proper tests

	@Test
	public void test() throws IOException {
		ConversionService conversionService = new DefaultFormattingConversionService();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new Jackson2ConversionModule(conversionService));
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		StringWriter writer = new StringWriter();
		JsonGenerator generator = mapper.getJsonFactory().createJsonGenerator(writer);
		Person person = new Person();
		person.setName(new Name(Title.MR, "Phillip", "Webb"));
		person.setDate(new Date());
		person.setAge(12);
		person.getNames().add(new Name(Title.MISS, "Test1A", "Test1B"));
		person.getNames().add(new Name(Title.MRS, "Test2A", "Test2B"));
		mapper.writeValue(generator, person);
		System.out.println(writer);

		JsonParser parser = mapper.getJsonFactory().createJsonParser(
				new StringReader(writer.toString()));
		Person person2 = parser.readValueAs(Person.class);
		System.out.println(person2.getName());

	}

	static class Person {

		private Name name = new Name();

		@NumberFormat(pattern="A#####")
		private int age;

		private List<Name> names = new ArrayList<Name>();

		@DateTimeFormat(pattern = "yyyy#MM#dd")
		private Date date;

		public Name getName() {
			return name;
		}

		public void setName(Name name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public List<Name> getNames() {
			return names;
		}

		public void setNames(List<Name> names) {
			this.names = names;
		}
	}

	static class Name {

		private Title title;

		private String first;

		private String last;

		public Name() {
		}

		public Name(Title title, String first, String last) {
			super();
			this.title = title;
			this.first = first;
			this.last = last;
		}

		public Title getTitle() {
			return title;
		}

		public void setTitle(Title title) {
			this.title = title;
		}

		public String getFirst() {
			return first;
		}

		public void setFirst(String first) {
			this.first = first;
		}

		public String getLast() {
			return last;
		}

		public void setLast(String last) {
			this.last = last;
		}
	}

	enum Title {
		MR, MRS, MISS
	}
}
