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

import java.lang.reflect.AnnotatedElement;

/**
 * {@link AnnotatedSource} backed by an {@link AnnotatedElement} and implemented
 * using standard Java reflection.
 *
 * @author Phillip Webb
 * @since 5.1
 */
class StandardAnnotatedSource<T extends AnnotatedElement> {
//
//	private final T source;
//
//	private final Annotation[] declaredAnnotations;
//
//	StandardAnnotatedSource(T source) {
//		Assert.notNull(source, "Source must not be null");
//		this.source = source;
//		this.declaredAnnotations = source.getDeclaredAnnotations();
//	}
//
//	protected final T getSource() {
//		return this.source;
//	}
//
//	@Override
//	public DeclaredAnnotations getDeclaredAnnotations() {
//		return () -> Arrays.stream(this.declaredAnnotations).map(
//				StandardDeclaredAnnotation::new).map(
//						DeclaredAnnotation.class::cast).iterator();
//	}
//
//	@Override
//	public AnnotatedSource getRelatedSuperClassAnnotationSource() {
//		return null;
//	}
//
//	@Override
//	public Iterable<AnnotatedSource> getRelatedInterfaceAnnotationSources() {
//		return Collections.emptySet();
//	}
//
//	@Override
//	public String toString() {
//		return this.source.toString();
//	}
//
//	/**
//	 * Adapt the specified {@link AnnotatedElement} to an
//	 * {@link AnnotatedSource}.
//	 * @param sourceElement the source element
//	 * @return an {@link AnnotatedSource} instance
//	 */
//	public static AnnotatedSource get(AnnotatedElement sourceElement) {
//		if (sourceElement instanceof Class<?>) {
//			return new StandardClassAnnotatedSource((Class<?>) sourceElement);
//		}
//		if (sourceElement instanceof Method) {
//			return new StandardMethodAnnotatedSource((Method) sourceElement);
//		}
//		return new StandardAnnotatedSource<>(sourceElement);
//	}
//
//	/**
//	 * {@link StandardAnnotatedSource} backed by a {@link Class} and implemented
//	 * using standard Java reflection.
//	 */
//	static class StandardClassAnnotatedSource extends StandardAnnotatedSource<Class<?>> {
//
//		StandardClassAnnotatedSource(Class<?> source) {
//			super(source);
//		}
//
//		@Override
//		public AnnotatedSource getRelatedSuperClassAnnotationSource() {
//			Class<?> superclass = getSource().getSuperclass();
//			if (superclass == null || Object.class == superclass) {
//				return null;
//			}
//			return get(superclass);
//		}
//
//		@Override
//		public Iterable<AnnotatedSource> getRelatedInterfaceAnnotationSources() {
//			return () -> Arrays.stream(getSource().getInterfaces()).map(
//					StandardAnnotatedSource::get).iterator();
//		}
//
//	}
//
//	/**
//	 * {@link StandardAnnotatedSource} backed by a {@link Method} and
//	 * implemented using standard Java reflection.
//	 */
//	static class StandardMethodAnnotatedSource extends StandardAnnotatedSource<Method> {
//
//		StandardMethodAnnotatedSource(Method source) {
//			super(BridgeMethodResolver.findBridgedMethod(source));
//		}
//
//		@Override
//		public AnnotatedSource getRelatedSuperClassAnnotationSource() {
//			Method method = getSource();
//			Class<?> superclass = method.getDeclaringClass().getSuperclass();
//			if (superclass == null || Object.class == superclass) {
//				return null;
//			}
//			for (Method candidate : superclass.getDeclaredMethods()) {
//				if (!Modifier.isPrivate(candidate.getModifiers())
//						&& isOverride(method, candidate)) {
//					return StandardAnnotatedSource.get(candidate);
//				}
//			}
//			return null;
//		}
//
//		@Override
//		public Iterable<AnnotatedSource> getRelatedInterfaceAnnotationSources() {
//			Method method = getSource();
//			Class<?>[] interfaces = method.getDeclaringClass().getInterfaces();
//			Predicate<Method> isOverride = candidate -> isOverride(method, candidate);
//			Stream<Method> candidates = Arrays.stream(interfaces).filter(
//					this::isCandidateInterface).flatMap(this::methods);
//			return () -> candidates.filter(isOverride).map(
//					StandardAnnotatedSource::get).iterator();
//		}
//
//		private boolean isCandidateInterface(Class<?> candidate) {
//			return !ClassUtils.isJavaLanguageInterface(candidate);
//		}
//
//		private Stream<Method> methods(Class<?> type) {
//			return Arrays.stream(type.getMethods());
//		}
//
//		private boolean isOverride(Method method, Method candidate) {
//			if (!candidate.getName().equals(method.getName())
//					|| candidate.getParameterCount() != method.getParameterCount()) {
//				return false;
//			}
//			Class<?>[] paramTypes = method.getParameterTypes();
//			if (Arrays.equals(candidate.getParameterTypes(), paramTypes)) {
//				return true;
//			}
//			for (int i = 0; i < paramTypes.length; i++) {
//				if (paramTypes[i] != ResolvableType.forMethodParameter(candidate, i,
//						method.getDeclaringClass()).resolve()) {
//					return false;
//				}
//			}
//			return true;
//		}
//
//	}
// FIXME
}
