# The `restless` Module

Providing a universal structure for client-sided REST actions, `restless` is a versatile base for use with any other
REST implementation Library. The main thought is to be able to use whatever REST library is equipped by other
end-software dependencies.

## Gradle and Maven copy-pasta

The guardian Framework is hosted under the comroid repository
at [maven.comroid.org](https://maven.comroid.org/org/comroid/restless)

### Gradle

```groovy
dependencies {
    implementation 'org.comroid:restless:VERSION'
}
```

### Maven

```xml
<dependency>
    <groupId>org.comroid</groupId>
    <artifactId>restless</artifactId>
    <version>VERSION</version>
</dependency>
```

## About

Like `uniform`, `restless` builds upon having just one main adapter class: `HttpAdapter`

Using this adapter, it is possible to open WebSocket connections, or just forward a `REST.Request` to be executed. The
core intention is to include a major method that forwards request calls to this adapter.

## HttpAdapter

The HttpAdapter is the core class of `uniform`. It extends `org.comroid.api.ContextualProvider`, providing a possible
context base.

There is a couple of pre-made `HttpAdapter` implementations to provide support for some commonly known serialization
libraries that exist. Some of which may not work as expected yet.

**Please note that you will be required to add the respective dependency to your runtime classpath.**

- `BasicJavaHttpAdapter` utilizes plain Java to send requests. **Does not support WebSockets.**
- `JavaHttpAdapter` utilizes JDK 11 features to send requests and handle WebSocket connections. **Requires Java 11+ to
  work.**
- `OkHttp4Adapter` utilizes `com.squareup.okhttp3:okhttp:4.+`

## Defining Endpoints

`restless` encourages building endpoint definitions as `enum` constants. It contains an interface `AccessibleEndpoint`
that takes care of most things, only leaving the necessity to add URL parameters.

An `AccessibleEndpoint` that has URL parameters needs to be supplied with arguments for those parameters,
using `AccessibleEndpoint.complete(Object... args)`.

To create a set of endpoints to send requests to, we recommend using the following class structure:

```java
public enum MyEndpoints implements AccessibleEndpoint {
    ENDPOINT("/endpoint"),
    // to include URL parameters, define the place of the parameter as a Java Formatter string notation
    ENDPOINT_WITH_URL_PARAMETERS("/endpoint/%s", "(url_parameter_regex)");

    private final String extension;
    private final String[] regexGroups;
    private final Pattern pattern;

    @Override
    public String getUrlBase() {
        // the url base, required when building a URL for a request
        return MyServer.URL_BASE;
    }

    @Override
    public String getUrlExtension() {
        return extension;
    }

    @Override
    public String[] getRegExpGroups() {
        return regexGroups;
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }

    Endpoint(String extension, @Language("RegExp") String... regexGroups) {
        this.extension = extension;
        this.regexGroups = regexGroups;
        // Important: the pattern provided by getPattern() needs to be compiled using `#buildUrlPattern()`
        this.pattern = buildUrlPattern();
    }
}
```
