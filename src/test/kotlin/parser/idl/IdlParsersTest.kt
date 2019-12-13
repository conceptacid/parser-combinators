package parser.idl

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import parser.core.Failure
import parser.core.State
import parser.core.Success
import parser.core.UnexpectedToken


class IdlParsersTest {

    @Test
    fun `Identifier Parser`() {
        assertThat(pIdentifier().run(State("Abcd")))
                .isEqualTo(Success(TypeIdentifier("Abcd"), State(input = "Abcd", col = 4, pos = 4)))

        assertThat(pIdentifier().run(State("abcd")))
                .isEqualTo(Failure(UnexpectedToken(label = "type-identifier", char = 'a', line = 0, col = 0)))

    }

    @Test
    fun `Message Field Parser`() {
        val input = "abc:A = 1;"
        val output = Field(FieldIdentifier("abc"), TypeIdentifier("A"), 1)
        assertThat(pField().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 0)))
    }


    @Test
    fun `Message Parser`() {
        val input = """
            message AbcCommand {
                line1:A = 1;
                line2 : B = 2;
                line3: C= 3;
            }""".trimIndent()


        val output = Message(TypeIdentifier("AbcCommand"), listOf(
                Field(FieldIdentifier("line1"), TypeIdentifier("A"), 1),
                Field(FieldIdentifier("line2"), TypeIdentifier("B"), 2),
                Field(FieldIdentifier("line3"), TypeIdentifier("C"), 3)
        ))

        assertThat(pMessage().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = 1, pos = input.length, line = 4)))

        val err1 = """
            message AbcCommand {
                line1: A = ;
            }""".trimIndent()

        assertThat(pMessage().run(State(err1))).isInstanceOf(Failure::class.java)
    }
}


// TODO: check uniqueness of field ids and tags