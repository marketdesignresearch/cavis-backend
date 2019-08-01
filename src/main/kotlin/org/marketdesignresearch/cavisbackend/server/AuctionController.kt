package org.marketdesignresearch.cavisbackend.server

import org.marketdesignresearch.cavisbackend.domains.AuctionFactory
import org.marketdesignresearch.cavisbackend.domains.DomainWrapper
import org.marketdesignresearch.cavisbackend.management.AuctionWrapper
import org.marketdesignresearch.cavisbackend.management.SessionManagement
import org.marketdesignresearch.mechlib.mechanism.auctions.IllegalBidException
import org.marketdesignresearch.mechlib.core.Bundle
import org.marketdesignresearch.mechlib.core.BundleBid
import org.marketdesignresearch.mechlib.core.BundleEntry
import org.marketdesignresearch.mechlib.core.Good
import org.marketdesignresearch.mechlib.core.bid.Bid
import org.marketdesignresearch.mechlib.core.bid.Bids
import org.marketdesignresearch.mechlib.core.price.LinearPrices
import org.marketdesignresearch.mechlib.core.price.Price
import org.marketdesignresearch.mechlib.core.Outcome
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.*
import kotlin.collections.HashSet

data class AuctionSetting(val domain: DomainWrapper, val auctionType: AuctionFactory, val auctionConfig: AuctionConfiguration = AuctionConfiguration())
data class JSONBid(val amount: BigDecimal, val bundle: Map<UUID, Int>)
data class PerRoundRequest(val round: Int)
data class JSONDemandQuery(val prices: Map<UUID, Double> = emptyMap(), val bidders: List<UUID> = emptyList(), val numberOfBundles: Int = 1)
data class JSONValueQuery(val bundles: List<Map<UUID, Int>>, val bidders: List<UUID> = emptyList())
data class JSONValueQueryResponse(val value: BigDecimal, val bundle: Bundle)

@CrossOrigin(origins = ["*"])
@RestController
class AuctionController {

    @PostMapping("/auctions", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun startAuction(@RequestBody body: AuctionSetting): ResponseEntity<AuctionWrapper> {
        return ResponseEntity.of(Optional.of(SessionManagement.create(body.domain.toDomain(), body.auctionType, body.auctionConfig)))
    }

    @GetMapping("/auctions")
    fun getAuctions(): ResponseEntity<Set<AuctionWrapper>> {
        return ResponseEntity.of(Optional.of(SessionManagement.get()))
    }

    @GetMapping("/auctions/{uuid}")
    fun getAuction(@PathVariable uuid: UUID): ResponseEntity<AuctionWrapper> {
        return ResponseEntity.of(Optional.ofNullable(SessionManagement.get(uuid)))
    }

    @DeleteMapping("/auctions/{uuid}")
    fun deleteAuction(@PathVariable uuid: UUID): ResponseEntity<Any> {
        val success = SessionManagement.delete(uuid)
        if (!success) return ResponseEntity.notFound().build()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/auctions/{uuid}/demandquery")
    fun postDemandQuery(@PathVariable uuid: UUID, @RequestBody body: JSONDemandQuery): ResponseEntity<Map<String, List<Bundle>>> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        val auction = auctionWrapper.auction
        val priceMap = hashMapOf<Good, Price>()
        body.prices.forEach { priceMap[auction.getGood(it.key)] = Price.of(it.value) }
        val prices = LinearPrices(priceMap)
        val bidders = if (body.bidders.isEmpty()) auction.domain.bidders else body.bidders.map { auction.getBidder(it) }
        val result = hashMapOf<String, List<Bundle>>()
        bidders.forEach { result[it.id.toString()] = it.getBestBundles(prices, body.numberOfBundles) }
        return ResponseEntity.ok(result)
    }

    @PostMapping("/auctions/{uuid}/valuequery")
    fun postValueQuery(@PathVariable uuid: UUID, @RequestBody body: JSONValueQuery): ResponseEntity<Map<String, List<JSONValueQueryResponse>>> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        val auction = auctionWrapper.auction
        val bidders = if (body.bidders.isEmpty()) auction.domain.bidders else body.bidders.map { auction.getBidder(it) }
        val bundles = arrayListOf<Bundle>()
        body.bundles.forEach {
            val bundleEntries = hashSetOf<BundleEntry>()
            it.forEach { (k, v) -> bundleEntries.add(BundleEntry(auction.getGood(k), v)) }
            bundles.add(Bundle(bundleEntries))
        }
        val result = hashMapOf<String, List<JSONValueQueryResponse>>()
        bidders.forEach { bidder ->
            val list = arrayListOf<JSONValueQueryResponse>()
            bundles.forEach { bundle ->
                list.add(JSONValueQueryResponse(bidder.getValue(bundle), bundle))
            }
            result[bidder.id.toString()] = list
        }

        return ResponseEntity.ok(result)
    }

    @PostMapping("/auctions/{uuid}/bids", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun addBids(@PathVariable uuid: UUID, @RequestBody bidderBids: Map<UUID, Set<JSONBid>>): ResponseEntity<Any> {
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
        try {
            auction.submitBids(bids)
        } catch (e: IllegalBidException) {
            return ResponseEntity.badRequest().body(e.message)
        }
        return ResponseEntity.ok(auctionWrapper)
    }

    @PostMapping("/auctions/{uuid}/propose", consumes = [])
    fun proposeBids(@PathVariable uuid: UUID, @RequestBody body: ArrayList<UUID>?): ResponseEntity<Bids> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        val auction = auctionWrapper.auction
        val uuids = body ?: arrayListOf()
        if (uuids.isEmpty()) uuids.addAll(auction.domain.bidders.map { it.id })
        val bids = Bids()
        uuids.forEach { bids.setBid(auction.getBidder(it), auction.proposeBid(auction.getBidder(it))) }
        return ResponseEntity.ok(bids)
    }

    @PostMapping("/auctions/{uuid}/close-round", consumes = [])
    fun closeRound(@PathVariable uuid: UUID): ResponseEntity<AuctionWrapper> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        val auction = auctionWrapper.auction
        auction.closeRound()
        // TODO: For now, we get result directly. I'll have to think about whether
        //  I should make this the default in the MechLib, as for most auctions the result will be quickly available
        auction.getOutcomeAtRound(auction.numberOfRounds - 1)
        return ResponseEntity.ok(auctionWrapper)
    }

    @PostMapping("/auctions/{uuid}/advance-round", consumes = [])
    fun advanceRound(@PathVariable uuid: UUID): ResponseEntity<AuctionWrapper> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        val auction = auctionWrapper.auction
        auction.advanceRound()
        // TODO: For now, we get result directly. I'll have to think about whether
        //  I should make this the default in the MechLib, as for most auctions the result will be quickly available
        auction.getOutcomeAtRound(auction.numberOfRounds - 1)
        return ResponseEntity.ok(auctionWrapper)
    }

    @PostMapping("/auctions/{uuid}/advance-phase", consumes = [])
    fun advancePhase(@PathVariable uuid: UUID): ResponseEntity<AuctionWrapper> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        val auction = auctionWrapper.auction
        if (auction.currentPhaseFinished()) auction.advanceRound() // Step out of the "finished" round
        while (!auction.currentPhaseFinished()) {
            auction.advanceRound()
        }
        // TODO: For now, we get result directly. I'll have to think about whether
        //  I should make this the default in the MechLib, as for most auctions the result will be quickly available
        auction.getOutcomeAtRound(auction.numberOfRounds - 1)
        return ResponseEntity.ok(auctionWrapper)
    }

    @PostMapping("/auctions/{uuid}/finish", consumes = [])
    fun finish(@PathVariable uuid: UUID): ResponseEntity<AuctionWrapper> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        val auction = auctionWrapper.auction
        while (!auction.finished()) {
            auction.advanceRound()
        }
        // TODO: For now, we get result directly. I'll have to think about whether
        //  I should make this the default in the MechLib, as for most auctions the result will be quickly available
        auction.getOutcomeAtRound(auction.numberOfRounds - 1)
        return ResponseEntity.ok(auctionWrapper)
    }

    @PutMapping("/auctions/{uuid}/reset")
    fun resetAuction(@PathVariable uuid: UUID, @RequestBody body: PerRoundRequest): ResponseEntity<Bids> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        return try {
            val bids = auctionWrapper.auction.getBidsAt(body.round)
            auctionWrapper.auction.resetToRound(body.round)
            ResponseEntity.ok(bids)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/auctions/{uuid}/result")
    fun getResult(@PathVariable uuid: UUID): ResponseEntity<Outcome> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(auctionWrapper.auction.outcome)
    }

    @GetMapping("/auctions/{uuid}/rounds/{round}/result")
    fun getResult(@PathVariable uuid: UUID, @PathVariable round: Int): ResponseEntity<Outcome> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        val mechanismResult = try {
            auctionWrapper.auction.getOutcomeAtRound(round)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        return ResponseEntity.ok(mechanismResult)
    }
}