@file:Suppress("UNUSED_VARIABLE")

package com.assaabloyglobalsolutions.jacksonbeanvalidation

import org.junit.jupiter.api.Test
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank


// stuff in general
// - must allow full deserialization via constructor (e.g. typical data class)
class KnownLimitationsTest {

    @Test
    fun `constraint violation path missing index for lists of complex objects`() {

        val ideallyExpected = listOf(
            "list[0].bar: must not be blank",
            "list[1].bar: must not be blank"
        )

        // data class ClassWithList(@field:Valid val list: List<Foo> = listOf())
        assertViolations<ClassWithList>(
            mapper,
            """
                {
                  "list" : [
                   { "bar": "" },
                   {}
                  ]
                }
            """.trimIndent(),
            "list[0].bar: must not be blank"
        )
    }

    @Test
    fun `only first failing map value is reported`() {

        val ideallyExpected = listOf(
            "map[a].otherNotBlank: must not be blank",
            "map[b].otherNotBlank: must match \"[^ ]+\""
        )

        // data class ClassWithMapOfNodes(@field:Valid val map: Map<String, Node> = mapOf())
        assertViolations<ClassWithMapOfNodes>(
            mapper,
            """
                {
                  "map" : { 
                    "a": { "otherNotBlank" : null, "someNullable" : "world" },
                    "b": { "otherNotBlank" : "hello", "someNullable" : " " }
                  }
                }
            """,
            "map.otherNotBlank: must not be blank"
        )
    }

    @Test
    fun `invalid null values in collections are not reported if existing entries emit other constraint violations`() {
        val ideallyExpected = listOf(
            "list[1].bar: must not be blank",
            "list[2]: must not be null",
        )

        // data class ClassWithList(@field:Valid val list: List<Foo> = listOf())
        assertViolations<ClassWithList>(
            mapper,
            """
                {
                  "list" : [
                   { "bar": "fine" },
                   { "bar": "" },
                   null
                  ]
                }
            """,
            "list[1].bar: must not be blank",
        )
    }
}


data class ClassWithList(
    @field:Valid val list: List<Foo> = listOf()
)

data class Foo(
    @field:NotBlank val bar: String
)

data class SomeValueHolder(@field:Valid val v: SomeValue)

@JvmInline
value class SomeValue(@field:NotBlank val s: String)