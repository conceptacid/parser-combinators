package scanner

import generator.generateKotlinFile
import idl.FileItem
import idl.ParseResult
import idl.ValidationResult
import idl.validator.validate
import idl.validator.validateAll
import org.junit.Test
import parser.json.JNull


class ScannerTest {

    @Test
    fun `find all kt files`() {


        fun ParseResult.andThen(fn: (List<FileItem>) -> ValidationResult) = when (this) {
            is ParseResult.Success -> fn(this.files)
            is ParseResult.ParsingError -> {
                println("Parsing Errors: \n $this")
                throw RuntimeException("there were parsing errors")
            }
        }


        fun ValidationResult.andThen(fn: (List<FileItem>) -> Unit) = when (this) {
            is ValidationResult.Success -> fn(files)
            is ValidationResult.Failure -> {
                println("Validation errors: \n $this")
                throw RuntimeException("there were validation errors")
            }
        }

        val res = parseIdlFiles("src/")
                .andThen {
                    it
                            .map { validate(it) }
                            .let { validateAll(it) }
                }
                .andThen {
                    val res = it.map { generateKotlinFile(it.file) }
                    println(res)
                }


    }
}