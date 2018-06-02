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
ElucidationEventRecorder recorder = new ElucidationEventRecorder("http://localhost:8080");

ConnectionEvent event = ConnectionEvent.builder()
            .eventDirection(Direction.INBOUND)
            .communicationType(CommunicationType.JMS)
            .connectionIdentifier("SOME_MESSAGE")
            .observedAt(ZonedDateTime.now())
            .serviceName("my-service")
            .build();

recorder.recordNewEvent(event);
```

#### Synchronous recording
By default all recordings happen asynchronously, but if synchronous recording is required there is an overloaded method for `recordNewEvent` that lets you switch.
```java
recorder.recordNewEvent(event, false);
```

#### Providing custom client
By default an new recorder will create a new Jersey client object to use, however, you can force the recorder to use a custom one by passing it into the constructor.
```java
Client client = ClientBuilder.newClient();
ElucidationEventRecorder recorder = new ElucidationEventRecorder(client, "http://localhost:8080");
```

---
Copyright (c) 2018, Fortitude Technologies, LLC