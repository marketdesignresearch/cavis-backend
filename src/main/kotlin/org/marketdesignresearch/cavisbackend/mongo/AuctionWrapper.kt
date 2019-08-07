package org.marketdesignresearch.cavisbackend.mongo

import org.marketdesignresearch.cavisbackend.domains.AuctionFactory
import org.marketdesignresearch.mechlib.mechanism.auctions.Auction
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*

@Document
data class AuctionWrapper(
        @Id
        val id: UUID,
        val auction: Auction,
        val auctionType: AuctionFactory
) {
        val createdAt: Date = Date()
}

@Repository
interface AuctionWrapperDAO: MongoRepository<AuctionWrapper, UUID>