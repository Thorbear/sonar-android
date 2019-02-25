# SonarQube Android Lint Plugin

## Description
This plugin enhances the Java Plugin by providing the ability to import the Android Lint reports.
The idea is to visualize Android Lint errors directly in SonarQube.

This plugin started out as a copy of https://github.com/ofields/sonar-android with source code converted from Java to Kotlin, and project structure converted from Maven to Gradle.

## Usage
* Most Android projects are compiled with Gradle, so if this is the case use the [SonarQube Scanner for Gradle](https://plugins.gradle.org/plugin/org.sonarqube) to analyse your Android project
* Tune the SonarQube quality profile by activating the Android Lint rules on which you'd like to see some issues reported into SonarQube
* Configure the Gradle project to execute the Android Lint engine before launching the SonarQube analysis
* Define in the Gradle project the property sonar.android.lint.report (default value points to : build/outputs/lint-results.xml) to specify the path to the Android Lint report
* Run your Analyzer command from the project root dir

## Compiling and Installing the plugin
* Clone the repository
* run `gradlew clean build` in your terminal
* copy the jar (in the new generated `build/libs` folder) to the `<path_to_your_sonar_install>/extensions/plugins` folder in your SonarQube installation
* restart sonar

## Upgrade Android Lint
1. Change `versions.lint` in `build.gradle`
2. Ensure `lint-rules-gen` compiles, `gradlew :lint-rules-gen:build`
3. Run `lint-rules-gen` - `gradlew clean build` TODO can we also execute the built artifact from Gradle?
4. Copy the generated XML to the correct location (from the root directory)
  - `cp lint-rules-gen/out/org/sonar/plugins/android/lint/android_lint_sonar_way.xml sonar-android-plugin/src/main/resources/org/sonar/plugins/android/lint/android_lint_sonar_way.xml`
  - `cp lint-rules-gen/out/org/sonar/plugins/android/lint/rules.xml sonar-android-plugin/src/main/resources/org/sonar/plugins/android/lint/rules.xml`
5. go to `its/plugin/projects/SonarAndroidSample` and run `gradlew lint`
6. Run the tests from the base directory `gradlew clean build`, fix as necessary.
7. Add missing rules to: `sonar-android-plugin/src/main/resources/org/sonar/plugins/android/lint/java-model.xml`

## Running analysis:
*TODO - I think this profile is automatically included if lint report path property is specified*
1. On a Maven project
 - mvn sonar:sonar -Dsonar.profile="Android Lint" in your project

2. On another project using sonar-runner
 - Add this property to your sonar-project.properties
  -> sonar.profile=Android Lint


## TODO
1. Apply PR from original repository with updated lint rules
2. Update dependencies
3. Clean up code
4. Check compatibility with newer versions of SonarQube
5. Check if we can detach from the SonarJava plugin to become standalone
6. Create a sample project
7. Integrate with a build server and SonarCloud
8. Include the `lint-rules-gen` module