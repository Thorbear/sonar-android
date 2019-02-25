package no.thorbear.sonar.plugins.android.lint

import no.thorbear.sonar.plugins.android.AndroidPlugin
import org.slf4j.LoggerFactory
import org.sonar.api.batch.Sensor
import org.sonar.api.batch.SensorContext
import org.sonar.api.batch.fs.FileSystem
import org.sonar.api.component.ResourcePerspectives
import org.sonar.api.config.Settings
import org.sonar.api.profiles.RulesProfile
import org.sonar.api.resources.Project
import java.io.File

class AndroidLintSensor(
    settings: Settings,
    private val profile: RulesProfile,
    private val perspectives: ResourcePerspectives,
    private val fs: FileSystem
) :
    Sensor {

    private val lintReport: File?

    init {
        this.lintReport = getFile(settings.getString(AndroidPlugin.LINT_REPORT_PROPERTY))
    }

    override fun analyse(project: Project, sensorContext: SensorContext) {
        AndroidLintProcessor(profile, perspectives, fs).process(lintReport!!)
    }

    override fun shouldExecuteOnProject(project: Project): Boolean {
        return lintReport != null && lintReport.exists()
    }

    private fun getFile(path: String): File? {
        try {
            var file = File(path)
            if (!file.isAbsolute) {
                file = File(fs.baseDir(), path).canonicalFile
            }
            return file
        } catch (e: Exception) {
            LOGGER.warn(
                "Lint report not found, please set {} property to a correct value.",
                AndroidPlugin.LINT_REPORT_PROPERTY
            )
            LOGGER.warn("Unable to resolve path : $path", e)
        }

        return null
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AndroidLintSensor::class.java)
    }
}