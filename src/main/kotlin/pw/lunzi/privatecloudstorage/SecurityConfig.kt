package pw.lunzi.privatecloudstorage

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfig : WebSecurityConfigurerAdapter() {


    override fun configure(http: HttpSecurity) {
        http
                .authorizeRequests()
                .antMatchers("/api/admin/**").hasRole("OKK")
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .and().userDetailsService(MyUserDetailsService())
    }
}