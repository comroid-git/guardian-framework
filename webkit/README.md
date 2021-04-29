# The `webkit` Module

Not only introducing a Server that is compatible with `restless` API, but also providing a simple, yet powerful
framework in order to host a fluent Web UI, `webkit` makes it easier than ever before to create a highly versatile
REST-API, and even to add a Web UI for such, featuring simple-to-use ✨internet magic✨ to directly communicate with the
Server.

**Webkit requires a WebSocket-compatible `HttpAdapter` in order to work.**

## Gradle and Maven copy-pasta

The guardian Framework is hosted under the comroid repository
at [maven.comroid.org](https://maven.comroid.org/org/comroid/webkit)

### Gradle

```groovy
dependencies {
    implementation 'org.comroid:webkit:VERSION'
}
```

### Maven

```xml
<dependency>
    <groupId>org.comroid</groupId>
    <artifactId>webkit</artifactId>
    <version>VERSION</version>
</dependency>
```

## About

Providing a template-based HTML Page builder and versatile content injection methods, `webkit` makes it easy as pie to
create any form of REST-API server.

## Defining and Handling Server Endpoints

`webkit`, such as `restless`, encourages building endpoint definitions as `enum` constants. It contains an
interface `ServerEndpoint` that extends `AccessibleEndpoint` and `EndpointHandler`.

The `EndpointHandler` is used to handle any incoming and fully parsed REST Request.

An `AccessibleEndpoint` defined in a universal API may be upgraded to a `ServerEndpoint`
using `ServerEndpoint.combined(AccessibleEndpoint, EndpointHandler)`.

To create a set of endpoints to be used on the server-side, we recommend using the following class structure:

```java
public enum MyEndpoints implements ServerEndpoint {
    ENDPOINT("/endpoint") {
        public REST.Response executeGET(
                Context context,
                REST.Header.List headers,
                String[] urlParams,
                UniNode body
        ) {
            // send an empty OK(200) response
            return new REST.Response(HTTPStatusCodes.OK);
        }
    },
    // to include URL parameters, define the place of the parameter as a Java Formatter string notation
    ENDPOINT_WITH_URL_PARAMETERS("/endpoint/%s", "(url_parameter_regex)") {
        public REST.Response executeGET(
                Context context,
                REST.Header.List headers,
                String[] urlParams,
                UniNode body
        ) throws RestEndpointException {
            try {
                UUID.fromString(urlParams[0]);
            } catch (IllegalArgumentException e) {
                // sends an error response containing code, message and underlying message
                throw new RestEndpointException(HTTPStatusCodes.BAD_REQUEST, "Invalid URL Parameter", e);
            }
        }
    };

    private final AccessibleEndpoint underlying;

    @Override
    public AccessibleEndpoint getEndpointBase() {
        return underlying;
    }

    Endpoint(AccessibleEndpoint endpoint) {
        this.underlying = endpoint;
    }
}
```

## Modules

All currently available modules:

| Module                                                                                                      | Short Description                                |
|-------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| [webkit-oauth2](https://github.com/comroid-git/guardian-framework/tree/master/webkit/oauth/README.md) (WIP) | Basic server-sided oauth2 implementation package |
