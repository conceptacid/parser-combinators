package parser.idl

import parser.core.*

data class Identifier(val id: String)
data class FieldId(val id: String)

data class Field(
        val id: FieldId
)

data class Message(
        val id: Identifier,
        val fields: List<Field>
)

private fun spaces() = zeroOrMore(pWhitespace())
private fun semicolon() = pChar(';') andl spaces()
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
    return spaces() andr pFieldId() andl (spaces() andr semicolon()) map { Field(it) }
}

fun pMessage(): Parser<Message> {
    val keyword = pString("message") label "message"

    val left = pChar('{') andl spaces()
    val right = spaces() andr pChar('}')

    val fields = zeroOrMore(pField())

    val body = pBetween(left, fields, right)
    val header = (keyword andl spaces()) andr (pIdentifier()) andl spaces()
    return (header followedBy body) label "message-definition" map { (identifier, body) ->
        Message(identifier, body.map {
            it as Field
        })
    }
}