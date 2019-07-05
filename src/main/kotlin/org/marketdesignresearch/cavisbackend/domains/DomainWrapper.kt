package org.marketdesignresearch.cavisbackend.domains

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.marketdesignresearch.mechlib.domain.Domain

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes(
    Type(value = AdditiveValueDomainWrapper::class, name = "additiveValue"),
    Type(value = UnitDemandValueDomainWrapper::class, name = "unitDemandValue"),
    Type(value = GSVMDomainWrapper::class, name = "gsvm")
)
interface DomainWrapper {
    fun toDomain(): Domain
}