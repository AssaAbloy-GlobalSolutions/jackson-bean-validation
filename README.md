## Jackson Bean Validation

Jackson Bean Validation module performs JSR 380 validation mapping during deserialization.
This process ensures validation checks occur before the actual type is deserialized. It is
particularly useful for validating non-nullable fields in Kotlin, ensuring they are not
erroneously mapped to null in the incoming JSON.

Any detected constraint violations are communicated through a `DataValidationException`,
which is thrown during `BeanDeserializer::deserialize`.


## Features
- **Automatic `@NotNull` Validation**: All non-nullable fields in Kotlin are implicitly treated
  as if they are annotated with `@NotNull`. This applies unless a field is already
  constrained by another annotation (e.g., `@NotBlank`).

- **Handling Existing Constraint Violations**: If a field with an existing constraint does not emit
  violations on null values, it is still treated as a `@NotNull` field.

- **Extended Validation Scope**: Implicit `@NotNull` behavior also applies to the values inside maps,
  collections, and arrays, wherever applicable.

- **Support for value classes**: Value classes (formerly, inline classes) are boxed before validation
  to preserve and recognize any constraint annotations.

- **Null Handling for Primitive Fields:** When enabled, `DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES`
  triggers a `@NotNull` violation for null values in primitive fields. 

### As an example:

Old structure to ensure `@NotNull` constraints in kotlin:

```kotlin
// @NotBlank likely won't be evaluated, because value classes are 
// typically inlined as their underlying type
@JvmInline
value class Inlined(@get:NotBlank val value: String?)

data class FooBar(
    @get:NotNull val foo: List<@NotNull String>?,
    @get:NotNull val bar: Inlined?,
    @get:NotBlank val string: String?,
)
```

With jackson bean validation, and the same constraint mapping and behavior, this becomes:

```kotlin
// constraint annotations now work on value classes
@JvmInline
value class Inlined(@get:NotBlank val value: String)

// @NotNull inferred from kotlin's type system
data class FooBar(
    val foo: List<String>,
    val bar: Inlined,
    @get:NotBlank val string: String, // no @NotNull due to @NotBlank
)
```

## Usage

Declare the dependency:

```xml
<dependency>
    <groupId>com.assaabloyglobalsolutions.jacksonbeanvalidation</groupId>
    <artifactId>jackson-jakarta-validation</artifactId>
    <version>${jackson-bean-validation.version}</version>
</dependency>
```

Alternatively, for javax:

```xml
<dependency>
    <groupId>com.assaabloyglobalsolutions.jacksonbeanvalidation</groupId>
    <artifactId>jackson-javax-validation</artifactId>
    <version>${jackson-bean-validation.version}</version>
</dependency>
```

And then register the module:

```kotlin
objectMapper
    .registerModule(kotlinBeanValidationModule(validator))
    .configure(FAIL_ON_NULL_FOR_PRIMITIVES, true) // optional: for @NotNull on primitive fields
```

A `DataValidationException` is thrown during deserialization if validation fails, containing the constraint
violations. It likely requires an exception mapper similar to that of a mapper for `ConstrainViolationException`.

```kotlin
@Provider
class JacksonConstraintViolationExceptionMapper : ExceptionMapper<DataValidationException> {
    override fun toResponse(exception: DataValidationException): Response {
        return ErrorResponseFactory.error(null, BAD_REQUEST, ErrorCodeV2.CONSTRAINT_VIOLATION, exception
            .violations
            .map { violation -> "${violation.message} (${violation.propertyPath})" }
            .sorted()
            .joinToString(", ")
        )
    }
}
```
