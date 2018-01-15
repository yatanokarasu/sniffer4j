# What is Sniffer4j?
Sniffer4j is tool for mesuring Java code performance included in the specified packages.

# Why did I need?
Although we can use several methods to obtain microbenchmarks, there are disadvantages as follows:

* **Hard code to the measurement target**:  
  We have to embed measurement code every time we want to measure,  
  and we DO NOT want to corrupt the implementation with code not related essential more than anything else....

* **AspectJ**:  
  We need to prepare advices for measuring microbenchmarks and also require AspectJ learning costs.

__*We want to obtain microbenchmarks more SIMPLY!!*__


# Features


- - -

# Usage
## Requirements
* Java 8+ (-Java 7 does not work)

## Build

See instructions as follows:

### If use Maven

~~~ shell
mvn package
~~~

### If use Gradle

~~~ shell
./gradlew jar
~~~

## Run the application

~~~ shell
java -javaagent:/path/to/sniffer4j.jar=<options> <YOUR_APP>
~~~

### Options

| Option | Type | Description | Example |
|---|---|---|---|
| `patterns` | Regex | Regular Expression that specifies the packages to be measure.<br />You can use `;` to specify multiple packages. | `patterns=org.example.*;com.example.controller` 
| `logpath` | String | **UNDERCONSTRUCTIONS** | `logpath=/path/to/sniffer4j.log` |
