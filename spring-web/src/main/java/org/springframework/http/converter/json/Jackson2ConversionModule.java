
package org.springframework.http.converter.json;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

// FIXME Javadoc

/**
 * Jackson {@link Module} providing integration with Spring's {@link ConversionService}.
 *
 * @author Phillip Webb
 */
public class Jackson2ConversionModule extends Module {

	private static final List<TypeDescriptor> TARGET_CANDIDATE_TYPES = Collections.unmodifiableList(Arrays.asList(
			TypeDescriptor.valueOf(Number.class), TypeDescriptor.valueOf(String.class)));

	private static final Class<?>[] EXCLUDED_SOURCE_CLASSES = { String.class,
		Iterator.class, Iterable.class };

	private final ConversionService conversionService;

	public Jackson2ConversionModule(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}

	@Override
	public String getModuleName() {
		return "Spring Conversion Support";
	}

	@Override
	public Version version() {
		return Version.unknownVersion();
	}

	@Override
	public void setupModule(SetupContext context) {
		context.appendAnnotationIntrospector(new ConversionAnnotationIntrospector());
	}

	/**
	 * Returns a {@link ConvertiblePair} for the annotated element or {@code null} if the
	 * element cannot be converted.
	 * @param annotated the annotated type
	 * @return a {@link ConvertiblePair} or {@code null}
	 * @see #asTypeDescriptor(Annotated)
	 */
	protected ConvertiblePair getConvertiblePair(Annotated annotated) {
		TypeDescriptor sourceType = asTypeDescriptor(annotated);
		for (TypeDescriptor targetType : getTargetCandidateTypes()) {
			if (isCandidate(sourceType, targetType)) {
				if (this.conversionService.canConvert(sourceType, targetType)) {
					return new ConvertiblePair(sourceType, targetType);
				}
			}
		}
		return null;
	}

	/**
	 * Adapts a {@link Annotated} type to a equivalent {@link TypeDescriptor}.
	 * @param annotated the annotated type
	 * @return an equivalent type descriptor
	 */
	protected final TypeDescriptor asTypeDescriptor(Annotated annotated) {
		if (annotated instanceof AnnotatedMethod) {
			PropertyDescriptor descriptor = BeanUtils.findPropertyForMethod(((AnnotatedMethod) annotated).getAnnotated());
			return new TypeDescriptor(new Property(descriptor.getPropertyType(),
					descriptor.getReadMethod(), descriptor.getWriteMethod()));
		}
		if (annotated instanceof AnnotatedField) {
			return new TypeDescriptor(((AnnotatedField) annotated).getAnnotated());
		}
		if (annotated instanceof AnnotatedParameter) {
			AnnotatedParameter annotatedParameter = (AnnotatedParameter) annotated;
			AnnotatedWithParams owner = annotatedParameter.getOwner();
			if (owner instanceof AnnotatedConstructor) {
				return new TypeDescriptor(new MethodParameter(
						((AnnotatedConstructor) owner).getAnnotated(),
						annotatedParameter.getIndex()));
			}
			if (owner instanceof AnnotatedMethod) {
				return new TypeDescriptor(new MethodParameter(
						((AnnotatedMethod) owner).getAnnotated(),
						annotatedParameter.getIndex()));
			}
		}
		return TypeDescriptor.valueOf(annotated.getRawType());
	}

	/**
	 * Returns an ordered list of candidate target {@link TypeDescriptor}s. By default
	 * this method returns a list containing [{@link Number}, {@link String}]. Candidates
	 * are further refined by the {@link #isCandidate(TypeDescriptor, TypeDescriptor)}
	 * method.
	 * @return a list of target candidate types
	 * @see #isCandidate(TypeDescriptor, TypeDescriptor)
	 */
	protected List<TypeDescriptor> getTargetCandidateTypes() {
		return TARGET_CANDIDATE_TYPES;
	}

	/**
	 * Determine if the specified source and target type pair are a candidate for
	 * conversion. The first candidates that
	 * {@link ConversionService#canConvert(TypeDescriptor, TypeDescriptor) can be
	 * converted} will be used during JSON serialization and deserialization.
	 * @param sourceType the source type
	 * @param targetType that target type
	 * @return {@code true} if the pair is a candidate.
	 */
	protected boolean isCandidate(TypeDescriptor sourceType, TypeDescriptor targetType) {

		// Arrays, Collections and Maps are handled by Jackson
		if (sourceType.isArray() || sourceType.isCollection() || sourceType.isMap()) {
			return false;
		}

		// Iterable Types and Strings are never converted
		for (Class<?> excludedSourceClass : EXCLUDED_SOURCE_CLASSES) {
			if (excludedSourceClass.isAssignableFrom(sourceType.getObjectType())) {
				return false;
			}
		}

		// Numbers are only converted if they have @NumberFormat
		if (Number.class.isAssignableFrom(sourceType.getObjectType())) {
			return (sourceType.hasAnnotation(NumberFormat.class) && targetType.getType().equals(
					String.class));
		}

		// Same type matches are not supported
		if (sourceType.getObjectType().equals(targetType.getObjectType())) {
			return false;
		}

		return true;
	}


	/**
	 * Pair of source and target {@link TypeDescriptor}s that can be converted.
	 */
	protected final static class ConvertiblePair {

		private final TypeDescriptor sourceType;

		private final TypeDescriptor targetType;

		public ConvertiblePair(TypeDescriptor sourceType, TypeDescriptor targetType) {
			Assert.notNull(sourceType, "SourceType must not be null");
			Assert.notNull(targetType, "TargetType must not be null");
			this.sourceType = sourceType;
			this.targetType = targetType;
		}

		public TypeDescriptor getSourceType() {
			return this.sourceType;
		}

		public TypeDescriptor getTargetType() {
			return this.targetType;
		}
	}


	/**
	 * {@link AnnotationIntrospector} providing integration with Spring's
	 * {@link ConversionService}.
	 */
	private class ConversionAnnotationIntrospector extends NopAnnotationIntrospector {

		@Override
		public Object findSerializer(Annotated annotated) {
			ConvertiblePair convertiblePair = getConvertiblePair(annotated);
			return convertiblePair == null ? null : new ConvertingJsonSerializer(
					convertiblePair);
		}

		@Override
		public Object findDeserializer(Annotated annotated) {
			ConvertiblePair convertiblePair = getConvertiblePair(annotated);
			return convertiblePair == null ? null : new ConvertingJsonDeserializer(
					convertiblePair);
		}
	}


	/**
	 * {@link JsonSerializer} that converts values as they are written.
	 */
	private class ConvertingJsonSerializer extends JsonSerializer<Object> implements
			ContextualSerializer {

		private final ConvertiblePair convertiblePair;

		private JsonSerializer<Object> targetSerializer;

		public ConvertingJsonSerializer(ConvertiblePair convertiblePair) {
			this.convertiblePair = convertiblePair;
		}

		public JsonSerializer<?> createContextual(SerializerProvider provider,
				BeanProperty property) throws JsonMappingException {
			this.targetSerializer = provider.findValueSerializer(
					this.convertiblePair.getTargetType().getType(), property);
			return this;
		}

		@Override
		public void serialize(Object value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException, JsonProcessingException {
			Object convertedValue = Jackson2ConversionModule.this.conversionService.convert(
					value, this.convertiblePair.getSourceType(),
					this.convertiblePair.getTargetType());
			this.targetSerializer.serialize(convertedValue, jgen, provider);
		}
	}


	/**
	 * {@link JsonDeserializer} that converts values as they are read.
	 */
	private class ConvertingJsonDeserializer extends JsonDeserializer<Object> {

		private final ConvertiblePair convertiblePair;

		public ConvertingJsonDeserializer(ConvertiblePair convertiblePair) {
			this.convertiblePair = convertiblePair;
		}

		@Override
		public Object deserialize(JsonParser jsonParser, DeserializationContext context)
				throws IOException, JsonProcessingException {
			// Note: conversion here is in reverse order (target -> source)
			Object value = jsonParser.readValueAs(this.convertiblePair.getTargetType().getType());
			return Jackson2ConversionModule.this.conversionService.convert(value,
					this.convertiblePair.targetType, this.convertiblePair.sourceType);
		}
	}
}
