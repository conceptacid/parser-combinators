package idl.generator

import idl.indentLines
import parser.core.Just
import parser.core.None
import parser.idl.*

fun generateTypescriptFile(file: File): String {
    val constructs = file.objects.map {
        when (it) {
            is Construct.DataObject -> generateClass(it.data)
            is Construct.ChoiceObject -> generateUnion(it.choice, file.packageIdentifier)
            is Construct.TopicObject -> generateTopics(it.topic)
            is Construct.Enumeration -> generateEnumeration(it.enumeration)
        }
    }.joinToString("\n\n")
    return constructs
}

fun generateTopics(topic: Topic): String {
    return "// TODO: generate topic or a service\n//const topic = \"${topic.text}\""
}

fun generateUnion(choice: Choice, namespace: Package): String {
    val classes = choice.options.map { generateCaseClass(it, choice, namespace) }.joinToString("\n\n")
    val unionText = "export type ${choice.id.id} = " + choice.options.map { it.id.id }.joinToString(" | ") + ";"
    return "$classes\n\n$unionText\n"
}

fun generateCaseClass(data: Option, choice: Choice, namespace: Package): String {
    val className = data.id.id
    val body = data.body
    val typeText = "    public readonly type = \"${namespace.text}.${choice.id.id}.$className\";"

    return "export class $className { " + when (body) {
        is Just -> {
            val classDefinition = Data(data.id, body.get())
            val constructorText = generateConstructor(classDefinition).lines().indentLines(1)
            val fromJsonText = generateFromJson(classDefinition).lines().indentLines(1)
            "\n$typeText\n$constructorText\n\n$fromJsonText\n"
        }
        None -> "\n$typeText\n"
    } + "}"
}

fun generateClass(classDefinition: Data): String {
    val className = classDefinition.id.id
    val constructorText = generateConstructor(classDefinition).lines().indentLines(1)
    val fromJsonText = generateFromJson(classDefinition).lines().indentLines(1)

    return "export class ${className} {\n$constructorText\n$fromJsonText\n}"
}

fun generateConstructor(data: Data): String {
    val propertyDeclaration = data.fields.map { generateClassField(it) }
    return """
    constructor(
${propertyDeclaration.indentLines(3, ",")}
        ) {}    
    """.trimIndent()
}

fun generateFromJson(data: Data): String {
    val className = data.id.id
    val propertyValidation = data.fields.map { "json.hasOwnProperty('${it.id.id}')" }
    val propertyInitialization = data.fields.map {
        if (it.fieldType is FieldType.CustomType) {
            "${it.fieldType.id.id}.fromJson(json.${it.id.id})"
        } else {
            "json.${it.id.id}"
        }
    }

    return """
public static createFromJson(json: any): $className {
    if(!(${propertyValidation.indentLines(2, " &&").trim()})) {
        throw new Error('wrong json format for $className' + json);
    }
    
    const obj = new $className(
        ${propertyInitialization.indentLines(2, ",").trim()}
    );
    
    return obj;
}"""
}


fun generateFieldType(fieldType: FieldType): String = when (fieldType) {
    FieldType.Int8 -> "number"
    FieldType.Int16 -> "number"
    FieldType.Int32 -> "number"
    FieldType.String -> "string"
    FieldType.Boolean -> "boolean"
    FieldType.Float -> "number"
    FieldType.GUID -> "string"
    FieldType.DateTime -> "Date"
    is FieldType.Map -> "Map<${generateFieldType(fieldType.keyType)}, ${generateFieldType(fieldType.valType)}>"
    is FieldType.List -> "Array<${generateFieldType(fieldType.valType)}>"
    is FieldType.CustomType -> fieldType.id.id
}

fun generateClassField(field: Field): String = "public ${field.id.id}: ${generateFieldType(field.fieldType)}"


fun generateEnumeration(enumeration: Enumeration): String {
    val members = enumeration.options.map {it.id + " = \"${it.id}\""}
    return "enum ${enumeration.id.id} {\n" + members.indentLines(1,",") + "\n}\n"
}