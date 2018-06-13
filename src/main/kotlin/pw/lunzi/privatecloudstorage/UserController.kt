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
    data class UserItem(val username: String, val userURL: String, val isAdmin: Boolean)

    /**
     * 获取用户列表
     */
    @GetMapping("/api/users/{page}", "/api/users")
    fun getUserList(@PathVariable(required = false) page: Int?): ResponseEntity<List<UserItem>> {
        val index = page ?: 1
        userLog.info("Someone get user list page \"$index\"")
        val userList = userRepository.findAll()
        val msgList = mutableListOf<UserItem>()
        for (i in 20 * (index - 1) until 20 * index) {
            if (i >= userList.size) break
            msgList.add(UserItem(userList[i].username, "${Config.hostname}/api/items/${userList[i].username}", userList[i].username == "root"))
        }
        return ResponseEntity(msgList, HttpStatus.OK)
    }

    /**
     * 注册用户
     */
    @PostMapping("/api/user/{username}")
    fun register(@RequestBody password: PasswordMsg, @PathVariable username: String): ResponseEntity<ReplyMsg> {
        userLog.info("Someone try to register user \"$username\"")

        return if (userRepository.countByUsername(username) > 0) {
            userLog.warn("Someone register user \"$username\" failed. Username already exist")
            ResponseEntity(ReplyMsg(false, "Username already used"), HttpStatus.FORBIDDEN)
        } else {
            userRepository.save(User(username, password.password))
            val userRootPath = FileItem(
                    username,
                    true,
                    true,
                    "$username/",
                    virtualPath = "/",
                    virtualName = username,
                    isPublic = true
            )
            userRootPath.mkdir()
            fileItemRepository.save(userRootPath)
            userLog.info("Someone register user \"$username\" success")
            ResponseEntity(ReplyMsg(true, "Register success"), HttpStatus.OK)
        }
    }

    /**
     * 获取用户详细信息
     */
    @GetMapping("/api/user/{username}")
    fun getUser(@PathVariable username: String): Any {
        userLog.info("Someone try to get user info \"$username\"")
        val user: User? = userRepository.findByUsername(username)
        if (user == null) {
            userLog.warn("Someone get user info \"$username\" failed. User not found")
            return ResponseEntity(ReplyMsg(false, "User not found"), HttpStatus.NOT_FOUND)
        }

        val message = mapOf(
                "isExist" to true,
                "username" to username,
                "space" to user.space,
                "index" to "${Config.hostname}/api/items/$username"
        )
        userLog.info("Someone get user info \"$username\" success")
        return ResponseEntity(message, HttpStatus.OK)
    }

    /**
     * 注销用户（删除用户）
     */
    @PreAuthorize("hasAnyRole('ROLE_MEMBER','ROLE_ADMIN')")
    @DeleteMapping("/api/user/{username}")
    fun deleteUser(@PathVariable username: String, @RequestBody password: PasswordMsg, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<ReplyMsg> {
        userLog.info("User \"${userDetails.username}\" try to delete user \"$username\"")
        val user: User? = userRepository.findByUsername(username)

        //Check user
        if (user == null) {
            userLog.warn("User \"${userDetails.username}\" delete user \"$username\" failed. User not found")
            return ResponseEntity(ReplyMsg(false, "Username not found"), HttpStatus.NOT_FOUND)
        }

        //Check permission and delete all items which belong the user
        if (user.password == password.password || userDetails.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            var count = 0
            fileItemRepository.findByOwnerName(username).forEach {
                fileItemRepository.delete(it)
                count++
                Utils.updateSize(it, -1 * it.size, fileItemRepository)
                if (fileItemRepository.findByRealPath(it.realPath).isEmpty() && !it.isDictionary) it.deleteFile()
            }
            userRepository.delete(user)
            userLog.info("User \"${userDetails.username}\" delete user \"$username\" success. Delete total $count items.")
            return ResponseEntity(ReplyMsg(true, "Delete user and $count file items success"), HttpStatus.OK)
        }

        userLog.warn("User \"${userDetails.username}\" delete user \"$username\" failed. Password is invalid")
        return ResponseEntity(ReplyMsg(false, "Password is invalid"), HttpStatus.FORBIDDEN)
    }

    /**
     * 更改用户可用空间
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/api/user/{username}/space")
    fun editUserSpace(@PathVariable username: String, @RequestBody msg: SpaceMsg, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<ReplyMsg> {
        //Check user is exist
        val user = userRepository.findByUsername(username)
                ?: return ResponseEntity(ReplyMsg(false, "Username not found"), HttpStatus.NOT_FOUND)

        user.space = msg.space
        userRepository.save(user)
        userLog.info("User \"${userDetails.username}\" change user \"$username\"'s space to \"${msg.space}\"")
        return ResponseEntity(ReplyMsg(true, "Change $username space to ${msg.space}"), HttpStatus.OK)
    }

    /**
     * 修改用户密码
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MEMBER')")
    @PutMapping("/api/user/{username}/password")
    fun editUserPassword(@PathVariable username: String, @RequestBody msg: ChangePasswordMsg, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<ReplyMsg> {
        //Check user is exist
        val user = userRepository.findByUsername(username)
                ?: return ResponseEntity(ReplyMsg(false, "Username not found"), HttpStatus.NOT_FOUND)

        //Check permission
        if (!userDetails.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            if (username != userDetails.username) {
                return ResponseEntity(ReplyMsg(false, "You can just edit your own account"), HttpStatus.FORBIDDEN)
            } else if (msg.oldPassword != user.password) {
                return ResponseEntity(ReplyMsg(false, "Old password is wrong"), HttpStatus.FORBIDDEN)
            }
        }

        user.password = msg.newPassword
        userRepository.save(user)
        userLog.info("User \"${userDetails.username}\" change user \"$username\"'s password success.")
        return ResponseEntity(ReplyMsg(true, "Change $username password success"), HttpStatus.OK)
    }
}
