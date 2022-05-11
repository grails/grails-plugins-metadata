# Grails Plugin Portal - Metadata

This repository hosts the metadata used by Grails Plugin Portal to list/show plugins information.

## Update Information on the Grails Plugin Portal
The Grails Plugin Portal uses the [grails-plugins.json](grails-plugins.json) metadata file to display plugin information.

When you publish a new plugin or release an existing plugin, you must send a pull request to this file to update the information on the portal.

1. When you release a new plugin, add the following information at the end of the JSON file:

```json
{
    "bintrayPackage": {
        "name": "My Plugin",
        "repo": "plugins",
        "owner": "puneetbehl",
        "desc": "A concise description",
        "labels": [
                "grails3",
                "spring-boot",
                "test-label"
        ],
        "licenses": [
                "Apache-2.0"
        ],
        "issueTrackerUrl": "https://github.com/puneetbehl/myplugin/issues",
        "latestVersion": "1.1",
        "updated": "2021-03-25T04:00:40.855Z",
        "systemIds": [
                "io.github.puneetbehl:myplugin"
        ],
        "vcsUrl": "https://github.com/puneetbehl/myplugin"
    }
    "documentationUrl": "https://puneetbehl.github.io/myplugin/",
    "mavenMetadataUrl": "https://repo1.maven.org/maven2/com/github/puneetbehl/myplugin/maven-metadata.xml",
} 
```
2. If you publish a new version to Maven Central, locate the entry in the JSON file and updatelatestVersion and mavenMetadataUrl. This will reflect the latest version in the plugin portal. In the future, we will use the mavenMetadataUrl field to automatically update the latest version.
