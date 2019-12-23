package generator

import idl.indentLines
import parser.core.Just
import parser.core.None
import parser.idl.*

fun generateKotlinFile(file: File): String {
    val packageText = "package " + file.packageIdentifier.text


    val imports = (file.imports +
            listOf(
                    Import("kotlinx.serialization.*"),
                    Import("java.util.UUID")
            ))
            .sortedBy { it.text }
            .map {
                generateImportText(it)
            }
            .joinToString("\n")

    val constructs = file.constructs.map {
        when (it) {
            is Construct.DataObject -> generateDataClass(it.data)
            is Construct.ChoiceObject -> generateSealedClass(it.choice)
            is Construct.TopicObject -> generateTopics(it.topic)
            is Construct.Enumeration -> generateEnumeration(it.enumeration)
        }
    }.joinToString("\n\n")


    return """
$packageText

$imports

$constructs
""".trimIndent()
}

fun generateEnumeration(enumeration: Enumeration): String {
    val members = enumeration.options.map {it.id + " = ${it.tag}"}
    return "enum class ${enumeration.id.id} {\n" + members.indentLines(1,",") + "\n}\n"
}

fun generateTopics(topic: Topic): String {
    return "const topic = \"${topic.text}\""
}

fun generateSealedClass(choice: Choice): String {
    val cases = choice.options
            .map { generateCaseClass(it, choice) }
            .map {"$it"}
            .joinToString("\n\n")
    return """
@Serializable
sealed class ${choice.id.id} {
$cases
}""".trimIndent()
}

fun generateCaseClass(data: Option, choice: Choice): String {
    val body = data.body

    val header = "${data.id.id}: ${choice.id.id}()"

    val lines = when (body) {
        is Just -> listOf("data class $header {") + body.get().map { generateClassField(it) }.indentLines(1) + listOf("}")
        None -> listOf("object $header")
    }
    val text = (listOf("@Serializable") + lines)
            .map { "    ${it}" }
            .joinToString("\n")

    return text
}

fun generateDataClass(data: Data): String {
    val fields = data.fields.map { generateClassField(it) }
    return """
@Serializable
data class ${data.id.id} (
${fields.indentLines(1, ",")}
)""".trimIndent()
}


fun generateImportText(import: Import): String = "import ${import.text}"

fun generateFieldType(fieldType: FieldType): String = when (fieldType) {
    FieldType.Int8 -> "Byte"
    FieldType.Int16 -> "Short"
    FieldType.Int32 -> "Int"
    FieldType.String -> "String"
    FieldType.Boolean -> "Boolean"
    FieldType.Float -> "Float"
    FieldType.GUID -> "UUID"
    FieldType.DateTime -> "DateTime"
    is FieldType.Map -> "Map<${generateFieldType(fieldType.keyType)}, ${generateFieldType(fieldType.valType)}>"
    is FieldType.List -> "List<${generateFieldType(fieldType.valType)}>"
    is FieldType.CustomType -> fieldType.id.id
}

fun generateClassField(field: Field): String = "val ${field.id.id}: ${generateFieldType(field.fieldType)}"


