package parser.idl

import parser.core.Maybe


/**
 *
 * package net.riedel.same.api.nios
 *
 * import net.riedel.rumba.api
 *
 * data Abc {
 *   name: String, tag = 1;
 *   headers: Map<Int32, String>, tag = 2;
 * }
 *
 * choice AbcOptions {
 *     virtual name: String, tag = 1    // common field, must be present in all cases
 *
 *     option Abc {
 *     }, tag = 1;
 *     option Bcd = 2 {
 *     }, tag = 2;
 *     option Def, tag = 3;           // object
 * }
 *
 * data AbcCommand {
 *    address: AbcOption.Abc, tag = 1;
 *    phone: Abc, tag = 2;
 *    repeated name: String tag = 3;
 * }
 *
 * topic "topic-name", request AbcCommand, response AbcResponse         // the response is optional
 *
 */


data class TypeIdentifier(val id: String)

data class EnumerationItem(val id: String, val tag: Int)
data class Enumeration(val id: TypeIdentifier, val options: List<EnumerationItem>)

data class Identifier(val id: String)

sealed class FieldType {
    object Int8 : FieldType()
    object Int16 : FieldType()
    object Int32 : FieldType()
    object String : FieldType()
    object Boolean : FieldType()
    object Float : FieldType()
    object GUID : FieldType()
    object DateTime : FieldType()
    data class Map(val keyType: FieldType, val valType: FieldType) : FieldType()
    data class List(val valType: FieldType) : FieldType()
    data class CustomType(val id: TypeIdentifier) : FieldType()
}

data class Field(
        val id: Identifier,
        val fieldType: FieldType,
        val tag: Int
)

data class Data(
        val id: TypeIdentifier,
        val fields: List<Field>
)

data class Option(
        val id: TypeIdentifier,
        val body: Maybe<List<Field>>,
        val tag: Int
)

data class Choice(
        val id: TypeIdentifier,
        val options: List<Option>
)

data class Topic(
        val text: String,
        val requestType: TypeIdentifier,
        val responseType: Maybe<TypeIdentifier>
)

data class Package(
        val text: String
)

data class Import(
        val text: String
)

sealed class Construct {
    data class Enumeration(val enumeration: parser.idl.Enumeration): Construct()
    data class DataObject(val data: Data): Construct()
    data class ChoiceObject(val choice: Choice): Construct()
    data class TopicObject(val topic: Topic): Construct()
}

data class File(
        val packageIdentifier: Package,
        val imports: List<Import>,
        val constructs: List<Construct>
)