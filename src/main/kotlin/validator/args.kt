package idl.validator

import arrow.core.Either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.either.applicative.map
import arrow.core.left
import arrow.core.right
import generator.generateKotlinFile
import idl.generator.generateTypescriptFile
import parser.core.*
import java.io.File
import parser.idl.File as IdlFile

typealias GeneratorFn = (IdlFile)->String

sealed class Arg {
    data class InputPath(val path: String) : Arg()
    data class OutputPath(val path: String) : Arg()
    data class Replace(val value: Boolean) : Arg()
}


sealed class ArgumentError {
    data class ArgumentParsingError(val text: String): ArgumentError()
    data class InvalidSourceDirectory(val path: String): ArgumentError()
    data class InvalidTargetDirectory(val path: String): ArgumentError()
}



data class Args(val generator: GeneratorFn, val outputExtension: String,  val inputPath: String = "", val outputPath: String = "", val replace: Boolean = true)
data class ValidatedArguments(val generator: GeneratorFn, val outputExtension: String, val sourceDirectory: File, val targetDirectory: File, val replace: Boolean)

fun pGenerationTarget(): Parser<Pair<GeneratorFn, String>> =
        (pString("kotlin") map { ::generateKotlinFile as GeneratorFn to "kt"}) or
                (pString("typescript") map { ::generateTypescriptFile as GeneratorFn to "ts" })

fun pPath(): Parser<String> {
    val alpha = ('A'..'Z').toList() + ('a'..'z').toList() + listOf('_', '~', '/', '-')
    val pathChar = pAnyOf(alpha)
    return oneOrMore(pathChar) map { it.joinToString("") }
}

fun spaces(): Parser<out Any> = zeroOrMore(pChar(' '))

fun pInputPath(): Parser<Arg> = spaces() andr pString("-i") andr spaces() andr pPath() map { Arg.InputPath(it) }
fun pOutputPath(): Parser<Arg> = spaces() andr pString("-o") andr spaces() andr pPath() map { Arg.OutputPath(it) }

fun pArg(): Parser<Arg> = pInputPath() or pOutputPath()

fun pArgs(): Parser<Args> = pDelimited(spaces(), pGenerationTarget(), oneOrMore(pArg())) map { (target, arguments) ->
    val (generator, outputExtension) = target
    arguments.fold(Args(generator, outputExtension)) { result, argument ->
        when (argument) {
            is Arg.InputPath -> result.copy(inputPath = argument.path)
            is Arg.OutputPath -> result.copy(outputPath = argument.path)
            is Arg.Replace -> result.copy(replace = argument.value)
            else -> TODO("Implement handling of $argument")
        }
    }
}


fun parseArguments(args: Array<String>): Either<ArgumentError, ValidatedArguments> {
    return try {
        val res = pArgs().invoke(State(args.joinToString(" ")))
        when (res) {
            is Success -> {
                if (!res.state.eof()) ArgumentError.ArgumentParsingError("could not match command line arguments: '${res.state.input.substring(res.state.pos)}'").left()
                else {
                    val inputDir = File(res.value.inputPath)
                    val outputDir = File(res.value.outputPath)

                    val maybeSourceDir = if(inputDir.exists() && inputDir.isDirectory) inputDir.right() else ArgumentError.InvalidSourceDirectory(res.value.inputPath).left()
                    val maybeTargetDir = if(outputDir.exists() && outputDir.isDirectory) outputDir.right() else ArgumentError.InvalidTargetDirectory(res.value.outputPath).left()

                    Either.applicative<ArgumentError>().tupled(maybeSourceDir, maybeTargetDir).map { (sourceDir, targetDir) ->
                        ValidatedArguments(res.value.generator, res.value.outputExtension, sourceDir, targetDir, res.value.replace)
                    }
                }
            }
            is Failure -> ArgumentError.ArgumentParsingError("command line paring failure: ${res.error}").left()
        }
    } catch (t: Throwable) {
        ArgumentError.ArgumentParsingError(t.message ?: "exception $t").left()
    }
}