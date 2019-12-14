package generator

import parser.idl.*

fun generateKotlinFile(file: File): String {
    val packageText = "package " + file.packageIdentifier.path.map {it.id}.joinToString(".")
    val imports = file.imports.map { generateImportText(it.text) }.joinToString("\n")
    val constructs = file.objects.map { when(it) {
        is Construct.DataObject -> generateDataClass(it.data)
        is Construct.ChoiceObject -> generateSealedClass(it.choice)
        is Construct.TopicObject -> generateTopics(it.topic)
    } } .joinToString("\n\n")

    return """
$packageText

$imports

$constructs
""".trimIndent()
}

fun generateTopics(topic: Topic): String {
    return "const topic = \"${topic.text}\""
}

fun generateSealedClass(choice: Choice): String {
    return """
@Serializable
sealed class ${choice.id.id} {
}""".trimIndent()
}

fun generateDataClass(data: Data): String {
    return """
@Serializable
data class ${data.id.id} (
)""".trimIndent()
}

fun generateImportText(text: String): String = "import $text"
