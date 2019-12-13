import parser.core.*

fun jUnescapedChar(): Parser<Char> =
        satisfy("char") { it != '\\' && it != '\"' }

fun jEscapedChar(): Parser<out Char> =
        choice(listOf(
                Pair("\\\"", '\"'),      // quote
                Pair("\\\\", '\\'),      // reverse solidus
                Pair("\\/", '/'),        // solidus
                Pair("\\b", '\b'),       // backspace
                Pair("\\f", 'f'),        // form feed
                Pair("\\n", '\n'),       // newline
                Pair("\\r", '\r'),       // cr
                Pair("\\t", '\t')        // tab
        ).map { pair ->
            pString(pair.first) map { pair.second }
        }) label "escaped-char"

fun jUnicodeChar(): Parser<out Char> {
    val backslash = pChar('\\')
    val uChar = pChar('u')
    val hexDigit = pAnyOf(('0'..'9').toList() + ('A'..'F').toList() + ('a'..'f').toList())

    return backslash andr uChar andr hexDigit and hexDigit and hexDigit and hexDigit map {
        it.joinToString("").toInt(16).toChar()
    }
}

fun quotedString(): Parser<String> {
    val quote = pChar('\"') label "quote"
    val jChar = jUnescapedChar() or jEscapedChar() or jUnicodeChar()

    return quote andr (zeroOrMore(jChar)) andl quote map { it.joinToString("") }
}