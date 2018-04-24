package pw.lunzi.privatecloudstorage

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * ***********************************************
 * Created by Lunzi on 3/27/2018.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */
@RestController
class LoginController(private val userRepository: UserRepository, private val fileItemRepository: FileItemRepository) {

    data class RegisterMsg(val username: String, val password: String)

    @PostMapping("register")
    fun register(@RequestBody msg: RegisterMsg): ReplyMsg {
        return if (userRepository.countByUsername(msg.username) > 0) {
            ReplyMsg(false, "Username already used")
        } else {
            userRepository.save(User(msg.username, msg.password))
            val userRootPath = FileItem(
                    msg.username,
                    true,
                    true,
                    FileItem.rootPath + msg.username + "/",
                    virtualPath = "/",
                    virtualName = msg.username,
                    isPublic = true
            )
            userRootPath.mkdir()
            fileItemRepository.save(userRootPath)
            ReplyMsg(true, "Register success")
        }
    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @GetMapping("whoami")
    fun whoAmI(@AuthenticationPrincipal user: UserDetails?): UserDetails? {
        return user
    }
}
