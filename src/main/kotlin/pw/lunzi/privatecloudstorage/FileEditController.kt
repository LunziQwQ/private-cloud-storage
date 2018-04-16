package pw.lunzi.privatecloudstorage

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class FileEditController(private val fileItemRepo: FileItemRepository) {

    data class RenameMsg(val path: String, val newName: String)
    data class DeleteMsg(val path: String)
    data class MoveMsg(val path: String, val newPath: String)
    data class ChangeAccessMsg(val path: String, val isPublic: Boolean)
    data class TransferMsg(val path: String, val newPath: String)

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("rename")
    fun rename(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: RenameMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndOwnerName(msg.path, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")
        fileItem.virtualName = msg.newName
        fileItemRepo.save(fileItem)
        return ReplyMsg(true, "Rename success")

    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("delete")
    fun delete(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: DeleteMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndOwnerName(msg.path, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")
        fileItemRepo.delete(fileItem)
        return ReplyMsg(true, "Delete success")

    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("move")
    fun move(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: MoveMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndOwnerName(msg.path, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")
        fileItem.virtualPath = msg.newPath
        fileItemRepo.save(fileItem)
        return ReplyMsg(true, "Move success")

    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("changeaccess")
    fun changeAccess(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: ChangeAccessMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndOwnerName(msg.path, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")
        fileItem.isPublic = msg.isPublic
        fileItemRepo.save(fileItem)
        return ReplyMsg(true, "Change access success")

    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("transfer")
    fun transfer(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: TransferMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndOwnerName(msg.path, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")
        if (fileItem.ownerName == user.username) return ReplyMsg(false, "File is already belong you")
        val newItem = fileItem.copy(ownerName = user.username, virtualPath = msg.newPath)

        if (fileItemRepo.countByVirtualPathAndOwnerName(msg.newPath, user.username) > 0)
            return ReplyMsg(false, "Path is already used")

        fileItemRepo.save(newItem)
        return ReplyMsg(true, "Transfer success")
    }
}