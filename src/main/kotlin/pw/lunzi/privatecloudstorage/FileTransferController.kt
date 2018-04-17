package pw.lunzi.privatecloudstorage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.*

/**
 * ***********************************************
 * Created by Lunzi on 4/5/2018.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */

@RestController
class FileTransferController(private val fileItemRepository: FileItemRepository) {

    data class FileMsg(val path: String)

    @RequestMapping("download")
    fun downloadFile(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: FileMsg): Any {
        val fileItem: FileItem? = fileItemRepository.findByVirtualPath(msg.path)

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
    fun uploadFile(@AuthenticationPrincipal user: UserDetails?, @RequestParam("file") files: List<MultipartFile>, @RequestParam("path") path: String): pw.lunzi.privatecloudstorage.ReplyMsg {
        if (user == null) return ReplyMsg(false, "Permisson denied")
        for (file in files) {
            val name = file.originalFilename ?: "null"

            val realPath = FileItem.rootPath + user.username + "/"

            val pathFile = File(realPath)
            if (!pathFile.exists()) pathFile.mkdirs()

            val fileMD5 = MessageDigest.getInstance("MD5")
            fileMD5.update(file.bytes)
            val MD5Name = bytesToHex(fileMD5.digest())

            val saveFile = File(realPath, MD5Name)
            if (saveFile.exists()) return ReplyMsg(false, "File is already exist")

            file.transferTo(saveFile)
            val fileItem = FileItem(
                    ownerName = user.username,
                    lastModified = Date(),
                    virtualName = name,
                    realPath = realPath + MD5Name,
                    isDictionary = false,
                    size = file.size,
                    virtualPath = path + name,
                    isPublic = false,
                    isUserRootPath = false,
                    children = ArrayList()
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

    private val hexArray = "0123456789ABCDEF".toCharArray()
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in 0 until bytes.size) {
            val v: Int = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}

