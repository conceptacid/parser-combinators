package idl

import parser.idl.File
import parser.idl.TypeIdentifier

sealed class SingleFileParseResult {
    data class Success(val item: FileItem) : SingleFileParseResult()
    data class Failure(val name: String, val path: String, val line: Int, val col: Int, val message: String) : SingleFileParseResult()
}

data class FileItem(val name: String, val path: String, val file: File)
data class InvalidFileItem(val name: String, val path: String, val error: FileValidationError)

sealed class FileValidationError {
    abstract val fileItem: FileItem
    data class DuplicateTypeIdentifiers(override val fileItem: FileItem, val typeIDs: List<TypeIdentifier>): FileValidationError()
    data class ErrorFile(override val fileItem: FileItem, val error: String): FileValidationError()
}


fun List<String>.indentLines(indent: Int, separator: String = ""): String = map { "    ".repeat(indent) + it}.joinToString("$separator\n")