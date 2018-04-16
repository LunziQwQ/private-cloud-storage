package pw.lunzi.privatecloudstorage

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * ***********************************************
 * Created by Lunzi on 3/31/18.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */
data class FileItem(
        val ownerName: String,
        val realPath: String,
        val isUserRootPath: Boolean,
        val isDictionary: Boolean,
        val size: Long = if (isDictionary) 0 else File(realPath).length(),
        var virtualPath: String,
        var virtualName: String,
        var children: List<FileItem>?,
        var isPublic: Boolean,
        var lastModified: Date,
        @Id val id: Int = (ownerName + ":" + virtualPath).hashCode()
){
    companion object {
        val rootPath: Path = Paths.get("/var/www/cloudStorage/")
    }
    fun isExist() = File(realPath).exists()

    fun saveFile(fis: FileInputStream) {
        val temp: File = File(realPath)
        if (isDictionary) {
            if (!isExist()) {
                temp.mkdirs()
            }
        } else {
            val fos = FileOutputStream(temp)
//        fos.write(fis.readAllBytes())
        }
    }

    fun getFOS(): FileOutputStream? = if (!isDictionary) FileOutputStream(File(realPath)) else null
}

@Repository
interface FileItemRepository : MongoRepository<FileItem, Long> {
    fun findByVirtualPathAndOwnerName(virtualPath: String, ownerName: String): FileItem?
    fun findByVirtualPath(virtualPath: String): FileItem?
    fun countByVirtualPathAndOwnerName(virtualPath: String, ownerName: String): Long
}




