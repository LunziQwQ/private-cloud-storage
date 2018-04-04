package pw.lunzi.privatecloudstorage

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService

/**
 * ***********************************************
 * Created by Lunzi on 4/2/2018.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */
class MyUserDetailsService : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        return User(username, "{noop}1123", listOf("ROLE_ADMIN", "ROLE_SADMIN").map { SimpleGrantedAuthority(it) })
    }

}