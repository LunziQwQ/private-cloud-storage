package pw.lunzi.privatecloudstorage

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
class ItemController(private val fileItemRepo: FileItemRepository) {

    /**
     * 对应不同请求的不同参数结构 ---------------------------------------
     * 用于SpringBoot自动解析Json
     */
    data class RenameMsg(val newName: String)

    data class MoveMsg(val newPath: String)
    data class ChangeAccessMsg(val isPublic: Boolean, val allowRecursion: Boolean)
    data class TransferMsg(val path: String, val name: String)
    data class DataItem(val itemName: String,
                        val path: String,
                        val size: Long,
                        val isDictionary: Boolean,
                        val isPublic: Boolean,
                        val lastModified: Date)

    /**
     * 目录下的Item列表
     */
    @GetMapping("/api/items/{username}/**")
    fun getItems(@AuthenticationPrincipal user: UserDetails?, @PathVariable username: String, request: HttpServletRequest): Any {
        //获取URL中的**部分
        val path = if (Utils.extractPathFromPattern(request).isEmpty()) "/$username/" else "/$username${Utils.extractPathFromPattern(request)}"
        itemEditLog.info("User \"${if (user != null) user.username else "Guest"}\" try to get index \"$path\".")

        //检查目录是否存在
        val superItem: FileItem? = Utils.getSuperItem(path, fileItemRepo)
        if (superItem == null) {
            itemEditLog.warn("User \"${if (user != null) user.username else "Guest"}\" Get index from path \"$path\" failed. Path is invalid")
            return ResponseEntity(ReplyMsg(false, "item is invalid"), HttpStatus.NOT_FOUND)
        }

        //获取该用户的Item，筛选是否在请求的目录下，将符合的加入返回列表dataList
        val fileItemList = fileItemRepo.findByOwnerName(username)
        val dataList = mutableListOf<DataItem>()

        //检查目录是否Public
        if (superItem.isPublic) {
            fileItemList.forEach {
                if (it.virtualPath == path) {
                    if (it.isPublic || (!it.isPublic && user != null && user.username == it.ownerName)) {
                        dataList.add(DataItem(it.virtualName, it.virtualPath, it.size, it.isDictionary, it.isPublic, it.lastModified))
                    }
                }
            }
        } else {
            //检查请求者是否拥有目录权限
            if (user != null && (superItem.ownerName == user.username || user.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN")))) {
                fileItemList.forEach {
                    if (it.virtualPath == path) {
                        dataList.add(DataItem(it.virtualName, it.virtualPath, it.size, it.isDictionary, it.isPublic, it.lastModified))
                    }
                }
            } else {
                itemEditLog.warn("User \"${if (user != null) user.username else "Guest"}\" get index \"$path\" failed. Permission denied")
                return ResponseEntity(ReplyMsg(false, "Permission denied"), HttpStatus.FORBIDDEN)
            }
        }
        itemEditLog.info("User \"${if (user != null) user.username else "Guest"}\" get index \"$path\" success.")
        return ResponseEntity(dataList, HttpStatus.OK)
    }

    /**
     * 重命名Item
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PutMapping("/api/item/**/name")
    fun rename(@AuthenticationPrincipal user: UserDetails, @RequestBody msg: RenameMsg, request: HttpServletRequest): ResponseEntity<ReplyMsg> {
        //获取URL中的**部分，即完整路径
        val completePath = Utils.getPath(Utils.extractPathFromPattern(request))
        val path = Utils.getPath(completePath)
        val name = Utils.getName(completePath)
        itemEditLog.info("User \"${user.username}\" try to rename item \"$path$name\" to $path${msg.newName}.")

        //获取对应路径的Item
        val fileItem: FileItem? = fileItemRepo.findByVirtualPathAndVirtualName(path, name)
        if (fileItem == null) {
            itemEditLog.warn("User \"${user.username}\" Rename item \"$path$name\" failed. Path is invalid")
            return ResponseEntity(ReplyMsg(false, "Sorry. Item is invalid"), HttpStatus.NOT_FOUND)
        }

        //若未登录或未拥有修改Item的权限
        if (fileItem.ownerName != user.username && !user.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            itemEditLog.warn("User \"${user.username}\" Rename item \"$path$name\" failed. Permission denied")
            return ResponseEntity(ReplyMsg(false, "Permission denied"), HttpStatus.FORBIDDEN)
        }

        //若重命名的为文件夹，递归更新子item路径
        if (fileItem.isDictionary) {
            val oldPath: CharSequence = path + name
            fileItemRepo.findByOwnerName(user.username).forEach {
                if (it.virtualPath.contains(oldPath)) {
                    fileItemRepo.delete(it)
                    it.virtualPath = it.virtualPath.replaceFirst(oldPath.toString(), path + msg.newName)
                    it.lastModified = Date()
                    fileItemRepo.save(it)
                }
            }
        }
        fileItemRepo.delete(fileItem)
        fileItem.virtualName = msg.newName
        fileItem.lastModified = Date()
        fileItemRepo.save(fileItem)

        itemEditLog.info("User \"${user.username}\" rename item \"$path$name\" to $path${msg.newName} success.")
        return ResponseEntity(ReplyMsg(true, "Rename $path$name to $path${msg.newName} success"), HttpStatus.OK)
    }

    /**
     * 删除Item
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @DeleteMapping("/api/item/**")
    fun delete(@AuthenticationPrincipal user: UserDetails, request: HttpServletRequest): ResponseEntity<ReplyMsg> {
        //获取URL中的**部分路径
        val completePath = Utils.extractPathFromPattern(request)
        val path = Utils.getPath(completePath)
        val name = Utils.getName(completePath)
        itemEditLog.info("User \"${user.username}\" try to delete item \"$path$name\".")

        //检查Item是否存在
        val fileItem: FileItem? = fileItemRepo.findByVirtualPathAndVirtualName(path, name)
        if (fileItem == null) {
            itemEditLog.warn("User \"${user.username}\" delete item \"$path$name\" failed. Path is invalid")
            return ResponseEntity(ReplyMsg(false, "Sorry. Item is invalid"), HttpStatus.NOT_FOUND)
        }

        //检查用户是否拥有权限
        if (fileItem.ownerName != user.username && !user.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            itemEditLog.warn("User \"${user.username}\" delete item \"$path$name\" failed. Permission deneid")
            return ResponseEntity(ReplyMsg(false, "Permission denied"), HttpStatus.FORBIDDEN)
        }

        //若Item为文件夹，递归删除其子Item
        if (fileItem.isDictionary) {
            fileItemRepo.delete(fileItem)
            var count = 1
            fileItemRepo.findByOwnerName(user.username).forEach {
                if (it.virtualPath.contains(path + name)) {
                    fileItemRepo.delete(it)
                    count++
                    Utils.updateSize(it, -1 * it.size, fileItemRepo)
                    if (fileItemRepo.findByRealPath(it.realPath).isEmpty() && !it.isDictionary) it.deleteFile()
                }
            }
            itemEditLog.info("User \"${user.username}\" delete item \"$path$name\" success. Total $count items")
            return ResponseEntity(ReplyMsg(true, "Delete folder ${path}${name}/ total $count items success"), HttpStatus.OK)
        } else {
            fileItemRepo.delete(fileItem)
            Utils.updateSize(fileItem, -1 * fileItem.size, fileItemRepo)
            if (fileItemRepo.findByRealPath(fileItem.realPath).isEmpty()) fileItem.deleteFile()
            itemEditLog.info("User \"${user.username}\" delete item \"$path$name\" success.")
            return ResponseEntity(ReplyMsg(true, "Delete ${fileItem.virtualPath}${fileItem.virtualName} success"), HttpStatus.OK)
        }
    }

    /**
     * 移动Item
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PutMapping("/api/item/**/path")
    fun move(@AuthenticationPrincipal user: UserDetails, @RequestBody msg: MoveMsg, request: HttpServletRequest): ResponseEntity<ReplyMsg> {
        //获取URL中的**部分路径
        val completePath = Utils.getPath(Utils.extractPathFromPattern(request))
        val path = Utils.getPath(completePath)
        val name = Utils.getName(completePath)

        itemEditLog.info("User \"${user.username}\" try to move item \"$path$name\" to \"${msg.newPath}$name\".")

        //Check origin file is legal
        val fileItem: FileItem? = fileItemRepo.findByVirtualPathAndVirtualName(path, name)
        if (fileItem == null) {
            itemEditLog.warn("User \"${user.username}\" move item \"$path$name\" to \"${msg.newPath}$name\" failed. Origin path is invalid.")
            return ResponseEntity(ReplyMsg(false, "Sorry. Item is invalid"), HttpStatus.NOT_FOUND)
        }

        //Check new path is legal
        if (Utils.getSuperItem(msg.newPath, fileItemRepo) == null) {
            itemEditLog.warn("User \"${user.username}\" move item \"$path$name\" to \"${msg.newPath}$name\" failed. New path is invalid.")
            return ResponseEntity(ReplyMsg(false, "Sorry. New path is invalid"), HttpStatus.BAD_REQUEST)
        }

        if (fileItem.ownerName != user.username && !user.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            itemEditLog.warn("User \"${user.username}\" move item \"$path$name\" to \"${msg.newPath}$name\" failed. Permission denied.")
            return ResponseEntity(ReplyMsg(false, "Permission denied"), HttpStatus.FORBIDDEN)
        }

        //Do move
        var count = 1
        if (fileItem.isDictionary) {
            fileItemRepo.findByOwnerName(user.username).forEach {
                if (it.virtualPath.contains(path + name)) {
                    count++
                    fileItemRepo.delete(it)
                    Utils.updateSize(it, -1 * it.size, fileItemRepo)
                    it.virtualPath = it.virtualPath.replaceFirst(path + name, msg.newPath + name)
                    it.lastModified = Date()
                    fileItemRepo.save(it)
                    Utils.updateSize(it, it.size, fileItemRepo)
                }
            }
        }
        fileItemRepo.delete(fileItem)
        Utils.updateSize(fileItem, -1 * fileItem.size, fileItemRepo)

        fileItem.virtualPath = msg.newPath
        fileItem.lastModified = Date()
        Utils.updateSize(fileItem, fileItem.size, fileItemRepo)
        fileItemRepo.save(fileItem)

        itemEditLog.info("User ${user.username} move $path$name to ${msg.newPath}$name total $count ${if (count == 1) "item" else "items"} success")
        return ResponseEntity(ReplyMsg(true, "Move $path$name to ${msg.newPath}$name total $count ${if (count == 1) "item" else "items"} success"), HttpStatus.OK)
    }

    /**
     * 更改Item权限（private/public）
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PutMapping("/api/item/**/access")
    fun changeAccess(@AuthenticationPrincipal user: UserDetails, @RequestBody msg: ChangeAccessMsg, request: HttpServletRequest): ResponseEntity<ReplyMsg> {
        //获取URL中的**部分路径
        val completePath = Utils.getPath(Utils.extractPathFromPattern(request))
        val path = Utils.getPath(completePath)
        val name = Utils.getName(completePath)

        itemEditLog.info("User ${user.username} try to change \"$path$name\" access to \"${if (msg.isPublic) "public" else " private"}\".")


        //Check origin file is legal
        val fileItem: FileItem? = fileItemRepo.findByVirtualPathAndVirtualName(path, name)
        if (fileItem == null) {
            itemEditLog.warn("User \"${user.username}\" change  \"$path$name\" access to \"${if (msg.isPublic) "public" else " private"}\" failed. Item is invalid.")
            return ResponseEntity(ReplyMsg(false, "Sorry. Item is invalid"), HttpStatus.NOT_FOUND)
        }

        //Check item belong
        if (fileItem.ownerName != user.username && !user.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            itemEditLog.warn("User \"${user.username}\" change  \"$path$name\" access to \"${if (msg.isPublic) "public" else " private"}\" failed. Permission denied.")
            return ResponseEntity(ReplyMsg(false, "Permission denied"), HttpStatus.FORBIDDEN)
        }

        //Do change
        var count = 1
        //若将一个Item权限从private更改为public，检查所有父目录，将所有父目录也设为public
        if (msg.isPublic) {
            var temp: FileItem = fileItem
            while (temp.virtualPath != "/") {
                temp = Utils.getSuperItem(temp, fileItemRepo) ?: break
                if (temp.isPublic != msg.isPublic) {
                    fileItemRepo.delete(temp)
                    temp.isPublic = msg.isPublic
                    temp.lastModified = Date()
                    fileItemRepo.save(temp)
                    count++
                }
            }
        }

        //若Item为文件夹且参数允许递归更改，递归更改所有子Item的权限
        if (fileItem.isDictionary && msg.allowRecursion) {
            fileItemRepo.findByOwnerName(user.username).forEach {
                if (it.virtualPath.contains(path + name)) {
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

        itemEditLog.info("User ${user.username} change $path$name total $count ${if (count == 1) "item" else "items"} access to ${if (msg.isPublic) "public" else " private"} success.")
        return ResponseEntity(ReplyMsg(true, "Change $path$name total $count ${if (count == 1) "item" else "items"} access to ${if (msg.isPublic) "public" else " private"} success"), HttpStatus.OK)
    }

    /**
     * 转存文件
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("/api/items/**")
    fun transfer(@AuthenticationPrincipal user: UserDetails, @RequestBody msg: TransferMsg, request: HttpServletRequest): ResponseEntity<ReplyMsg> {
        //获取URL中**部分Path
        val newPath = Utils.extractPathFromPattern(request)

        itemEditLog.info("User ${user.username} try to transfer \"${msg.path}${msg.name}\" to \"$newPath${msg.name}\".")

        //Check origin file is legal
        val fileItem: FileItem? = fileItemRepo.findByVirtualPathAndVirtualName(msg.path, msg.name)
        if (fileItem == null) {
            itemEditLog.warn("User ${user.username} transfer \"${msg.path}${msg.name}\" to \"$newPath${msg.name}\" failed. Item is invalid.")
            return ResponseEntity(ReplyMsg(false, "Sorry. Item is invalid"), HttpStatus.NOT_FOUND)
        }
        //Check the file is already belong you
        if (fileItem.ownerName == user.username) {
            itemEditLog.warn("User ${user.username} transfer \"${msg.path}${msg.name}\" to \"$newPath${msg.name}\" failed. Item is already belong you.")
            return ResponseEntity(ReplyMsg(false, "Item is already belong you"), HttpStatus.ALREADY_REPORTED)
        }

        //Check new path is legal
        if (Utils.getSuperItem(newPath, fileItemRepo) == null) {
            itemEditLog.warn("User ${user.username} transfer \"${msg.path}${msg.name}\" to \"$newPath${msg.name}\" failed. New path is invalid.")
            return ResponseEntity(ReplyMsg(false, "New path is invalid"), HttpStatus.BAD_REQUEST)
        }

        if (!fileItem.isPublic && !user.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            itemEditLog.warn("User ${user.username} transfer \"${msg.path}${msg.name}\" to \"$newPath${msg.name}\" failed. Permission denied.")
            return ResponseEntity(ReplyMsg(false, "Permission denied"), HttpStatus.FORBIDDEN)
        }

        //Do transfer
        var count = 1
        //若转存的Item是文件夹，递归转存文件夹下的所有public文件
        if (fileItem.isDictionary) {
            fileItemRepo.findByOwnerName(fileItem.ownerName).forEach {
                if (it.virtualPath.contains(msg.path + msg.name) && it.isPublic) {
                    fileItemRepo.save(FileItem(
                            user.username,
                            false,
                            it.isDictionary,
                            it.realPath,
                            it.size,
                            it.virtualPath.replace(msg.path, newPath),
                            it.virtualName,
                            it.isPublic,
                            Date()
                    ))
                    Utils.updateSize(it, it.size, fileItemRepo)
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
                fileItem.virtualPath.replace(msg.path, newPath),
                fileItem.virtualName,
                fileItem.isPublic,
                Date()
        ))
        Utils.updateSize(fileItem, fileItem.size, fileItemRepo)
        itemEditLog.info("User ${user.username} try to transfer \"${msg.path}${msg.name}\" to \"$newPath${msg.name}\" total $count ${if (count == 1) "item" else "items"} success.")
        return ResponseEntity(ReplyMsg(true, "Transfer ${msg.path}${msg.name} to $newPath${msg.name} total $count ${if (count == 1) "item" else "items"} success"), HttpStatus.OK)
    }

    /**
     * 创建文件夹
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("/api/item/**")
    fun makeDir(@AuthenticationPrincipal user: UserDetails, request: HttpServletRequest): ResponseEntity<ReplyMsg> {
        //获取URL中**部分Path
        val completePath = Utils.extractPathFromPattern(request)
        val path = Utils.getPath(completePath)
        val name = Utils.getName(completePath)

        itemEditLog.info("User ${user.username} try to create dir \"$completePath\"")

        //Check folder is not exist
        val testExist: FileItem? = fileItemRepo.findByVirtualPathAndVirtualName(path, name)
        if (testExist != null) {
            itemEditLog.warn("User ${user.username} create dir \"$completePath\" failed. Dictionary is already exist.")
            return ResponseEntity(ReplyMsg(false, "Dictionary is already exist"), HttpStatus.ALREADY_REPORTED)
        }

        //Check super path is legal
        val superItem: FileItem? = Utils.getSuperItem(path, fileItemRepo)
        if (superItem == null) {
            itemEditLog.warn("User ${user.username} create dir \"$completePath\" failed. Super path is invalid.")
            return ResponseEntity(ReplyMsg(false, "Super path not exist"), HttpStatus.NOT_FOUND)
        }

        //Check permission
        if (superItem.ownerName != user.username && !user.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            itemEditLog.warn("User ${user.username} create dir \"$completePath\" failed. Permission denied.")
            return ResponseEntity(ReplyMsg(false, "Permission denied"), HttpStatus.FORBIDDEN)
        }

        //Do mkdir
        val newDir = FileItem(
                user.username,
                false,
                true,
                null,
                virtualPath = path,
                virtualName = name
        )
        fileItemRepo.save(newDir)

        itemEditLog.info("User ${user.username} create dir \"$completePath\" success.")
        return ResponseEntity(ReplyMsg(true, "Create dictionary $path$name success"), HttpStatus.OK)
    }
}