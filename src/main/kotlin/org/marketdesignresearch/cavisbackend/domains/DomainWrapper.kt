package org.marketdesignresearch.cavisbackend.domains

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.marketdesignresearch.mechlib.core.Domain

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes(
    Type(value = AdditiveValueDomainWrapper::class, name = "additiveValue"),
    Type(value = UnitDemandValueDomainWrapper::class, name = "unitDemandValue"),
    Type(value = SynergyDomainWrapper::class, name = "synergy"),
    Type(value = LLGDomainWrapper::class, name = "llg"),
    Type(value = GSVMDomainWrapper::class, name = "gsvm"),
    Type(value = LSVMDomainWrapper::class, name = "lsvm"),
    Type(value = MRVMDomainWrapper::class, name = "mrvm")
)
interface DomainWrapper {
    fun toDomain(): Domain
    fun getName(): String
}