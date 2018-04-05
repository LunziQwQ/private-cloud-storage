package pw.lunzi.privatecloudstorage

import org.springframework.data.mongodb.repository.MongoRepository

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
        val space: Int,
        val publicFiles: List<FileItem>,
        val privateFiles: List<FileItem>,
        val sharedFiles: List<FileItem>
)


interface userRepository : MongoRepository<User, Long> {
    fun findByUsername(username: String): User
}