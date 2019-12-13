package parser.idl

import parser.core.*

data class TypeIdentifier(val id: String)
data class FieldIdentifier(val id: String)

data class Field(
        val id: FieldIdentifier,
        val typeID: TypeIdentifier,
        val tag: Int
)

data class Message(
        val id: TypeIdentifier,
        val fields: List<Field>
)

private fun spaces() = zeroOrMore(pWhitespace())
private fun delimiters() = spaces().ignore()
private fun semicolon() = pChar(';') andl spaces()
private fun colon() = pChar(':') andl spaces()
private fun equalSign() = pChar('=') andl spaces()

private fun separatedBy(parser: Parser<out Any>, sep: Parser<out Any>): Parser<List<Any>> =
        parser and zeroOrMore(sep andr parser)


fun pFieldId(): Parser<FieldIdentifier> {
    val alpha = ('A'..'Z').toList() + ('a'..'z').toList() + listOf('_')
    val firstChar = pAnyOf(alpha)
    val otherChar = pAnyOf(alpha + ('0'..'9').toList())
    return firstChar and zeroOrMore(otherChar) label "field-identifier" map { FieldIdentifier(it.joinToString("")) }
}

fun pIdentifier(): Parser<TypeIdentifier> {
    val firstChar = pAnyOf(('A'..'Z').toList())
    val otherChar = pAnyOf(('0'..'9').toList() + ('A'..'Z').toList() + ('a'..'z').toList() + listOf('_'))
    return firstChar and zeroOrMore(otherChar) label "type-identifier" map { TypeIdentifier(it.joinToString("")) }
}

fun pField(): Parser<Field> {
    return pDelimited(delimiters(), pFieldId(), pChar(':'), pIdentifier(), pChar('='), pInt(), pChar(';')) map { (fieldId, _, typeId, _, tag, _) ->
        Field(fieldId, typeId, tag)
    } label "field-definition"
}

fun pMessage(): Parser<Message> {
    val header = pDelimited(delimiters(), pString("message"), pIdentifier())
    val body = pDelimited(delimiters(), pChar('{'), zeroOrMore(pField() andl delimiters()), pChar('}'))
    return pDelimited(delimiters(), header, body) label "message-definition" map { (header, body) ->
        val (_, id) = header
        val (_, fields, _) = body
        Message(id, fields.map { it as Field })
    }
}