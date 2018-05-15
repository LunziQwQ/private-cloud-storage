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

    @GetMapping("/api/sharelink/{username}/**")
    fun getShareURL(@AuthenticationPrincipal user: UserDetails?, @PathVariable username: String, request: HttpServletRequest): ResponseEntity<ReplyMsg> {
        val path = if (Utils.extractPathFromPattern(request).isEmpty()) "/$username/" else "/$username${Utils.extractPathFromPattern(request)}"

        val fileItem = fileItemRepository.findByVirtualPathAndVirtualName(Utils.getPath(path), Utils.getName(path))
                ?: return ResponseEntity(ReplyMsg(false, "File is invalid"), HttpStatus.NOT_FOUND)

        if (!fileItem.isPublic && (user == null || (fileItem.ownerName != user.username && !user.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))))) {
            return ResponseEntity(ReplyMsg(false, "Permission denied"), HttpStatus.FORBIDDEN)
        }

        val shareItem = ShareItem(fileItem, sharedUserName = if (user == null) "guest" else user.username)
        shareItemRepository.save(shareItem)
        return ResponseEntity(ReplyMsg(true, shareItem.url), HttpStatus.OK)
    }

    @GetMapping("/api/shareitem/{item}")
    fun getShareIndex(@PathVariable item: String): Any {
        val url = "${Config.hostname}/api/shareitem/$item"
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