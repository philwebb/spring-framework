/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.SimpleInstantiationStrategy;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Encapsulates the logic required to invoke a {@code @Bean} method contained in a
 * {@code @Configuration} class. Ensures that real bean method is only called when the
 * bean is first created and delegates to the bean factory for subsequent calls. Also
 * deals with creating {@link FactoryBean} proxies to ensure proper scoping semantics even
 * when working against the FactoryBean instance directly.
 *
 * @author Phillip Webb
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 5.0
 * @see ConfigurationClassEnhancer
 * @see PreProcessedConfiguration
 */
public class BeanMethodInvoker {

	private static final Log logger = LogFactory.getLog(BeanMethodInvoker.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();

	private final ConfigurableBeanFactory beanFactory;

	private final Method proxyMethod;

	private final MethodInvoker realMethodInvoker;

	/**
	 * Create a new {@link BeanMethodInvoker} instance.
	 * 
	 * @param beanFactory the source bean factory
	 * @param proxyMethod a reference to the method
	 * @param realMethodInvoker Interface used to invoke the real bean method when
	 *        necessary
	 */
	BeanMethodInvoker(ConfigurableBeanFactory beanFactory, Method proxyMethod,
			MethodInvoker realMethodInvoker) {
		this.beanFactory = beanFactory;
		this.proxyMethod = proxyMethod;
		this.realMethodInvoker = realMethodInvoker;
	}

	/**
	 * Invoke the bean method, returning a result from the BeanFactory. The real bean
	 * method is only invoked when the bean factory requires it.
	 * 
	 * @param args the method arguments
	 * @return the method result
	 * @throws UndeclaredThrowableException on an checked exception from the real method
	 * @see #invokeOrThrow(Object...)
	 */
	public <T> T invoke(Object... args) {
		try {
			return invokeOrThrow(args);
		}
		catch (Throwable ex) {
			ReflectionUtils.rethrowRuntimeException(ex);
			return null;
		}

	}

	/**
	 * Invoke the bean method, returning a result from the BeanFactory. The real bean
	 * method is only invoked when the bean factory requires it.
	 * 
	 * @param args the method arguments
	 * @return the method result
	 * @throws Throwable on invocation exception
	 * @see #invoke(Object...)
	 */
	@SuppressWarnings("unchecked")
	public <T> T invokeOrThrow(Object... args) throws Throwable {
		String beanName = getBeanName();
		Object enhancedFactoryBean = getEnhancedFactoryBean(beanName);
		if (enhancedFactoryBean != null) {
			return (T) enhancedFactoryBean;
		}
		if (isCurrentlyInvokedFactoryMethod()) {
			return (T) invokeRealMethod(args);
		}
		return (T) getBeanInstanceFromFactory(beanName, args);
	}

	private String getBeanName() {
		String name = BeanAnnotationHelper.determineBeanNameFor(this.proxyMethod);
		if (isScopedProxy()) {
			String scopedBeanName = ScopedProxyCreator.getTargetBeanName(name);
			if (this.beanFactory.isCurrentlyInCreation(scopedBeanName)) {
				return scopedBeanName;
			}
		}
		return name;
	}

	private boolean isScopedProxy() {
		Scope scope = AnnotatedElementUtils.findMergedAnnotation(this.proxyMethod,
				Scope.class);
		return (scope != null) && (scope.proxyMode() != ScopedProxyMode.NO);
	}

	/**
	 * If the requested bean is a {@link FactoryBean} create a subclass proxy that
	 * intercepts calls to {@code getObject()} and returns any cached bean instance. This
	 * ensures that the semantics of calling a FactoryBean from within {@code @Bean}
	 * method is the same as that of referring to a FactoryBean within XML (see SPR-6602).
	 * 
	 * @param beanName the bean name
	 * @return an enhanced factory bean or {@code null}
	 */
	private Object getEnhancedFactoryBean(String beanName) {
		if (isFactoryBean(beanName)) {
			Object factoryBean = getFactoryBean(beanName);
			if (canEnhanceFactoryBean(factoryBean)) {
				return enhanceFactoryBean(beanName, factoryBean);
			}
		}
		return null;
	}

	private boolean isFactoryBean(String beanName) {
		return factoryContainsBean(this.beanFactory,
				BeanFactory.FACTORY_BEAN_PREFIX + beanName)
				&& factoryContainsBean(this.beanFactory, beanName);
	}

	/**
	 * Check the BeanFactory to see whether the bean named <var>beanName</var> already
	 * exists. Accounts for the fact that the requested bean may be "in creation", i.e.:
	 * we're in the middle of servicing the initial request for this bean. From an
	 * enhanced factory method's perspective, this means that the bean does not actually
	 * yet exist, and that it is now our job to create it for the first time by executing
	 * the logic in the corresponding factory method.
	 * <p>
	 * Said another way, this check repurposes
	 * {@link ConfigurableBeanFactory#isCurrentlyInCreation(String)} to determine whether
	 * the container is calling this method or the user is calling this method.
	 * 
	 * @param beanName name of bean to check for
	 * @return whether <var>beanName</var> already exists in the factory
	 */
	private boolean factoryContainsBean(ConfigurableBeanFactory beanFactory,
			String beanName) {
		return (beanFactory.containsBean(beanName)
				&& !beanFactory.isCurrentlyInCreation(beanName));
	}

	private Object getFactoryBean(String beanName) {
		return this.beanFactory.getBean(BeanFactory.FACTORY_BEAN_PREFIX + beanName);
	}

	private boolean canEnhanceFactoryBean(Object factoryBean) {
		return !(factoryBean instanceof ScopedProxyFactoryBean);
	}

	/**
	 * Invoke the actual implementation of the method to actually create the bean
	 * instance. This is called when the factory is calling the bean method in order to
	 * instantiate and register the bean (i.e. via a getBean() call).
	 * 
	 * @param args the method arguments
	 * @return the method result
	 * @throws Throwable on error
	 */
	private Object invokeRealMethod(Object... args) throws Throwable {
		if (logger.isWarnEnabled() && hasReturnType(BeanFactoryPostProcessor.class)) {
			logger.warn(String.format(
					"@Bean method %s.%s is non-static and returns an object "
							+ "assignable to Spring's BeanFactoryPostProcessor interface. This will "
							+ "result in a failure to process annotations such as @Autowired, "
							+ "@Resource and @PostConstruct within the method's declaring "
							+ "@Configuration class. Add the 'static' modifier to this method to avoid "
							+ "these container lifecycle issues; see @Bean javadoc for complete details.",
					this.proxyMethod.getDeclaringClass().getSimpleName(),
					this.proxyMethod.getName()));
		}
		return this.realMethodInvoker.invoke(args);
	}

	private boolean hasReturnType(Class<?> type) {
		return type.isAssignableFrom(this.proxyMethod.getReturnType());
	}

	/**
	 * Called when the user (i.e. not the factory) is requesting this bean through a call
	 * to the bean method, direct or indirect. The bean may have already been marked as
	 * 'in creation' in certain autowiring scenarios; if so, temporarily set the
	 * in-creation status to false in order to avoid an exception.
	 * 
	 * @param beanName the bean name
	 * @param args the method args
	 * @return the bean instance
	 */
	private Object getBeanInstanceFromFactory(String beanName, Object[] args) {
		boolean alreadyInCreation = this.beanFactory.isCurrentlyInCreation(beanName);
		try {
			if (alreadyInCreation) {
				this.beanFactory.setCurrentlyInCreation(beanName, false);
			}
			Object beanInstance = getBean(beanName, args);
			checkReturnType(beanName, beanInstance);
			Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
			if (currentlyInvoked != null) {
				String outerBeanName = BeanAnnotationHelper.determineBeanNameFor(
						currentlyInvoked);
				this.beanFactory.registerDependentBean(beanName, outerBeanName);
			}
			return beanInstance;
		}
		finally {
			if (alreadyInCreation) {
				this.beanFactory.setCurrentlyInCreation(beanName, true);
			}
		}
	}

	private Object getBean(String beanName, Object[] args) {
		if (canUseArgs(beanName, args)) {
			return this.beanFactory.getBean(beanName, args);
		}
		return this.beanFactory.getBean(beanName);
	}

	private boolean canUseArgs(String beanName, Object[] args) {
		if (ObjectUtils.isEmpty(args)) {
			return false;
		}
		if (this.beanFactory.isSingleton(beanName)) {
			// Stubbed null arguments just for reference purposes,
			// expecting them to be autowired for regular singleton references?
			// A safe assumption since @Bean singleton arguments cannot be optional...
			for (Object arg : args) {
				if (arg == null) {
					return false;
				}
			}
		}
		return true;
	}

	private void checkReturnType(String beanName, Object beanInstance) {
		if (!ClassUtils.isAssignableValue(this.proxyMethod.getReturnType(),
				beanInstance)) {
			String msg = String.format(
					"@Bean method %s.%s called as a bean reference "
							+ "for type [%s] but overridden by non-compatible bean instance of type [%s].",
					this.proxyMethod.getDeclaringClass().getSimpleName(),
					this.proxyMethod.getName(),
					this.proxyMethod.getReturnType().getName(),
					beanInstance.getClass().getName());
			try {
				BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition(
						beanName);
				msg += " Overriding bean of same name declared in: "
						+ beanDefinition.getResourceDescription();
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore - simply no detailed message then.
			}
			throw new IllegalStateException(msg);
		}
	}

	/**
	 * Create a subclass proxy that intercepts calls to getObject(), delegating to the
	 * current BeanFactory instead of creating a new instance. These proxies are created
	 * only when calling a FactoryBean from within a Bean method, allowing for proper
	 * scoping semantics even when working against the FactoryBean instance directly. If a
	 * FactoryBean instance is fetched through the container via &-dereferencing, it will
	 * not be proxied. This too is aligned with the way XML configuration works.
	 */
	private Object enhanceFactoryBean(String beanName, Object factoryBean) {
		try {
			boolean finalClass = Modifier.isFinal(factoryBean.getClass().getModifiers());
			boolean finalMethod = Modifier.isFinal(
					factoryBean.getClass().getMethod("getObject").getModifiers());
			if (finalClass || finalMethod) {
				return enhanceFinalFactoryBean(beanName, factoryBean, finalClass);
			}
		}
		catch (NoSuchMethodException ex) {
			// No getObject() method -> shouldn't happen, but as long as nobody is trying
			// to call it...
		}
		return createCglibProxyForFactoryBean(factoryBean, this.beanFactory, beanName);
	}

	private Object enhanceFinalFactoryBean(String beanName, Object factoryBean,
			boolean finalClass) {
		if (this.proxyMethod.getReturnType().isInterface()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Creating interface proxy for FactoryBean '" + beanName
						+ "' of type [" + factoryBean.getClass().getName()
						+ "] for use within " + "another @Bean method because its "
						+ (finalClass ? "implementation class" : "getObject() method")
						+ " is final: Otherwise a getObject() call would not be routed to the factory.");
			}
			return createInterfaceProxyForFactoryBean(factoryBean,
					this.proxyMethod.getReturnType(), this.beanFactory, beanName);
		}
		if (logger.isInfoEnabled()) {
			logger.info("Unable to proxy FactoryBean '" + beanName + "' of type ["
					+ factoryBean.getClass().getName()
					+ "] for use within another @Bean method because its "
					+ (finalClass ? "implementation class" : "getObject() method")
					+ " is final: A getObject() call will NOT be routed to the factory. "
					+ "Consider declaring the return type as a FactoryBean interface.");
		}
		return factoryBean;
	}

	/**
	 * Check whether the given method corresponds to the container's currently invoked
	 * factory method. Compares method name and parameter types only in order to work
	 * around a potential problem with covariant return types (currently only known to
	 * happen on Groovy classes).
	 */
	private boolean isCurrentlyInvokedFactoryMethod() {
		Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
		return (currentlyInvoked != null
				&& this.proxyMethod.getName().equals(currentlyInvoked.getName())
				&& Arrays.equals(this.proxyMethod.getParameterTypes(),
						currentlyInvoked.getParameterTypes()));
	}

	private Object createInterfaceProxyForFactoryBean(final Object factoryBean,
			Class<?> interfaceType, ConfigurableBeanFactory beanFactory,
			String beanName) {
		ClassLoader classloader = factoryBean.getClass().getClassLoader();
		Class<?>[] interfaces = new Class<?>[] { interfaceType };
		InvocationHandler handler = (proxy, method, args) -> {
			if (method.getName().equals("getObject") && args == null) {
				return beanFactory.getBean(beanName);
			}
			return ReflectionUtils.invokeMethod(method, factoryBean, args);
		};
		return Proxy.newProxyInstance(classloader, interfaces, handler);
	}

	private Object createCglibProxyForFactoryBean(final Object factoryBean,
			final ConfigurableBeanFactory beanFactory, final String beanName) {
		Object factoryBeanProxy = getFactoryBeanProxy(factoryBean);

		((Factory) factoryBeanProxy).setCallback(0, new MethodInterceptor() {

			@Override
			public Object intercept(Object obj, Method method, Object[] args,
					MethodProxy proxy) throws Throwable {
				if (method.getName().equals("getObject") && args.length == 0) {
					return beanFactory.getBean(beanName);
				}
				return proxy.invoke(factoryBean, args);
			}
		});

		return factoryBeanProxy;
	}

	private Object getFactoryBeanProxy(final Object factoryBean) {
		Enhancer enhancer = getEnhancer(factoryBean);
		Class<?> factoryBeanClass = enhancer.createClass();
		// Ideally create enhanced FactoryBean proxy without constructor side effects,
		// analogous to AOP proxy creation in ObjenesisCglibAopProxy...
		if (objenesis.isWorthTrying()) {
			try {
				return objenesis.newInstance(factoryBeanClass, enhancer.getUseCache());
			}
			catch (ObjenesisException ex) {
				logger.debug(
						"Unable to instantiate enhanced FactoryBean using Objenesis, "
								+ "falling back to regular construction",
						ex);
			}
		}
		try {
			return ReflectionUtils.accessibleConstructor(factoryBeanClass).newInstance();
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Unable to instantiate enhanced FactoryBean using Objenesis, "
							+ "and regular FactoryBean instantiation via default constructor fails as well",
					ex);
		}
	}

	private Enhancer getEnhancer(final Object factoryBean) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(factoryBean.getClass());
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
		enhancer.setCallbackType(MethodInterceptor.class);
		return enhancer;
	}

	/**
	 * Interface used to invoke the real method.
	 */
	@FunctionalInterface
	static interface MethodInvoker {

		Object invoke(Object... args) throws Throwable;
	}
}
