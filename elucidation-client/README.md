# Elucidation Client
Client code to assist in sending connection events to the elucidation server

Installation
---
* Maven

```xml
<dependency>
  <groupId>com.fortitudetec</groupId>
  <artifactId>elucidation-client</artifactId>
  <version>1.0</version>
</dependency>
```

Usage
---
```java
ElucidationClient client = ElucidationClient.of(recorder, eventFactory);
client.recordNewEvent(data);
```

#### Building the event recorder
The `ElucidationEventRecorder` can be built in 3 ways:
* Provide the base url to the elucidation server
* Provide a custom Jersey client and the base url to the elucidation server
* Provide a custom Jersey client and a `Supplier<String>` that will return the base url on demand

By default, if not provided, the recorder will create a new Jersey client to use to communicate with the elucidation server, however, 
there may be times when some customizations to the client are necessary (i.e. connect and read timeouts).

#### Creating an Event Factory
The event factory is of type `Function<T, Optional<ConnectionEvent>>`.  This will allow the implementor to custom build out the 
`ConnectionEvent` object on demand.  The function will recieve data (T) and return an Optional consisting of the build `ConnectionEvent` object.
If the Optional returns empty, then the recording will be skipped.

#### Recording status
Once `recordNewEvent` is called, a `CompletableFuture` is returned that will allow you go access the result of the record.  The result
will contain:
* status
    * RECORDED_OK
    * ERROR_RECORDING
    * SKIPPED_RECORDING
* one of the following fields
    * skipMessage
    * errorMessage
    * exception

---
Copyright (c) 2018, Fortitude Technologies, LLC