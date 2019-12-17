package idl

import parser.idl.File

sealed class SingleFileParseResult {
    data class Success(val item: FileItem) : SingleFileParseResult()
    data class Failure(val name: String, val path: String, val line: Int, val col: Int, val message: String) : SingleFileParseResult()
}

data class FileItem(val name: String, val path: String, val file: File)
data class InvalidFileItem(val name: String, val path: String, val error: String)

sealed class SingleFileValidationResult {
    data class ValidFile(val fileItem: FileItem): SingleFileValidationResult()
    data class ErrorFile(val fileItem: FileItem, val error: String): SingleFileValidationResult()
}


fun List<String>.indentLines(indent: Int, separator: String = ""): String = map { "    ".repeat(indent) + it}.joinToString("$separator\n")