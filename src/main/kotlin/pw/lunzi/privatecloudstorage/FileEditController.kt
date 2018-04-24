package pw.lunzi.privatecloudstorage

import com.sun.org.apache.xpath.internal.operations.Bool
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
    data class ChangeAccessMsg(val path: String, val name: String, val isPublic: Boolean, val allowRecursion:Boolean)
    data class TransferMsg(val path: String, val name: String, val newPath: String)
    data class MkdirMsg(val path: String, val name: String)

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("rename")
    fun rename(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: RenameMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(msg.path, msg.name, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")

        if (fileItem.isDictionary) {
            val oldPath: CharSequence = msg.path + msg.name
            fileItemRepo.findByOwnerName(user.username).forEach {
                if (it.virtualPath.contains(oldPath)) {
                    fileItemRepo.delete(it)
                    it.virtualPath = it.virtualPath.replaceFirst(oldPath.toString(), msg.path + msg.newName)
                    it.lastModified = Date()
                    fileItemRepo.save(it)
                }
            }
        }
        fileItemRepo.delete(fileItem)
        fileItem.virtualName = msg.newName
        fileItem.lastModified = Date()
        fileItemRepo.save(fileItem)

        return ReplyMsg(true, "Rename ${msg.path}${msg.name} to ${msg.path}${msg.newName} success")

    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("delete")
    fun delete(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: DeleteMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(msg.path, msg.name, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")

        if (fileItem.isDictionary) {
            fileItemRepo.delete(fileItem)
            var count = 1
            fileItemRepo.findByOwnerName(user.username).forEach {
                if (it.virtualPath.contains(msg.path + msg.name)) {
                    fileItemRepo.delete(it)
                    count++
                    if (fileItemRepo.findByRealPath(it.realPath).isEmpty() && !it.isDictionary) it.deleteFile()
                }
            }
            return ReplyMsg(true, "Delete folder ${fileItem.virtualPath}${fileItem.virtualName}/ total $count items success")
        } else {
            fileItemRepo.delete(fileItem)
            if (fileItemRepo.findByRealPath(fileItem.realPath).isEmpty()) fileItem.deleteFile()
            return ReplyMsg(true, "Delete ${fileItem.virtualPath}${fileItem.virtualName} success")
        }
    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("move")
    fun move(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: MoveMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        //Check origin file is legal
        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(msg.path, msg.name, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")

        //Check new path is legal
        val newPathStr = FileItem.getSuperPath(msg.newPath)
        val newPathName = FileItem.getSuperName(msg.newPath)
        if (fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(newPathStr, newPathName, user.username) == null) {
            return ReplyMsg(false, "Sorry. New path is invalid")
        }


        //Do move
        var count = 1
        if (fileItem.isDictionary) {
            fileItemRepo.findByOwnerName(user.username).forEach {
                if (it.virtualPath.contains(msg.path + msg.name)) {
                    count++
                    fileItemRepo.delete(it)
                    it.virtualPath = it.virtualPath.replaceFirst(msg.path + msg.name, msg.newPath + msg.name)
                    it.lastModified = Date()
                    fileItemRepo.save(it)
                }
            }
        }
        fileItemRepo.delete(fileItem)
        fileItem.virtualPath = msg.newPath
        fileItem.lastModified = Date()
        fileItemRepo.save(fileItem)

        return ReplyMsg(true, "Move ${msg.path}${msg.name} to ${msg.newPath}${msg.name} total $count ${if (count == 1) "item" else "items"} success")
    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("changeaccess")
    fun changeAccess(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: ChangeAccessMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        //Check origin file is legal
        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(msg.path, msg.name, user.username)
                ?: return ReplyMsg(false, "Sorry. File is invalid")

        //Do change
        var count = 1
        var temp = fileItem
        while (temp.virtualPath != "/") {
            temp = FileItem.getSuperItem(temp, fileItemRepo) ?: break
            if (temp.isPublic != msg.isPublic) {
                fileItemRepo.delete(temp)
                temp.isPublic = msg.isPublic
                temp.lastModified = Date()
                fileItemRepo.save(temp)
                count++
            }
        }

        if (fileItem.isDictionary && msg.allowRecursion) {
            fileItemRepo.findByOwnerName(user.username).forEach {
                if (it.virtualPath.contains(msg.path + msg.name)) {
                    fileItemRepo.delete(it)
                    it.isPublic = msg.isPublic
                    it.lastModified = Date()
                    fileItemRepo.save(it)
                    count++
                }
            }
        }
        fileItemRepo.delete(fileItem)
        fileItem.isPublic = msg.isPublic
        fileItem.lastModified = Date()
        fileItemRepo.save(fileItem)

        return ReplyMsg(true, "Change ${msg.path}${msg.name} total $count ${if (count == 1) "item" else "items"} access success")

    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("transfer")
    fun transfer(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: TransferMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permission denied")

        //Check origin file is legal
        val fileItem: FileItem = fileItemRepo.findByVirtualPathAndVirtualName(msg.path, msg.name)
                ?: return ReplyMsg(false, "Sorry. File is invalid")

        //Check the file is already belong you
        if (fileItem.ownerName == user.username) return ReplyMsg(false, "File is already belong you")

        //Check new path is legal
        if (fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(
                        FileItem.getSuperPath(msg.newPath),
                        FileItem.getSuperName(msg.newPath),
                        user.username) == null) {
            return ReplyMsg(false, "New path is invalid")
        }

        //Do transfer
        var count = 1
        if (fileItem.isDictionary) {
            fileItemRepo.findByOwnerName(fileItem.ownerName).forEach {
                if (it.virtualPath.contains(msg.path + msg.name)) {
                    fileItemRepo.save(FileItem(
                            user.username,
                            false,
                            it.isDictionary,
                            it.realPath,
                            it.size,
                            it.virtualPath.replace(msg.path, msg.newPath),
                            it.virtualName,
                            it.isPublic,
                            Date()
                    ))
                    count++
                }
            }
        }
        fileItemRepo.save(FileItem(
                user.username,
                false,
                fileItem.isDictionary,
                fileItem.realPath,
                fileItem.size,
                fileItem.virtualPath.replace(msg.path, msg.newPath),
                fileItem.virtualName,
                fileItem.isPublic,
                Date()
        ))

        return ReplyMsg(true, "Transfer ${msg.path}${msg.name} to ${msg.newPath}${msg.name} total $count ${if (count == 1) "item" else "items"} success")
    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("mkdir")
    fun makeDir(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: MkdirMsg): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        //Check folder is not exist
        val testExist: FileItem? = fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(msg.path, msg.name, user.username)
        if (testExist != null) return ReplyMsg(false, "Dictionary is already exist")

        //Check super path is legal
        if (fileItemRepo.findByVirtualPathAndVirtualNameAndOwnerName(
                        FileItem.getSuperPath(msg.path),
                        FileItem.getSuperName(msg.path),
                        user.username) == null) {
            return ReplyMsg(false, "Super path not exist")
        }

        val newDir = FileItem(
                user.username,
                false,
                true,
                null,
                virtualPath = msg.path,
                virtualName = msg.name
        )
        fileItemRepo.save(newDir)
        return ReplyMsg(true, "Create dictionary ${msg.path}${msg.name} success")

    }
}