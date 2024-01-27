package com.assaabloyglobalsolutions.jacksonbeanvalidation

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer
import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.jakarta.withParentPath

internal class PathReconstructingDeserializer(
    deserializer: JsonDeserializer<*>
) : DelegatingDeserializer(deserializer) {
    override fun newDelegatingInstance(
        newDelegatee: JsonDeserializer<*>
    ): JsonDeserializer<*> = PathReconstructingDeserializer(newDelegatee)

    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): Any? = deserializeWithPathReconstruction(p) { super.deserialize(p, ctxt) }
}


private fun <T> deserializeWithPathReconstruction(
    p: JsonParser,
    f: () -> T?
): T? {
    try {
        return f()
    } catch (e: JsonMappingException) {
        val dve = e.cause as? DataValidationException ?: throw e
        val violations = dve.violations
            .map { violation -> violation.withParentPath(p.resolvedName) }
        throw DataValidationException(violations)
    }
}

private val JsonParser.resolvedName: String
    get() = when {
        parsingContext.hasCurrentName() -> parsingContext.currentName
        else                            -> "$parsingContext"
    }
