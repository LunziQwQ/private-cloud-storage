package pw.lunzi.privatecloudstorage

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
class SecurityConfig(private val userRepository: UserRepository) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http
                .csrf().disable()
//                .authorizeRequests()
//                .antMatchers("/api/admin/**").hasRole("OKK")
//                .anyRequest().permitAll()
//                .and()
                .formLogin()
                .loginPage("/login.html")
                .defaultSuccessUrl("/")
                .and().userDetailsService(MyUserDetailsService(userRepository))
    }
}