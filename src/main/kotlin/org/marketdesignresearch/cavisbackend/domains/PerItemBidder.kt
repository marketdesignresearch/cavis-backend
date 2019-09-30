package org.marketdesignresearch.cavisbackend.domains

data class PerItemBidder(val name: String, val min: Int = 0, val max: Int = 100000)