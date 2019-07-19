package org.marketdesignresearch.cavisbackend.domains

import org.marketdesignresearch.mechlib.domain.Domain
import org.spectrumauctions.sats.core.model.gsvm.GlobalSynergyValueModel
import org.spectrumauctions.sats.core.model.lsvm.LocalSynergyValueModel
import org.spectrumauctions.sats.mechanism.domains.GSVMDomain
import org.spectrumauctions.sats.mechanism.domains.LSVMDomain

/**
 * One straight-forward implementation for an additive value domain is to use OR-values on the individual goods.
 */
data class LSVMDomainWrapper(val numberOfNationalBidders: Int = 1, val numberOfRegionalBidders: Int = 5, val seed: Long = System.currentTimeMillis()): DomainWrapper {
    override fun toDomain(): Domain {
        val model = LocalSynergyValueModel()
        model.setNumberOfNationalBidders(numberOfNationalBidders)
        model.setNumberOfRegionalBidders(numberOfRegionalBidders)
        val bidders = model.createNewPopulation(seed)
        return LSVMDomain(bidders)
    }

    override fun getName() = "LSVM Domain"

}