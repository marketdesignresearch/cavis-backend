package org.marketdesignresearch.cavisbackend.management

import org.marketdesignresearch.mechlib.domain.Domain
import org.marketdesignresearch.mechlib.domain.auction.Auction
import org.marketdesignresearch.mechlib.mechanisms.MechanismType
import java.util.*
import kotlin.collections.HashMap

object SessionManagement {

    private val sessions: HashMap<UUID, Auction> = HashMap()

    fun create(domain: Domain, type: MechanismType): UUID {
        val uuid = UUID.randomUUID()
        sessions[uuid] = Auction(domain, type)
        return uuid
    }

    fun get(uuid: UUID): Auction? {
        return sessions[uuid]
    }

}