package org.marketdesignresearch.cavisbackend.management

import org.marketdesignresearch.mechlib.domain.Domain
import org.marketdesignresearch.mechlib.domain.auction.Auction
import org.marketdesignresearch.mechlib.mechanisms.MechanismType
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashMap

data class AuctionWrapper(val uuid: UUID, val auction: Auction)

object SessionManagement {

    private val sessions: HashMap<UUID, Auction> = HashMap()

    fun create(domain: Domain, type: MechanismType): AuctionWrapper {
        val uuid = UUID.randomUUID()
        val auction = Auction(domain, type)
        sessions[uuid] = auction
        return AuctionWrapper(uuid, auction)
    }

    fun get(uuid: UUID): Auction? {
        return sessions[uuid]
    }

    fun get(): Set<AuctionWrapper> {
        // TODO: Do this nicer
        return sessions.entries.stream().map{AuctionWrapper(it.key, it.value)}.collect(Collectors.toSet())
    }

}