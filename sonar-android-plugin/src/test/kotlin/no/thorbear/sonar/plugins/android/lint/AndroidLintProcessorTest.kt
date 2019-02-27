package no.thorbear.sonar.plugins.android.lint

import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Matchers.anyString
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.sonar.api.batch.fs.FilePredicate
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.InputPath
import org.sonar.api.batch.fs.internal.DefaultFileSystem
import org.sonar.api.batch.fs.internal.DefaultInputFile
import org.sonar.api.component.Perspective
import org.sonar.api.component.ResourcePerspectives
import org.sonar.api.issue.Issuable
import org.sonar.api.issue.Issue
import org.sonar.api.profiles.RulesProfile
import org.sonar.api.rules.ActiveRule
import org.sonar.api.rules.Rule
import java.io.File

class AndroidLintProcessorTest {

    private lateinit var perspectives: ResourcePerspectives
    private lateinit var fs: DefaultFileSystem
    private lateinit var rulesProfile: RulesProfile

    @Before
    fun setUp() {
        // Setup mocks
        rulesProfile = mock(RulesProfile::class.java)
        val activeRule = mock(ActiveRule::class.java)
        `when`(activeRule.rule).thenReturn(Rule.create("repoKey", "ruleKey"))
        `when`(rulesProfile.getActiveRule(anyString(), anyString())).thenReturn(activeRule)

        fs = object : DefaultFileSystem(File("")) {
            override fun inputFiles(predicate: FilePredicate): Iterable<InputFile> {
                return arrayListOf<InputFile>(DefaultInputFile("relativePath"))
            }
        }
        perspectives = mock(ResourcePerspectives::class.java)
    }

    @Test
    fun process_empty_report() {
        // Process report
        try {
            AndroidLintProcessor(
                rulesProfile,
                perspectives,
                fs
            ).process(File("src/test/resources/lint-report-empty.xml"))
        } catch (e: Exception) {
            fail()
        }

        verify(rulesProfile, never())?.getActiveRule(anyString(), anyString())
    }

    @Test
    fun process_report_with_relative_path() {
        // Process report
        AndroidLintProcessor(rulesProfile, perspectives, fs).process(File("src/test/resources/lint-report.xml"))

        // Check we raise 30 issues on 21 different rules
        verify(rulesProfile, times(21))?.getActiveRule(anyString(), anyString())
        verify(perspectives, times(30))?.`as`(any<Class<out Perspective<*>>>(), any<InputPath>())
    }

    @Test
    @Throws(Exception::class)
    fun process_report_with_absolute_path() {
        // Process report
        AndroidLintProcessor(
            rulesProfile,
            perspectives,
            fs
        ).process(File("src/test/resources/lint-results_absolute_path.xml"))

        // Check we raise 8 issues on 8 different rules
        verify(rulesProfile, times(8))?.getActiveRule(anyString(), anyString())
        verify(perspectives, times(8))?.`as`(any<Class<out Perspective<*>>>(), any<InputPath>())
    }

    @Test
    @Throws(Exception::class)
    fun should_handle_bad_xml_results() {
        AndroidLintProcessor(
            rulesProfile,
            perspectives,
            fs
        ).process(File("src/test/resources/lint-bad-report.xml"))
        verify(rulesProfile, never())?.getActiveRule(anyString(), anyString())
        verify(perspectives, never())?.`as`(any<Class<out Perspective<*>>>(), any<InputPath>())
    }

    @Test
    fun issuable_call() {
        val issuable = mock(Issuable::class.java)
        `when`(perspectives.`as`(any<Class<out Perspective<*>>>(), any<InputPath>())).thenReturn(issuable)
        `when`(issuable.newIssueBuilder()).thenReturn(mock(Issuable.IssueBuilder::class.java, SelfReturningAnswer()))

        AndroidLintProcessor(rulesProfile, perspectives, fs).process(File("src/test/resources/lint-report.xml"))

        verify(perspectives, times(30))?.`as`(any<Class<out Perspective<*>>>(), any<InputPath>())
        verify(issuable, times(30)).addIssue(any(Issue::class.java))
    }


    @Test
    fun unknown_issue_should_not_be_reported() {
        `when`(rulesProfile.getActiveRule(anyString(), anyString())).thenReturn(null)
        AndroidLintProcessor(
            rulesProfile,
            perspectives,
            fs
        ).process(File("src/test/resources/lint-unknown-rule-report.xml"))

        verify(perspectives, never())?.`as`(any<Class<out Perspective<*>>>(), any<InputPath>())

    }

    inner class SelfReturningAnswer : Answer<Any> {
        @Throws(Throwable::class)
        override fun answer(invocation: InvocationOnMock?): Any? {
            val mock = invocation?.mock
            return if (invocation != null && invocation.method.returnType.isInstance(mock)) {
                mock
            } else {
                RETURNS_DEFAULTS.answer(invocation)
            }
        }
    }
}