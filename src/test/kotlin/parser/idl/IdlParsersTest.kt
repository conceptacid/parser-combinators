package parser.idl

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import parser.core.*
import java.io.File


class IdlParsersTest {
    @Test
    fun `Identifier Parser`() {
        assertThat(pTypeIdentifier().run(State("Abcd")))
                .isEqualTo(Success(TypeIdentifier("Abcd"), State(input = "Abcd", col = 4, pos = 4)))

        assertThat(pTypeIdentifier().run(State("abcd")))
                .isEqualTo(Failure(UnexpectedToken(label = "type-identifier", char = 'a', line = 0, col = 0)))
    }

    @Test
    fun `Custom Field Parser`() {
        val input = "abc:A, tag = 1;"
        val output = Field(Identifier("abc"), FieldType.CustomType(TypeIdentifier("A")), 1)
        assertThat(pField().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 0)))
    }

    @Test
    fun `String Field Parser`() {
        val input = "abc:String ,tag= 1;"
        val output = Field(Identifier("abc"), FieldType.String, 1)
        assertThat(pField().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 0)))
    }

    @Test
    fun `String Field Type Parser`() {
        val input = "String"
        val output = FieldType.String
        assertThat(pFieldType(0).run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 0)))
    }

    @Test
    fun `List level 1 Field Type Parser`() {
        val input = "List<String>"
        val output = FieldType.List(FieldType.String)
        assertThat(pFieldType(0).run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 0)))
    }

    @Test
    fun `List level 2 Field Type Parser`() {
        val input = "List<List<Boolean>>"
        val output = FieldType.List(FieldType.List(FieldType.Boolean))
        assertThat(pFieldType(0).run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 0)))
    }

    @Test
    fun `Comment Parser`() {
        val input = "// blafazls\n"
        val output = "blafazls"
        assertThat(pComment().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 1)))
    }

    @Test
    fun `Delimeter Parser`() {
        val input = "// blafazls\n"
        val output = Unit
        assertThat(delimiters().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 1)))
    }

    @Test
    fun `List level 3 Field Type Parser`() {
        val input = "List<List<List<Boolean>>>"
        val output = FieldType.List(FieldType.List(FieldType.List(FieldType.Boolean)))
        assertThat(pFieldType(0).run(State(input)))
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
                Field(Identifier("line1"), FieldType.CustomType(TypeIdentifier("A")), 1),
                Field(Identifier("line2"), FieldType.Int32, 2),
                Field(Identifier("line3"), FieldType.String, 3)
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

    @Test
    fun `One Topic Parser`() {
        val input = "topic \"abc\", request = Bla, response = Foo"
        val output = Topic("abc", TypeIdentifier("Bla"), Maybe.just(TypeIdentifier("Foo")))
        assertThat(pTopic().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 0)))
    }

    @Test
    fun `One Import Parser`() {
        val input = "import bla.fazl"
        val output = Import("bla.fazl")
        assertThat(pImport().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 0)))
    }


    @Test
    fun `Commented construct Parser`() {
        val input = "//topic \"hello\", request = A, response = B\ntopic \"a\", request=C\n"
        val output = Topic("a", TypeIdentifier("C"), Maybe.none())
        assertThat(pConstruct().run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = input.length, pos = input.length, line = 0)))
    }

    @Test
    fun `Many Imports Parser`() {
        val input = """
            import bla.fazl;
            import upf.murgl;
        """.trimMargin()

        val output = listOf(
                Import("bla.fazl"),
                Import("upf.murgl"))

        val p = zeroOrMore(optional(delimiters()) andr pImport() andl optional(delimiters()) andl pChar(';') andl optional(delimiters()))
        //val p = separatedBy(pChar(';'), pImport())
        assertThat(p.run(State(input)))
                .isEqualTo(Success(output, State(input = input, col = 29, pos = input.length, line = 1)))
    }

    @Test
    fun `real file`() {

        println(System.getProperty("user.dir"))

        val f = File("src/test/kotlin/parser/idl/example.idl")
        f.useLines {
            val lines = it.toList().joinToString("\n")
            //println(lines)
            val res = pFile().run(State(lines))
            //println(res)
            val ast = res as Success<parser.idl.File>
            if(!ast.state.eof()) {
                println("remaining input: $res")
                throw RuntimeException("not all lines parsed")
            }
            println("\nPARSED:")
            println(ast.value.packageIdentifier)
            println(ast.value.imports.joinToString("\n"))
            println(ast.value.objects.joinToString("\n"))
        }

    }
}


// TODO: check uniqueness of field ids and tags