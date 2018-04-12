package pw.lunzi.privatecloudstorage

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class IndexController{

    @PostMapping("index")
    fun getOtherIndex(@AuthenticationPrincipal user: UserDetails?, username: String) {
        TODO()

    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("myindex")
    fun getMyIndex(@AuthenticationPrincipal user: UserDetails?){
        TODO()
    }

    @PostMapping("getsharelink")
    fun getShareURL(@AuthenticationPrincipal user: UserDetails?){
        TODO()
    }

    @PostMapping("share")
    fun getShareIndex(){
        TODO()
    }


}
