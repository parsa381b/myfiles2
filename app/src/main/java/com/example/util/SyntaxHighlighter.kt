package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.util.regex.Pattern

object SyntaxHighlighter {

    private val KEYWORD_COLOR = Color(0xFF7C4DFF)
    private val STRING_COLOR = Color(0xFF2E7D32)
    private val COMMENT_COLOR = Color(0xFF8A9099)
    private val NUMBER_COLOR = Color(0xFFE65100)
    private val ANNOTATION_COLOR = Color(0xFF00838F)
    private val TAG_COLOR = Color(0xFFC2185B)
    private val JSON_KEY_COLOR = Color(0xFF0277BD)

    private val KEYWORDS = setOf(
        "abstract", "actual", "annotation", "as", "break", "by", "catch", "class", "companion", "const",
        "constructor", "continue", "data", "do", "else", "enum", "expect", "false", "final", "finally",
        "for", "fun", "get", "if", "import", "in", "infix", "init", "inline", "inner", "interface",
        "internal", "is", "lateinit", "noinline", "null", "object", "open", "operator", "out", "override",
        "package", "private", "protected", "public", "reified", "return", "sealed", "set", "super",
        "suspend", "tailrec", "this", "throw", "true", "try", "typealias", "val", "var", "vararg",
        "when", "where", "while",
        // Java/JS/TS/Python
        "def", "let", "function", "async", "await", "export", "default", "from", "static", "void", "int",
        "boolean", "float", "double", "long", "yield", "lambda", "elif", "pass", "None", "self", "with"
    )

    private val KEYWORDS_REGEX = Pattern.compile("\\b(${KEYWORDS.joinToString("|")})\\b")
    private val STRING_REGEX = Pattern.compile("(\"[^\"]*\"|'[^']*')")
    private val COMMENT_REGEX = Pattern.compile("(//.*|#.*|/\\*[\\s\\S]*?\\*/)")
    private val NUMBER_REGEX = Pattern.compile("\\b(\\d+(\\.\\d+)?)\\b")
    private val ANNOTATION_REGEX = Pattern.compile("(@\\w+)")
    private val TAG_REGEX = Pattern.compile("(</?[a-zA-Z0-9_-]+(\\s+[^>]*)?>)")
    private val JSON_KEY_REGEX = Pattern.compile("\"([^\"]+)\"\\s*:")

    fun highlight(code: String, extension: String, isDark: Boolean): AnnotatedString {
        val stringColor = if (isDark) Color(0xFF81C784) else STRING_COLOR
        val keywordColor = if (isDark) Color(0xFFB388FF) else KEYWORD_COLOR
        val commentColor = if (isDark) Color(0xFF90A4AE) else COMMENT_COLOR
        val numberColor = if (isDark) Color(0xFFFFB74D) else NUMBER_COLOR
        val annotationColor = if (isDark) Color(0xFF4DD0E1) else ANNOTATION_COLOR
        val tagColor = if (isDark) Color(0xFFF06292) else TAG_COLOR
        val jsonKeyColor = if (isDark) Color(0xFF4FC3F7) else JSON_KEY_COLOR

        return buildAnnotatedString {
            append(code)

            if (extension in setOf("json")) {
                val keyMatcher = JSON_KEY_REGEX.matcher(code)
                while (keyMatcher.find()) {
                    addStyle(
                        SpanStyle(color = jsonKeyColor, fontWeight = FontWeight.SemiBold),
                        keyMatcher.start(),
                        keyMatcher.end()
                    )
                }
            } else if (extension in setOf("html", "xml")) {
                val tagMatcher = TAG_REGEX.matcher(code)
                while (tagMatcher.find()) {
                    addStyle(
                        SpanStyle(color = tagColor, fontWeight = FontWeight.Medium),
                        tagMatcher.start(),
                        tagMatcher.end()
                    )
                }
            } else {
                // Keywords
                val kwMatcher = KEYWORDS_REGEX.matcher(code)
                while (kwMatcher.find()) {
                    addStyle(
                        SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold),
                        kwMatcher.start(),
                        kwMatcher.end()
                    )
                }
            }

            // Strings
            val strMatcher = STRING_REGEX.matcher(code)
            while (strMatcher.find()) {
                addStyle(SpanStyle(color = stringColor), strMatcher.start(), strMatcher.end())
            }

            // Numbers
            val numMatcher = NUMBER_REGEX.matcher(code)
            while (numMatcher.find()) {
                addStyle(SpanStyle(color = numberColor), numMatcher.start(), numMatcher.end())
            }

            // Annotations
            val annMatcher = ANNOTATION_REGEX.matcher(code)
            while (annMatcher.find()) {
                addStyle(SpanStyle(color = annotationColor, fontWeight = FontWeight.Medium), annMatcher.start(), annMatcher.end())
            }

            // Comments (processed last so comment style overrides inner keywords/numbers)
            val comMatcher = COMMENT_REGEX.matcher(code)
            while (comMatcher.find()) {
                addStyle(
                    SpanStyle(color = commentColor, fontStyle = FontStyle.Italic),
                    comMatcher.start(),
                    comMatcher.end()
                )
            }
        }
    }
}
