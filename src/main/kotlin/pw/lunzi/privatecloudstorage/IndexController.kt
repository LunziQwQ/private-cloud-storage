package pw.lunzi.privatecloudstorage

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*


@RestController
class IndexController(val fileItemRepository: FileItemRepository, val shareItemRepository: ShareItemRepository) {

    data class DataItem(val itemName: String,
                        val path: String,
                        val size: Long,
                        val isDictionary: Boolean,
                        val isPublic: Boolean,
                        val lastModified: Date)

    data class IndexMsg(val username: String, val path: String)
    data class GetShareUrlMsg(val path: String, val name: String, val username: String)

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
                        dataList.add(DataItem(it.virtualName, it.virtualPath, it.size, it.isDictionary, it.isPublic, it.lastModified))
                    }
                }
            }
        } else {
            if (user != null && superItem.ownerName == user.username) {
                fileItemList.forEach {
                    if (it.virtualPath == msg.path) {
                        dataList.add(DataItem(it.virtualName, it.virtualPath, it.size, it.isDictionary, it.isPublic, it.lastModified))
                    }
                }
            } else {
                return ReplyMsg(false, "Permission denied")
            }
        }
        return dataList
    }

    @PostMapping("getsharelink")
    fun getShareURL(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: GetShareUrlMsg): ReplyMsg {
        val fileItem = fileItemRepository.findByVirtualPathAndVirtualNameAndOwnerName(msg.path, msg.name, msg.username)
                ?: return ReplyMsg(false, "File is invalid")

        if (!fileItem.isPublic)
            return ReplyMsg(false, "File is not public")

        val shareItem = ShareItem(fileItem, sharedUserName = if (user == null) "guest" else user.username)
        shareItemRepository.save(shareItem)
        return ReplyMsg(true, shareItem.url.replace("\\\"", ""))
    }

    @GetMapping("share")
    fun getShareIndex(item: String): Any {
        val url = "${Config.hostname}/share?item=$item"
        val shareItem = shareItemRepository.findByUrl(url)
                ?: return ReplyMsg(false, "Share link is invalid")

        //Check the validity
        val now = Date()
        if ((now.time - shareItem.createTime.time) / (1000 * 60 * 60 * 24) > 30)
            return ReplyMsg(false, "Share link is expired")

        return DataItem(
                shareItem.item.virtualName,
                shareItem.item.virtualPath,
                shareItem.item.size,
                shareItem.item.isDictionary,
                shareItem.item.isPublic,
                shareItem.item.lastModified
        )
    }
}
