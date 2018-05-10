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

    @GetMapping("/api/file/**")
    fun downloadFile(@AuthenticationPrincipal user: UserDetails?, request: HttpServletRequest): Any {
        val completePath = Utils.extractPathFromPattern(request)
        val path = Utils.getPath(completePath)
        val name = Utils.getName(completePath)
        val fileItem: FileItem? = fileItemRepository.findByVirtualPathAndVirtualName(path, name)
        return if (fileItem != null) {
            if (fileItem.isPublic) {
                getFileResponseEntity(fileItem)
            } else {
                if (user != null && user.username == fileItem.ownerName) getFileResponseEntity(fileItem)
                else ResponseEntity(ReplyMsg(false, "Permission denied"), HttpStatus.FORBIDDEN)
            }
        } else {
            ResponseEntity(ReplyMsg(false, "Sorry. File is invalid"), HttpStatus.NOT_FOUND)
        }
        //TODO("Compress and download the folder")
    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("/api/file/**")
    fun uploadFile(@AuthenticationPrincipal user: UserDetails?, @RequestParam("file") files: List<MultipartFile>, request: HttpServletRequest): ResponseEntity<Array<ReplyMsg>> {
        val path = Utils.getLegalPath(Utils.extractPathFromPattern(request))
        val replyMsgList: MutableList<ReplyMsg> = mutableListOf()
        if (user == null) return ResponseEntity(arrayOf(ReplyMsg(false, "Permission denied")), HttpStatus.FORBIDDEN)

        //Check the path is legal
        val superItem = Utils.getSuperItem(path, fileItemRepository)
                ?: return ResponseEntity(arrayOf(ReplyMsg(false, "Path is invalid")), HttpStatus.NOT_FOUND)
        if (superItem.ownerName != user.username) {
            return ResponseEntity(arrayOf(ReplyMsg(false, "You don't own the path: $path")), HttpStatus.FORBIDDEN)
        }

        for (file in files) {
            val name = file.originalFilename ?: "null"
            val realPath = FileItem.rootPath + user.username + "/"

            //Check if exist
            if (fileItemRepository.countByVirtualPathAndVirtualNameAndOwnerName(path, name, user.username) > 0) {
                replyMsgList.add(ReplyMsg(false, "$name -> File name is already exist"))
                continue
            }

            //Get the file MD5
            val fileMD5 = MessageDigest.getInstance("MD5")
            fileMD5.update(file.bytes)
            val md5Name = String(Hex.encode(fileMD5.digest())).toUpperCase()

            //Save the fileItem
            val fileItem = FileItem(
                    ownerName = user.username,
                    virtualName = name,
                    realPath = realPath + md5Name,
                    isDictionary = false,
                    size = file.size,
                    virtualPath = path,
                    isUserRootPath = false
            )

            //check the user's usage enough
            val userRoot = fileItemRepository.findByVirtualPathAndOwnerName("/", user.username)
            if (userRoot.isEmpty() || userRoot[0].size + file.size > userRepository.findByUsername(user.username)!!.space) {
                replyMsgList.add(ReplyMsg(false, "$name -> User space is not enough"))
            } else {
                fileItemRepository.save(fileItem)
                Utils.updateSize(fileItem, file.size, fileItemRepository)

                //Storage the real file
                val saveFile = File(realPath, md5Name)
                if (saveFile.exists()) {
                    replyMsgList.add(ReplyMsg(true, "$name -> Upload success but file is already exist"))
                } else {
                    file.transferTo(saveFile)
                    replyMsgList.add(ReplyMsg(true, "$name -> Upload success"))
                }
            }
        }
        return ResponseEntity(replyMsgList.toTypedArray(), HttpStatus.OK)
    }

    private fun getFileResponseEntity(fileItem: FileItem): ResponseEntity<InputStreamResource> {
        val file = File(fileItem.realPath)
        val resource = InputStreamResource(FileInputStream(file))
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=${fileItem.virtualName}")
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource)
    }
}
