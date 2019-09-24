package org.marketdesignresearch.cavisbackend

import org.marketdesignresearch.cavisbackend.api.JwtAuthorizationFilter
import org.marketdesignresearch.cavisbackend.mongo.AuctionWrapperDAO
import org.marketdesignresearch.cavisbackend.mongo.DocumentToSATSGoodConverter
import org.marketdesignresearch.cavisbackend.mongo.GSVMBidderToDocumentConverter
import org.marketdesignresearch.cavisbackend.mongo.SATSGoodToDocumentConverter
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.web.servlet.HandlerExceptionResolver
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

    @Bean
    fun init(auctionWrapperDAO: AuctionWrapperDAO) = ApplicationRunner {
        SessionManagement.loadAll(auctionWrapperDAO.findAllActiveIsTrueWithoutSATS())
    }

    @Bean
    fun sentryExceptionResolver(): HandlerExceptionResolver {
        return io.sentry.spring.SentryExceptionResolver()
    }

    @Bean
    fun sentryServletContextInitializer(): ServletContextInitializer {
        return io.sentry.spring.SentryServletContextInitializer()
    }

    @Configuration
    @EnableWebSecurity
    @EnableGlobalMethodSecurity(securedEnabled = true)
    class SecurityConfiguration : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http
                    .csrf().disable()
                    .authorizeRequests()
                    .anyRequest().anonymous()
                    .and()
                    .addFilter(JwtAuthorizationFilter(authenticationManager()))
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }

        //override fun configure(web: WebSecurity) {
        //    web.ignoring().antMatchers("/auctions/**")
        //}
        @Throws(Exception::class)
        override fun configure(auth: AuthenticationManagerBuilder) {
            auth.inMemoryAuthentication()
        }
    }



}

fun main(args: Array<String>) {
    runApplication<CavisBackendApplication>(*args)
}
