package pw.lunzi.privatecloudstorage

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.io.File
import java.nio.file.Files
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
        val isUserRootPath: Boolean,
        val isDictionary: Boolean,
        val realPath: String?,
        val size: Long = if (isDictionary) 0 else File(realPath).length(),
        var virtualPath: String,
        var virtualName: String,
        var isPublic: Boolean = false,
        var lastModified: Date = Date(),
        @Id val id: Int = (ownerName + realPath + Date()).hashCode()
){
    companion object {
        const val rootPath: String = "/var/www/cloudStorage/"
        fun getLegalVirtualPath(path: String): String {
            var result: String = if (path[path.lastIndex] != '/') "$path/" else path
            result = if (path[0] != '/') "/$result" else result
            return result
        }

        fun getSuperPath(virtualPath: String): String {
            var path = getLegalVirtualPath(virtualPath)
            path = path.substring(0, path.length - 1)
            return path.substring(0, path.lastIndexOf('/') + 1)
        }

        fun getSuperName(virtualPath: String): String {
            var path = getLegalVirtualPath(virtualPath)
            path = path.substring(0, path.length - 1)
            return path.substring(path.lastIndexOf('/') + 1)
        }
    }
    fun isExist() = File(realPath).exists()

    fun deleteFile() = Files.deleteIfExists(Paths.get(realPath))

    fun mkdir() = File(realPath).mkdir()


}

@Repository
interface FileItemRepository : MongoRepository<FileItem, Long> {
    fun findByVirtualPathAndOwnerName(virtualPath: String, ownerName: String): Array<FileItem>
    fun findByVirtualPathAndVirtualNameAndOwnerName(virtualPath: String, virtualName: String, ownerName: String): FileItem?
    fun findByVirtualPathAndVirtualName(virtualPath: String, virtualName: String): FileItem?
    fun countByVirtualPathAndVirtualNameAndOwnerName(virtualPath: String, virtualName: String, ownerName: String): Long
    fun countByVirtualPathAndOwnerName(virtualPath: String, ownerName: String): Long
    fun findByIsUserRootPathAndOwnerName(isUserRootPath: Boolean = true, ownerName: String): FileItem?
    fun findByIsPublicAndOwnerName(isPublic: Boolean, ownerName: String): Array<FileItem>
    fun findByOwnerName(ownerName: String): Array<FileItem>
    fun findByRealPath(realPath: String?): Array<FileItem>

}




