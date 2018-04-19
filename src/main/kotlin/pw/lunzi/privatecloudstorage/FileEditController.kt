package pw.lunzi.privatecloudstorage

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class FileEditController(private val fileItemRepo: FileItemRepository) {

    data class RenameMsg(val path: String, val name: String, val newName: String)
    data class DeleteMsg(val path: String, val name: String)
    data class MoveMsg(val path: String, val name: String, val newPath: String)
    data class ChangeAccessMsg(val path: String, val name: String, val isPublic: Boolean)
    data class TransferMsg(val path: String, val name: String, val newPath: String)
    data class MkdirMsg(val path: String, val name: String)

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("rename")
    fun rename(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: RenameMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(msg.path, msg.name, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")
        fileItem.virtualName = msg.newName
        fileItem.lastModified = Date()
        fileItemRepo.save(fileItem)
        return ReplyMsg(true, "Rename success")

    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("delete")
    fun delete(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: DeleteMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(msg.path, msg.name, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")

        if (fileItem.isDictionary) {
            TODO()
        } else {
            fileItem.deleteFile()
            fileItemRepo.delete(fileItem)
        }
        return ReplyMsg(true, "Delete success")

    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("move")
    fun move(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: MoveMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(msg.path, msg.name, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")

        //TODO("check the newPath is exist")

        if (fileItem.isDictionary) {
            TODO()
        } else {
            fileItem.virtualPath = msg.newPath
            fileItem.lastModified = Date()
            fileItemRepo.save(fileItem)
        }
        return ReplyMsg(true, "Move success")
    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("changeaccess")
    fun changeAccess(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: ChangeAccessMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(msg.path, msg.name, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")
        if (fileItem.isDictionary) {
            TODO()
        } else {
            fileItem.isPublic = msg.isPublic
            fileItem.lastModified = Date()
            fileItemRepo.save(fileItem)
        }

        return ReplyMsg(true, "Change access success")

    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("transfer")
    fun transfer(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: TransferMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(msg.path, msg.name, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")
        if (fileItem.ownerName == user.username) return ReplyMsg(false, "File is already belong you")

        if (fileItem.isDictionary) {
            TODO()
        } else {
            val newItem = fileItem.copy(ownerName = user.username, virtualPath = msg.newPath)

            if (fileItemRepo.countByVirtualPathAndOwnerName(msg.newPath, user.username) == 0L)
                return ReplyMsg(false, "Path is now exist")

            newItem.lastModified = Date()
            fileItemRepo.save(newItem)
        }

        return ReplyMsg(true, "Transfer success")
    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("mkdir")
    fun makeDir(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: MkdirMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")
        val testExist: FileItem? = fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(msg.path, msg.name, user.username)

        return if (testExist != null) ReplyMsg(false, "Dictionary is already exist")
        else {
            val newDir = FileItem(
                    user.username,
                    false,
                    true,
                    null,
                    virtualPath = msg.path,
                    virtualName = msg.name
            )
            fileItemRepo.save(newDir)
            ReplyMsg(true, "Create dictionary ${msg.path} success")
        }
    }
}