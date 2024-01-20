package com.assaabloyglobalsolutions.jacksonbeanvalidation

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.module.kotlin.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.fail
import jakarta.validation.Validation

val mapper = jacksonObjectMapper()
    .registerModule(kotlinBeanValidationModule(Validation.buildDefaultValidatorFactory().validator))
    .registerModule(KotlinModule.Builder()
        .enable(KotlinFeature.SingletonSupport)
    .build())
    .configure(WRITE_DATES_AS_TIMESTAMPS, false)
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(FAIL_ON_NULL_FOR_PRIMITIVES, true)

inline fun <reified T : Any> assertViolations(
    mapper: ObjectMapper,
    json: String,
    vararg constraintViolations: String
) {
    assertViolations({ mapper.treeToValue<T>(mapper.readTree(json)) }, *constraintViolations)
}

fun assertViolations(
    f: () -> Any,
    vararg constraintViolations: String
) {
    val violations = constraintViolations.map { " - $it" }
    try {
        f()
        if (constraintViolations.isNotEmpty())
            fail("no violations but expected ${violations.size}: ${violations.joinToString()}")
    } catch (e: DataValidationException) {
        Assertions.assertThat(e.message!!.lines().drop(1))
            .containsExactlyInAnyOrder(*violations.toTypedArray())
    }
}
