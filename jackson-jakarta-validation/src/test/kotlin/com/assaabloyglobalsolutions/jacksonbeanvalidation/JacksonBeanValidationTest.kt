package com.assaabloyglobalsolutions.jacksonbeanvalidation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

class JacksonBeanValidationTest {

    @Test
    fun `implicit @NotNull on non-nullable types unless other constraints act on null`() {
        assertViolations<NestedFoo>(
            mapper,
            """ { "nested": {} } """,
            "notNullString: must not be null",
            "nested.otherNotBlank: must not be blank",
        )
    }

    @Test
    fun `nested json paths are reconstructed to match the root object`() {
        assertViolations<NestedFoo>(
            mapper,
            """
            {
                "notNullString": "a",
                "nested": {
                    "otherNotBlank": "b",
                    "someNullable": "",
                    "child": { 
                        "otherNotBlank": "c",
                        "child": {  "someNullable": "" }
                    }
                }
            }
            """,
            "nested.someNullable: must match \"[^ ]+\"",
            "nested.child.child.otherNotBlank: must not be blank",
            "nested.child.child.someNullable: must match \"[^ ]+\"",
        )
    }

    @Test
    fun `constructors with default values are not affected by omitted json fields`() {
        assertThat(mapper.treeToValue<DefaultArgs>(mapper.createObjectNode()))
            .isEqualTo(DefaultArgs())
    }

    @Test
    fun `implicit @NotNull on maps disallowing null values`() {
        val invalid = mapper.readTree("""
            {
              "map" : { 
                "a": null,
                "b": { "otherNotBlank" : "hello", "someNullable" : "world" },
                "c": null
              }
            }
        """.trimIndent())

        assertViolations({ mapper.treeToValue<ClassWithMapOfNodes>(invalid) },
            "map[a]: must not be null",
            "map[c]: must not be null"
        )
    }

    @Test
    fun `implicit @NotNull on collections disallowing null values`() {
        val invalid = mapper.readTree("""
            {
              "list" : [ 
                null,
                { "otherNotBlank" : "hello", "someNullable" : "world" },
                null
              ]
            }
        """.trimIndent())

        assertViolations({ mapper.treeToValue<ClassWithListOfNodes>(invalid) },
            "list[0]: must not be null",
            "list[2]: must not be null"
        )
    }

    @Test
    fun `class with array`() {
        val invalid = mapper.readTree("""
            {
              "nodes" : [ 
                  null,
                  { "otherNotBlank" : "hello", "someNullable" : "world" },
                  null
              ],
              "strings" : [ 
                  "",
                  null,
                  "valid"
              ]
            }
        """.trimIndent())

        assertViolations({ mapper.treeToValue<ClassWithArrays>(invalid) },
            "nodes[0]: must not be null",
            "nodes[2]: must not be null",
            "strings[1]: must not be null",
        )
    }


    @Test
    fun `assert json path is correct when interjected with other deserializers`() {
        assertViolations<OuterHolder>(
            mapper,
            """
            {
              "holder": { "node" : { "otherNotBlank" : "", "someNullable" : "world" } }
            }
            """,
            "holder.node.otherNotBlank: must not be blank",
        )
    }

    @Test
    fun `value classes work`() {
        // value classes are a bit tricky...
        //
        // 1) at the java level - where bean validation operates - function signatures typically
        // don't refer to value classes but to the underlying type.
        //
        // 2) kotlin's reflection API require any value class values to be represented by
        // the value class itself. this is in contrast to java's reflection API, which is
        // oblivious to any value classes unless they are part of a collection or map signature.
        //
        // value classes therefore have to be boxed for reflection to work, and before validation
        // in order to not lose any constraint annotations sitting on the value class.
        //
        // ref kotlin reflection: https://github.com/spring-projects/spring-framework/issues/31698

        // works: happy path
        mapper.readValue<SomeValueHolder>("""{ "v": { "s": "hello" } } }""")

        // works: nullability is enforced
        assertViolations<SomeValueHolder>(
            mapper,
            """{ "v": { "s": null } } }""",
            "v: must not be null",
        )
        assertViolations<SomeValueHolder>(
            mapper,
            """{ "v": null } } """,
            "v: must not be null",
        )

        // works: constraint mappings inside value classes
        assertViolations<SomeValueHolder>(
            mapper,
            """{ "v": { "s": "" } } }""",
            "v: must not be blank",
        )
    }

    @Test
    fun `heed @JsonProperty name override`() {
        // happy path
        mapper.readValue<CustomProperties>(
            """{ "custom_foo": "hello", "custom_bar": "world" } }"""
        )

        // null violations
         assertViolations<CustomProperties>(
            mapper,
            """{ "custom_foo": null, "custom_bar": null } }""",
            "custom_foo: must not be null",
            "custom_bar: must not be null",
        )
    }

    @Test
    fun `primitive fields are treated as @NotNull if FAIL_ON_NULL_FOR_PRIMITIVES`() {
        assertViolations<ClassWithInt>(
            mapper,
            """{ "value": null }""",
            "value: must not be null",
        )

        assertViolations<ClassWithInt>(
            mapper,
            """{ }""",
            "value: must not be null",
        )
    }

    @Test
    fun `null in non-nullable collection`() {
        assertViolations<ClassWithStrings>(
            mapper,
            """{ "strings": [ null ] }""",
            "strings[0]: must not be null",
        )
    }

    @Test
    fun `constraint validation of nested containers`() {
        assertViolations<NestedListClass>(
            mapper,
            """ { "strings" : [[ "hi", null ]] } """,
            "strings[0][1]: must not be null",
        )
    }

    @Test @Disabled
    fun `heed json property naming convention override`() {
        TODO()
    }

    // these tests are essentially just here to make sure that no deserialization
    // path has been short-circuited by our deserializer.
    @Nested inner class JsonCreatorTests {

        @Test
        fun `@JsonCreator works on static multi argument functions and constructors`() {
            assertThat(
                mapper.readValue<JcFooBar2>(""" { "foo": "123", "bar": "456" } """)
            ).isEqualTo(
                mapper.readValue<JcFooBar2>(""" { "foo": 123, "bar": 456 } """)
            )
        }

        @Test
        fun `@JsonCreator works on static single argument functions and constructors`() {
            assertThat(
                mapper.treeToValue<JcFooBar1>(TextNode("1"))
            ).isEqualTo(
                mapper.treeToValue<JcFooBar1>(IntNode(1))
            )
        }

        @Test
        fun `happy path validation for class with @JsonCreator`() {
            assertViolations<JcFooBar2>(
                mapper,
                """ { "foo": null, "bar": null } """,
                "foo: must not be blank",
                "bar: must not be null",
            )
        }
    }
}

data class NestedListClass(
    @field:Valid val strings: List<List<String>?>,
)

data class ClassWithInt(
    val value: Int,
    val nullableValue: Int?
)

data class ClassWithStrings(
    val strings: List<String>
)

data class CustomProperties(
    @field:JsonProperty("custom_foo")
    val foo: String,
    @field:JsonProperty("custom_bar")
    val bar: String,
)

data class JcFooBar1 @JsonCreator constructor(
    val hmm: Int
) {
    companion object {
        @JvmStatic @JsonCreator
        fun parse(s: String) = JcFooBar1(s.toInt())
    }
}

data class JcFooBar2 @JsonCreator constructor(
    @field:NotBlank
    val foo: String,
    val bar: String
) {
    companion object {
        @JvmStatic @JsonCreator
        fun parse(foo: Int, bar: Int) = JcFooBar2(foo.toString(), bar.toString())
    }
}

data class NestedFoo(
    val notNullString: String,
    @field:Valid
    val nested: Node?
)

data class Node(
    @field:NotBlank
    val otherNotBlank: String,
    @field:Pattern(regexp = "[^ ]+")
    val someNullable: String? = null,
    val child: Node? = null
) {
    constructor(aNotNull: String, child: Node)
        : this(aNotNull, null, child)
}

data class ClassWithListOfNodes(
    @field:Valid val list: List<Node> = listOf()
)

data class ClassWithArrays(
    @field:Valid val nodes: Array<Node>,
    @field:Valid val strings: Array<String>,
)

data class ClassWithMapOfNodes(
    @field:Valid val map: Map<String, Node> = mapOf()
)

data class DefaultArgs(
    val foo: String = "hello"
)

data class OuterHolder(
    @field:Valid val holder: NodeHolder
)

// not using constructor deserialization
class NodeHolder {
    @field:NotNull var node: Node? = null
}
