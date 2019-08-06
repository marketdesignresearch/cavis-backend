package org.marketdesignresearch.cavisbackend

import org.apache.commons.codec.digest.DigestUtils
import org.marketdesignresearch.mechlib.core.Bundle
import java.util.*

fun Bundle.sha256Hex(): String {
    return DigestUtils.sha256Hex(
            bundleEntries.map { it.good.uuid.toString() + it.amount }
                    .sorted()
                    .joinToString(separator = "")
    )
}

fun <T : Any> Optional<T>.toNullable(): T? = this.orElse(null);