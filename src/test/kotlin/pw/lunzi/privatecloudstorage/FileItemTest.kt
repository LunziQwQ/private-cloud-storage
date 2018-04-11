package pw.lunzi.privatecloudstorage

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

@RunWith(SpringRunner::class)
@DataMongoTest
class FileItemTest {
    @Autowired
    lateinit var fileItemRepository: FileItemRepository

    @Test
    fun testFileItemRepo() {
        val testFileItem = FileItem(
                "Lunzi",
                (FileItem.rootPath.toString() + "root/test"),
                ("Lunzi/test"),
                "test",
                false,
                false,
                null,
                true,
                Date()
        )
        fileItemRepository.save(testFileItem)
        Assert.assertTrue(fileItemRepository.countByVirtualPathAndOwnerName(testFileItem.virtualPath, testFileItem.ownerName) > 0)
        Assert.assertEquals(fileItemRepository.findByVirtualPathAndOwnerName(testFileItem.virtualPath, testFileItem.ownerName), testFileItem)
        fileItemRepository.delete(testFileItem)
        Assert.assertTrue(fileItemRepository.countByVirtualPathAndOwnerName(testFileItem.virtualPath, testFileItem.ownerName) == 0L)
    }
}