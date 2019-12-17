package idl.validator

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import generator.generateKotlinFile
import idl.generator.generateTypescriptFile
import parser.core.*
import parser.idl.File

typealias GeneratorFn = (File)->String

sealed class Arg {
    data class InputPath(val path: String) : Arg()
    data class OutputPath(val path: String) : Arg()
    data class Replace(val value: Boolean) : Arg()
}

data class ArgumentParsingError(val text: String)
data class Args(val generator: GeneratorFn, val inputPath: String = "", val outputPath: String = "", val replace: Boolean = true)

fun pGenerationTarget(): Parser<GeneratorFn> =
        (pString("kotlin") map { ::generateKotlinFile as GeneratorFn}) or
                (pString("typescript") map { ::generateTypescriptFile as GeneratorFn })

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
    arguments.fold(Args(target)) { result, argument ->
        when (argument) {
            is Arg.InputPath -> result.copy(inputPath = argument.path)
            is Arg.OutputPath -> result.copy(outputPath = argument.path)
            is Arg.Replace -> result.copy(replace = argument.value)
            else -> TODO("Implement handling of $argument")
        }
    }
}


fun parseArguments(args: Array<String>): Either<ArgumentParsingError, Args> {
    return try {
        val res = pArgs().invoke(State(args.joinToString(" ")))
        when (res) {
            is Success -> {
                if (!res.state.eof()) ArgumentParsingError("could not match command line arguments: '${res.state.input.substring(res.state.pos)}'").left()
                else res.value.right()
            }
            is Failure -> ArgumentParsingError("command line paring failure: ${res.error}").left()
        }
    } catch (t: Throwable) {
        ArgumentParsingError(t.message ?: "exception $t").left()
    }
}