package pw.lunzi.privatecloudstorage

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import org.springframework.util.AntPathMatcher
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

class Utils {
    companion object {
        fun extractPathFromPattern(request: HttpServletRequest): String {
            val path = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
            val bestMatchPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String
            return AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path)
        }

        fun getLegalVirtualPath(path: String): String {
            var result: String = if (path[path.lastIndex] != '/') "$path/" else path
            result = if (path[0] != '/') "/$result" else result
            return result
        }

        private fun getSuperPath(virtualPath: String): String {
            var path = getLegalVirtualPath(virtualPath)
            path = path.substring(0, path.length - 1)
            return path.substring(0, path.lastIndexOf('/') + 1)
        }

        private fun getSuperName(virtualPath: String): String {
            var path = getLegalVirtualPath(virtualPath)
            path = path.substring(0, path.length - 1)
            return path.substring(path.lastIndexOf('/') + 1)
        }


        fun getName(completePath: String) = getSuperName(completePath)
        fun getPath(completePath: String) = getSuperPath(completePath)

        fun getSuperItem(vp: String, repo: FileItemRepository) =
                repo.findByVirtualPathAndVirtualName(getSuperPath(vp), getSuperName(vp))

        fun getSuperItem(vp: String, ownerName: String, repo: FileItemRepository) =
                repo.findByVirtualPathAndVirtualNameAndOwnerName(getSuperPath(vp), getSuperName(vp), ownerName)

        fun getSuperItem(item: FileItem, repo: FileItemRepository) =
                repo.findByVirtualPathAndVirtualNameAndOwnerName(getSuperPath(item.virtualPath), getSuperName(item.virtualPath), item.ownerName)

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

data class ReplyMsg(val result: Boolean, val message: String)


@Repository
interface UserRepository : MongoRepository<User, Long> {
    fun findByUsername(username: String): User?
    fun countByUsername(username: String): Long
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

@Repository
interface ShareItemRepository : MongoRepository<ShareItem, Long> {
    fun findByUrl(url: String): ShareItem?
    fun findByUrlAndSharedUserName(url: String, sharedUserName: String): ShareItem?
}