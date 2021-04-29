# The guardian Framework

The guardian Framework aims at enabling a developer to easily create REST-Based APIs, Wrappers for those APIs and even
contains a simple module to integrate a Web UI with said API.

Every module of the guardian Framework depends on the [comroid common Java API](https://github.com/comroid-git/api)

## Gradle and Maven copy-pasta

The guardian Framework is hosted under the comroid repository
at [maven.comroid.org](https://maven.comroid.org/org/comroid)

### Gradle

```groovy
repositories {
    maven { url 'https://maven.comroid.org' }
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>comroid</id>
        <url>https://maven.comroid.org</url>
    </repository>
</repositories>
```

## Modules

The guardian Framework is composed of a couple of modules, some of which can be used on their own.

All currently available modules are:

| Module                                                                                                                | Short Description                                                 | Internal Dependencies |
|-----------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|-----------------------|
| [mutatio](https://github.com/comroid-git/guardian-framework/blob/master/mutatio/README.md)                            | Reference and Reference Pipeline Implementation                   | none                  |
| [core](https://github.com/comroid-git/guardian-framework/blob/master/core/README.md)                                  | guardian Core Utilities                                           | `mutatio`             |
| [uniform](https://github.com/comroid-git/guardian-framework/blob/master/uniform/README.md)                            | Universal Adapter solution for data types, such as JSON, XML, ... | `mutatio`             |
| [varbind](https://github.com/comroid-git/guardian-framework/blob/master/varbind/README.md)                            | `uniform` and `mutatio` compatible Databinding solution           | `uniform`             |
| [restless](https://github.com/comroid-git/guardian-framework/blob/master/restless/README.md)                          | Client-focused, universal adapter solution for REST-based Actions | `varbind`             |
| [webkit](https://github.com/comroid-git/guardian-framework/blob/master/webkit/README.md)                              | Server-focused REST and Website Framework                         | `restless`            |
| [spellbind](https://github.com/comroid-git/guardian-framework/blob/master/spellbind/README.md) (currently inoperable) | Instance extension using Interface Proxies                        | `core`                |
