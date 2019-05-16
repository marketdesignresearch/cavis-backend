package org.marketdesignresearch.cavisbackend.server

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.marketdesignresearch.cavisbackend.domains.Setting
import org.marketdesignresearch.cavisbackend.management.SessionManagement
import org.marketdesignresearch.mechlib.domain.Bundle
import org.marketdesignresearch.mechlib.domain.BundleBid
import org.marketdesignresearch.mechlib.domain.BundleEntry
import org.marketdesignresearch.mechlib.domain.auction.Auction
import org.marketdesignresearch.mechlib.domain.bid.Bid
import org.marketdesignresearch.mechlib.domain.bid.Bids
import org.marketdesignresearch.mechlib.mechanisms.AuctionResult
import org.marketdesignresearch.mechlib.mechanisms.MechanismType
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.*
import kotlin.collections.HashSet

data class AuctionSetting(val setting: Setting, val type: MechanismType)
data class JSONBid(val amount: BigDecimal, val bundle: Map<String, Int>)

@CrossOrigin(origins = ["*"])
@RestController
class AuctionController {

    @PostMapping("/auctions", consumes = [MediaType.ALL_VALUE])
    fun startAuction(@RequestBody body: AuctionSetting): UUID {
        return SessionManagement.create(body.setting.toDomain(), body.type)
    }

    /*@PostMapping("/test", consumes = [MediaType.ALL_VALUE])
    fun test(@RequestBody body: AnimalProtocol): String {
        return body.animal.callName()
    }*/

    @GetMapping("/auctions/{uuid}")
    fun getAuction(@PathVariable uuid: UUID): Auction? {
        return SessionManagement.get(uuid)
    }

    @PostMapping("/auctions/{uuid}/bids", consumes = [MediaType.ALL_VALUE])
    fun addBids(@PathVariable uuid: UUID, @RequestBody bidderBids: Map<String, Set<JSONBid>>): String { // TODO: Have reasonable responses
        val auction = SessionManagement.get(uuid) ?: return "Not found"
        val bids = Bids()
        bidderBids.forEach { (bidderId, jsonBids) ->
            run {
                val bundleBids = HashSet<BundleBid>()
                jsonBids.forEach {
                    val bundleEntries = HashSet<BundleEntry>()
                    it.bundle.forEach { (k, v) -> bundleEntries.add(BundleEntry(auction.getGood(k), v)) }
                    bundleBids.add(BundleBid(it.amount, Bundle(bundleEntries), UUID.randomUUID().toString())) // FIXME: get rid of IDs per BundleBid
                }
                bids.setBid(auction.getBidder(bidderId), Bid(bundleBids))
            }
        }
        auction.addRound(bids)
        return "Bids added!"
    }

    @GetMapping("/auctions/{uuid}/result")
    fun getAllocation(@PathVariable uuid: UUID): AuctionResult? {
        val auction = SessionManagement.get(uuid) ?: return AuctionResult.NONE
        auction.auctionResult
        return auction.auctionResult
    }
    // Get allocation /auctions/{uuid}/allocation

}


data class AnimalProtocol(val animal: Animal)

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = Cat::class, name = "cat"),
        JsonSubTypes.Type(value = Dog::class, name = "dog")
)
interface Animal {
    fun callName(): String
}

data class Cat(val name: String): Animal {
    override fun callName(): String {
        return name
    }
}

data class Dog(val firstName: String, val secondName: String): Animal {
    override fun callName(): String {
        return "First: $firstName, Second: $secondName"
    }
}