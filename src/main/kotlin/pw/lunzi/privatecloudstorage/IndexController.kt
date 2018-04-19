package pw.lunzi.privatecloudstorage

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*


@RestController
class IndexController{

    data class DataItem(val itemName: String, val size: Long, val isDictionary: Boolean, val isPublic: Boolean, val lastModified: Date)
    data class FileMsg(val fileArray: Array<DataItem>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FileMsg) return false

            if (!Arrays.equals(fileArray, other.fileArray)) return false

            return true
        }

        override fun hashCode(): Int {
            return Arrays.hashCode(fileArray)
        }
    }

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
