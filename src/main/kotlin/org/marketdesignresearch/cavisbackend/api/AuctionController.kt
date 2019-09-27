package org.marketdesignresearch.cavisbackend.api

import org.marketdesignresearch.cavisbackend.SessionManagement
import org.marketdesignresearch.cavisbackend.domains.AuctionFactory
import org.marketdesignresearch.cavisbackend.domains.DomainWrapper
import org.marketdesignresearch.cavisbackend.mongo.AuctionWrapper
import org.marketdesignresearch.cavisbackend.mongo.AuctionWrapperDAO
import org.marketdesignresearch.mechlib.core.*
import org.marketdesignresearch.mechlib.core.bid.Bid
import org.marketdesignresearch.mechlib.core.bid.Bids
import org.marketdesignresearch.mechlib.core.price.LinearPrices
import org.marketdesignresearch.mechlib.core.price.Price
import org.marketdesignresearch.mechlib.mechanism.auctions.IllegalBidException
import org.marketdesignresearch.mechlib.mechanism.auctions.pvm.PVMAuction
import org.marketdesignresearch.mechlib.mechanism.auctions.sequential.SequentialAuction
import org.spectrumauctions.sats.core.model.SATSBidder
import org.spectrumauctions.sats.core.model.SATSGood
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.*
import kotlin.collections.HashSet



data class AuctionSetting(
        val domain: DomainWrapper,
        val auctionType: AuctionFactory,
        val auctionConfig: AuctionConfiguration = AuctionConfiguration(),
        val name: String = "",
        val seed: Long = System.currentTimeMillis(),
        val tags: List<String> = emptyList(),
        val private: Boolean = false
)
data class JSONBid(val amount: BigDecimal, val bundle: Map<UUID, Int>)
data class PerRoundRequest(val round: Int)
data class JSONDemandQuery(val prices: Map<UUID, Double> = emptyMap(), val bidders: List<UUID> = emptyList(), val numberOfBundles: Int = 1)
data class JSONValueQuery(val bundles: List<Map<UUID, Int>>, val bidders: List<UUID> = emptyList())
data class JSONValueQueryResponse(val value: BigDecimal, val bundle: Bundle)
data class JSONInferredValueQuery(val bundle: Map<UUID, Int>, val bidder: UUID)
data class JSONInferredValueQueryResponse(val inferredValues: List<BigDecimal>)
data class AuctionListItem(val id: UUID, val name: String, val createdAt: Date, val domain: String, val auctionType: String,
                           val numberOfBidders: Int, val numberOfGoods: Int, val roundsPlayed: Int, val seed: Long, val tags: List<String>) {
    constructor(aw: AuctionWrapper) : this(aw.id, aw.name, aw.createdAt, aw.auction.domain.name, aw.auctionType.prettyName, aw.auction.domain.bidders.size, aw.auction.domain.goods.size, aw.auction.numberOfRounds, aw.seed, aw.tags)
}

data class AuctionEdit(val name: String?, val tags: List<String>?, val private: Boolean?)

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/auctions")
class AuctionController(private val auctionWrapperDAO: AuctionWrapperDAO) {

    private fun save(auctionWrapper: AuctionWrapper) {
        if (auctionWrapper.auction.domain.goods.none { it is SATSGood } &&
                auctionWrapper.auction.domain.bidders.none { it is SATSBidder }) {
            auctionWrapperDAO.save(auctionWrapper)
        }
    }

    private fun hasAccess(auctionWrapper: AuctionWrapper) : Boolean {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth != null && auth.authorities.map{it.authority}.contains("ROLE_IDENTIFIED")) {
            return !auctionWrapper.private || auctionWrapper.owners.contains(auth.name)
        }
        return !auctionWrapper.private
    }

    private fun filter(auctionWrappers: Collection<AuctionWrapper>): Collection<AuctionWrapper> {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth != null && auth.authorities.map{it.authority}.contains("ROLE_IDENTIFIED")) {
            return auctionWrappers.filter { !it.private || it.owners.contains(auth.name) }
        }
        return auctionWrappers.filter { !it.private }
    }

    @PostMapping("/", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun startAuction(@RequestBody body: AuctionSetting): ResponseEntity<AuctionWrapper> {
        val auth = SecurityContextHolder.getContext().authentication
        val auctionWrapper = if (auth != null && auth.authorities.map{it.authority}.contains("ROLE_IDENTIFIED")) {
            SessionManagement.create(
                    domain = body.domain.toDomain(body.seed),
                    type = body.auctionType,
                    auctionConfig = body.auctionConfig,
                    seed = body.seed,
                    name = body.name,
                    tags = body.tags,
                    private = body.private,
                    owners = listOf(auth.name))
        } else {
            SessionManagement.create(
                    domain = body.domain.toDomain(body.seed),
                    type = body.auctionType,
                    auctionConfig = body.auctionConfig,
                    seed = body.seed,
                    name = body.name,
                    tags = body.tags)
        }
        save(auctionWrapper)
        return ResponseEntity.ok(auctionWrapper)
    }

    @GetMapping("/")
    fun getAuctions(@AuthenticationPrincipal principal: Any): ResponseEntity<List<AuctionListItem>> {
        return ResponseEntity.ok(filter(SessionManagement.get()).map { AuctionListItem(it) })
    }

    @GetMapping("/archived")
    fun getArchivedAuctions(): ResponseEntity<List<AuctionListItem>> {
        val auctionWrappers = auctionWrapperDAO.findAllActiveIsFalseWithoutSATS()
        return ResponseEntity.ok(filter(auctionWrappers).map { AuctionListItem(it) })
    }

    @GetMapping("/{uuid}")
    fun getAuction(@PathVariable uuid: UUID): ResponseEntity<AuctionWrapper?> {
        val auctionWrapper = SessionManagement.get(uuid) ?: run {
            val inDB = auctionWrapperDAO.findByIdWithoutSATS(uuid)
            if (inDB != null && hasAccess(inDB)) {
                SessionManagement.load(inDB)
                inDB.active = true
                save(inDB)
            }
            inDB
        }
        if (auctionWrapper != null && !hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        return ResponseEntity.of(Optional.ofNullable(auctionWrapper))
    }

    @DeleteMapping("/{uuid}")
    fun deleteAuction(@PathVariable uuid: UUID): ResponseEntity<Any> {
        val auctionWrapper = SessionManagement.get(uuid)
        if (auctionWrapper != null && !hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val success = SessionManagement.delete(uuid)
        val inDB = auctionWrapperDAO.findByIdOrNull(uuid)
        if (inDB != null && hasAccess(inDB)) auctionWrapperDAO.delete(inDB)
        if (!success && inDB == null) return ResponseEntity.notFound().build()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{uuid}/archive")
    fun archiveAuction(@PathVariable uuid: UUID): ResponseEntity<Any> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        SessionManagement.delete(uuid)
        auctionWrapper.active = false
        save(auctionWrapper)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{uuid}")
    fun patchAuction(@PathVariable uuid: UUID, @RequestBody body: AuctionEdit): ResponseEntity<AuctionWrapper> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        if (body.name != null) auctionWrapper.name = body.name
        if (body.tags != null) auctionWrapper.tags = body.tags
        if (body.private != null) auctionWrapper.private = body.private
        save(auctionWrapper)
        return ResponseEntity.ok(auctionWrapper)
    }

    @PostMapping("/{uuid}/demandquery")
    fun postDemandQuery(@PathVariable uuid: UUID, @RequestBody body: JSONDemandQuery): ResponseEntity<Map<String, List<Bundle>>> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val auction = auctionWrapper.auction
        val priceMap = hashMapOf<Good, Price>()
        body.prices.forEach { priceMap[auction.getGood(it.key)] = Price.of(it.value) }
        val prices = LinearPrices(priceMap)
        val bidders = if (body.bidders.isEmpty()) auction.domain.bidders else body.bidders.map { auction.getBidder(it) }
        val result = hashMapOf<String, List<Bundle>>()
        bidders.forEach { result[it.id.toString()] = it.getBestBundles(prices, body.numberOfBundles) }
        return ResponseEntity.ok(result)
    }

    @PostMapping("/{uuid}/valuequery")
    fun postValueQuery(@PathVariable uuid: UUID, @RequestBody body: JSONValueQuery): ResponseEntity<Map<String, List<JSONValueQueryResponse>>> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
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
            var alreadyWon = Bundle.EMPTY
            if (auction is SequentialAuction) {
                for (i in 0 until auction.numberOfRounds) {
                    alreadyWon = alreadyWon.merge(auction.getOutcomeAtRound(i).allocation.allocationOf(bidder).bundle)
                }
            }
            val list = arrayListOf<JSONValueQueryResponse>()
            bundles.forEach { bundle ->
                list.add(JSONValueQueryResponse(bidder.getValue(bundle, alreadyWon), bundle))
            }
            result[bidder.id.toString()] = list
        }

        return ResponseEntity.ok(result)
    }

    @PostMapping("/{uuid}/inferredvaluequery")
    fun postInferredValueQuery(@PathVariable uuid: UUID, @RequestBody body: JSONInferredValueQuery): ResponseEntity<JSONInferredValueQueryResponse> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val auction = auctionWrapper.auction
        if (auction !is PVMAuction) return ResponseEntity.badRequest().build()
        val bidder = auction.getBidder(body.bidder)
        val bundleEntries = hashSetOf<BundleEntry>()
        body.bundle.forEach { (k, v) -> bundleEntries.add(BundleEntry(auction.getGood(k), v)) }
        val bundle = Bundle(bundleEntries)
        val result = arrayListOf<BigDecimal>()
        for (i in 0 until auction.numberOfRounds) {
            result.add(auction.getInferredValue(bidder, bundle, i))
        }
        return ResponseEntity.ok(JSONInferredValueQueryResponse(result))
    }

    @PostMapping("/{uuid}/bids", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun addBids(@PathVariable uuid: UUID, @RequestBody bidderBids: Map<UUID, Set<JSONBid>>): ResponseEntity<Any> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
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
        save(auctionWrapper)
        return ResponseEntity.ok(auctionWrapper)
    }

    @PostMapping("/{uuid}/propose", consumes = [])
    fun proposeBids(@PathVariable uuid: UUID, @RequestBody body: ArrayList<UUID>?): ResponseEntity<Bids> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val auction = auctionWrapper.auction
        val uuids = body ?: arrayListOf()
        if (uuids.isEmpty()) uuids.addAll(auction.domain.bidders.map { it.id })
        val bids = Bids()
        uuids.forEach { bids.setBid(auction.getBidder(it), auction.proposeBid(auction.getBidder(it))) }
        save(auctionWrapper)
        return ResponseEntity.ok(bids)
    }

    @PostMapping("/{uuid}/close-round", consumes = [])
    fun closeRound(@PathVariable uuid: UUID): ResponseEntity<AuctionWrapper> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val auction = auctionWrapper.auction
        auction.closeRound()
        // TODO: For now, we get result directly. I'll have to think about whether
        //  I should make this the default in the MechLib, as for most auctions the result will be quickly available
        auction.getOutcomeAtRound(auction.numberOfRounds - 1)
        save(auctionWrapper)
        return ResponseEntity.ok(auctionWrapper)
    }

    @PostMapping("/{uuid}/advance-round", consumes = [])
    fun advanceRound(@PathVariable uuid: UUID): ResponseEntity<AuctionWrapper> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val auction = auctionWrapper.auction
        auction.advanceRound()
        // TODO: For now, we get result directly. I'll have to think about whether
        //  I should make this the default in the MechLib, as for most auctions the result will be quickly available
        auction.getOutcomeAtRound(auction.numberOfRounds - 1)
        save(auctionWrapper)
        return ResponseEntity.ok(auctionWrapper)
    }

    @PostMapping("/{uuid}/advance-phase", consumes = [])
    fun advancePhase(@PathVariable uuid: UUID): ResponseEntity<AuctionWrapper> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val auction = auctionWrapper.auction
        if (auction.currentPhaseFinished()) auction.advanceRound() // Step out of the "finished" round
        while (!auction.currentPhaseFinished()) {
            auction.advanceRound()
        }
        // TODO: For now, we get result directly. I'll have to think about whether
        //  I should make this the default in the MechLib, as for most auctions the result will be quickly available
        auction.getOutcomeAtRound(auction.numberOfRounds - 1)
        save(auctionWrapper)
        return ResponseEntity.ok(auctionWrapper)
    }

    @PostMapping("/{uuid}/finish", consumes = [])
    fun finish(@PathVariable uuid: UUID): ResponseEntity<AuctionWrapper> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val auction = auctionWrapper.auction
        while (!auction.finished()) {
            auction.advanceRound()
        }
        // TODO: For now, we get result directly. I'll have to think about whether
        //  I should make this the default in the MechLib, as for most auctions the result will be quickly available
        auction.getOutcomeAtRound(auction.numberOfRounds - 1)
        save(auctionWrapper)
        return ResponseEntity.ok(auctionWrapper)
    }

    @PutMapping("/{uuid}/reset")
    fun resetAuction(@PathVariable uuid: UUID, @RequestBody body: PerRoundRequest): ResponseEntity<Bids> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        return try {
            val bids = auctionWrapper.auction.getBidsAt(body.round)
            auctionWrapper.auction.resetToRound(body.round)
            save(auctionWrapper)
            ResponseEntity.ok(bids)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/{uuid}/result")
    fun getResult(@PathVariable uuid: UUID): ResponseEntity<Outcome> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val outcome = auctionWrapper.auction.outcome
        save(auctionWrapper)
        return ResponseEntity.ok(outcome)
    }

    @GetMapping("/{uuid}/rounds/{round}/result")
    fun getResult(@PathVariable uuid: UUID, @PathVariable round: Int): ResponseEntity<Outcome> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val mechanismResult = try {
            auctionWrapper.auction.getOutcomeAtRound(round)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        save(auctionWrapper)
        return ResponseEntity.ok(mechanismResult)
    }

    @GetMapping("/{uuid}/efficient-allocation")
    fun getEfficientAllocation(@PathVariable uuid: UUID): ResponseEntity<Allocation> {
        val auctionWrapper = SessionManagement.get(uuid) ?: return ResponseEntity.notFound().build()
        if (!hasAccess(auctionWrapper)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val efficientAllocation = auctionWrapper.auction.domain.efficientAllocation
        save(auctionWrapper)
        return ResponseEntity.ok(efficientAllocation)
    }
}