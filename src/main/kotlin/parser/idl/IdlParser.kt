package parser.idl

import parser.core.*

data class Identifier(val id: String)
data class FieldId(val id: String)

data class Field(
        val id: FieldId,
        val typeID: Identifier,
        val tag: Int
)

data class Message(
        val id: Identifier,
        val fields: List<Field>
)

private fun spaces() = zeroOrMore(pWhitespace())
private fun delimiters() = spaces().ignore()
private fun semicolon() = pChar(';') andl spaces()
private fun colon() = pChar(':') andl spaces()
private fun equalSign() = pChar('=') andl spaces()

private fun separatedBy(parser: Parser<out Any>, sep: Parser<out Any>): Parser<List<Any>> =
        parser and zeroOrMore(sep andr parser)


fun pFieldId(): Parser<FieldId> {
    val alpha = ('A'..'Z').toList() + ('a'..'z').toList() + listOf('_')
    val firstChar = pAnyOf(alpha)
    val otherChar = pAnyOf(alpha + ('0'..'9').toList())
    return firstChar and zeroOrMore(otherChar) label "field-identifier" map { FieldId(it.joinToString("")) }
}

fun pIdentifier(): Parser<Identifier> {
    val firstChar = pAnyOf(('A'..'Z').toList())
    val otherChar = pAnyOf(('0'..'9').toList() + ('A'..'Z').toList() + ('a'..'z').toList() + listOf('_'))
    return firstChar and zeroOrMore(otherChar) label "type-identifier" map { Identifier(it.joinToString("")) }
}


fun pField(): Parser<Field> {
    return pDelimited(delimiters(), pFieldId(), pChar(':'), pIdentifier(), pChar('='), pInt(), pChar(';')) map { (fieldId, _, typeId, _, tag, _) ->
        Field(fieldId, typeId, tag)
    } label "field-definition"
}

fun pMessage(): Parser<Message> {
    val keyword = pString("message") label "message"

    val left = pChar('{') andl spaces()
    val right = spaces() andr pChar('}')

    val fields = zeroOrMore(pField() andl delimiters())

    val header = pDelimited(delimiters(), pString("message"), pIdentifier())
    val body = pDelimited(delimiters(), pChar('{'), fields, pChar('}'))

    return pDelimited(delimiters(), header, body) label "message-definition" map { (header, body) ->
        val (_, id) = header
        val (_, fields, _) = body
        Message(id, fields.map { it as Field })
    }
}