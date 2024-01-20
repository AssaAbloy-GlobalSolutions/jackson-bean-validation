package com.assaabloyglobalsolutions.jacksonbeanvalidation.reflect

import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

/** true for Map<K, V> where V is non-nullable */
internal val KType.isNonNullableMap: Boolean
    get() = jvmErasure.isSubclassOf(Map::class)
        && arguments.getOrNull(1)?.type?.isMarkedNullable != true

/** true for Collection<T> where T is non-nullable */
internal val KType.isNonNullableCollection: Boolean
    get() = jvmErasure.isSubclassOf(Collection::class)
        && arguments.getOrNull(0)?.type?.isMarkedNullable != true

/** true for Array<T> where T is non-nullable */
internal val KType.isNonNullableArray: Boolean
    // comparing jvmErasure against Array::class does not work, not sure why
    get() = jvmErasure.qualifiedName == "kotlin.Array"
        && arguments.getOrNull(0)?.type?.isMarkedNullable != true
