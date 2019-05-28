# Elucidation Server

A bundle for Dropwizard to support the collection and reporting of Elucidation events.

## Usage

### Add dependency
#### Maven
```xml
    <dependency>
        <groupId>com.fortitudetec</groupId>
        <artifactId>elucidation-bundle</artifactId>
        <version>2.0.0-SNAPSHOT</version>
    </dependency>
```

### Add a bundle to your application
```java
    @Override
    public void initialize(final Bootstrap<AppConfiguration> bootstrap) {
        ...
        bootstrap.addBundle(new ElucidationBundle() {
            @Override
            public PooledDataSourceFactory getDataSourceFactory(AppConfiguration configuration) {
                return dataSourceFactory;
            }
        });
        ...
    }
```

This will setup the Elucidation bundle with the default settings and using your dataSourceFactory. __NOTE:__ This bundle requires JDBI 3!

One setting you can override at this point is the _timeToLive_ property.  This property sets how long events will remain in the database.  The default is 7 days.

---
Copyright (c) 2018, Fortitude Technologies, LLC