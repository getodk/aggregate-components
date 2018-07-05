# ODK Aggregate AppEngine Updater
![Platform](https://img.shields.io/badge/platform-Java-blue.svg)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Slack status](http://slack.opendatakit.org/badge.svg)](http://slack.opendatakit.org)
 
ODK Aggregate AppEngine Updater helps upload [ODK Aggregate](https://github.com/opendatakit/aggregate) to Google App Engine.

* ODK website: [https://opendatakit.org](https://opendatakit.org)
* ODK Aggregate usage instructions: [https://opendatakit.org/use/aggregate/](https://opendatakit.org/use/aggregate/)
* ODK forum: [https://forum.opendatakit.org](https://forum.opendatakit.org)
* ODK developer Slack chat: [http://slack.opendatakit.org](http://slack.opendatakit.org) 
* ODK developer Slack archive: [http://opendatakit.slackarchive.io](http://opendatakit.slackarchive.io) 
* ODK developer wiki: [https://github.com/opendatakit/opendatakit/wiki](https://github.com/opendatakit/opendatakit/wiki)

## Getting the code

1. Fork the Aggregate Components project ([why and how to fork](https://help.github.com/articles/fork-a-repo/))

1. Clone your fork of the project locally. At the command line:

        git clone https://github.com/YOUR-GITHUB-USERNAME/aggregate-components

### Import 

1. On the welcome screen, click `Import Project`, navigate to your aggregate-components folder, and select the `build.gradle` file. 

1. Make sure you check `Use auto-import` option in the `Import Project from Gradle` dialog 

1. Once the project is imported, IntelliJ may ask you configure any detected GWT, Spring or web Facets, you can ignore these messages

### Build a JAR with dependencies (shadow JAR)

1. Run `./gradlew clean shadowJar`

1. You can find the JAR file including all dependencies in the `build/libs` directory. 

## Contributing

Any and all contributions to the project are welcome. ODK Aggregate is used across the world primarily by organizations with a social purpose so you can have real impact!

If you're ready to contribute code, see [the contribution guide](https://github.com/opendatakit/aggregate/blob/master/CONTRIBUTING.md).

## Troubleshooting

* If you get an **Invalid Gradle JDK configuration found** error importing the code, you might not have set the `JAVA_HOME` environment variable. Try [these solutions](https://stackoverflow.com/questions/32654016/).
