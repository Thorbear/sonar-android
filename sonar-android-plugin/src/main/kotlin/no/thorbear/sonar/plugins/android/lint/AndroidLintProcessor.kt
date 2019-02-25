package no.thorbear.sonar.plugins.android.lint

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import org.simpleframework.xml.core.Persister
import org.slf4j.LoggerFactory
import org.sonar.api.batch.fs.FileSystem
import org.sonar.api.component.Component
import org.sonar.api.component.ResourcePerspectives
import org.sonar.api.issue.Issuable
import org.sonar.api.profiles.RulesProfile
import org.sonar.api.rules.ActiveRule
import java.io.File

class AndroidLintProcessor(
    private val profile: RulesProfile,
    private val perspectives: ResourcePerspectives,
    private val fileSystem: FileSystem
) {

    private val logger = LoggerFactory.getLogger(AndroidLintProcessor::class.java)

    fun process(lintXml: File) {
        val serializer = Persister()
        try {
            logger.info("Processing android lint report: {}", lintXml.path)
            val lintIssues = serializer.read(LintIssues::class.java, lintXml)
            for (lintIssue in lintIssues.issues) {
                processIssue(lintIssue)
            }
        } catch (e: Exception) {
            logger.error("Exception reading {}", lintXml.path, e)
        }
    }

    private fun processIssue(lintIssue: LintIssue) {
        val rule = profile.getActiveRule(AndroidLintRulesDefinition.REPOSITORY_KEY, lintIssue.id)
        if (rule != null) {
            logger.debug("Processing Issue: {}", lintIssue.id)
            for (lintLocation in lintIssue.locations) {
                processIssueForLocation(rule, lintIssue, lintLocation)
            }
        } else {
            logger.warn("Unable to find rule for {}", lintIssue.id)
        }
    }

    private fun processIssueForLocation(rule: ActiveRule, lintIssue: LintIssue, lintLocation: LintLocation) {
        val inputFile = fileSystem.inputFile(fileSystem.predicates().hasPath(lintLocation.file!!))
        if (inputFile != null) {
            logger.debug("Processing File {} for Issue {}", lintLocation.file, lintIssue.id)
            val issuable = perspectives.`as`(Issuable::class.java, inputFile as Component<*>)
            if (issuable != null) {
                val issue = issuable.newIssueBuilder()
                    .ruleKey(rule.rule.ruleKey())
                    .message(lintIssue.message)
                    .line(lintLocation.line)
                    .build()
                issuable.addIssue(issue)
                return
            }
        }
        logger.warn("Unable to find file {} to report issue", lintLocation.file)
    }

    @Root(name = "location", strict = false)
    data class LintLocation(
        @field:Attribute
        var file: String? = null,
        @field:Attribute(required = false)
        var line: Int? = null
    )

    @Root(name = "issues", strict = false)
    data class LintIssues(
        @field:ElementList(required = false, inline = true, empty = false)
        var issues: MutableList<LintIssue> = mutableListOf()
    )

    @Root(name = "issue", strict = false)
    data class LintIssue(
        @field:Attribute
        var id: String? = null,
        @field:Attribute
        var message: String? = null,
        @field:ElementList(inline = true)
        var locations: MutableList<LintLocation> = mutableListOf()
    )
}