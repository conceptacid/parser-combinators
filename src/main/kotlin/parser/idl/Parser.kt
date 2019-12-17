package parser.idl

import parser.core.*
import quotedString

fun pComment(): Parser<String> = pString("//") andr pAllUntilNewline() andl pChar('\n')

private fun spaces() = zeroOrMore(pWhitespace())
fun delimiters() = (spaces().ignore() or pComment().ignore())
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
    return firstChar and zeroOrMore(otherChar) label "type-identifier" map {
        TypeIdentifier(it.joinToString(""))
    }
}

fun pTag(): Parser<Int> {
    return pDelimited(delimiters(), pString("tag"), pChar('='), pInt()) map { it.c }
}

fun pMapKeyType(): Parser<FieldType> {
    return (pString("Int8") map { FieldType.Int8 as FieldType }) or
            (pString("Int16") map { FieldType.Int16 as FieldType }) or
            (pString("Int32") map { FieldType.Int32 as FieldType }) or
            (pString("String") map { FieldType.String as FieldType }) or
            (pString("Boolean") map { FieldType.Boolean as FieldType }) or
            (pString("Float") map { FieldType.Float as FieldType }) or
            (pString("GUID") map { FieldType.GUID as FieldType }) or
            (pString("DateTime") map { FieldType.DateTime as FieldType })
}

fun pKeyValuePair(level: Int): Parser<Pair<FieldType, FieldType>> {
    return pDelimited(delimiters(), pMapKeyType(), pChar(','), pFieldType(level + 1)) map { it.a to it.c }
}

fun pMap(level: Int): Parser<FieldType> {
    return if (level < 2) {
        return pDelimited(delimiters(), pString("Map<"), pKeyValuePair(level), pString(">")) map { FieldType.Map(it.b.first, it.b.second) }
    } else {
        fail("too many nested type specifiers")
    }
}


fun pList(level: Int): Parser<FieldType> {
    return if (level < 2) {
        pDelimited(delimiters(), pString("List<"), pFieldType(level + 1), pString(">")) map { FieldType.List(it.b) }
    } else {
        fail("too many nested type specifiers")
    }
}


fun pFieldType(level: Int): Parser<FieldType> {
    return (pString("Int8") map { FieldType.Int8 as FieldType }) or
            (pString("Int16") map { FieldType.Int16 as FieldType }) or
            (pString("Int32") map { FieldType.Int32 as FieldType }) or
            (pString("String") map { FieldType.String as FieldType }) or
            (pString("Boolean") map { FieldType.Boolean as FieldType }) or
            (pString("Float") map { FieldType.Float as FieldType }) or
            (pString("GUID") map { FieldType.GUID as FieldType }) or
            (pString("DateTime") map { FieldType.DateTime as FieldType }) or
            pList(level) or
            pMap(level) or
            (pTypeIdentifier() map { FieldType.CustomType(it) as FieldType })
}

fun pField(): Parser<Field> {
    return pDelimited(delimiters(), pIdentifier(), pChar(':'), pFieldType(0),
            pChar(','), pTag(),
            pChar(';')) map { (fieldId, _, fieldType, _, tag, _) ->
        Field(fieldId, fieldType, tag)
    } label "field-definition"
}

fun pListOfFields(): Parser<List<Field>> {
    return pDelimited(delimiters(), pChar('{'), zeroOrMore(pField() andl delimiters()), pChar('}')) map { (_, fields, _) ->
        val res = fields.map { it as Field }

//        // TODO: move it to validation step
//        val duplicateFieldIds = res.map { it.id }.findDuplicates()
//        if (duplicateFieldIds.isNotEmpty()) throw RuntimeException("Duplicated field IDs: $duplicateFieldIds")
//        val duplicatedTags = res.map { it.tag }.findDuplicates()
//        if (duplicatedTags.isNotEmpty()) throw RuntimeException("Duplicated tags: $duplicatedTags")

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
//        // TODO: move it validation
//        val duplicateIDs = options.map { it.id }.findDuplicates()
//        if (duplicateIDs.isNotEmpty()) throw RuntimeException("Duplicated option types: $duplicateIDs")
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

fun pEnumeration(): Parser<Enumeration> {
    return optional(delimiters()) andr pDelimited(delimiters(), pString("enum"), pTypeIdentifier(),
            pChar('{'), oneOrMore(pEnumerationItem()), pChar('}')) map { (_, id, _, items, _) ->
        Enumeration(id, items.map {it as EnumerationItem})
    }
}

fun pEnumerationItem() : Parser<EnumerationItem> {
    val alpha = ('A'..'Z').toList() + ('a'..'z').toList() + listOf('_')
    val firstChar = pAnyOf(alpha)
    val otherChar = pAnyOf(alpha + ('0'..'9').toList())
    val terminalChar = optional(pChar(';') or pChar(','))
    val enumItemID = firstChar and zeroOrMore(otherChar) label "enum-item" map { it.joinToString("") }
    return optional(delimiters()) andr pDelimited(delimiters(), enumItemID, pChar('='), pInt(), terminalChar) map { (id, _, tag, _) ->
        EnumerationItem(id, tag)
    }
}

fun pPackage(): Parser<Package> {
    val packagePath = separatedBy(pIdentifier(), pChar('.')) map {
        it.map { it as Identifier }.map { it.id }.joinToString(".")
    }
    return optional(delimiters()) andr pString("package") andr delimiters() andr packagePath andl optional(delimiters()) map { Package(it) }
}

fun pImport(): Parser<Import> {
    val importPath = separatedBy(pIdentifier(), pChar('.')) map {
        it.map { it as Identifier }.map { it.id }.joinToString(".")
    }
    return pDelimited(delimiters(), pString("import"), importPath) map { Import(it.b) }
}

fun pImports(): Parser<List<Import>> {
    return zeroOrMore(optional(delimiters()) andr pImport() andl optional(delimiters())) map { it.map { it as Import } }
}

fun pConstruct(): Parser<Construct> {
    return delimiters() andr
            (pData() andl optional(delimiters()) label "data-construct" map { Construct.DataObject(it) as Construct }) or
            (pEnumeration() andl optional(delimiters()) label "enumeration-construct" map { Construct.Enumeration(it) as Construct }) or
            (pChoice() andl optional(delimiters()) label "choice-construct" map { Construct.ChoiceObject(it) as Construct }) or
            (pTopic() andl optional(delimiters()) label "topic-construct" map { Construct.TopicObject(it) as Construct }) andl optional(delimiters())
}

fun pConstructs(): Parser<List<Construct>> {
    return zeroOrMore(optional(delimiters()) andr pConstruct() andl optional(delimiters())) map { it.map { it as Construct } }
}

fun pFile(): Parser<File> {
    return pDelimited(delimiters(), pPackage(), pImports(), pConstructs()) map { (packageIdentity, imports, objects) ->
        File(packageIdentity, imports, objects)
    }
}