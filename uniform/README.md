# The `uniform` Module

First of all, _uniform is not a serialization library_, it only ever had one goal: To eliminate the ever-lasting
confusion and hassles when using several data types. You will be required to include a foreign serialization library and
an adapter class for that.

It is compatible with every data type that uses at least String/Value maps or Arrays to store data, such as JSON, XML,
form data, YML....

## Gradle and Maven copy-pasta

The guardian Framework is hosted under the comroid repository
at [maven.comroid.org](https://maven.comroid.org/org/comroid/uniform)

### Gradle

```groovy
dependencies {
    implementation 'org.comroid:uniform:VERSION'
}
```

### Maven

```xml
<dependency>
    <groupId>org.comroid</groupId>
    <artifactId>uniform</artifactId>
    <version>VERSION</version>
</dependency>
```

## About

`uniform` contains some simple classes to help implement possibly every data type into the same interfaces, such as:

- `UniNode` is the core data node interface. Represents an array of data, an object of data, or just one value within
  one of these.
- `UniObjectNode` and `UniArrayNode` are representations of objects and array nodes of data. They contain some helper
  methods to assist with modifying the data.
- `SerializationAdapter` is an adapter class that can parse Strings into UniNodes, or create empty UniNodes.

## SerializationAdapter

The SerializationAdapter is the core class of `uniform`. It extends `org.comroid.api.ContextualProvider`, providing a
possible context base.

There is a couple of pre-made `SerializationAdapter` implementations to provide support for some commonly known
serialization libraries that exist. Some of which may not work as expected yet.

**Please note that you will be required to add the respective dependency to your runtime classpath.**

- JSON
    - `JacksonJSONAdapter.instance` utilizes `com.fasterxml.jackson.core:jackson-databind`
    - `FastJSONLib.fastJsonLib` utilizes `com.alibaba:fastjson`
    - `OrgJsonLib.orgJsonLib` utilizes `org.json:json`
- XML
    - `JacksonXMLAdapter.instance` utilizes `com.fasterxml.jackson.dataformat:jackson-dataformat-xml`
- PROPERTIES
    - `JavaPropertiesSerializationAdapter.JavaPropertiesAdapter` utilizes the Java `.properties` format (no additional
      dependency required)
- HTML (experimental)
    - `JsoupXmlParser` utilizes `org.jsoup:jsoup`
