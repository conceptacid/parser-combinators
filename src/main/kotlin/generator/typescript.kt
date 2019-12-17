package idl.generator

import idl.indentLines
import parser.idl.*

fun generateTypescriptFile(file: File): String {
    val constructs = file.objects.map {
        when (it) {
            is Construct.DataObject -> generateClass(it.data)
            is Construct.ChoiceObject -> generateUnion(it.choice)
            is Construct.TopicObject -> generateTopics(it.topic)
        }
    }.joinToString("\n\n")
    return constructs
}

fun generateTopics(topic: Topic): String {
    return "// TODO: generate topic or a service\n//const topic = \"${topic.text}\""
}

fun generateUnion(choice: Choice): String {
    return ""

//
//    val cases = choice.options
//            .map { generateCaseClass(it, choice) }
//            .map {"$it"}
//            .joinToString("\n\n")
//    return """
//@Serializable
//sealed class ${choice.id.id} {
//$cases
//}""".trimIndent()
}




fun generateClass(data: Data): String {
    val className = data.id.id
    val constructorText = generateConstructor(data).lines().indentLines(1)
    val fromJsonText = generateFromJson(data).lines().indentLines(1)

    return """
export class ${className} {
$constructorText
        
$fromJsonText
}""".trimIndent()
}

fun generateConstructor(data: Data): String {
    val propertyDeclaration = data.fields.map { generateClassField(it) }
    return """
    constructor(
${propertyDeclaration.indentLines(3, ",")}
        ) {
        }    
    """.trimIndent()
}

fun generateFromJson(data: Data): String {
    val className = data.id.id
    val propertyValidation = data.fields.map { "json.hasOwnProperty('${it.id.id}')" }
    val propertyInitialization = data.fields.map {
        if(it.fieldType is FieldType.CustomType) {
            "${it.fieldType.id.id}.fromJson(json.${it.id.id})"
        } else {
            "json.${it.id.id}"
        }
    }

    return """
public static createFromJson(json: any): $className {
    if(!(${propertyValidation.indentLines(2, " &&").trim()}) {
        throw new Error('wrong json format for $className' + json);
    }
    
    const obj = new $className(
        ${propertyInitialization.indentLines(2, ",").trim()}
    );
    
    return obj;
}
"""
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


