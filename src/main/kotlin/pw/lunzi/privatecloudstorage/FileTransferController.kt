package pw.lunzi.privatecloudstorage

import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path
import java.nio.file.Paths
import javax.servlet.http.HttpServletRequest

/**
 * ***********************************************
 * Created by Lunzi on 4/5/2018.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */

@RestController
class FileTransferController {

    val rootPath: Path = Paths.get("/var/www/cloudStorage/root")

    @RequestMapping("download")
    fun downloadFile(request: HttpServletRequest): ResponseEntity<InputStreamResource> {
        val json: String = request.getParameter("json")
//        val fileItem: FileItem = jacksonObjectMapper().readValue(json)
//        val file = fileItem.realPath.toFile()
        val file = File("/var/www/cloudStorage/root/test")
        val resource = InputStreamResource(FileInputStream(file))
        return ResponseEntity.ok()
//                .header("Content-Disposition","attachment; filename=${fileItem.virtualName}")
                .header("Content-Disposition","attachment; filename=fuckingTest")
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource)
    }

    @PostMapping("upload")
    fun uploadFile() {

    }

}

