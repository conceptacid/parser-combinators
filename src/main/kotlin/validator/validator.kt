package idl.validator

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import idl.FileItem
import idl.InvalidFileItem
import idl.SingleFileValidationResult

// TODO: validate
// 1. uniqueness of ids and tags
// 2. if all types exist
// 3. uniqueness of topics
fun validate(fileItem: FileItem): SingleFileValidationResult = SingleFileValidationResult.ValidFile(fileItem)

fun validateAll(fileItems: List<SingleFileValidationResult>): Either<List<InvalidFileItem>, List<FileItem>> = fileItems
        .fold(  emptyList<FileItem>() to emptyList<InvalidFileItem>()  ) {acc, item ->
            when(item) {
                is SingleFileValidationResult.ValidFile -> acc.copy(first = acc.first + item.fileItem)
                is SingleFileValidationResult.ErrorFile -> acc.copy(second = acc.second + InvalidFileItem(item.fileItem.name, item.fileItem.path, item.error))
            }
        }
        .let {
            if(it.second.isNotEmpty()) it.second.left()
            else it.first.right()
        }