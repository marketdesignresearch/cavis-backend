package org.marketdesignresearch.cavisbackend

import org.marketdesignresearch.cavisbackend.mongo.AllocationConverter
import org.marketdesignresearch.cavisbackend.mongo.BidsConverter
import org.marketdesignresearch.cavisbackend.mongo.PaymentConverter
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.convert.converter.Converter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.util.*

@SpringBootApplication
@EnableSwagger2
class CavisBackendApplication {
    private val converters = ArrayList<Converter<*, *>>()

    @Bean
    fun api(): Docket = Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.any())
            .paths(PathSelectors.ant("/auctions/**"))
            .build()

    @Bean
    fun customConversions(): MongoCustomConversions {
        converters.add(AllocationConverter())
        converters.add(PaymentConverter())
        converters.add(BidsConverter())
        return MongoCustomConversions(converters)
    }

}

fun main(args: Array<String>) {
    runApplication<CavisBackendApplication>(*args)
}
