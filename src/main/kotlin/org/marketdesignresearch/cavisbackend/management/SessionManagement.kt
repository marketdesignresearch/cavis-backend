package org.marketdesignresearch.cavisbackend.management

import org.marketdesignresearch.mechlib.domain.Domain
import org.marketdesignresearch.mechlib.domain.auction.Auction
import org.marketdesignresearch.mechlib.mechanisms.MechanismType
import java.util.*
import kotlin.collections.HashMap

data class CreateAuctionResult(val uuid: UUID, val auction: Auction)

object SessionManagement {

    private val sessions: HashMap<UUID, Auction> = HashMap()

    fun create(domain: Domain, type: MechanismType): CreateAuctionResult {
        val uuid = UUID.randomUUID()
        val auction = Auction(domain, type)
        sessions[uuid] = auction
        return CreateAuctionResult(uuid, auction)
    }

    fun get(uuid: UUID): Auction? {
        return sessions[uuid]
    }

}