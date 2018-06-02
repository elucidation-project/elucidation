# Elucidation Server

Running the server
---

1. Download the elucidation-server.jar
2. Create a config.yml file (Add in at least your database configuration)
3. Start application with `java -jar elucidation-server.jar server config.yml`
4. To check that your application is running enter url `http://localhost:8080`

Health Check
---

To see your applications health enter url `http://localhost:8081/healthcheck`

Configuration
---

* dataSourceFactory - The settings for your database (currently only supports hsql and postgresql)

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

Building the server
---

To build the server, download the code and run `mvn clean package`.  The server jar will be found in the target directory