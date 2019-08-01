package org.marketdesignresearch.cavisbackend.domains

import org.spectrumauctions.sats.core.model.mrvm.MultiRegionModel
import org.spectrumauctions.sats.mechanism.domains.MRVMDomain

/**
 * One straight-forward implementation for an additive value domain is to use OR-values on the individual goods.
 */
data class MRVMDomainWrapper(val numberOfNationalBidders: Int = 3, val numberOfRegionalBidders: Int = 4, val numberOfLocalBidders: Int = 3, val seed: Long = System.currentTimeMillis()): DomainWrapper {
    override fun toDomain(): MRVMDomain {
        val model = MultiRegionModel()
        model.setNumberOfNationalBidders(numberOfNationalBidders)
        model.setNumberOfRegionalBidders(numberOfRegionalBidders)
        model.setNumberOfLocalBidders(numberOfLocalBidders)
        val bidders = model.createNewPopulation(seed)
        return MRVMDomain(bidders)
    }

    override fun getName() = "MRVM Domain"

}