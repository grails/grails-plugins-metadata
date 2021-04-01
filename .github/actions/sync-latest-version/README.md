# Grails Plugin Sync Latest Version Action

Read `mavenMetadataUrl` field for each plugin in the `grails-plugins.json` metadata file then update the latestVersion and dependency coordinates by fetching the latest version and dependency coordinates from Maven Metadata if there is a new version.

## Example usage

```yaml
uses: ./.github/actions/sync-latest-version
```
