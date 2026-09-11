# PowSyBl Web Services Commons

[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Slack](https://img.shields.io/badge/slack-powsybl-blueviolet.svg?logo=slack)](https://join.slack.com/t/powsybl/shared_invite/zt-36jvd725u-cnquPgZb6kpjH8SKh~FWHQ)

## Description

**powsybl-ws-commons** is a shared library used by most [GridSuite](https://github.com/gridsuite) / PowSyBl **web service** microservices. It factorizes cross-cutting concerns that would otherwise be duplicated in every server: standardized HTTP error responses, request log sanitization, safe archive (zip/tar) extraction, a Spring Boot auto-configuration module providing sensible defaults (Tomcat hardening, global exception handling), and a default `application.yaml` shared by all services (common HTTP, JPA/Hikari, S3, RabbitMQ and Elasticsearch settings).

It is a plain Java library (not a Spring Boot application): most of its features are optional and only activate through Spring Boot's auto-configuration mechanism when the consuming service is itself a Spring Boot web application.

---

## Technical Stack

- Plain Java library, with **optional** Spring Boot dependencies (`spring-boot`, `spring-boot-autoconfigure` declared `optional=true`) so that using this library does not force a transitive dependency on Spring Boot for non-Spring consumers.
- Spring Boot auto-configuration (`@AutoConfiguration`, `spring.factories`/`AutoConfiguration.imports` mechanism)
- Jackson (`jackson-databind`, `jackson-datatype-jsr310`) for `PowsyblWsProblemDetail` (de)serialization
- Apache Commons Compress (`commons-compress`) for TAR archive handling
- Embedded Tomcat (`tomcat-embed-core`) for connector customization
- Micrometer tracing bridge (OpenTelemetry) — trace id propagated into `PowsyblWsProblemDetail.traceId` via SLF4J `MDC`
- `powsybl-commons` (`ReportResourceBundle` SPI)

---

## Usage

Add the dependency to the consuming service's `pom.xml` (the version is inherited from the `powsybl-ws-dependencies` BOM, no need to specify it):

```xml
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-ws-commons</artifactId>
</dependency>
```

That's it — **no code change is required**. As soon as the jar is on the classpath of a Spring Boot application, `PowsyblWsCommonAutoConfiguration` is automatically picked up (via the standard `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` mechanism), and:

- the Tomcat customization (`encodedSolidusHandling=PASS_THROUGH`) is applied if the service is a web application with an embedded Tomcat,
- the default global exception handler (`BaseExceptionHandler`) is registered,
- the library's own `application.yaml` (default HTTP/JPA/S3/RabbitMQ/Elasticsearch settings shared by GridSuite services) is loaded and merged with the service's own configuration.

Both auto-configured features can be disabled or tuned independently through properties (see [Spring Boot Auto-Configuration](#spring-boot-auto-configuration) below) — for example, to opt out of the default exception handler in favor of a custom one:

```yaml
powsybl-ws:
  autoconfigure:
    base-exception-handler:
      enable: false
```

To use the other utility classes (`LogUtils`, `ZipUtils`, `SecuredZipInputStream`, `SecuredTarInputStream`, error model classes), simply call them directly from the code — they have no Spring dependency and work regardless of auto-configuration.

---

## Spring Boot Auto-Configuration

A Spring Boot `@AutoConfiguration` (`PowsyblWsCommonAutoConfiguration`) is automatically picked up by any consuming Spring Boot application. It is configurable through properties under the `powsybl-ws.autoconfigure.*` prefix.

### Tomcat customization: encoded slashes in URL paths

By default, Tomcat rejects (or mishandles) URL paths containing an encoded slash (`%2F`), as a security measure to avoid path-based filter bypasses. This is a problem for GridSuite/PowSyBl services, since some equipment ids (e.g. CGMES ids) can themselves contain a `/` and are passed as `@PathVariable` in REST URLs — without this customization, such requests would be rejected with an HTTP 400 even when properly encoded by the client.

This library auto-configures the embedded Tomcat connector to set its [`encodedSolidusHandling`](https://tomcat.apache.org/tomcat-10.1-doc/config/http.html#Common_Attributes) attribute to [`PASS_THROUGH`](https://tomcat.apache.org/tomcat-10.1-doc/api/org/apache/tomcat/util/buf/EncodedSolidusHandling.html#PASS_THROUGH), so that `%2F` is decoded to a literal `/` and passed through to Spring MVC instead of being rejected.

| Property | Type | Default | Description |
|---|---|---|---|
| `powsybl-ws.autoconfigure.tomcat-customize.enable` | boolean | `true` | Master switch enabling/disabling this whole Tomcat customization mechanism (no `TomcatConnectorCustomizer` bean is created when `false`). |
| `powsybl-ws.autoconfigure.tomcat-customize.encoded-solidus-handling` | boolean | `true` | Whether to actually set the connector's `encodedSolidusHandling` attribute to `PASS_THROUGH` (only relevant when the customization is enabled). |

### Base exception handler

A global `@ControllerAdvice` (`BaseExceptionHandler`), enabled by default, converts unhandled exceptions into `PowsyblWsProblemDetail` responses:

- calls to another microservice that fail (`HttpStatusCodeException`) are re-wrapped, keeping their original status and appending an entry to the error `chain`;
- any other unhandled exception (including Spring's own `ErrorResponse`-based exceptions) falls back to a generic `PowsyblWsProblemDetail`, logged as an error and returned as HTTP 500 if not otherwise typed.

Services with their own error-handling strategy (e.g. a dedicated business exception handler) can disable it and rely solely on their own `@ControllerAdvice`; services built on the `gridsuite-computation` library typically keep it enabled alongside their own handler, since each only reacts to its own specific exception types.

| Property | Type | Default | Description |
|---|---|---|---|
| `powsybl-ws.autoconfigure.base-exception-handler.enable` | boolean | `true` | Enable/disable this handler. |

---

## Error Model: `PowsyblWsProblemDetail`

`PowsyblWsProblemDetail` extends Spring's standard `ProblemDetail` (RFC 7807) with fields useful in a microservices context:

- `server`: name of the service that produced (or last re-wrapped) the error.
- `businessErrorCode` / `businessErrorValues`: a stable, machine-readable error code and structured contextual data for a business exception.
- `timestamp`, `traceId` (read from SLF4J `MDC`, populated by the tracing bridge).
- `path`: the request path where the error occurred.
- `chain`: an ordered list of `ChainEntry` (fromServer, toServer, method, path, timestamp), built incrementally via `wrap(...)` every time the error crosses a service boundary — useful to reconstruct, from the final caller's point of view, the whole call path that led to the error.

Typical usage in a service exposing its own business exceptions:

```java
public class MyBusinessException extends AbstractBusinessException {
    // ...
    @Override
    public BusinessErrorCode getBusinessErrorCode() { ... }
}

@ControllerAdvice
public class MyExceptionHandler extends AbstractBusinessExceptionHandler<MyBusinessException, MyErrorCode> {
    // map business codes to HTTP statuses
}
```

When a service calls another one and receives an `HttpStatusCodeException`, `BaseExceptionHandler`/`ErrorUtils` transparently extract the upstream `PowsyblWsProblemDetail` from the response body (falling back to a generic one if the body isn't a `PowsyblWsProblemDetail`) and re-wrap it, so that the final response returned to the original caller carries the full chain of services involved.

---

