# Elucidation Bundle

A bundle for Dropwizard to support the collection and reporting of Elucidation events.

## Usage

### Add dependency

#### Maven
```xml
<dependency>
    <groupId>org.kiwiproject</groupId>
    <artifactId>elucidation-bundle</artifactId>
    <version>[current-version]</version>
</dependency>
```

### Add a bundle to your application
```java
@Override
public void initialize(final Bootstrap<AppConfiguration> bootstrap) {

    // other initialization...

    bootstrap.addBundle(new ElucidationBundle<AppConfiguration>() {
        @Override
        public PooledDataSourceFactory getDataSourceFactory(AppConfiguration configuration) {
            return configuration.getDataSourceFactory();
        }
    });
}
```

This will set up the Elucidation bundle with the default settings and using your dataSourceFactory. __NOTE:__ This
bundle requires JDBI 3!

### Configuration

The following sections describe available options for configuring elucidation.

#### Time to Live

This configuration property sets how long events will remain in the database.  The default is 7 days.

#### Communication Definitions

This configuration property defines the various types of communications between a service and other services, and
whether an event is "dependent" or not. It is represented in `ElucidationConfiguration` by
a `List<CommunicationDefinition>`.
The default definitions include "HTTP" and "JMS".

Note this property is currently only configurable in code due to the requirement that a `CommunicationDefinition`
define not only the communication type (a `String`),  but also to implement the `isDependentEvent` method which
determines at runtime whether each incoming event is dependent or not. 

We define an event as "dependent" differently for different types of communication. For example, an event
that represents an HTTP request we are making to some other resource on the internet is considered dependent,
because we require that remote service to exist in order for the call to succeed. Another example of a dependent
event is an incoming message from some asynchronous message source (e.g. RabbitMQ or JMS/ActiveMQ or Kafka),
since we need the other service to produce the message in order for us to consume it.

An example of an event that is <strong>not</strong> dependent include an outgoing asynchronous message
that we publish for others to consume. In this case, we don't know or care if anyone actually consumes the
message and thus don't depend on any other service. Another example of an event that is not dependent is
an incoming HTTP request that we are handling and returning a response to a remote client. In this situation,
while that other service is dependent on us, we are not dependent on it; thus the event is not dependent.

To change or add to the default configuration, you can implement the `CommunicationDefinition` or
`AsyncMessagingCommunicationDefinition` interface. Alternatively you can use the
`CommunicationDefinition#forDependentDirection` factory method if the direction is the only determinant
whether the event is dependent or not.

As an example of using custom communication definitions, suppose we define a configuration class for our
Dropwizard application like this:

```java
@Getter
@Setter
public class AppConfiguration extends Configuration implements ElucidationConfiguration<AppConfiguration> {

    @Valid
    @NotNull
    private DataSourceFactory dataSource = new DataSourceFactory();

    @NotNull
    private Duration timeToLive = Duration.days(14);

    @Override
    public List<CommunicationDefinition> getCommunicationDefinitions(AppConfiguration configuration) {
        var rabbitMq = CommunicationDefinition.forDependentDirection("RabbitMQ", Direction.INBOUND);
        var kafka = CommunicationDefinition.forDependentDirection("Kafka", Direction.INBOUND);
        var gRPC = CommunicationDefinition.forDependentDirection("gRPC", Direction.OUTBOUND);

        return ElucidationConfiguration.defaultDefinitionsAnd(rabbitMq, kafka, gRPC);
    }
}

``` 

Then, in your application's `initialize` method, you simply add the `ElucidationBundle` as shown above. Alternatively,
because `ElucidationBundle` implements `ElucidationConfiguration` you could perform the configuration override
for the communication definitions when adding the bundle, as shown below:

```java
@Override
public void initialize(Bootstrap<AppConfiguration> bootstrap) {
    
    // other initialization...
    
    var elucidationBundle = new ElucidationBundle<AppConfiguration>() {
        @Override
        public PooledDataSourceFactory getDataSourceFactory(AppConfiguration configuration) {
            return configuration.getDataSource();
        }

        @Override
        public List<CommunicationDefinition> getCommunicationDefinitions(AppConfiguration configuration) {
            var rabbitMq = CommunicationDefinition.forDependentDirection("RabbitMQ", Direction.INBOUND);
            var kafka = CommunicationDefinition.forDependentDirection("Kafka", Direction.INBOUND);
            var gRPC = CommunicationDefinition.forDependentDirection("gRPC", Direction.OUTBOUND);

            return ElucidationConfiguration.defaultDefinitionsAnd(rabbitMq, kafka, gRPC);
        }
    };
    bootstrap.addBundle(elucidationBundle);
}
```

Both of the above examples will contain the default communication definitions plus three custom ones defined as
"RabbitMQ", "Kafka", and "gRPC".

### Register JDBI Exception Mappers

This property determine whether to register the JDBI `LoggingSQLExceptionMapper` and `LoggingJdbiExceptionMapper`
components. The default is `true`.

---

Copyright (c) 2023 Elucidation Project \
Copyright (c) 2018 - 2020 Fortitude Technologies, LLC
