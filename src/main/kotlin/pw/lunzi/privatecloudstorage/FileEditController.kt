package pw.lunzi.privatecloudstorage

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class  FileEditController{

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("rename")
    fun rename(@AuthenticationPrincipal user: UserDetails?){
        TODO()
    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("delete")
    fun delete(@AuthenticationPrincipal user: UserDetails?){
        TODO()
    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("move")
    fun move(@AuthenticationPrincipal user: UserDetails?){
        TODO()
    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("changeaccess")
    fun changeAccess(@AuthenticationPrincipal user: UserDetails?){
        TODO()
    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("transfer")
    fun transfer(@AuthenticationPrincipal user: UserDetails?){
        TODO()
    }
}