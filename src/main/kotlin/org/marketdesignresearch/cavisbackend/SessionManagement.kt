package org.marketdesignresearch.cavisbackend

import org.marketdesignresearch.cavisbackend.domains.AuctionFactory
import org.marketdesignresearch.cavisbackend.api.AuctionConfiguration
import org.marketdesignresearch.cavisbackend.mongo.AuctionWrapper
import org.marketdesignresearch.mechlib.core.Domain
import java.util.*
import kotlin.collections.HashMap


object SessionManagement {

    private val sessions: HashMap<UUID, AuctionWrapper> = HashMap()

    fun create(domain: Domain,
               type: AuctionFactory,
               auctionConfig: AuctionConfiguration,
               seed: Long,
               name: String = "",
               tags: List<String> = emptyList(),
               private: Boolean = false,
               owners: List<String> = emptyList()
    ): AuctionWrapper {
        val uuid = UUID.randomUUID()
        val auction = type.getAuction(domain, auctionConfig)
        val auctionWrapper = AuctionWrapper(
                id = uuid,
                auction = auction,
                auctionType = type,
                seed = seed,
                name = name,
                tags = tags,
                auctionConfig = auctionConfig,
                private = private,
                owners = owners)
        sessions[uuid] = auctionWrapper
        return auctionWrapper
    }

    fun get(uuid: UUID): AuctionWrapper? {
        return sessions[uuid]
    }

    fun get(): Set<AuctionWrapper> {
        return sessions.values.toSet()
    }

    fun delete(uuid: UUID): Boolean {
        return sessions.remove(uuid) != null
    }

    fun load(auctionWrapper: AuctionWrapper) {
        sessions[auctionWrapper.id] = auctionWrapper
    }

    fun loadAll(auctionWrapperList: List<AuctionWrapper>) {
        auctionWrapperList.forEach { sessions[it.id] = it }
    }

}