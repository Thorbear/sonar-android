@file:Suppress("DEPRECATION")

package no.thorbear.sonar.plugins.android.lint

import com.android.tools.lint.detector.api.Severity
import no.thorbear.sonar.plugins.android.utils.Resources
import org.codehaus.staxmate.SMInputFactory
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import org.simpleframework.xml.core.Persister
import org.slf4j.LoggerFactory
import org.sonar.api.profiles.ProfileExporter
import org.sonar.api.profiles.RulesProfile
import org.sonar.api.rules.ActiveRule
import org.sonar.api.rules.RulePriority
import org.sonar.api.rules.RulePriority.*
import java.io.Writer
import java.util.*
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException

class AndroidLintProfileExporter :
    ProfileExporter(AndroidLintRulesDefinition.REPOSITORY_KEY, AndroidLintRulesDefinition.REPOSITORY_NAME) {

    private val logger = LoggerFactory.getLogger(AndroidLintProfileExporter::class.java)
    private val ruleKeys: MutableCollection<String>
    private val resources = Resources()

    /**
     * Constructor to be used on batch side as ProfileExporter is a batch extension and thus might not
     * be injected RuleFinder.
     */
    init {
        ruleKeys = arrayListOf()
        loadRuleKeys()
        setSupportedLanguages("java", "xml")
        mimeType = "text/xml"
    }

    private fun loadRuleKeys() {
        val xmlFactory = XMLInputFactory.newInstance()
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
        xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, java.lang.Boolean.FALSE)
        // just so it won't try to load DTD in if there's DOCTYPE
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, java.lang.Boolean.FALSE)
        xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, java.lang.Boolean.FALSE)
        val inputFactory = SMInputFactory(xmlFactory)
        // TODO include these resources
        val reader = resources.asReader(AndroidLintRulesDefinition.RULES_XML_PATH)
        try {
            val rootC = inputFactory.rootElementCursor(reader)
            rootC.advance() // <rules>

            val rulesC = rootC.childElementCursor("rule")
            while (rulesC.next != null) {
                // <rule>
                val cursor = rulesC.childElementCursor()
                while (cursor.next != null) {
                    if ("key".equals(cursor.localName, ignoreCase = true)) {
                        val key = cursor.collectDescendantText(false).trim()
                        ruleKeys.add(key)
                    }
                }
            }

        } catch (e: XMLStreamException) {
            throw IllegalStateException("XML is not valid", e)
        }

    }

    override fun exportProfile(profile: RulesProfile, writer: Writer) {
        val serializer = Persister()
        try {
            serializer.write(createLintProfile(profile.activeRules), writer)
        } catch (e: Exception) {
            logger.error("Could not export lint profile", e)
        }

    }

    private fun createLintProfile(activeRules: List<ActiveRule>): LintProfile {
        val profile = LintProfile()
        val activeKeys = HashMap<String, RulePriority>()
        val issues = arrayListOf<LintIssue>()
        for (rule in activeRules) {
            activeKeys[rule.ruleKey] = rule.severity
        }
        for (ruleKey in ruleKeys) {
            issues.add(getLintIssue(ruleKey, activeKeys))
        }
        // ensure order of issues in output, sort by key.
        Collections.sort(issues, IssueComparator())
        profile.issues = issues
        return profile
    }

    private class IssueComparator : Comparator<LintIssue> {
        override fun compare(o1: LintIssue, o2: LintIssue): Int {
            return o1.id.compareTo(o2.id)
        }
    }

    @Root(name = "lint", strict = false)
    internal class LintProfile {
        @ElementList(inline = true)
        var issues: List<LintIssue>? = null
    }

    @Root(name = "issue", strict = false)
    internal class LintIssue {
        @Attribute
        var id: String = ""
        @Attribute(required = false)
        var severity: String? = null
        @Attribute(required = false)
        var priority: Int? = null

        constructor() {
            // No arg constructor used by profile importer
        }

        constructor(ruleKey: String, severity: String, priority: Int?) {
            this.id = ruleKey
            this.severity = severity
            this.priority = priority
        }
    }

    private fun getLintIssue(key: String, activeKeys: Map<String, RulePriority>): LintIssue {
        if (!activeKeys.containsKey(key)) {
            return LintIssue(key, Severity.IGNORE.description, null)
        }

        var lintSeverity = ""
        val severity = activeKeys[key]
        if (severity == BLOCKER) {
            lintSeverity = Severity.FATAL.description
        }
        if (severity == CRITICAL) {
            lintSeverity = Severity.ERROR.description
        }
        if (severity == MAJOR) {
            lintSeverity = Severity.ERROR.description
        }
        if (severity == MINOR) {
            lintSeverity = Severity.WARNING.description
        }
        if (severity == INFO) {
            lintSeverity = Severity.INFORMATIONAL.description
        }
        return LintIssue(key, lintSeverity, null)
    }
}