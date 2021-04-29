# The `mutatio` Module

The main focus of `mutatio` lies within two tasks:

- Being able to store computation results and caching them; until the dependency References have changed
- Implementation of a versatile Reference; or rather Event Pipeline

## Gradle and Maven copy-pasta

The guardian Framework is hosted under the comroid repository
at [maven.comroid.org](https://maven.comroid.org/org/comroid/mutatio)

### Gradle

```groovy
dependencies {
    implementation 'org.comroid:mutatio:VERSION'
}
```

### Maven

```xml
<dependency>
    <groupId>org.comroid</groupId>
    <artifactId>mutatio</artifactId>
    <version>VERSION</version>
</dependency>
```

## About

`mutatio` introduces six main structures:

- The basic `Reference<T>` class; interface `Ref<T>`
    - Provides the platform for all computation-caching tasks
    - Can be mutated directly using methods inspired by basic Java Stream API
    - Must be created with `Reference#create`

- A map-compatible reference: `KeyedReference<K, V>`; interface `KeyRef<K, V>`
    - Can also hold a key value
    - Extends `Reference<V>` and `Map.Entry<K, V>`
    - Must be created with `KeyedReference#createKey`

- Versatile map storage for keyed References: The internal `ReferenceAtlas`
    - Holds a base stage, a computation step and a map of Accessor-References
    - This class is mostly used for internal operations
    - Yield content can be mutated directly using methods inspired by basic Java Stream API

- A list of references: `ReferenceList<T>`; interface `RefList<T>`
    - A `java.util.List<T>` compatible reference structure
    - Based on `ReferenceAtlas`
    - Created via Constructor

- A map of references: `ReferenceMap<K, V>`; interface `RefMap<K, V>`
    - A `java.util.Map<K, V>` compatible reference strucutre
    - Based on `ReferenceAtlas`
    - Created via Constructor

- The base class for an event pipeline: `ReferencePipe`; interface `RefContainer<K, V>`
    - Serves as the base for asynchronous event pipelines
    - Based on `ReferenceAtlas`
    - Created via Constructor
