# Elucidation Server

How to start the Elucidation Server application
---

1. Run `mvn clean install` to build your application
1. Start application with `java -jar target/elucidation-server-1.0-SNAPSHOT.jar server config.yml`
1. To check that your application is running enter url `http://localhost:8080`

Health Check
---

To see your applications health enter url `http://localhost:8081/healthcheck`

Configuration
---

* dataSourceFactory - The settings for your database
```yaml
dataSourceFactory:
  driverClass: org.h2.Driver
  url: jdbc:h2:~/test-run
  user: sa
```
* timeToLive - The amount of time connection events should persist (Default: 7 days)
```yaml
timeToLive: 7 days
```
