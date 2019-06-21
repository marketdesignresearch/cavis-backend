package org.marketdesignresearch.cavisbackend.server

import org.marketdesignresearch.cavisbackend.domains.DomainWrapper
import org.marketdesignresearch.cavisbackend.management.AuctionWrapper
import org.marketdesignresearch.cavisbackend.management.SessionManagement
import org.marketdesignresearch.mechlib.auction.AuctionFactory
import org.marketdesignresearch.mechlib.domain.Bundle
import org.marketdesignresearch.mechlib.domain.BundleBid
import org.marketdesignresearch.mechlib.domain.BundleEntry
import org.marketdesignresearch.mechlib.domain.bid.Bid
import org.marketdesignresearch.mechlib.domain.bid.Bids
import org.marketdesignresearch.mechlib.mechanisms.MechanismResult
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.*
import kotlin.collections.HashSet

data class AuctionSetting(val domain: DomainWrapper, val auctionType: AuctionFactory)
data class JSONBid(val amount: BigDecimal, val bundle: Map<String, Int>)
data class ResetRequest(val round: Int)

@CrossOrigin(origins = ["*"])
@RestController
class AuctionController {

    @PostMapping("/auctions", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun startAuction(@RequestBody body: AuctionSetting): ResponseEntity<AuctionWrapper> {
        return ResponseEntity.of(Optional.of(SessionManagement.create(body.domain.toDomain(), body.auctionType)))
    }

    @GetMapping("/auctions")
    fun getAuctions(): ResponseEntity<Set<AuctionWrapper>> {
        return ResponseEntity.of(Optional.of(SessionManagement.get()))
    }

    /*@PostMapping("/test", consumes = [MediaType.ALL_VALUE])
    fun test(@RequestBody body: AnimalProtocol): String {
        return body.animal.callName()
    }*/

    @GetMapping("/auctions/{uuid}")
    fun getAuction(@PathVariable uuid: UUID): ResponseEntity<AuctionWrapper> {
        return ResponseEntity.of(Optional.ofNullable(SessionManagement.get(uuid)))
    }

    @PostMapping("/auctions/{uuid}/bids", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun addBids(@PathVariable uuid: UUID, @RequestBody bidderBids: Map<UUID, Set<JSONBid>>): ResponseEntity<AuctionWrapper> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        val auction = auctionWrapper.auction
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
        // TODO: For now, we get result directly. I'll have to think about whether
        //  I should make this the default in the MechLib, as for most auctions the result will be quickly available
        val result = auction.getAuctionResultAtRound(auction.numberOfRounds - 1)
        return ResponseEntity.ok(auctionWrapper)
    }

    @PutMapping("/auctions/{uuid}/reset")
    fun resetAuction(@PathVariable uuid: UUID, @RequestBody body: ResetRequest): ResponseEntity<AuctionWrapper> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        try {
            auctionWrapper.auction.resetToRound(body.round)
        } catch(e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        return ResponseEntity.ok(auctionWrapper)
    }


    @GetMapping("/auctions/{uuid}/result")
    fun getAllocation(@PathVariable uuid: UUID): ResponseEntity<MechanismResult> {
        return ResponseEntity.of(Optional.ofNullable(SessionManagement.get(uuid)?.auction?.mechanismResult))
    }

}