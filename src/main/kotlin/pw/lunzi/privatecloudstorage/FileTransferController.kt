package pw.lunzi.privatecloudstorage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.codec.Hex
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * ***********************************************
 * Created by Lunzi on 4/5/2018.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */

@RestController
class FileTransferController(private val fileItemRepository: FileItemRepository, private val userRepository: UserRepository) {

    data class FileMsg(val path: String, val name: String)

    @RequestMapping("download")
    fun downloadFile(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: FileMsg): Any {
        val fileItem: FileItem? = fileItemRepository.findByVirtualPathAndVirtualName(msg.path, msg.name)

        return if (fileItem != null) {
            if (fileItem.isPublic)
                getFileResponseEntity(fileItem)
            else
                if (user != null && user.username == fileItem.ownerName) getFileResponseEntity(fileItem)
                else getErrResponseEntity("Permisson denied")
        } else getErrResponseEntity("Sorry. File is invalid")
    }

    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("upload")
    fun uploadFile(@AuthenticationPrincipal user: UserDetails?, @RequestParam("file") files: List<MultipartFile>, @RequestParam("path") path: String): ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")

        for (file in files) {
            val name = file.originalFilename ?: "null"

            val realPath = FileItem.rootPath + user.username + "/"

            val fileMD5 = MessageDigest.getInstance("MD5")
            fileMD5.update(file.bytes)
            val md5Name = String(Hex.encode(fileMD5.digest())).toUpperCase()

            val saveFile = File(realPath, md5Name)
            if (saveFile.exists()) return ReplyMsg(false, "File is already exist")

            file.transferTo(saveFile)
            val fileItem = FileItem(
                    ownerName = user.username,
                    virtualName = name,
                    realPath = realPath + md5Name,
                    isDictionary = false,
                    size = file.size,
                    virtualPath = path,
                    isUserRootPath = false
            )
            fileItemRepository.save(fileItem)
        }
        return ReplyMsg(true, "Upload file success")
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

    private fun getErrResponseEntity(msg: String): ResponseEntity<String> {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/json"))
                .body(jacksonObjectMapper().writeValueAsString(ReplyMsg(false, msg)))
    }


}
