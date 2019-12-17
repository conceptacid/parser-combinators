package idl.writer

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.io.File
import java.io.FileWriter

sealed class WriterError {
    data class InvalidOutputDir(val outputDir: File): WriterError()
    data class FileOperationError(val relativePath: String, val reason: Throwable): WriterError()
}

fun writeFile(outputDir: File, outputFileRelativePath: String, contents: String): Either<WriterError, Unit> {
    return if(outputDir.isDirectory && outputDir.exists()) {
        try {
            val file = File(outputDir.absolutePath + "/" + outputFileRelativePath)
            file.parentFile.mkdirs()
            val writer = FileWriter(file)
            try {
                writer.write(contents)
            }
            finally {
                writer.close()
            }
            Unit.right()
        }
        catch(t: Throwable) {
            WriterError.FileOperationError(outputFileRelativePath, t).left()
        }
    }
    else WriterError.InvalidOutputDir(outputDir).left()
}