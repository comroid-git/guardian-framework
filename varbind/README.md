# The `varbind` Module

`varbind` provides classes for versatile databinding.

## Gradle and Maven copy-pasta

The guardian Framework is hosted under the comroid repository
at [maven.comroid.org](https://maven.comroid.org/org/comroid/varbind)

### Gradle

```groovy
dependencies {
    implementation 'org.comroid:varbind:VERSION'
}
```

### Maven

```xml
<dependency>
    <groupId>org.comroid</groupId>
    <artifactId>varbind</artifactId>
    <version>VERSION</version>
</dependency>
```

## About

VarBind consists of three parts:

- `GroupBind<T extends DataContainer>`
    - Type Definition
    - Provided as a singular `public static @RootBind final` fields within the respective classes
- `VarBind<..>`
    - Binding definition
    - Provided as `public static final` fields within the respective classes
    - Stores all information required to extract, compute and finalize all data
- `DataContainer` interface; implementation class: `DataContainerBase`
    - `ReferenceAtlas` to contain all extracted and computed data of an object
    - Data can easily be updated using `DataContainer#updateFrom(UniObjectNode)`

## GroupBind

Type definitions in form of GroupBinds are required for extracting data. They provide all information about what data is
carried.

A Type definition needs to be stored respecting the following rules:

- It must be a `public static final` Field
- It must be annotated with `@org.comroid.varbind.annotation.RootBind`
- Its type parameter should be its respective class

With the `GroupBind`, we can now create `Bindings` using the `GroupBind#createBinding` method.

Recommended Layout:

```java
public class MyEventData extends DataContainerBase {
    @RootBind
    public static final GroupBind<MyEventData> TYPE_DEFINITION
            = new GroupBind<>(MyApp.SERIALIZATION_ADAPTER, "my-event-data");
}
```

## DataContainer

A class that data is bound upon must extend `DataContainerBase`, which contains the basic implementation for
any `DataContainer` object.

Data on extraction-level can be obtained through References obtained from `DataContainer#getExtractionReference`, data
that has been computed to its final form can be obtained through `DataContainer#getComputedReference`.

It is recommended to store computation and/or extraction references as `final` fields, mimicking object properties.

Full Example:

```java
public class MyEventData extends DataContainerBase {
    @RootBind
    public static final GroupBind<MyEventData> TYPE_DEFINITION
            = new GroupBind<>(MyApp.SERIALIZATION_ADAPTER, "my-event-data");
    // an example for a simple String field within data
    public static final VarBind<MyEventData, String, String, String> EVENT_TYPE
            = TYPE_DEFINITION.createBinding("eventType")
            // extract as string
            .extractAs(StandardValueType.STRING)
            // this shorthand method creates the binding and maintains the type. result binding is not marked as required
            .build();
    // this is an example for a more advanced computation process
    public static final VarBind<MyEventData, String, String[], Set<String>> STRINGS
            // extract data from field named 'names'
            = TYPE_DEFINITION.createBinding("names")
            // extraction:   extract data as a singular String
            .extractAs(StandardValueType.STRING)
            // computation:  split the data apart into an array
            .andRemap(string -> string.split(";"))
            // finalization: create a set from all computed arrays
            .resolveRefs(refs -> refs.stream()
                    // we have a stream of arrays that we need to flatten
                    .flatMap(Stream::of)
                    .collect(Collectors.toSet()))
            // properties:   the data may never be `null` when updating
            .setRequired(true)
            .build();
    public final Ref<String> eventType = getComputedReference(EVENT_TYPE);

    public String getEventType() {
        // throws an AssertionError when no data is stored for event type 
        return eventType.assertion("Event Type missing");
    }

    public boolean setEventType(EventType someEnumType) {
        // when modifying the object, we need to actually store their extraction base values 
        // the extraction reference will then update, thus outdating the computation reference
        return put(EVENT_TYPE, someEnumType.getString());
    }
}
```
