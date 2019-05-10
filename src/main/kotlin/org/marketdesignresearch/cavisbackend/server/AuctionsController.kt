package org.marketdesignresearch.cavisbackend.server

import org.marketdesignresearch.cavisbackend.management.SessionManagement
import org.marketdesignresearch.mechlib.domain.Domain
import org.marketdesignresearch.mechlib.domain.SimpleGood
import org.marketdesignresearch.mechlib.domain.auction.Auction
import org.marketdesignresearch.mechlib.domain.bidder.XORBidder
import org.marketdesignresearch.mechlib.mechanisms.MechanismType
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.*

data class AuctionSetting(val bidders: Set<XORBidder>, val goods: Set<SimpleGood>, val type: MechanismType)

@RestController
class AuctionsController {

    // FIXME: With the current AuctionSetting class, the response ends up as
    //  -> Content type 'application/json;charset=UTF-8' not supported
    //  (De)Serializing issue?
    @PostMapping("/auctions", consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun startAuction(@RequestBody body: AuctionSetting): UUID {
        return SessionManagement.create(Domain(body.bidders, body.goods), body.type)
    }

    @GetMapping("/auctions/{uuid}")
    fun getAuction(@PathVariable uuid: UUID): Auction? {
        return SessionManagement.get(uuid)
    }

}