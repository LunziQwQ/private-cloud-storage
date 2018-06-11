package pw.lunzi.privatecloudstorage

import org.springframework.data.annotation.Id
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
        var size: Long = if (isDictionary) 0 else File(realPath).length(),
        var virtualPath: String,
        var virtualName: String,
        var isPublic: Boolean = false,
        var lastModified: Date = Date(),
        @Id val id: Int = (ownerName + realPath + virtualPath + virtualName + Date()).hashCode()
) {
    fun isExist() = File(getLocalRealPath()).exists()

    fun deleteFile() = Files.deleteIfExists(Paths.get(getLocalRealPath()))

    fun mkdir() = File(getLocalRealPath()).mkdir()

    fun getLocalRealPath() = Config.savePath + realPath
}


