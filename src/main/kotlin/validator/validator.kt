package idl.validator

import idl.FileItem
import idl.InvalidFileItem
import idl.SingleFileValidationResult
import idl.ValidationResult

// TODO: validate
// 1. uniqueness of ids and tags
// 2. if all types exist
// 3. uniqueness of topics
fun validate(fileItem: FileItem): SingleFileValidationResult = SingleFileValidationResult.ValidFile(fileItem)

fun validateAll(fileItems: List<SingleFileValidationResult>): ValidationResult = fileItems
        .fold(  emptyList<FileItem>() to emptyList<InvalidFileItem>()  ) {acc, item ->
            when(item) {
                is SingleFileValidationResult.ValidFile -> acc.copy(first = acc.first + item.fileItem)
                is SingleFileValidationResult.ErrorFile -> acc.copy(second = acc.second + InvalidFileItem(item.fileItem.name, item.fileItem.path, item.error))
            }
        }
        .let {
            if(it.second.isNotEmpty()) ValidationResult.Failure(it.second)
            else ValidationResult.Success(it.first)
        }