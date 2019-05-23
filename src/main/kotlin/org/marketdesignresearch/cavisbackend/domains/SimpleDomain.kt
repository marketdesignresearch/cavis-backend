package org.marketdesignresearch.cavisbackend.domains

import org.marketdesignresearch.mechlib.domain.Domain
import org.marketdesignresearch.mechlib.domain.SimpleGood
import org.marketdesignresearch.mechlib.domain.bidder.XORBidder

data class SimpleDomain(val bidders: List<XORBidder>, val goods: List<SimpleGood>): DomainWrapper {
    override fun toDomain(): Domain {
        return Domain(bidders.toSet(), goods.toSet())
    }
}