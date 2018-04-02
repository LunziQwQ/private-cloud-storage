package pw.lunzi.privatecloudstorage

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
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
        val realPath: Path,
        val virtualPath: URL,
        val isUserRootPath: Boolean,
        val isDictionary: Boolean,
        var children: List<FileItem>,
        val isPublic: Boolean,
        val lastModified: Date,
        val size: Int
){
    companion object {
        val rootPath: Path = Paths.get("/var/cloudStorage/root")
    }
}

fun FileItem.isExist() = realPath.toFile().exists()

fun FileItem.save(fis: FileInputStream) {
    val temp: File = realPath.toFile()
    if (isDictionary) {
        if (!isExist()) {
            temp.mkdirs()
        }
    } else {
        val fos = FileOutputStream(temp)
        fos.write(fis.readAllBytes())
    }
}

fun FileItem.getFile(): FileOutputStream = if (!isDictionary) FileOutputStream(realPath.toFile()) else null!!