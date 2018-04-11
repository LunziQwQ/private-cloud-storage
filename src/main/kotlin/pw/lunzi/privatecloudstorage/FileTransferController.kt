package pw.lunzi.privatecloudstorage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.FileInputStream

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
    data class ReplyMsg(val message: String)

    @RequestMapping("download")
    fun downloadFile(@AuthenticationPrincipal user: UserDetails?, @RequestBody msg: FileMsg): ResponseEntity<*> {
        val fileItem: FileItem? = fileItemRepository.findByVirtualPath(msg.path)

        if (fileItem != null ) {
            if(fileItem.isPublic)
                return getFileResponseEntity(fileItem)
            else
                return if(user!=null && user.username == fileItem.ownerName) getFileResponseEntity(fileItem)
                else getErrResponseEntity("Permisson denied")
        }else return getErrResponseEntity("Sorry. File is invalid")
    }

    fun getFileResponseEntity(fileItem: FileItem): ResponseEntity<InputStreamResource> {
        val file = File(fileItem.realPath)
        val resource = InputStreamResource(FileInputStream(file))
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=${fileItem.virtualName}")
                .header("Content-Disposition", "attachment; filename=fuckingTest")
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource)
    }

    fun getErrResponseEntity(msg:String):ResponseEntity<*>{
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/json"))
                .body(jacksonObjectMapper().writeValueAsString(ReplyMsg(msg)))
    }

    @GetMapping("whoami")
    fun test(@AuthenticationPrincipal user: UserDetails?): UserDetails? {
        return user
    }


    @PostMapping("upload")
    fun uploadFile() {

    }

}

