package pw.lunzi.privatecloudstorage

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import org.springframework.util.AntPathMatcher
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest
import org.apache.log4j.Logger



class Utils {
    companion object {

        /**
         * 获取请求中的**部分
         * 如Mapping("/api/item/**/access")
         */
        fun extractPathFromPattern(request: HttpServletRequest): String {
            val path = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
            val bestMatchPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String
            return getLegalPath(AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path))
        }

        /**
         * 获取合法的路径，统一处理为以/结尾
         */
        fun getLegalPath(path: String): String {
            if (path.isEmpty()) return "/"
            var result: String = if (path[path.lastIndex] != '/') "$path/" else path
            result = if (path[0] != '/') "/$result" else result
            return result
        }

        /**
         * 获取上一级路径
         */
        private fun getSuperPath(virtualPath: String): String {
            var path = getLegalPath(virtualPath)
            path = path.substring(0, path.length - 1)
            return path.substring(0, path.lastIndexOf('/') + 1)
        }

        /**
         * 获取目录名称
         */
        private fun getSuperName(virtualPath: String): String {
            var path = getLegalPath(virtualPath)
            path = path.substring(0, path.length - 1)
            return path.substring(path.lastIndexOf('/') + 1)
        }

        /**
         * 获取路径的文件名
         */
        fun getName(completePath: String) = getSuperName(completePath)

        /**
         * 获取路径的目录名
         */
        fun getPath(completePath: String) = getSuperPath(completePath)

        /**
         * 获取上一级路径的FileItem
         */
        fun getSuperItem(vp: String, repo: FileItemRepository) =
                repo.findByVirtualPathAndVirtualName(getSuperPath(vp), getSuperName(vp))

        /**
         * 获取上一级路径的FileItem
         */
        fun getSuperItem(item: FileItem, repo: FileItemRepository) =
                repo.findByVirtualPathAndVirtualName(getSuperPath(item.virtualPath), getSuperName(item.virtualPath))

        /**
         * update the Super item.size with recursion
         */
        fun updateSize(item: FileItem?, size: Long, repo: FileItemRepository) {
            val temp = getSuperItem(item ?: return, repo) ?: return
            temp.size += size
            repo.save(temp)
            updateSize(temp, size, repo)
        }

    }
}

/**
 * 统一返回的消息结构
 */
data class ReplyMsg(val result: Boolean, val message: String)

/**
 * Spring data 数据持久化Repo --------------------------------------------------
 */
@Repository
interface UserRepository : MongoRepository<User, Long> {
    fun findByUsername(username: String): User?
    fun countByUsername(username: String): Long
}

@Repository
interface FileItemRepository : MongoRepository<FileItem, Long> {
    fun findByVirtualPathAndOwnerName(virtualPath: String, ownerName: String): Array<FileItem>
    fun findByVirtualPathAndVirtualName(virtualPath: String, virtualName: String): FileItem?
    fun countByVirtualPathAndVirtualNameAndOwnerName(virtualPath: String, virtualName: String, ownerName: String): Long
    fun findByOwnerName(ownerName: String): Array<FileItem>
    fun findByRealPath(realPath: String?): Array<FileItem>

}

@Repository
interface ShareItemRepository : MongoRepository<ShareItem, Long> {
    fun findByUrl(url: String): ShareItem?
}

/**
 * Log4j 日志输出 -------------------------------------------------------------
 */
val loginLog: Logger = Logger.getLogger("Login")
val fileTransLog: Logger = Logger.getLogger("FileTransfer")
val itemEditLog: Logger = Logger.getLogger("ItemEdit")