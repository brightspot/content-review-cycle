# Content Freshness Review Cycle

> [!WARNING]
> This is a high-level summary of the functionality this extension provides.

This extension provides the ability for users the ability to set up review cycles for different content types. Users can configure cycle durations in Admin > Sites & Settings for each content type along with a timeframe of when a banner warning would appear at the top of the content edit page which will allow you to review or dismiss it. Additionally, users can also override the cycle duration of specific content types. Notifications can also be set up in Admin > Users & Roles > Notifications. Content that is past due or due in a specific amount of days will be shown as a notification in the CMS header.

* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [Usage](#usage)
* [Documentation](#documentation)
* [Versioning](#versioning)
* [Contributing](#contributing)
* [Local Development](#local-development)
* [License](#license)

## Prerequisites

> [!WARNING]
> This section should list any prerequisites that must be met before the extension can be installed or used. 
> If a specific version of Brightspot is needed, it should be listed here.
> If any external APIs are used (AWS, GCP, or any other third party service), they should be listed here.

This extension requires an instance of [Brightspot](https://www.brightspot.com/) and access to the project source code.

## Installation

Gradle:
```groovy
api 'com.brightspot:content-freshness:1.0.0'
```

Maven:
```xml
<dependency>
    <groupId>com.brightspot</groupId>
    <artifactId>content-freshness</artifactId>
    <version>1.0.0</version>
</dependency>
```

Substitute `1.0.0` for the desired version found on the [releases](/releases) list.

## Usage

> [!WARNING]
> This section describes how a developer would use this extension in their project.
> It should include code samples, if applicable, as well as a link to the end user documentation. 

To opt in to this behavior, implement the `HasReviewCycle` interface on your content type:

```java
public class MyContentType extends Content implements HasReviewCycle {
    // ...
}
```
Now, in Sites and Settings > CMS > Review Cycle Settings you will be able to select that content type. Additionally, you will be able to override specific content on the content edit page by going to the overrides cluster.

## Documentation

The latest Javadocs can be found [here](https://artifactory.psdops.com/public/com/brightspot/platform-extension-example/%5BRELEASE%5D/platform-extension-example-%5BRELEASE%5D-javadoc.jar!/index.html).

## Versioning

The version numbers for this extension will strictly follow [Semantic Versioning](https://semver.org/).

## Contributing
Pull requests are welcome. For major changes, please open an issue first to
discuss what you would like to change.

## Local Development

Assuming you already have a local Brightspot instance up and running, you can 
test this extension by running the following command from this project's root 
directory to install a `SNAPSHOT` to your local Maven repository:

```shell
./gradlew publishToMavenLocal
```

Next, ensure your project's `build.gradle` file contains 

```groovy
repositories {
    mavenLocal()
}
```

Then, add the following to your project's `build.gradle` file:

```groovy
dependencies {
    api 'com.brightspot:content-freshness:1.0.0-SNAPSHOT'
}
```

Finally, compile your project and run your local Brightspot instance.

## License

See: [LICENSE](LICENSE).
