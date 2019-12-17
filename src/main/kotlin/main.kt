package idl

import arrow.core.flatMap
import arrow.effects.extensions.io.fx.fx
import arrow.syntax.function.pipe
import generator.generateKotlinFile
import idl.generator.generateTypescriptFile
import idl.validator.*
import scanner.parseIdlFilesIO
import kotlin.system.exitProcess


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
                    !effect { parseIdlFilesIO(args.inputPath).mapLeft { Error.IdlParsingErrors(it) }.map { args to it } }
                }
                .flatMap { (args, fileItems) ->
                    fileItems
                            .map { validate(it) }
                            .pipe(::validateAll)
                            .map { args to it }
                            .mapLeft { Error.InterfaceValidationError(it) }

                }
                .map { (args, fileItems) ->
                    args to fileItems.map { it to args.generator(it.file) } // todo: map to original file path
                }
                .map { (args, files) ->
                    !effect {
                        files.map {
                            println("creating file ${it.first}")
                            println("contents follow \n${it.second}")
                        }

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