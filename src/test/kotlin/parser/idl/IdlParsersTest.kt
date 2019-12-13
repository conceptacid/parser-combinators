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
        assertThat(pTypeIdentifier().run(State("Abcd")))
                .isEqualTo(Success(TypeIdentifier("Abcd"), State(input = "Abcd", col = 4, pos = 4)))

        assertThat(pTypeIdentifier().run(State("abcd")))
                .isEqualTo(Failure(UnexpectedToken(label = "type-identifier", char = 'a', line = 0, col = 0)))
    }

    @Test
    fun `Custom Type Field Parser`() {
        val input = "abc:A, tag = 1;"
        val output = Field(FieldIdentifier("abc"), FieldType.CustomType(TypeIdentifier("A")), 1)
        assertThat(pField().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 0)))
    }

    @Test
    fun `String Type Field Parser`() {
        val input = "abc:String ,tag= 1;"
        val output = Field(FieldIdentifier("abc"), FieldType.String, 1)
        assertThat(pField().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 0)))
    }

    @Test
    fun `Message Parser`() {
        val input = """
            data AbcCommand {
                line1:A,tag=1;
                line2 : Int32    , tag= 2;
                line3: String, tag=   3;
            }""".trimIndent()


        val output = Data(TypeIdentifier("AbcCommand"), listOf(
                Field(FieldIdentifier("line1"), FieldType.CustomType(TypeIdentifier("A")), 1),
                Field(FieldIdentifier("line2"), FieldType.Int32, 2),
                Field(FieldIdentifier("line3"), FieldType.String, 3)
        ))

        assertThat(pData().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = 1, pos = input.length, line = 4)))

        val err1 = "message AbcCommand { line1: A = ;}"
        assertThat(pData().run(State(err1))).isInstanceOf(Failure::class.java)

        val err2 = "message Abc Command { line1: A = 1; }"
        assertThat(pData().run(State(err2))).isInstanceOf(Failure::class.java)

        val err3 = "message AbcCommand { line1 A = 1; }"
        assertThat(pData().run(State(err3))).isInstanceOf(Failure::class.java)

    }
}


// TODO: check uniqueness of field ids and tags