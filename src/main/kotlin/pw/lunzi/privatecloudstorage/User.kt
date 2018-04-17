package pw.lunzi.privatecloudstorage

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

/**
 * ***********************************************
 * Created by Lunzi on 3/31/18.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */

data class User(
        val username: String,
        var password: String,
        var space: Int? = 256000,
        val publicFiles: List<FileItem> = ArrayList(),
        val privateFiles: List<FileItem> = ArrayList(),
        val sharedFiles: List<FileItem> = ArrayList(),
        @Id val id: Int = username.hashCode()
)

@Repository
interface UserRepository : MongoRepository<User, Long> {
    fun findByUsername(username: String): User?
    fun countByUsername(username: String): Long
}
