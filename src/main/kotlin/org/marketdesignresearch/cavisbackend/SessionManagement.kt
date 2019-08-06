package org.marketdesignresearch.cavisbackend

import org.marketdesignresearch.cavisbackend.domains.AuctionFactory
import org.marketdesignresearch.cavisbackend.api.AuctionConfiguration
import org.marketdesignresearch.cavisbackend.mongo.AuctionWrapper
import org.marketdesignresearch.mechlib.core.Domain
import java.util.*
import kotlin.collections.HashMap


object SessionManagement {

    private val sessions: HashMap<UUID, AuctionWrapper> = HashMap()

    fun create(domain: Domain, type: AuctionFactory, auctionConfig: AuctionConfiguration): AuctionWrapper {
        val uuid = UUID.randomUUID()
        val auction = type.getAuction(domain, auctionConfig)
        sessions[uuid] = AuctionWrapper(uuid, auction, type)
        return AuctionWrapper(uuid, auction, type)
    }

    fun get(uuid: UUID): AuctionWrapper? {
        return sessions[uuid]
    }

    fun get(): Set<AuctionWrapper> {
        return sessions.values.toSet()
    }

    fun load(auctionWrapper: AuctionWrapper) {
        sessions[auctionWrapper.id] = auctionWrapper
    }

    fun delete(uuid: UUID): Boolean {
        return sessions.remove(uuid) != null
    }

}