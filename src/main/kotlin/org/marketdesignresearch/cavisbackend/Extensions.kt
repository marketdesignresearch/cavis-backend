package org.marketdesignresearch.cavisbackend

import org.apache.commons.codec.digest.DigestUtils
import org.marketdesignresearch.mechlib.core.Bundle

fun Bundle.sha256Hex(): String {
    return DigestUtils.sha256Hex(
            bundleEntries.map { it.good.uuid.toString() + it.amount }
                    .sorted()
                    .joinToString(separator = "")
    )
}