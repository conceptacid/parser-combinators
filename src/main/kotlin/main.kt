package idl

import arrow.core.Either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.fix
import arrow.core.flatMap
import arrow.data.extensions.list.traverse.traverse
import arrow.data.fix
import arrow.effects.extensions.io.fx.fx
import arrow.syntax.function.pipe
import idl.validator.ArgumentError
import idl.validator.parseArguments
import idl.validator.validate
import idl.validator.validateAll
import idl.writer.WriterError
import idl.writer.writeFile
import scanner.ScannerError
import scanner.parseIdlFilesIO
import kotlin.system.exitProcess


sealed class Error {
    data class BadArguments(val error: ArgumentError) : Error()
    data class ScannerFinishedWithErrors(val error: ScannerError) : Error()
    data class InterfaceValidationError(val files: List<InvalidFileItem>) : Error()
    data class WritingFileFailed(val error: WriterError) : Error()
}


fun main(args: Array<String>) {

    val program = fx {
        parseArguments(args)
                .mapLeft { Error.BadArguments(it) }
                .flatMap { args ->
                    !effect { parseIdlFilesIO(args.sourceDirectory).mapLeft { Error.ScannerFinishedWithErrors(it) }.map { args to it } }
                }
                .flatMap { (args, fileItems) ->
                    fileItems
                            .map { validate(it) }
                            .pipe(::validateAll)
                            .map { args to it }
                            .mapLeft { Error.InterfaceValidationError(it) }

                }
                .map { (args, fileItems) ->
                    args to fileItems.map { it.path to args.generator(it.file) } // todo: map to original file path
                }
                .flatMap { (args, files) ->
                    !effect {
                        if (args.replace) {
                            args.targetDirectory.deleteRecursively()
                            args.targetDirectory.mkdir()
                        }

                        files.traverse(Either.applicative()) {
                            val relativePath = it.first.replaceAfterLast('.', args.outputExtension)
                            println("creating a file '${relativePath}'")
                            writeFile(args.targetDirectory, relativePath, it.second)
                            //println("contents follow \n${it.second}")
                        }.fix().map { it.fix() }.mapLeft { Error.WritingFileFailed(it) }
                    }
                }
    }

    val exitCode = program.unsafeRunSync()
            .fold({
                println("ERROR: \n")
                println("$it")
                -1
            }, {
                println("SUCCESS: ${it.size} file(s) written.\n")
                0
            })

    exitProcess(exitCode)
}