package org.marketdesignresearch.cavisbackend.domains

import org.spectrumauctions.sats.core.model.lsvm.LocalSynergyValueModel
import org.spectrumauctions.sats.mechanism.domains.LSVMDomain

/**
 * One straight-forward implementation for an additive value domain is to use OR-values on the individual goods.
 */
data class LSVMDomainWrapper(val numberOfNationalBidders: Int = 1, val numberOfRegionalBidders: Int = 5): DomainWrapper {
    override fun toDomain(seed: Long): LSVMDomain {
        val model = LocalSynergyValueModel()
        model.setNumberOfNationalBidders(numberOfNationalBidders)
        model.setNumberOfRegionalBidders(numberOfRegionalBidders)
        val bidders = model.createNewPopulation(seed)
        return LSVMDomain(bidders)
    }

    override fun getName() = "LSVM Domain"

}