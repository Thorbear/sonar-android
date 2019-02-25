@file:Suppress("DEPRECATION")

package no.thorbear.sonar.plugins.android.lint

import com.android.tools.lint.checks.BuiltinIssueRegistry
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Severity.*
import org.simpleframework.xml.core.Persister
import org.slf4j.LoggerFactory
import org.sonar.api.profiles.ProfileImporter
import org.sonar.api.profiles.RulesProfile
import org.sonar.api.rule.RuleKey
import org.sonar.api.rule.Severity
import org.sonar.api.rules.RuleFinder
import org.sonar.api.rules.RulePriority
import org.sonar.api.utils.ValidationMessages
import java.io.Reader

class AndroidLintProfileImporter(
    private val ruleFinder: RuleFinder
) : ProfileImporter(AndroidLintRulesDefinition.REPOSITORY_KEY, AndroidLintRulesDefinition.REPOSITORY_NAME) {

    private val logger = LoggerFactory.getLogger(AndroidLintProfileImporter::class.java)

    init {
        setSupportedLanguages("java", "xml")
    }

    override fun importProfile(reader: Reader, messages: ValidationMessages): RulesProfile {
        val serializer = Persister()
        val rulesProfile = RulesProfile.create()
        try {
            val lintProfile =
                serializer.read(AndroidLintProfileExporter.LintProfile::class.java, reader)
            for (lintIssue in lintProfile.issues!!) {
                val rule = ruleFinder.findByKey(RuleKey.of(AndroidLintRulesDefinition.REPOSITORY_KEY, lintIssue.id))
                if (rule == null) {
                    messages.addWarningText("Rule " + lintIssue.id + " is unknown and has been skipped")
                } else {
                    val issue = BuiltinIssueRegistry().getIssue(lintIssue.id)
                    val lintSeverity = getLintSeverity(lintIssue, issue, messages)
                    if (!isIgnored(lintSeverity)) {
                        val priority = if (lintIssue.priority != null) lintIssue.priority else issue!!.priority
                        val rulePriority = RulePriority.valueOf(getSeverity(lintSeverity, priority!!))
                        rulesProfile.activateRule(rule, rulePriority)
                    }
                }
            }
        } catch (e: Exception) {
            messages.addErrorText("Android lint profile could not be imported.")
            logger.error("Android lint profile could not be imported.", e)
        }

        return rulesProfile
    }

    private fun getSeverity(
        lintSeverity: com.android.tools.lint.detector.api.Severity,
        priority: Int
    ): String {
        var result: String
        when (lintSeverity) {
            FATAL -> result = Severity.BLOCKER
            ERROR -> {
                result = Severity.MAJOR
                if (priority >= PRIORITY_THRESHOLD) {
                    result = Severity.CRITICAL
                }
            }
            WARNING -> {
                result = Severity.MINOR
                if (priority >= PRIORITY_THRESHOLD) {
                    result = Severity.MAJOR
                }
            }
            INFORMATIONAL -> result = Severity.INFO
            IGNORE -> throw IllegalStateException("An unknown severity has been imported.")
            else -> throw IllegalStateException("An unknown severity has been imported.")
        }
        return result
    }

    private fun isIgnored(lintSeverity: com.android.tools.lint.detector.api.Severity): Boolean {
        return IGNORE == lintSeverity
    }

    private fun getLintSeverity(
        lintIssue: AndroidLintProfileExporter.LintIssue,
        issue: Issue?,
        messages: ValidationMessages
    ): com.android.tools.lint.detector.api.Severity {
        var lintSeverity: com.android.tools.lint.detector.api.Severity? = null
        if (lintIssue.severity != null) {
            for (severity in values()) {
                if (lintIssue.severity.equals(severity.description, true)) {
                    lintSeverity = severity
                    break
                }
            }
            if (lintSeverity == null) {
                logger.warn("Severity not found in Android Lint severities")
                messages.addWarningText("Could not recognize severity " + lintIssue.severity + " for rule " + lintIssue.id + " default severity is used")
            }
        }
        if (lintSeverity == null) {
            lintSeverity = issue!!.defaultSeverity
        }
        return lintSeverity
    }

    companion object {
        const val PRIORITY_THRESHOLD = 7
    }
}