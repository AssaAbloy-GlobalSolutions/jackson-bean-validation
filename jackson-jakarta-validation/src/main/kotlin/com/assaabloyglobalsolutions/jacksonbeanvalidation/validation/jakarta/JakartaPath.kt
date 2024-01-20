package com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.jakarta

import jakarta.validation.Path

internal class JakartaPath(private val delegate: Path) : Path {
    override fun iterator() = delegate.iterator()
    override fun toString() = delegate.toString().replace(".[", "[")
}