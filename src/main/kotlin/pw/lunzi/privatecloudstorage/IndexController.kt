package pw.lunzi.privatecloudstorage

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.util.*
import javax.xml.crypto.Data


@RestController
class IndexController(val fileItemRepository: FileItemRepository){

    data class DataItem(val itemName: String,
                        val size: Long,
                        val isDictionary: Boolean,
                        val isPublic: Boolean,
                        val lastModified: Date)
    data class IndexMsg(val username: String, val path: String)


    @PostMapping("index")
    fun getIndex(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: IndexMsg): Any {
        val superItem = FileItem.getSuperItem(msg.path, fileItemRepository)
                ?: return ReplyMsg(false, "Path is invalid")

        val fileItemList = fileItemRepository.findByOwnerName(msg.username)

        val dataList = mutableListOf<DataItem>()

        if (superItem.isPublic) {
            fileItemList.forEach {
                if (it.virtualPath == msg.path) {
                    if (it.isPublic || (!it.isPublic && user != null && user.username == it.ownerName)) {
                        dataList.add(DataItem(it.virtualName, it.size, it.isDictionary, it.isPublic, it.lastModified))
                    }
                }
            }
        } else {
            if (user != null && superItem.ownerName == user.username) {
                fileItemList.forEach {
                    if (it.virtualPath == msg.path) {
                        dataList.add(DataItem(it.virtualName, it.size, it.isDictionary, it.isPublic, it.lastModified))
                    }
                }
            } else {
                return ReplyMsg(false, "Permission denied")
            }
        }
        return dataList
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
