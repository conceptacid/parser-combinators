package scanner

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import idl.FileItem
import idl.SingleFileParseResult
import parser.core.*
import parser.idl.pFile
import java.io.File

sealed class ScannerError {
    data class InvalidSourceDirectory(val path: String) : ScannerError()
    data class ParsingFinishedWithErrors(val errors: List<SingleFileParseResult.Failure>): ScannerError()
}

suspend fun findAllFilesRecursive(sourceDirectory: File, extension: String): Either<ScannerError.InvalidSourceDirectory, Sequence<File>> {
    return if (sourceDirectory.exists() && sourceDirectory.isDirectory) sourceDirectory.walkTopDown().filter { it.isFile && it.extension == extension }.right()
    else ScannerError.InvalidSourceDirectory(sourceDirectory.path).left()
}

suspend fun parseIdlFilesIO(sourceDirectory: File): Either<ScannerError, List<FileItem>> =
        findAllFilesRecursive(sourceDirectory, "idl")
        .flatMap { it.toList()
                .map {
                    try {
                        val text = it.readText()
                        when (val result = pFile().run(State(text))) {
                            is Success -> {
                                if (!result.state.eof()) {
                                    val sample = result.state.input.substring(result.state.pos, result.state.input.length)
                                    SingleFileParseResult.Failure(it.name, it.path, result.state.line, result.state.col, "Could not parse file after '$sample'")
                                } else {
                                    val sourceDirectoryPath = sourceDirectory.absolutePath
                                    val fileAbsolutePath = it.absolutePath
                                    if(!fileAbsolutePath.startsWith(sourceDirectoryPath)) throw AssertionError("file path is invalid '$fileAbsolutePath") // must not happen
                                    val fileRelativePath = fileAbsolutePath.substring(sourceDirectoryPath.length).substringAfter("/")
                                    SingleFileParseResult.Success(FileItem(it.name, fileRelativePath, result.value))
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
                .fold(emptyList<SingleFileParseResult.Failure>() to emptyList<FileItem>()) { acc, res ->
                    when (res) {
                        is SingleFileParseResult.Success -> acc.copy(second = acc.second + res.item)
                        is SingleFileParseResult.Failure -> acc.copy(first = acc.first + res)
                    }
                }
                .let {
                    if (it.first.isNotEmpty()) ScannerError.ParsingFinishedWithErrors(it.first).left()
                    else it.second.right()
                }
        }

