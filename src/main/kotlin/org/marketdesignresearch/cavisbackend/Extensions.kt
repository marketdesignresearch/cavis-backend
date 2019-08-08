package org.marketdesignresearch.cavisbackend

import org.apache.commons.codec.digest.DigestUtils
import org.marketdesignresearch.mechlib.core.Allocation
import org.marketdesignresearch.mechlib.core.Bundle
import org.marketdesignresearch.mechlib.core.SimpleXORDomain
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.CCAuction
import java.util.*

fun Bundle.sha256Hex(): String {
    return DigestUtils.sha256Hex(
            bundleEntries.map { it.good.uuid.toString() + it.amount }
                    .sorted()
                    .joinToString(separator = "")
    )
}

fun CCAuction(): CCAuction = CCAuction(SimpleXORDomain(listOf(), listOf()))