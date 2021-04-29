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
