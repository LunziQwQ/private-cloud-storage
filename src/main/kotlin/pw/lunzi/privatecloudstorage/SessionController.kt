package pw.lunzi.privatecloudstorage

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SessionController {

    //Framework support the login and logout

    /*
    PostMapping("/api/session") -> login
        参数(x-www-form-urlencoded):
		key: username    value: xxxxxxx
        key: password    value: xxxxxxx
    */

    /*
    DeleteMapping("/api/session") -> logout
     */

    /**
     * 查看当前登录状态
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @GetMapping("/api/session")
    fun whoAmI(@AuthenticationPrincipal user: UserDetails?): Any {
        return ResponseEntity(user, HttpStatus.OK)
    }
}