package pw.lunzi.privatecloudstorage

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

/**
 * ***********************************************
 * Created by Lunzi on 4/2/2018.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */
class MyUserDetailsService(private val userRepository: UserRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        if (userRepository.countByUsername(username) > 0) {
            val user = userRepository.findByUsername(username)!!
            return User(username, ("{noop}" + user.password), (
                    if (username == "root") listOf("ROLE_MEMBER", "ROLE_ADMIN")
                    else listOf("ROLE_MEMBER")
                    ).map { SimpleGrantedAuthority(it) })
        } else {
            throw UsernameNotFoundException("User not exist")
        }
    }
}