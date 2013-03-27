# Spring Framework 4 Sneak Peek: Embedded Servlet Containers

Embedded servlet containers provide an easy way to develop standalone web applications that can 'just run'. Jetty was probably the first server to offerer embedded support, but these days most servlet containers (including Tomcat) and even many full Java EE application servers can be used in an embedded way. Spring has always strived to be the framework that you can use anywhere, and whilst it is certainly possible to use an embedded Servlet container with 3.x, the [forthcoming Spring Framework 4.0](TODO) will make it significantly easier.


## Background

Before discussing embedded servlet containers it is worth a refresher on how classic deployments currently work. Usually here, your Spring application is packaged as a `WAR` file before being deployed to a server. Lets assume that you have a container that supports [Servlet 3.0 style registration](TODO), you might have a Spring [web app initializer](TODO) that looks like this:

    public class MyWebAppInitializer implements WebApplicationInitializer {

      @Override
      public void onStartup(ServletContext servletContext) throws ServletException {
        AnnotationConfigWebApplicationContext context =
            new AnnotationConfigWebApplicationContext(MyConfiguration.class);
        ServletRegistration.Dynamic registration = servletContext.addServlet(
          "dispatcher", new DispatcherServlet(context));
        registration.setLoadOnStartup(1);
        registration.addMapping("/");
      }

    }

    @EnableWebMvc
    @Import(MyController.class)
    public MyConfiguration {
    }

__NOTE:__ `AbstractDispatcherServletInitializer` is usually a better choice for this type of initialization.

Deploying this application to a running container will cause a number of things to happen:

- The `MyWebAppInitializer` class will be automatically found and the `onStartup` method will be called.
- A new `AnnotationConfigWebApplicationContext` will be created. The context will be configured using `MyConfiguration.class` when it is refreshed, but this does not happen just yet.
- A `DispatcherServlet` is registered with the container.
- Sometime later the `DispatcherServlet` will be loaded.
- The `DispatcherServlet` will 'refresh' the Spring context, initializing all beans.

For Servlet 2.5 containers the process is similar, with the exception that the Spring context will be created by the `DispatcherServlet` and loaded using the value of the `contextConfigLocation` `init-param`.

The application will remain running until the `DispatcherServlet` is stopped. The important point it that `DispatcherServlet` is responsible for managing the Spring context.


## Embedded Containers

To get the most from using an embedded servlet container we really want to flip the classic deployment model on its head. We want the Spring context to be created and refreshed first so that it can contain and manage all aspects of the application. The Spring context itself will contain everything needed to configure and start the embedded servlet container.

The new `EmbeddedWebApplicationContext` is a special type of `WebApplicationContext` that supports these ideas. Lets take a look at an example:

    public class MySpringApplication {

      public static void main(String... args) {
        AnnotationConfigEmbeddedWebApplicationContext context =
            new AnnotationConfigEmbeddedWebApplicationContext();
        context.register(MyConfig.class);
        context.refresh();

        // The XmlEmbeddedWebApplicationContext is also available
        // if you prefer XML configuration
      }

    }

    @Configuration
    @EnableWebMvc
    @Import(MyController.class)
    public class MyConfiguration {

      @Bean
      public EmbeddedServletContainerFactory servletContainerFactory() {
        return new TomcatEmbeddedServletContainerFactory();
      }

      @Bean
      public DispatcherServlet dispatcher() {
        return new DispatcherServlet();
      }

    }

Running `MySpringApplication.main()` will start the server and listen for requests on port 8080. We are free to use the usual Spring MVC semantics to handle requests, for example requests to `http://localhost:8080/hello` would respond "Hello World!" with the following `@Controller`:

    @Controller
    public class MyController {

      @RequestMapping("/hello")
      @ResponseBody
      public String sayHello() {
        return "Hello World!"
      }

    }

The main difference between our new application and the earlier example is that we no longer have a `WebApplicationInitializer`. Instead the `DispatcherServlet` is now defined in the same way as any other Spring bean. We also must define a single `EmbeddedServletContainerFactory` bean that will be used to create and start the embedded servlet container. In this example I have used Tomcat but you could easily switch to `JettyEmbeddedServletContainerFactory` if you prefer.

Here are the steps that now happen when the application runs:

- The `MySpringApplication` will create, configure and refresh a `AnnotationConfigEmbeddedWebApplicationContext`.
- The `EmbeddedWebApplicationContext` will used a single `EmbeddedServletContainerFactory` bean to create the servlet container.
- Any `Servlet` and `Filter` beans will be registered with the servlet container.
- The servlet container will be started.


## Container Configuration

The `TomcatEmbeddedServletContainerFactory` and `JettyEmbeddedServletContainerFactory` can be configured in the same way as any other Spring bean. For example, you can use the `setPort` property if you don't like the default choice of 8080. One nice advantage of of this approach is that you can easily mix-in other Spring concepts when configuring your embedded container. For example, we can support a command line `--port` option with just a few more lines of code:

    public class MySpringApplication {

      public static void main(String... args) {
        AnnotationConfigEmbeddedWebApplicationContext context =
          new AnnotationConfigEmbeddedWebApplicationContext();

        // Add support for command line property sources
        context.getEnvironment().getPropertySources().addFirst(
            new SimpleCommandLinePropertySource(args));

        // Register PropertySourcesPlaceholderConfigurer to support
        // ${variable} expansion
        context.register(MyConfig.class, PropertySourcesPlaceholderConfigurer.class);

        context.refresh();
      }

    }

    @Configuration
    @EnableWebMvc
    @Import(MyController.class)
    public class MyConfiguration {

      @Value("${port:8080}")
      private int port;

      @Bean
      public EmbeddedServletContainerFactory servletContainerFactory() {
        return new TomcatEmbeddedServletContainerFactory(this.port);
      }

      @Bean
      public DispatcherServlet dispatcher() {
        return new DispatcherServlet();
      }

    }


## Advanced ServletContext Initialization

By default the `EmbeddedServletContainerFactory` will search and register all the `javax.servlet.Servlet` and `java.servlet.Filter` beans that it contains. A single servlet will be mapped to the URL `/*`, or if your context defines multiple servlets the lowercase 'bean name' will be used as a prefix (for example, `/myservletbean1/*`, `/myservletbean2/*`). Filters are always mapped to all URLs.

In some situations you might need more control of the registration process. For example, you might want the same servlet mapped to several URLs, or you might want a custom filter mapping. In a classic deployment you would usual deal with this type of custom configuration in your `WebApplicationInitiializer`, with the embedded context the new `ServletContextInitializer` interface can be used. The ServletContextInitializer interface is identical to WebApplicationInitiializer, with the exception that it will not be scanned and loaded automatically by the servlet container. Defining one or more ServletContextInitializer beans signals to the EmbeddedServletContainerFactory that it should not automatically register servlet or filter beans.

For convenience a couple of ServletContextInitializer implementations are provided with Spring. You can use the `ServletRegistrationBean` and `FilterRegistrationBean` to register servlets and filters respectively.

For example:

    @Configuration
    public class MyConfiguration {

      @Bean
      public ServletRegistrationBean customServlet() {
        ServletRegistrationBean registration = new ServletRegistrationBean();
        registration.setServlet(dispatcher());
        registration.addUrlMappings("/spring/*", "*.spring")
      }

      @Bean
      public DispatcherServlet dispatcher() {
        return new DispatcherServlet();
      }

    }

## Summary

It should be very easy to use an embedding Tomcat or Jetty server with Spring Framework 4, opening up all sorts of interesting new ways to run and deploy your applications. Using an embedded servlet container allows you to take precise control of how your application runs and really reduces the burden for people wanting to try your application.

Please do try the Spring Framework 4 milestone releases and provide feedback!

