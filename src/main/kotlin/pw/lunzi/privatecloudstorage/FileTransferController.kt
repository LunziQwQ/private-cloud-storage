package pw.lunzi.privatecloudstorage

import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.codec.Hex
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.servlet.http.HttpServletRequest

/**
 * ***********************************************
 * Created by Lunzi on 4/5/2018.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */

@RestController
class FileTransferController(private val fileItemRepository: FileItemRepository, private val userRepository: UserRepository) {

    /**
     * 下载文件
     */
    @GetMapping("/api/file/**")
    fun downloadFile(@AuthenticationPrincipal user: UserDetails?, request: HttpServletRequest): Any {
        //剥离出**部分的文件路径
        val completePath = Utils.extractPathFromPattern(request)
        val path = Utils.getPath(completePath)
        val name = Utils.getName(completePath)

        //根据路径获取Item
        val fileItem: FileItem? = fileItemRepository.findByVirtualPathAndVirtualName(path, name)

        //若Item 公开或拥有权限 且 不为文件夹，启动下载，返回文件流
        return if (fileItem != null && !fileItem.isDictionary) {
            if (fileItem.isPublic) {
                fileTransLog.info("User \"${if (user != null) user.username else "Guest"}\" download \"$completePath\" success")
                getFileResponseEntity(fileItem)
            } else {
                if (user != null && user.username == fileItem.ownerName) {
                    fileTransLog.info("User \"${user.username}\" download \"$completePath\" success")
                    getFileResponseEntity(fileItem)
                } else {
                    fileTransLog.warn("User \"${if (user != null) user.username else "Guest"}\" download \"$completePath\" failed. Permission denied.")
                    ResponseEntity(ReplyMsg(false, "Permission denied"), HttpStatus.FORBIDDEN)
                }
            }
        } else {
            fileTransLog.warn("User \"${if (user != null) user.username else "Guest"}\" download \"$completePath\" failed. FileItem not found.")
            ResponseEntity(ReplyMsg(false, "Sorry. File is invalid"), HttpStatus.NOT_FOUND)
        }
        //TODO("Compress and download the folder")
    }

    /**
     * 上传文件
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("/api/file/**")
    fun uploadFile(@AuthenticationPrincipal user: UserDetails?, @RequestParam("file") files: List<MultipartFile>, request: HttpServletRequest): ResponseEntity<Array<ReplyMsg>> {
        //获取**的部分，即要上传到的虚拟路径
        val path = Utils.getLegalPath(Utils.extractPathFromPattern(request))

        //对应每个上传文件的返回消息列表
        val replyMsgList: MutableList<ReplyMsg> = mutableListOf()

        //游客无法上传文件
        if (user == null) {
            fileTransLog.warn("A guest try to upload files. Permission denied")
            return ResponseEntity(arrayOf(ReplyMsg(false, "Permission denied")), HttpStatus.FORBIDDEN)
        }

        //Check the path is legal
        val superItem: FileItem? = Utils.getSuperItem(path, fileItemRepository)
        if (superItem == null) {
            fileTransLog.warn("User \"${user.username}\" upload file failed. Path \"$path\" is invalid")
            return ResponseEntity(arrayOf(ReplyMsg(false, "Path is invalid")), HttpStatus.NOT_FOUND)
        }

        //检查路径是否属于上传者
        if (superItem.ownerName != user.username) {
            fileTransLog.warn("User \"${user.username}\" upload file failed. Path \"$path\" is not belong him")
            return ResponseEntity(arrayOf(ReplyMsg(false, "You don't own the path: $path")), HttpStatus.FORBIDDEN)
        }

        //一一处理上传文件列表
        for (file in files) {
            //若上传时无文件名，设为null
            val name = file.originalFilename ?: "null"

            //检查同目录下是否存在同名文件
            if (fileItemRepository.countByVirtualPathAndVirtualNameAndOwnerName(path, name, user.username) > 0) {
                fileTransLog.warn("User \"${user.username}\" upload file \"$path$name\" failed. File name is already exist in same path.")
                replyMsgList.add(ReplyMsg(false, "$name -> File name is already exist"))
                continue
            }

            //获取文件内容的MD5，留作本地存储的文件名
            val fileMD5 = MessageDigest.getInstance("MD5")
            fileMD5.update(file.bytes)
            val md5Name = String(Hex.encode(fileMD5.digest())).toUpperCase()

            //Save the fileItem
            val fileItem = FileItem(
                    ownerName = user.username,
                    virtualName = name,
                    realPath = user.username + "/" + md5Name,
                    isDictionary = false,
                    size = file.size,
                    virtualPath = path,
                    isUserRootPath = false
            )

            //check the user's usage enough
            val userRoot = fileItemRepository.findByVirtualPathAndOwnerName("/", user.username)
            if (userRoot.isEmpty() || userRoot[0].size + file.size > userRepository.findByUsername(user.username)!!.space) {
                fileTransLog.warn("User \"${user.username}\" upload file \"$path$name\" failed. User space is not enough.")
                replyMsgList.add(ReplyMsg(false, "$name -> User space is not enough"))
            } else {
                fileItemRepository.save(fileItem)
                Utils.updateSize(fileItem, file.size, fileItemRepository)

                //Storage the real file
                val saveFile = File(fileItem.getLocalRealPath())

                //检查是否已存在同MD5的文件，若存在则不执行存储，仅仅保存索引
                if (saveFile.exists()) {
                    fileTransLog.info("User \"${user.username}\" upload file \"$path$name\" success. But have same file in system. Just save the Item data.")
                    replyMsgList.add(ReplyMsg(true, "$name -> Upload success but file is already exist"))
                } else {
                    file.transferTo(saveFile)
                    fileTransLog.info("User \"${user.username}\" upload file \"$path$name\" success.")
                    replyMsgList.add(ReplyMsg(true, "$name -> Upload success"))
                }
            }
        }
        return ResponseEntity(replyMsgList.toTypedArray(), HttpStatus.OK)
    }


    /**
     * 构建文件流
     */
    private fun getFileResponseEntity(fileItem: FileItem): ResponseEntity<InputStreamResource> {
        val file = File(fileItem.getLocalRealPath())
        val resource = InputStreamResource(FileInputStream(file))
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=${fileItem.virtualName}")
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource)
    }
}
