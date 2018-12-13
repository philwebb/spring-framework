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

package org.springframework.core.annotation.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * A collection of {@link DeclaredAnnotation} instances.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 5.2
 */
public interface DeclaredAnnotations extends Iterable<DeclaredAnnotation> {

	/**
	 * {@link DeclaredAnnotation} instances that can be used when there are no
	 * declared annotations.
	 */
	static final DeclaredAnnotations NONE = new SimpleDeclaredAnnotations(null,
			Collections.emptySet());

	/**
	 * Return the source that contained the declared annotations or {@code null}
	 * if the source is not known. The actual instance returned by this method
	 * is technology specific but it should have a valid
	 * {@code equals()}/{@code hashcode()} methods and a {@code toString()} that
	 * can be used for logging. For reflection based implementations, an
	 * {@link AnnotatedElement} should be used as the source.
	 */
	@Nullable
	Object getSource();

	/**
	 * Return the number of declared annotations contained in this collection.
	 * @return the number of declared annotations
	 */
	int size();

	/**
	 * Find a declared annotation of the specified type from this collection.
	 * @param annotationType the type required
	 * @return a declared annotation or {@code null}
	 */
	@Nullable
	default DeclaredAnnotation find(String annotationType) {
		for (DeclaredAnnotation candidate : this) {
			if (candidate.getType().equals(annotationType)) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Create a new {@link DeclaredAnnotations} instance from the source Java
	 * {@link Annotation Annotations}.
	 * @param element the source of the annotations or {@code null} if the
	 * source is unknown
	 * @param annotations the declared annotations
	 * @return a {@link DeclaredAnnotations} instance containing the annotations
	 */
	static DeclaredAnnotations from(@Nullable AnnotatedElement element,
			Annotation... annotations) {
		return from(IntrospectionFailures.LOG, element, annotations);
	}

	/**
	 * Create a new {@link DeclaredAnnotations} instance from the source Java
	 * {@link Annotation Annotations}.
	 * @param introspectionFailures strategy used to determine how introspection
	 * failures should be handled
	 * @param element the source of the annotations or {@code null} if the
	 * source is unknown
	 * @param annotations the declared annotations
	 * @return a {@link DeclaredAnnotations} instance containing the annotations
	 */
	static DeclaredAnnotations from(IntrospectionFailures introspectionFailures,
			@Nullable AnnotatedElement element, Annotation... annotations) {
		Assert.notNull(annotations, "Annotations must not be null");
		return from(introspectionFailures, element, Arrays.asList(annotations));
	}

	/**
	 * Create a new {@link DeclaredAnnotations} instance from the source Java
	 * {@link Annotation Annotations}.
	 * @param element the source of the annotations or {@code null} if the
	 * source is unknown
	 * @param annotations the declared annotations
	 * @return a {@link DeclaredAnnotations} instance containing the annotations
	 */
	static DeclaredAnnotations from(@Nullable AnnotatedElement element,
			Collection<Annotation> annotations) {
		return from(IntrospectionFailures.LOG, element, annotations);
	}

	/**
	 * Create a new {@link DeclaredAnnotations} instance from the source Java
	 * {@link Annotation Annotations}.
	 * @param introspectionFailures strategy used to determine how introspection
	 * failures should be handled
	 * @param element the source of the annotations or {@code null} if the
	 * source is unknown
	 * @param annotations the declared annotations
	 * @return a {@link DeclaredAnnotations} instance containing the annotations
	 */
	static DeclaredAnnotations from(IntrospectionFailures introspectionFailures,
			@Nullable AnnotatedElement element, Collection<Annotation> annotations) {
		Assert.notNull(introspectionFailures, "IntrospectionFailures must not be null");
		Assert.notNull(annotations, "Annotations must not be null");
		List<DeclaredAnnotation> adapted = new ArrayList<>(annotations.size());
		for (Annotation annotation : annotations) {
			try {
				adapted.add(DeclaredAnnotation.from(annotation));
			}
			catch (Throwable ex) {
				introspectionFailures.handle(element, ex);
			}
		}
		return of(element, adapted);
	}

	/**
	 * Create a new in-memory {@link DeclaredAnnotations} instance containing
	 * the specified annotations.
	 * @param source the annotation source or {@code null} if the source is
	 * unknown
	 * @param annotations the contained annotations
	 * @return a new {@link DeclaredAnnotations} instance
	 */
	static DeclaredAnnotations of(@Nullable Object source,
			DeclaredAnnotation... annotations) {
		return new SimpleDeclaredAnnotations(source, annotations);
	}

	/**
	 * Create a new in-memory {@link DeclaredAnnotations} containing the
	 * specified annotations.
	 * @param annotations the contained annotations
	 * @return a new {@link DeclaredAnnotations} instance
	 */
	static DeclaredAnnotations of(@Nullable Object source,
			Collection<DeclaredAnnotation> annotations) {
		return new SimpleDeclaredAnnotations(source, annotations);
	}

	/**
	 * Strategies for dealing with annotation introspection failures.
	 */
	enum IntrospectionFailures {

		/**
		 * Drop the annotation and log the problem at INFO level.
		 */
		LOG {

			@Override
			protected void handle(AnnotatedElement element, Throwable failure) {
				Log logger = getLogger();
				if (logger.isInfoEnabled()) {
					logger.info("Failed to introspect annotations"
							+ (element != null ? " on " + element : "") + ": " + failure);
				}
			}

		},

		/**
		 * Silently drop the annotation and continue.
		 */
		IGNORE {

			@Override
			protected void handle(AnnotatedElement element, Throwable failure) {
			}

		},

		/**
		 * Propagate the thrown failure.
		 */
		THROW {

			protected void handle(AnnotatedElement element, Throwable failure) {
				ReflectionUtils.rethrowRuntimeException(failure);
			}

		};

		@Nullable
		private static Log logger;

		protected abstract void handle(AnnotatedElement element, Throwable failure);

		protected final static Log getLogger() {
			Log logger = IntrospectionFailures.logger;
			if (logger == null) {
				logger = LogFactory.getLog(DeclaredAnnotations.class);
				IntrospectionFailures.logger = logger;
			}
			return logger;
		}

	}

}
