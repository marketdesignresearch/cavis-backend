package org.marketdesignresearch.cavisbackend

import org.marketdesignresearch.mechlib.domain.*
import org.marketdesignresearch.mechlib.domain.bidder.SimpleBidder
import org.marketdesignresearch.mechlib.mechanisms.vcg.ORVCGAuction
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.math.BigDecimal

@SpringBootApplication
class CavisBackendApplication {

    private val logger = LoggerFactory.getLogger(CavisBackendApplication::class.java)

    @Bean
    fun init() = ApplicationRunner {
        logger.info("CPLEX_STUDIO_DIR129: {}", System.getenv("CPLEX_STUDIO_DIR129"))
        logger.info("CPLEX_STUDIO_KEY set: {}", if (System.getenv("CPLEX_STUDIO_KEY") != null) "True" else "False")

        logger.info("For now, let's just run a test to make sure all dependencies are in place...")

        val A: Good = SimpleGood("0")
        val B: Good = SimpleGood("1")
        val C: Good = SimpleGood("2")
        val D: Good = SimpleGood("3")

        val bid1 = BundleBid(BigDecimal.valueOf(2), setOf(A), "1")
        val bid2 = BundleBid(BigDecimal.valueOf(3), setOf(A, B, D), "2")
        val bid3 = BundleBid(BigDecimal.valueOf(2), setOf(B, C), "3")
        val bid4 = BundleBid(BigDecimal.valueOf(1), setOf(C, D), "4")
        val bids = Bids()
        bids.setBid(SimpleBidder("B" + 1), Bid(setOf(bid1)))
        bids.setBid(SimpleBidder("B" + 2), Bid(setOf(bid2)))
        bids.setBid(SimpleBidder("B" + 3), Bid(setOf(bid3)))
        bids.setBid(SimpleBidder("B" + 4), Bid(setOf(bid4)))

        val am = ORVCGAuction(bids)
        val payment = am.payment

        println("Total allocation value: ${am.allocation.totalAllocationValue.toDouble()}")
        for (i in 1..4) {
            println("Payment of bidder $i: ${payment.paymentOf(SimpleBidder("B$i")).amount.toDouble()}")
        }
    }

}

fun main(args: Array<String>) {
    runApplication<CavisBackendApplication>(*args)
}
