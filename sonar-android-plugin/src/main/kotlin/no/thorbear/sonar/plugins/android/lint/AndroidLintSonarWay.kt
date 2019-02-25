package no.thorbear.sonar.plugins.android.lint

import no.thorbear.sonar.plugins.android.utils.Resources
import org.sonar.api.profiles.ProfileDefinition
import org.sonar.api.profiles.RulesProfile
import org.sonar.api.profiles.XMLProfileParser
import org.sonar.api.utils.ValidationMessages

class AndroidLintSonarWay(
    private val parser: XMLProfileParser
) : ProfileDefinition() {

    // Cannot be in constructor, because sonar performs injection at runtime
    private val resources: Resources = Resources()

    override fun createProfile(validationMessages: ValidationMessages): RulesProfile {
        resources.asReader(PROFILE_XML_PATH).use {
            return parser.parse(it, validationMessages)
        }
    }

    companion object {
        const val PROFILE_XML_PATH = "/no/thorbear/sonar/plugins/android/lint/android_lint_sonar_way.xml"
    }
}