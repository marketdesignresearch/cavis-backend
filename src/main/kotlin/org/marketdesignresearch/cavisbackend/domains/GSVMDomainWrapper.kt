package org.marketdesignresearch.cavisbackend.domains

import org.marketdesignresearch.mechlib.domain.Domain
import org.spectrumauctions.sats.core.model.gsvm.GlobalSynergyValueModel
import org.spectrumauctions.sats.mechanism.domains.GSVMDomain

/**
 * One straight-forward implementation for an additive value domain is to use OR-values on the individual goods.
 */
data class GSVMDomainWrapper(val numberOfNationalBidders: Int = 1, val numberOfRegionalBidders: Int = 6, val seed: Long = System.currentTimeMillis()): DomainWrapper {
    override fun toDomain(): GSVMDomain {
        val model = GlobalSynergyValueModel()
        model.setNumberOfNationalBidders(numberOfNationalBidders)
        model.setNumberOfRegionalBidders(numberOfRegionalBidders)
        val bidders = model.createNewPopulation(seed)
        return GSVMDomain(bidders)
    }

    override fun getName() = "GSVM Domain"

}