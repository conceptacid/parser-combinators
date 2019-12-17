package idl.validator

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import idl.FileItem
import idl.FileValidationError
import idl.InvalidFileItem
import parser.idl.Construct
import parser.idl.findDuplicates

// TODO: validate
// 1. uniqueness of ids and tags
// 2. if all types exist
// 3. uniqueness of topics
fun validate(fileItem: FileItem): Either<FileValidationError, FileItem> {


    // TODO: check types across all files unique by {ID, package}
    val duplicateTypes = fileItem.file.objects
            .mapNotNull {
                when (it) {
                    is Construct.DataObject -> it.data.id
                    is Construct.ChoiceObject -> it.choice.id
                    is Construct.TopicObject -> null
                    is Construct.Enumeration -> it.enumeration.id
                }
            }
            .findDuplicates()

    // TODO: use validatedNel to communicate all errors
    return if (duplicateTypes.isNotEmpty()) FileValidationError.DuplicateTypeIdentifiers(fileItem, duplicateTypes.keys.toList()).left()
    else fileItem.right()

}


fun validateAll(fileItems: List<Either<FileValidationError, FileItem>>): Either<List<InvalidFileItem>, List<FileItem>> = fileItems
        .fold(emptyList<FileItem>() to emptyList<InvalidFileItem>()) { acc, item ->
            when (item) {
                is Either.Left -> {
                    val name = item.a.fileItem.name
                    val path = item.a.fileItem.path
                    acc.copy(second = acc.second + InvalidFileItem(name, path, item.a))
                }
                is Either.Right -> acc.copy(first = acc.first + item.b)
            }
        }
        .let {
            if (it.second.isNotEmpty()) it.second.left()
            else it.first.right()
        }