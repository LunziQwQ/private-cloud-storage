package pw.lunzi.privatecloudstorage

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
        val name: String,
        val password: String,
        val space: Int? = 256000,
        val publicFiles: List<FileItem> = ArrayList(),
        val privateFiles: List<FileItem> = ArrayList(),
        val sharedFiles: List<FileItem> = ArrayList()
)

@Repository
interface UserRepository : MongoRepository<User, Long> {
    fun findByUsername(username: String): User
}
