# Content Review Cycle

This extension provides a review-cycle feature that reminds editors to review specific assets at fixed intervals.

* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [Usage](#usage)
* [Documentation](#documentation)
* [Versioning](#versioning)
* [Contributing](#contributing)
* [Local Development](#local-development)

## Prerequisites

This extension requires an instance of [Brightspot](https://www.brightspot.com/) (version 4.5.15 or later) and access to the project source code.

## Installation

Gradle:
```groovy
api 'com.brightspot:content-review-cycle:1.0.0'
```

Maven:
```xml
<dependency>
    <groupId>com.brightspot</groupId>
    <artifactId>content-review-cycle</artifactId>
    <version>1.0.0</version>
</dependency>
```
Substitute `1.0.0` for the desired version found on the [releases](https://github.com/brightspot/content-review-cycle/tags) page.

## Usage

You explicitly activate this extension for individual content types by implementing the `HasReviewCycle` interface:

```java
public class MyContentType extends Content implements HasReviewCycle {
    // ...
}
```

When you implement this interface on a content type, that content type is now available to be configured for the Review Cycle feature by an administrator. Please see the user guide for instructions on configuring this feature.

## Documentation

The latest Javadocs can be found [here](https://artifactory.psdops.com/public/com/brightspot/content-review-cycle/%5BRELEASE%5D/content-review-cycle-%5BRELEASE%5D-javadoc.jar!/index.html).

## Versioning

The version numbers for this extension will strictly follow [Semantic Versioning](https://semver.org/). The latest release can be found [here](https://github.com/brightspot/content-review-cycle/tags).

## Contributing

If you have feedback, suggestions or comments on this open-source platform extension, please feel free to make them publicly on the issues tab [here](https://github.com/brightspot/content-review-cycle/issues).

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## Local Development

Assuming you already have a local Brightspot instance up and running, you can test this extension by running the following command from this project's root directory to install a `SNAPSHOT` to your local Maven repository:

```shell
./gradlew publishToMavenLocal
```

Next, ensure your project's `build.gradle` file contains the following:

```groovy
repositories {
    mavenLocal()
}
```

Then, add the following to your project's `build.gradle` file:

```groovy
dependencies {
    api 'com.brightspot:content-review-cycle:1.0.0-SNAPSHOT'
}
```

Finally, compile your project and run your local Brightspot instance.
