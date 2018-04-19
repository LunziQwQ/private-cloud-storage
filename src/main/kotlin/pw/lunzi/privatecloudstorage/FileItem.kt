package pw.lunzi.privatecloudstorage

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.io.File
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
        @Id val id: Int = (ownerName + realPath).hashCode()
){
    companion object {
        const val rootPath: String = "/var/www/cloudStorage/"
    }
    fun isExist() = File(realPath).exists()

    fun deleteFile() = File(realPath).delete()

    fun mkdir() = File(realPath).mkdir()

    fun getSuperPath(): String {
        TODO()
    }

    fun getSuperName(): String {
        TODO()
    }
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
}




