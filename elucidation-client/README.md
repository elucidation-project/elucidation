# Elucidation Client
Client code to assist in sending connection events to the elucidation server

Installation
---
* Maven

```xml
<dependency>
  <groupId>org.kiwiproject</groupId>
  <artifactId>elucidation-client</artifactId>
  <version>[current-version]</version>
</dependency>
```

Usage
---
```java
ElucidationClient client = ElucidationClient.of(recorder, eventFactory);
client.recordNewEvent(data);
```

#### Building the event recorder
The `ElucidationRecorder` can be built in 3 ways:
* Provide the base url to the elucidation server
* Provide a custom Jersey client and the base url to the elucidation server
* Provide a custom Jersey client and a `Supplier<String>` that will return the base url on demand

By default, if not provided, the recorder will create a new Jersey client to use to communicate with the elucidation server, however, 
there may be times when some customizations to the client are necessary (i.e. connect and read timeouts).

#### Creating an Event Factory
The event factory is of type `Function<T, Optional<ConnectionEvent>>`.  This will allow the implementor to custom build out the 
`ConnectionEvent` object on demand.  The function will receive data (T) and return an Optional consisting of the built `ConnectionEvent` object.
If the Optional returns empty, then the recording will be skipped.

#### Recording status
Once `recordNewEvent` is called, a `CompletableFuture` is returned that will allow you to access the result of the record.
The result will contain:
* status
    * SUCCESS
    * ERROR
    * SKIPPED
* one of the following fields
    * skipMessage
    * errorMessage
    * exception

#### Track Identifiers
The `ElucidationClient` has a method that allows the recording of identifiers to track for usage. Identifiers can be tracked by calling:

```java
client.trackIdentifiers("my-service", "HTTP", List.of("GET /path", "POST /path"));
```
Once `trackIdentifiers` is called, a `CompletableFuture` is returned that will allow you to access the result of the tracking. The result is the same as the result for `recordNewEvent`.

#### Helpers
There are a few helpers that can assist a service in recording and tracking identifiers.

##### EndpointTrackingListener
This `ApplicationEventListener` will inspect the registered resources at startup and load the list of identifiers in elucidation. This listener can be enabled by adding the following to your Dropwizard `App#run` method.

```java
@Override
    public void run(DummyConfig configuration, Environment environment) {
        ...
        environment.jersey().register(new EndpointTrackingListener<>(
                    environment.jersey().getResourceConfig(), 
                    "my-service", 
                    client));
    }
```

##### InboundHttpResultTrackingFilter
This `ContainerRequestFilter` will intercept an HTTP request and record the INBOUND HTTP event. The filter can be enabled by adding the following to your Dropwizard `App#run` method. 

```java
@Override
    public void run(DummyConfig configuration, Environment environment) {
        ...
        environment.jersey().register(new InboundHttpRequestTrackingFilter(
                    "my-service", 
                    client, 
                    new HttpCommunicationDefinition()));
    }
```

In the event that you would like to have this filter also record accompanying OUTBOUND events to limit client side code, you can enable that by adding a 4th parameter which specifies a HTTP Header name that will contain the name of the service making the call. An example would be:
```java
@Override
    public void run(DummyConfig configuration, Environment environment) {
        ...
        environment.jersey().register(new InboundHttpRequestTrackingFilter(
                    "my-service", 
                    client, 
                    new HttpCommunicationDefinition(),
                    InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER));
    }
```

---
Copyright (c) 2018 - 2020 Fortitude Technologies, LLC
