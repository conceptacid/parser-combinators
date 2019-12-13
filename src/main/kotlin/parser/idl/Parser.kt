package parser.idl

import parser.core.*
import quotedString

private fun spaces() = zeroOrMore(pWhitespace())
fun delimiters() = spaces().ignore()
fun <T> List<T>.findDuplicates() = groupBy { it }.mapValues { it.value.size }.filter { it.value > 1 }

fun pIdentifier(): Parser<Identifier> {
    val alpha = ('A'..'Z').toList() + ('a'..'z').toList() + listOf('_')
    val firstChar = pAnyOf(alpha)
    val otherChar = pAnyOf(alpha + ('0'..'9').toList())
    return firstChar and zeroOrMore(otherChar) label "identifier" map { Identifier(it.joinToString("")) }
}

fun pTypeIdentifier(): Parser<TypeIdentifier> {
    val firstChar = pAnyOf(('A'..'Z').toList())
    val otherChar = pAnyOf(('0'..'9').toList() + ('A'..'Z').toList() + ('a'..'z').toList() + listOf('_'))
    return firstChar and zeroOrMore(otherChar) label "type-identifier" map { TypeIdentifier(it.joinToString("")) }
}

fun pTag(): Parser<Int> {
    return pDelimited(delimiters(), pString("tag"), pChar('='), pInt()) map { it.c }
}

fun pKeyValuePair(): Parser<Pair<FieldType, FieldType>> {
    return pDelimited(delimiters(), pFieldType(), pChar(','), pFieldType()) map { it.a to it.c }
}
/*
fun pMap() : Parser<FieldType> {
    return pDelimited(delimiters(), pString("Map<"), pKeyValuePair(), pString(">")) map { FieldType.Map(it.b.first, it.b.second)}
}

fun pList() : Parser<FieldType> {
    return pDelimited(delimiters(), pString("List<"), pFieldType(), pString(">")) map { FieldType.List(it.b)}
}
*/

fun pFieldType(): Parser<FieldType> {
    return pString("Int8") map { FieldType.Int8 as FieldType } or
            (pString("Int16") map { FieldType.Int16 as FieldType }) or
            (pString("Int32") map { FieldType.Int32 as FieldType }) or
            (pString("String") map { FieldType.String as FieldType }) or
            (pString("Boolean") map { FieldType.Boolean as FieldType }) or
            (pString("Float") map { FieldType.Float as FieldType }) or
            (pTypeIdentifier() map { FieldType.CustomType(it) as FieldType })
    //pMap() or
    //pList()

    // TODO: add GUID, DateTime
}

fun pField(): Parser<Field> {
    return pDelimited(delimiters(), pIdentifier(), pChar(':'), pFieldType(),
            pChar(','), pTag(),
            pChar(';')) map { (fieldId, _, fieldType, _, tag, _) ->
        Field(fieldId, fieldType, tag)
    } label "field-definition"
}

fun pListOfFields(): Parser<List<Field>> {
    return pDelimited(delimiters(), pChar('{'), zeroOrMore(pField() andl delimiters()), pChar('}')) map { (_, fields, _) ->
        val res = fields.map { it as Field }

        // TODO: move it to validation step
        val duplicateFieldIds = res.map { it.id }.findDuplicates()
        if (duplicateFieldIds.isNotEmpty()) throw RuntimeException("Duplicated field IDs: $duplicateFieldIds")
        val duplicatedTags = res.map { it.tag }.findDuplicates()
        if (duplicatedTags.isNotEmpty()) throw RuntimeException("Duplicated tags: $duplicatedTags")


        res
    }
}

fun pData(): Parser<Data> {
    val header = pDelimited(delimiters(), pString("data"), pTypeIdentifier())
    val body = pListOfFields()
    return pDelimited(delimiters(), header, body) label "data-definition" map { (header, body) ->
        val (_, id) = header
        Data(id, body)
    }
}

fun pOption(): Parser<Option> {
    return pDelimited(delimiters(), pString("option"), pTypeIdentifier(),
            optional(pListOfFields()), pChar(','), pTag(), pChar(';')) map { (_, typeIdentifier, body, _, tag, _) ->
        Option(typeIdentifier, body, tag)
    }
}

fun pChoiceBody(): Parser<List<Option>> {
    return pDelimited(delimiters(), pChar('{'), zeroOrMore(pOption() andl delimiters()), pChar('}')) map { (_, options, _) ->
        options.map { it as Option }
    }
}

fun pChoice(): Parser<Choice> {
    val identifier = pDelimited(delimiters(), pString("choice"), pTypeIdentifier()) map { it.b }
    val body = pChoiceBody()
    return pDelimited(delimiters(), identifier, body) map { (identifier, options) ->
        // TODO: move it validation
        val duplicateIDs = options.map { it.id }.findDuplicates()
        if (duplicateIDs.isNotEmpty()) throw RuntimeException("Duplicated option types: $duplicateIDs")
        Choice(identifier, options)
    }
}


fun pTopic(): Parser<Topic> {
    val requestIdParser = pDelimited(delimiters(), pChar(','), pString("request"), pChar('='), pTypeIdentifier()) map { it.d }
    val responseParser = pDelimited(delimiters(), pChar(','), pString("response"), pChar('='), pTypeIdentifier()) map { it.d }

    return pDelimited(delimiters(), pString("topic"), quotedString(),
            requestIdParser, optional(responseParser)) map { (_, text, requestType, responseType) ->
        Topic(text, requestType, responseType)
    }
}

fun separatedBy(parser: Parser<out Any>, sep: Parser<out Any>): Parser<List<Any>> =
        parser and zeroOrMore(sep andr parser)

fun pPackage(): Parser<Package> {
    val packagePath = separatedBy(pIdentifier(), pChar('.')) map { Package(it.map { it as Identifier }) }
    return optional(delimiters()) andr pString("package") andr delimiters() andr packagePath andl optional(delimiters())// andl pChar(';')
}

//fun pPackageId(): Parser<String> {
//    val alpha = ('A'..'Z').toList() + ('a'..'z').toList() + listOf('_')
//    val firstChar = pAnyOf(alpha)
//    val otherChar = pAnyOf(alpha + ('0'..'9').toList() + listOf('.'))
//    return firstChar and zeroOrMore(otherChar) label "packate-identifier" map { it.joinToString("") }
//
//}

fun pImport(): Parser<Import> {
    val importPath = separatedBy(pIdentifier(), pChar('.')) map {
        it.map { it as Identifier }.map { it.id }.joinToString(".")
    }
    return pDelimited(delimiters(), pString("import"), importPath) map { Import(it.b) }
}

fun pImports(): Parser<List<Import>> {
    return zeroOrMore(optional(delimiters()) andr pImport() andl optional(delimiters())) map { it.map { it as Import } }
}

fun pIdlObject(): Parser<IdlObject> {
    return (pData() andl delimiters() map { IdlObject.DataObject(it) as IdlObject }) or
            (pChoice() andl delimiters() map { IdlObject.ChoiceObject(it) as IdlObject }) or
            (pTopic() andl delimiters() map { IdlObject.TopicObject(it) as IdlObject })
}

fun pIdlObjects(): Parser<List<IdlObject>> {
    return zeroOrMore(optional(delimiters()) andr pIdlObject() andl optional(delimiters())) map { it.map { it as IdlObject } }
}

fun pIdlFile(): Parser<IdlFile> {
    return pDelimited(delimiters(), pPackage(), pImports(), pIdlObjects()) map { (packageIdentity, importsAsAny, objectsAsAny) ->
        val imports = importsAsAny.map { it as Import }
        val objects = objectsAsAny.map { it as IdlObject }

        // TODO: move it validation
        val duplicateTypes = objects
                .mapNotNull {
                    when (it) {
                        is IdlObject.DataObject -> it.data.id
                        is IdlObject.ChoiceObject -> it.choice.id
                        is IdlObject.TopicObject -> null
                    }
                }
                .findDuplicates()
        if (duplicateTypes.isNotEmpty()) throw RuntimeException("Duplicate type definitions: ${duplicateTypes}")

        IdlFile(packageIdentity, imports.map { it as Import }, objects)
    }
}