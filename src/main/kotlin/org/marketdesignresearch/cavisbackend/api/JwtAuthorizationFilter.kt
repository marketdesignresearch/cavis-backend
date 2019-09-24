package org.marketdesignresearch.cavisbackend.api

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtAuthorizationFilter(authenticationManager: AuthenticationManager) : BasicAuthenticationFilter(authenticationManager) {

    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse,
                                  filterChain: FilterChain) {
        val header: String? = request.getHeader("Authorization")
        if (header == null || !header.startsWith("Bearer ")) { // No valid Authorization supplied
            SecurityContextHolder.getContext().authentication = AnonymousAuthenticationToken("anonymous", "anonymous", setOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))
        } else {
            try {
                val jwt = JwtDecoders.fromOidcIssuerLocation("https://accounts.google.com").decode(header.replace("Bearer ", ""))
                // TODO: Transform this into an Oauth2 (?) token
                SecurityContextHolder.getContext().authentication = AnonymousAuthenticationToken(jwt.claims["sub"] as String, jwt.claims["sub"], setOf(SimpleGrantedAuthority("ROLE_IDENTIFIED")))
            } catch (e: JwtException) {
                throw AccessDeniedException("You provided an invalid authentication token.", e)
            }
        }
        filterChain.doFilter(request, response)
    }
}
