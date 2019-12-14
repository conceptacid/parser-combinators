package idl

import parser.idl.File

sealed class SingleFileParseResult {
    data class Success(val item: FileItem) : SingleFileParseResult()
    data class Failure(val name: String, val path: String, val line: Int, val col: Int, val message: String) : SingleFileParseResult()
}

sealed class ParseResult {
    data class Success(val files: List<FileItem>) : ParseResult()
    data class ParsingError(val files: List<SingleFileParseResult.Failure>) : ParseResult()
}

data class FileItem(val name: String, val path: String, val file: File)
data class InvalidFileItem(val name: String, val path: String, val error: String)

sealed class SingleFileValidationResult {
    data class ValidFile(val fileItem: FileItem): SingleFileValidationResult()
    data class ErrorFile(val fileItem: FileItem, val error: String): SingleFileValidationResult()
}

sealed class ValidationResult {
    data class Success(val files: List<FileItem>): ValidationResult()
    data class Failure(val files: List<InvalidFileItem>): ValidationResult()   // TODO()
}