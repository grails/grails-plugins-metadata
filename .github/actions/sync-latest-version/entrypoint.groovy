import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChild
import groovy.xml.slurpersupport.NodeChildren
import java.text.*

@CompileStatic
public class SyncLatestVersion {

    void execute() {
        final File metadataJsonFile = new File('grails-plugins.json')
        if (metadataJsonFile.exists()) {
            final JsonSlurper jsonSlurper = new JsonSlurper()
            final List<Map<String, ?>> pluginList = (List<Map<String, ?>>) jsonSlurper.parse(metadataJsonFile)
            pluginList.stream()
                    .forEach(plugin -> {
                        if (plugin.containsKey('mavenMetadataUrl') && plugin['mavenMetadataUrl']) {
                            fetchLatestVersion((String) plugin['mavenMetadataUrl'])
                                    .ifPresent(latest -> {
                                        final String latestVersionFromFile = plugin['bintrayPackage']['latestVersion']
                                        if (latest.versionText && latest.versionText != latestVersionFromFile) {
                                            plugin['bintrayPackage']['latestVersion'] = latest.versionText
                                            plugin['bintrayPackage']['updated'] = latest.updated
                                        }
                                    })
                        }
                    })
            metadataJsonFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(pluginList))
        } else {
            println "File grails-plugins.json not found at $metadataJsonFile.path"
        }
    }

    @CompileDynamic
    @SuppressWarnings('GrMethodMayBeStatic')
    private Optional<SoftwareVersion> fetchLatestVersion(String mavenMetadataUrl) {
        final XmlSlurper xmlSlurper = new XmlSlurper()
        final GPathResult metadata = xmlSlurper.parse(mavenMetadataUrl)
        final NodeChildren versions = metadata.versioning.versions.version
        final List<SoftwareVersion> versionList = []
        versions.forEach { NodeChild version ->
            try {
                versionList.add(SoftwareVersion.build(version.text()))
            }
            catch(NumberFormatException ignore) {
                println "NumberFormatException caught when parsing version string ${version.text()} for url $mavenMetadataUrl"
            }
        }
        Optional<SoftwareVersion> version = versionList.stream()
                .filter(version -> version && !version.isSnapshot())
                .sorted((v1, v2) -> { v2 <=> v1 })
                .findFirst()
        version.ifPresent(latest -> { latest.updated = updated(metadata.versioning.lastUpdated.text()) })
        version
    }

    String updated(String inputDate) {
        if (inputDate) {
            final DateFormat xmlInputFormat = new SimpleDateFormat("yyyyMMddHHmmss")
            final Date date = xmlInputFormat.parse(inputDate)
            final DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
            utcFormat.format(date)
        }
    }
}

@CompileStatic
class SoftwareVersion implements Comparable<SoftwareVersion> {

    int major
    int minor
    int patch

    Snapshot snapshot

    String versionText
    String updated

    static SoftwareVersion build(String version) {
        String[] parts = version.split("\\.")
        SoftwareVersion softVersion = null
        if (parts.length >= 3 && version.matches(/^\d+.\d+.\d+.*/)) {
            softVersion = new SoftwareVersion()
            softVersion.versionText = version
            softVersion.major = parts[0].toInteger()
            softVersion.minor = parts[1].toInteger()
            if (parts.length > 3) {
                softVersion.snapshot = new Snapshot(parts[3])
            } else if (parts[2].contains('-')) {
                String[] subparts = parts[2].split("-")
                softVersion.patch = subparts.first() as int
                softVersion.snapshot = new Snapshot(subparts[1..-1].join("-"))
                return softVersion
            }
            softVersion.patch = parts[2].toInteger()
        }
        softVersion
    }

    boolean isSnapshot() {
        snapshot != null
    }

    @Override
    int compareTo(SoftwareVersion o) {
        int majorCompare = this.major <=> o.major
        if (majorCompare != 0) {
            return majorCompare
        }

        int minorCompare = this.minor <=> o.minor
        if (minorCompare != 0) {
            return minorCompare
        }

        int patchCompare = this.patch <=> o.patch
        if (patchCompare != 0) {
            return patchCompare
        }

        if (this.isSnapshot() && !o.isSnapshot()) {
            return -1
        } else if (!this.isSnapshot() && o.isSnapshot()) {
            return 1
        } else if (this.isSnapshot() && o.isSnapshot()) {
            return this.getSnapshot() <=> o.getSnapshot()
        } else {
            return 0
        }
    }

    @Override
    String toString() {
        return "SoftwareVersion{" +
                "major=" + major +
                ", minor=" + minor +
                ", patch=" + patch +
                ", snapshot=" + snapshot +
                ", versionText='" + versionText + '\'' +
                '}';
    }
}

@CompileStatic
class Snapshot implements Comparable<Snapshot> {

    private String text

    int getMilestoneVersion() {
        text.replace("M", "").toInteger()
    }

    int getReleaseCandidateVersion() {
        text.replace("RC", "").toInteger()
    }

    boolean isBuildSnapshot() {
        text == "BUILD-SNAPSHOT"
    }

    boolean isReleaseCandidate() {
        text.startsWith("RC")
    }

    boolean isMilestone() {
        text.startsWith("M")
    }

    Snapshot(String text) {
        this.text = text
    }

    @Override
    int compareTo(Snapshot o) {

        if (this.buildSnapshot && !o.buildSnapshot) {
            return 1
        } else if (!this.buildSnapshot && o.buildSnapshot) {
            return -1
        } else if (this.buildSnapshot && o.buildSnapshot) {
            return 0
        }

        if (this.releaseCandidate && !o.releaseCandidate) {
            return 1
        } else if (!this.releaseCandidate && o.releaseCandidate) {
            return -1
        } else if (this.releaseCandidate && o.releaseCandidate) {
            return this.releaseCandidateVersion <=> o.releaseCandidateVersion
        }

        if (this.milestone && !o.milestone) {
            return 1
        } else if (!this.milestone && o.milestone) {
            return -1
        } else if (this.milestone && o.milestone) {
            return this.milestoneVersion <=> o.milestoneVersion
        }

        return 0
    }
}

new SyncLatestVersion().execute()
