package idl

import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.effects.extensions.io.fx.fx
import arrow.syntax.function.pipe
import generator.generateKotlinFile
import idl.validator.*
import scanner.parseIdlFilesIO
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis


sealed class Error {
    data class ArgumentError(val error: ArgumentParsingError) : Error()
    data class InvalidInputDirectory(val error: String) : Error()
    data class InterfaceValidationError(val files: List<InvalidFileItem>) : Error()
    data class IdlParsingErrors(val errors: List<SingleFileParseResult.Failure>) : Error()
}


fun main(args: Array<String>) {

    val program = fx {
        parseArguments(args)
                .mapLeft { Error.ArgumentError(it) }
                .flatMap { args ->
                    !effect { parseIdlFilesIO(args.inputPath).mapLeft { Error.IdlParsingErrors(it) }.map {args to it} }
                }
                .flatMap { (args, fileItems) ->
                    val res = fileItems.map { validate(it) }.pipe(::validateAll)
                    when (res) {
                        is ValidationResult.Success -> (args to res.files).right()
                        is ValidationResult.Failure -> Error.InterfaceValidationError(res.files).left()
                    }

                }
                .map {(args, fileItems) ->
                    args to when(args.target) {
                        GenerationTarget.Kotlin -> fileItems.map { generateKotlinFile(it.file) } // todo: map to original file path
                        GenerationTarget.TypeScript -> TODO()
                        GenerationTarget.Protobuf -> TODO()
                    }

                }
    }

    val exitCode = program.unsafeRunSync()
            .fold({
                println("ERROR: \n")
                println("$it")
                -1
            }, {
                println("res = $it")
                println("SUCCESS:\n")
                0
            })

    exitProcess(exitCode)
}