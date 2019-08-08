package org.marketdesignresearch.cavisbackend

import org.marketdesignresearch.cavisbackend.mongo.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2


@SpringBootApplication
@EnableSwagger2
class CavisBackendApplication {

    @Bean
    fun api(): Docket = Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.any())
            .paths(PathSelectors.ant("/auctions/**"))
            .build()

    @Bean
    fun customConversions() = MongoCustomConversions(listOf(
            SATSGoodToDocumentConverter(),
            DocumentToSATSGoodConverter(),
            GSVMBidderToDocumentConverter()
    ))

}

fun main(args: Array<String>) {
    runApplication<CavisBackendApplication>(*args)
}
