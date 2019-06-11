package org.marketdesignresearch.cavisbackend.management

import org.marketdesignresearch.mechlib.auction.Auction
import org.marketdesignresearch.mechlib.auction.AuctionFactory
import org.marketdesignresearch.mechlib.domain.Domain
import java.util.*
import kotlin.collections.HashMap

data class AuctionWrapper(val uuid: UUID, val auction: Auction, val auctionType: AuctionFactory)

object SessionManagement {

    private val sessions: HashMap<UUID, AuctionWrapper> = HashMap()

    fun create(domain: Domain, type: AuctionFactory): AuctionWrapper {
        val uuid = UUID.randomUUID()
        val auction = type.getAuction(domain)
        sessions[uuid] = AuctionWrapper(uuid, auction, type)
        return AuctionWrapper(uuid, auction, type)
    }

    fun get(uuid: UUID): AuctionWrapper? {
        return sessions[uuid]
    }

    fun get(): Set<AuctionWrapper> {
        return sessions.values.toSet()
    }

}