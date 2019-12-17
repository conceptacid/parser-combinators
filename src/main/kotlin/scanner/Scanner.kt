package scanner

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import idl.FileItem
import idl.SingleFileParseResult
import parser.core.*
import parser.idl.pFile
import java.io.File

fun findAllFilesRecursive(directory: String, extension: String): Sequence<File> {
    return File(directory).walkTopDown().filter { it.isFile && it.extension == extension }
}

suspend fun parseIdlFilesIO(directory: String): Either<List<SingleFileParseResult.Failure>, List<FileItem>> = findAllFilesRecursive(directory, "idl").toList()
        .map {
            try {
                val text = it.readLines().joinToString("\n")
                when (val result = pFile().run(State(text))) {
                    is Success -> {
                        if (!result.state.eof()) {
                            val sample = result.state.input.substring(result.state.pos, result.state.input.length)
                            SingleFileParseResult.Failure(it.name, it.path, result.state.line, result.state.col, "Could not parse file after '$sample'")
                        } else {
                            SingleFileParseResult.Success(FileItem(it.name, it.path, result.value))
                        }
                    }
                    is Failure -> when (result.error) {
                        is UnexpectedToken -> SingleFileParseResult.Failure(it.name, it.path, result.error.line, result.error.col, "Unexpected token '${result.error.label}'")
                        is NoMoreInput -> SingleFileParseResult.Failure(it.name, it.path, 0, 0, "No more input '${result.error.label}'")
                        is SyntaxError -> SingleFileParseResult.Failure(it.name, it.path, result.error.line, result.error.col, "Syntax Error '${result.error.label}'")
                    }
                }
            } catch (e: Throwable) {
                SingleFileParseResult.Failure(it.name, it.path, 0, 0, e.message
                        ?: "Unspecified exception during parsing")
            }
        }
        .fold( emptyList<SingleFileParseResult.Failure>() to emptyList<FileItem>()) { acc, res -> when(res) {
            is SingleFileParseResult.Success -> acc.copy(second = acc.second + res.item)
            is SingleFileParseResult.Failure -> acc.copy(first = acc.first + res)
        } }
        .let {
            if(it.first.isNotEmpty()) it.first.left()
            else it.second.right()
        }
