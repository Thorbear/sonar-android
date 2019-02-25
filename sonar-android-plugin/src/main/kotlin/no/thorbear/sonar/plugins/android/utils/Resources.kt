package no.thorbear.sonar.plugins.android.utils

import java.io.InputStreamReader
import java.nio.charset.Charset

class Resources {

    fun asReader(
        path: String,
        charset: Charset = Charsets.UTF_8,
        classLoader: Class<out Any> = javaClass
    ): InputStreamReader {
        return InputStreamReader(classLoader.getResourceAsStream(path), charset)
    }
}