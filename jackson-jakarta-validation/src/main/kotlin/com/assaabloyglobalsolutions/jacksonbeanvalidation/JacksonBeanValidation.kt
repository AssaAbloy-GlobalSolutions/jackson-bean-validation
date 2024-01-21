package com.assaabloyglobalsolutions.jacksonbeanvalidation

import com.fasterxml.jackson.databind.deser.BeanDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.KotlinBeanValidator
import jakarta.validation.Validator

/**
 * Performs JSR 380 validation mapping during deserialization. This process occurs
 * before the actual type is deserialized, thereby enabling the validation of non-nullable
 * fields that are erroneously mapped to `null` in the incoming JSON.
 *
 * All non-nullable fields in kotlin are implicitly treated as if annotated by `@NotNull`,
 * unless already constrained by another annotation (e.g. `@NotBlank`). If existing
 * constraint violations don't emit violations on `null`, then the field is still
 * treated as a `@NotNull` field.
 *
 * Implicit `@NotNull` behavior is also applied to the values inside maps, collections and
 * arrays when applicable.
 *
 * Any detected constraint violations are communicated through a [DataValidationException],
 * which is thrown during [BeanDeserializer.deserialize].
 */
fun kotlinBeanValidationModule(
    validator: Validator
): SimpleModule {
    val kbv = KotlinBeanValidator(validator)
    return SimpleModule("com.assaabloyglobalsolutions.jacksonbeanvalidation.JacksonBeanValidation")
        .setDeserializerModifier(ConstraintValidatingDeserializerModifier(kbv))
}
