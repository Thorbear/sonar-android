package no.thorbear.sonar.plugins.android

import no.thorbear.sonar.plugins.android.lint.*
import org.sonar.api.Property
import org.sonar.api.SonarPlugin

@Property(
    key = AndroidPlugin.LINT_REPORT_PROPERTY,
    defaultValue = AndroidPlugin.LINT_REPORT_PROPERTY_DEFAULT,
    name = "Lint Report file",
    description = "Path (absolute or relative) to the lint-results.xml file.",
    project = true,
    module = true,
    global = false
)
class AndroidPlugin : SonarPlugin() {

    override fun getExtensions(): List<Class<out Any>> {
        return listOf(
            AndroidLintProfileExporter::class.java,
            AndroidLintProfileImporter::class.java,
            AndroidLintRulesDefinition::class.java,
            AndroidLintSensor::class.java,
            AndroidLintSonarWay::class.java
        )
    }

    companion object {
        const val LINT_REPORT_PROPERTY = "sonar.android.lint.report"
        const val LINT_REPORT_PROPERTY_DEFAULT = "build/outputs/lint-results.xml"
    }
}