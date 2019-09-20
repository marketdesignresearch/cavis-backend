package org.marketdesignresearch.cavisbackend.mongo

import org.marketdesignresearch.cavisbackend.api.AuctionConfiguration
import org.marketdesignresearch.cavisbackend.domains.AuctionFactory
import org.marketdesignresearch.mechlib.mechanism.auctions.Auction
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Document
data class AuctionWrapper(
        @Id
        val id: UUID,
        val auction: Auction,
        val auctionType: AuctionFactory,
        val seed: Long,
        var name: String = "",
        var tags: List<String> = arrayListOf(),
        var auctionConfig: AuctionConfiguration = AuctionConfiguration(),
        val createdAt: Date = Date(),
        var active: Boolean = true
)

@Repository
interface AuctionWrapperDAO: MongoRepository<AuctionWrapper, UUID> {
        @Query("{'auction.domain._class' : { '\$regex': '^((?!\\\\.sats\\\\.)[\\\\s\\\\S])*\$' }, '_id' : ?0 }")
        fun findByIdWithoutSATS(uuid: UUID): AuctionWrapper?

        @Query("{'auction.domain._class' : { '\$regex': '^((?!\\\\.sats\\\\.)[\\\\s\\\\S])*\$' }, 'active' : true }")
        fun findAllActiveIsTrueWithoutSATS(): List<AuctionWrapper>

        @Query("{'auction.domain._class' : { '\$regex': '^((?!\\\\.sats\\\\.)[\\\\s\\\\S])*\$' }, 'active' : false }")
        fun findAllActiveIsFalseWithoutSATS(): List<AuctionWrapper>
}