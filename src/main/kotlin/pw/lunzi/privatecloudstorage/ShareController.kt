package pw.lunzi.privatecloudstorage

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
class ShareController(val shareItemRepository: ShareItemRepository, val fileItemRepository: FileItemRepository) {

    //分享Item，获取分享URL
    @GetMapping("/api/sharelink/{username}/**")
    fun getShareURL(@AuthenticationPrincipal user: UserDetails?, @PathVariable username: String, request: HttpServletRequest): ResponseEntity<ReplyMsg> {
        //获取URL中的**部分
        val path = if (Utils.extractPathFromPattern(request).isEmpty()) "/$username/" else "/$username${Utils.extractPathFromPattern(request)}"

        shareItemLog.info("User \"${if (user != null) user.username else "Guest"}\" try to share item \"$path\"")

        //Check item exist
        val fileItem: FileItem? = fileItemRepository.findByVirtualPathAndVirtualName(Utils.getPath(path), Utils.getName(path))
        if (fileItem == null) {
            shareItemLog.warn("User \"${if (user != null) user.username else "Guest"}\" share item \"$path\" failed. Item is invalid")
            return ResponseEntity(ReplyMsg(false, "File is invalid"), HttpStatus.NOT_FOUND)
        }

        //Check permission
        if (!fileItem.isPublic && (user == null || (fileItem.ownerName != user.username && !user.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))))) {
            shareItemLog.warn("User \"${if (user != null) user.username else "Guest"}\" share item \"$path\" failed. Permission denied")
            return ResponseEntity(ReplyMsg(false, "Permission denied"), HttpStatus.FORBIDDEN)
        }

        //Do share
        val shareItem = ShareItem(fileItem, sharedUserName = if (user == null) "Guest" else user.username)
        shareItemRepository.save(shareItem)

        shareItemLog.info("User \"${if (user != null) user.username else "Guest"}\" share item \"$path\" success. URL: \"${shareItem.url}\"")
        return ResponseEntity(ReplyMsg(true, shareItem.url), HttpStatus.OK)
    }

    /**
     * 通过ShareURL获取Item
     */
    @GetMapping("/api/shareitem/{item}")
    fun getShareIndex(@PathVariable item: String): Any {
        //通过参数构建URL
        val url = "${Config.hostname}/api/shareitem/$item"

        shareItemLog.info("Someone get item by URL: \"$url\"")

        //通过URL获取ShareItem
        val shareItem = shareItemRepository.findByUrl(url)
                ?: return ResponseEntity(ReplyMsg(false, "Share link is invalid"), HttpStatus.NOT_FOUND)

        //Check item have delete
        if (!fileItemRepository.existsById(shareItem.item.id.toLong())) {
            return ResponseEntity(ReplyMsg(false, "Share file is already be deleted"), HttpStatus.GONE)
        }

        //Check the validity
        val now = Date()
        if ((now.time - shareItem.createTime.time) / (1000 * 60 * 60 * 24) > 30)
            return ResponseEntity(ReplyMsg(false, "Share link is expired"), HttpStatus.GONE)

        return ResponseEntity(ItemController.DataItem(
                shareItem.item.virtualName,
                shareItem.item.virtualPath,
                shareItem.item.size,
                shareItem.item.isDictionary,
                shareItem.item.isPublic,
                shareItem.item.lastModified
        ), HttpStatus.OK)
    }
}