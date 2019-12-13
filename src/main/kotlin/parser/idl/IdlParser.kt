package parser.idl
import parser.core.*

data class Identifier(val id: String)

data class Message(
        val id: Identifier
)

private fun spaces() = zeroOrMore(pWhitespace())

fun pIdentifier(): Parser<Identifier> {
    val firstChar = pAnyOf(('A'..'Z').toList() )
    val otherChar = pAnyOf(('0'..'9').toList() + ('A'..'Z').toList() + ('a'..'z').toList() + listOf('_'))
    return firstChar and zeroOrMore(otherChar) label "type-identifier" map { Identifier(it.joinToString("")) }
}

private fun pMessage(): Parser<Message> {
    val keyword = pString("message") label "message"
    return (keyword and spaces()) andr pIdentifier() label "message-definition" map {identifier ->
        Message(identifier)
    }
}