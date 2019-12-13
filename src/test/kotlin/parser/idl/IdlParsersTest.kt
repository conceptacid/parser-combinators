package parser.idl

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import parser.core.Failure
import parser.core.State
import parser.core.Success
import parser.core.UnexpectedToken
import parser.idl.Identifier
import parser.idl.pIdentifier
import parser.json.jNull


class IdlParsersTest {

    @Test
    fun `Identifier Parser`() {
        assertThat(pIdentifier().run(State("Abcd")))
                .isEqualTo(Success(Identifier("Abcd"), State(input = "Abcd", col = 4, pos = 4)))

        assertThat(pIdentifier().run(State("abcd")))
                .isEqualTo(Failure(UnexpectedToken(label = "type-identifier", char = 'a', line = 0, col = 0)))

    }

    @Test
    fun `Message Parser`() {
        val input = """
            message AbcCommand {
                line1: A = 1;
                line2: B = 2;
                line3: C = 3;
            }""".trimIndent()

        print(input)

        val output = Message(Identifier("AbcCommand"), listOf(
                Field(FieldId("line1"), Identifier("A"), 1),
                Field(FieldId("line2"), Identifier("B"), 2),
                Field(FieldId("line3"), Identifier("C"), 3)
        ))

        assertThat(pMessage().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = 1, pos = input.length, line = 4)))
    }
}


// TODO: check uniqueness of field ids and tags