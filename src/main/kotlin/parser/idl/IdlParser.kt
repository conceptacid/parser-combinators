package parser.idl

import parser.core.*


/**
 * data Abc {
 *   name: String, tag = 1;
 *   headers: Map<Int32, String>, tag = 2;
 * }
 *
 * choice AbcOptions {
 *     virtual name: String, tag = 1    // common field, must be present in all cases
 *
 *     option Abc {
 *     }, tag = 1;
 *     option Bcd = 2 {
 *     }, tag = 2;
 *     option Def, tag = 3;           // object
 * }
 *
 * data AbcCommand {
 *    address: AbcOption.Abc, tag = 1;
 *    phone: Abc, tag = 2;
 *    repeated name: String tag = 3;
 * }
 *
 * topic "topic-name", request AbcCommand, response AbcResponse         // the response is optional
 *
 */


data class TypeIdentifier(val id: String)

data class FieldIdentifier(val id: String)

sealed class FieldType {
    object Int8 : FieldType()
    object Int16 : FieldType()
    object Int32 : FieldType()
    object String : FieldType()
    object Boolean : FieldType()
    object Float : FieldType()
    //data class Map(val keyType: FieldType, val valType: FieldType) : FieldType()
    //data class List(val valType: FieldType) : FieldType()
    data class CustomType(val id: TypeIdentifier) : FieldType()
}

data class Field(
        val id: FieldIdentifier,
        val fieldType: FieldType,
        val tag: Int
)

data class Data(
        val id: TypeIdentifier,
        val fields: List<Field>
)

data class Option(
        val id: TypeIdentifier,
        val body: Maybe<List<Field>>,
        val tag: Int
)

data class Choice(
        val id: TypeIdentifier,
        val options: List<Option>
)

data class Topic(
        val text: String,
        val requestType: TypeIdentifier,
        val responseType: Maybe<TypeIdentifier>
)

private fun spaces() = zeroOrMore(pWhitespace())
private fun delimiters() = spaces().ignore()

fun pFieldIdentifier(): Parser<FieldIdentifier> {
    val alpha = ('A'..'Z').toList() + ('a'..'z').toList() + listOf('_')
    val firstChar = pAnyOf(alpha)
    val otherChar = pAnyOf(alpha + ('0'..'9').toList())
    return firstChar and zeroOrMore(otherChar) label "field-identifier" map { FieldIdentifier(it.joinToString("")) }
}

fun pTypeIdentifier(): Parser<TypeIdentifier> {
    val firstChar = pAnyOf(('A'..'Z').toList())
    val otherChar = pAnyOf(('0'..'9').toList() + ('A'..'Z').toList() + ('a'..'z').toList() + listOf('_'))
    return firstChar and zeroOrMore(otherChar) label "type-identifier" map { TypeIdentifier(it.joinToString("")) }
}

fun pTag(): Parser<Int> {
    return pDelimited(delimiters(), pString("tag"), pChar('='), pInt()) map { it.c }
}

fun pKeyValuePair(): Parser<Pair<FieldType, FieldType>> {
    return pDelimited(delimiters(), pFieldType(), pChar(','), pFieldType()) map { it.a to it.c }
}
/*
fun pMap() : Parser<FieldType> {
    return pDelimited(delimiters(), pString("Map<"), pKeyValuePair(), pString(">")) map { FieldType.Map(it.b.first, it.b.second)}
}

fun pList() : Parser<FieldType> {
    return pDelimited(delimiters(), pString("List<"), pFieldType(), pString(">")) map { FieldType.List(it.b)}
}
*/

fun pFieldType(): Parser<FieldType> {
    return pString("Int8") map { FieldType.Int8 as FieldType } or
            (pString("Int16") map { FieldType.Int16 as FieldType }) or
            (pString("Int32") map { FieldType.Int32 as FieldType }) or
            (pString("String") map { FieldType.String as FieldType }) or
            (pString("Boolean") map { FieldType.Boolean as FieldType }) or
            (pString("Float") map { FieldType.Float as FieldType }) or
            (pTypeIdentifier() map { FieldType.CustomType(it) as FieldType })
    //pMap() or
    //pList()
}

fun pField(): Parser<Field> {
    return pDelimited(delimiters(), pFieldIdentifier(), pChar(':'), pFieldType(),
            pChar(','), pTag(),
            pChar(';')) map { (fieldId, _, fieldType, _, tag, _) ->
        Field(fieldId, fieldType, tag)
    } label "field-definition"
}

fun pListOfFields(): Parser<List<Field>> {
    return pDelimited(delimiters(), pChar('{'), zeroOrMore(pField() andl delimiters()), pChar('}')) map { (_, fields, _) ->
        fields.map { it as Field }
    }
}

fun pData(): Parser<Data> {
    val header = pDelimited(delimiters(), pString("data"), pTypeIdentifier())
    val body = pListOfFields()
    return pDelimited(delimiters(), header, body) label "data-definition" map { (header, body) ->
        val (_, id) = header
        Data(id, body)
    }
}

fun pOption(): Parser<Option> {
    return pDelimited(delimiters(), pString("option"), pTypeIdentifier(),
            optional(pListOfFields()), pChar(','), pTag(), pChar(';')) map { (_, typeIdentifier, body, _, tag, _) ->
        Option(typeIdentifier, body, tag)
    }
}

fun pChoiceBody(): Parser<List<Option>> {
    return pDelimited(delimiters(), pChar('{'), zeroOrMore(pOption() andl delimiters()), pChar('}')) map { (_, options, _) ->
        options.map { it as Option }
    }
}

fun pChoice(): Parser<Choice> {
    val identifier = pDelimited(delimiters(), pString("choice"), pTypeIdentifier()) map { it.b }
    val body = pChoiceBody()
    return pDelimited(delimiters(), identifier, body) map { (identifier, options) ->
        Choice(identifier, options)
    }
}

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

fun pTopic() : Parser<Topic> {
    val requestIdParser = pDelimited(delimiters(), pChar(',') , pString("request"), pTypeIdentifier() ) map { it.c }
    val responseParser = pDelimited(delimiters(), pChar(',') , pString("response"), pTypeIdentifier() ) map { it.c }

    return pDelimited(delimiters(), pString("topic"), quotedString(),
            requestIdParser, optional( responseParser)) map { (_, text, requestType, responseType) ->
        Topic(text, requestType, responseType)
    }
}

