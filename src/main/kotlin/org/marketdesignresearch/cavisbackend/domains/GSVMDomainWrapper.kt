package org.marketdesignresearch.cavisbackend.domains

import org.marketdesignresearch.mechlib.domain.Domain
import org.spectrumauctions.sats.core.model.gsvm.GlobalSynergyValueModel
import org.spectrumauctions.sats.mechanism.domains.GSVMDomain

/**
 * One straight-forward implementation for an additive value domain is to use OR-values on the individual goods.
 */
data class GSVMDomainWrapper(val numberOfRegionalBidders: Int = 6, val numberOfNationalBidders: Int = 1, val seed: Long = System.currentTimeMillis()): DomainWrapper {
    override fun toDomain(): Domain {
        val model = GlobalSynergyValueModel()
        model.setNumberOfRegionalBidders(numberOfRegionalBidders)
        model.setNumberOfNationalBidders(numberOfNationalBidders)
        val bidders = model.createNewPopulation(seed)
        return GSVMDomain(bidders)
    }
}