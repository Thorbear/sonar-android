package no.thorbear.sonar.plugins.android.lint

import no.thorbear.sonar.plugins.android.utils.Resources
import org.sonar.api.server.rule.RulesDefinition
import org.sonar.api.server.rule.RulesDefinitionXmlLoader
import org.sonar.squidbridge.rules.SqaleXmlLoader

class AndroidLintRulesDefinition(
    private val xmlLoader: RulesDefinitionXmlLoader
) : RulesDefinition {

    // Cannot be in constructor, because sonar performs injection at runtime
    private val resources: Resources = Resources()

    override fun define(context: RulesDefinition.Context) {
        val repository = context.createRepository(REPOSITORY_KEY, "java").setName(REPOSITORY_NAME)
        resources.asReader(RULES_XML_PATH).use { reader ->
            xmlLoader.load(repository, reader)
            SqaleXmlLoader.load(repository, "/no/thorbear/sonar/plugins/android/lint/java-model.xml")
            repository.done()
        }
    }

    companion object {
        const val REPOSITORY_KEY = "android-lint"
        const val REPOSITORY_NAME = "Android Lint"
        const val RULES_XML_PATH = "/no/thorbear/sonar/plugins/android/lint/rules.xml"
    }
}