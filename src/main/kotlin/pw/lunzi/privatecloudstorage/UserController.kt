package pw.lunzi.privatecloudstorage

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

/**
 * ***********************************************
 * Created by Lunzi on 3/27/2018.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */
@RestController
class UserController(private val userRepository: UserRepository, private val fileItemRepository: FileItemRepository) {

    data class PasswordMsg(val password: String)
    data class SpaceMsg(val space: Int)
    data class ChangePasswordMsg(val oldPassword: String, val newPassword: String)

    @PostMapping("/api/user/{username}")
    fun register(@RequestBody password: PasswordMsg, @PathVariable username: String): ResponseEntity<ReplyMsg> {
        return if (userRepository.countByUsername(username) > 0) {
            ResponseEntity(ReplyMsg(false, "Username already used"), HttpStatus.FORBIDDEN)
        } else {
            userRepository.save(User(username, password.password))
            val userRootPath = FileItem(
                    username,
                    true,
                    true,
                    FileItem.rootPath + username + "/",
                    virtualPath = "/",
                    virtualName = username,
                    isPublic = true
            )
            userRootPath.mkdir()
            fileItemRepository.save(userRootPath)
            ResponseEntity(ReplyMsg(true, "Register success"), HttpStatus.OK)
        }
    }

    @GetMapping("/api/user/{username}")
    fun getUser(@PathVariable username: String): Any {
        val user = userRepository.findByUsername(username)
                ?: return ResponseEntity(ReplyMsg(false, "User not found"), HttpStatus.NOT_FOUND)
        val message = mapOf(
                "isExist" to true,
                "username" to username,
                "space" to user.space,
                "index" to "${Config.hostname}/api/items/$username"
        )
        return ResponseEntity(message, HttpStatus.OK)
    }

    @PreAuthorize("hasAnyRole('ROLE_MEMBER','ROLE_ADMIN')")
    @DeleteMapping("/api/user/{username}")
    fun deleteUser(@PathVariable username: String, @RequestBody password: PasswordMsg, @AuthenticationPrincipal userDetails: UserDetails?): ResponseEntity<ReplyMsg> {
        if (userDetails == null) return ResponseEntity(ReplyMsg(false, "You are not login"), HttpStatus.UNAUTHORIZED)
        val user = userRepository.findByUsername(username)
                ?: return ResponseEntity(ReplyMsg(false, "Username not found"), HttpStatus.NOT_FOUND)

        if (user.password == password.password || userDetails.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            var count = 0
            fileItemRepository.findByOwnerName(username).forEach {
                fileItemRepository.delete(it)
                count++
                Utils.updateSize(it, -1 * it.size, fileItemRepository)
                if (fileItemRepository.findByRealPath(it.realPath).isEmpty() && !it.isDictionary) it.deleteFile()
            }
            userRepository.delete(user)
            return ResponseEntity(ReplyMsg(true, "Delete user and $count file items success"), HttpStatus.OK)
        }
        return ResponseEntity(ReplyMsg(false, "Password is invalid"), HttpStatus.FORBIDDEN)
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/api/user/{username}/space")
    fun editUserSpace(@PathVariable username: String, @RequestBody msg: SpaceMsg, @AuthenticationPrincipal userDetails: UserDetails?): ResponseEntity<ReplyMsg> {
        if (userDetails == null || !userDetails.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN")))
            return ResponseEntity(ReplyMsg(false, "Permission denied"), HttpStatus.FORBIDDEN)
        val user = userRepository.findByUsername(username)
                ?: return ResponseEntity(ReplyMsg(false, "Username not found"), HttpStatus.NOT_FOUND)
        user.space = msg.space
        userRepository.save(user)
        return ResponseEntity(ReplyMsg(true, "Change $username space to ${msg.space}"), HttpStatus.OK)
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MEMBER')")
    @PutMapping("/api/user/{username}/password")
    fun editUserPassword(@PathVariable username: String, @RequestBody msg: ChangePasswordMsg, @AuthenticationPrincipal userDetails: UserDetails?): ResponseEntity<ReplyMsg> {
        if (userDetails == null)
            return ResponseEntity(ReplyMsg(false, "You are not login"), HttpStatus.FORBIDDEN)
        val user = userRepository.findByUsername(username)
                ?: return ResponseEntity(ReplyMsg(false, "Username not found"), HttpStatus.NOT_FOUND)

        if (!userDetails.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            if (username != userDetails.username) {
                return ResponseEntity(ReplyMsg(false, "You can just edit your own account"), HttpStatus.FORBIDDEN)
            } else if (msg.oldPassword != user.password) {
                return ResponseEntity(ReplyMsg(false, "Old password is wrong"), HttpStatus.FORBIDDEN)
            }
        }

        user.password = msg.newPassword
        userRepository.save(user)
        return ResponseEntity(ReplyMsg(true, "Change $username password success"), HttpStatus.OK)
    }
}
