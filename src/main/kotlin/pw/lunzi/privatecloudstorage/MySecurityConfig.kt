package pw.lunzi.privatecloudstorage

import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
class MySecurityConfig(private val userRepository: UserRepository) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http
                .authorizeRequests()

                .and().formLogin()
                .loginProcessingUrl("/api/session").permitAll()
                .successHandler({ _: HttpServletRequest?, response: HttpServletResponse?, _: Authentication? ->
                    response!!.setHeader("Content-Type", "application/json;charset=utf-8")
                    response.status = HttpStatus.OK.value()
                    response.writer.print("{\"result\":true,\"message\":\"Login success\"}")
                    response.writer.flush()

                })
                .failureHandler({ _: HttpServletRequest?, response: HttpServletResponse?, _: AuthenticationException? ->
                    response!!.setHeader("Content-Type", "application/json;charset=utf-8")
                    response.status = HttpStatus.UNAUTHORIZED.value()
                    response.writer.print("{\"result\":false,\"message\":\"Username or password wrong\"}")
                    response.writer.flush()
                })

                .and().logout()
                .logoutRequestMatcher(AntPathRequestMatcher("/api/session", "DELETE"))
                .logoutSuccessHandler({ _: HttpServletRequest?, response: HttpServletResponse?, _: Authentication? ->
                    response!!.setHeader("Content-Type", "application/json;charset=utf-8")
                    response.status = HttpStatus.OK.value()
                    response.writer.print("{\"result\":true,\"message\":\"Logout success\"}")
                    response.writer.flush()
                })

                .and()
                .csrf().disable()
                .userDetailsService(MyUserDetailsService(userRepository))
    }
}