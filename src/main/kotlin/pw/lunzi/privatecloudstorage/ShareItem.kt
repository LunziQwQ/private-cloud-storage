package pw.lunzi.privatecloudstorage

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*


data class ShareItem(
        val item: FileItem,
        val createTime: Date = Date(),
        val sharedUserName: String = item.ownerName,
        val url: String = "${Config.hostname}/api/shareitem/${(item.id.toString() + createTime.toString()).hashCode()}"
)
