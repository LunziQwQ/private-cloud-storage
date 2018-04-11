package pw.lunzi.privatecloudstorage

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
class LoginController(private val userRepository: UserRepository) {

    data class RegisterMsg(val username: String, val password: String)
    data class ReplyMsg(val result: Boolean, val message: String)


    @PostMapping("register")
    fun register(@RequestBody msg: RegisterMsg): ReplyMsg {
        if (userRepository.countByUsername(msg.username) > 0) {
            return ReplyMsg(false, "Username already used")
        } else {
            userRepository.save(User(msg.username, msg.password))
            return ReplyMsg(true, "Register success")
        }
    }
}
