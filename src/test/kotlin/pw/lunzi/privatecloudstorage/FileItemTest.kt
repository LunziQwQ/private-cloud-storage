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
    fun testSave() {
        val testFileItem = FileItem(
                ownerName = "Lunzi",
                realPath = (FileItem.rootPath + "root/test"),
                virtualPath = ("Lunzi/test"),
                virtualName = "test",
                isDictionary = false,
                isUserRootPath = false,
                children = null,
                isPublic = true,
                lastModified = Date()
        )
        fileItemRepository.save(testFileItem)
        Assert.assertTrue(fileItemRepository.countByVirtualPathAndOwnerNameAndIsAvailable(testFileItem.virtualPath, testFileItem.ownerName, true) > 0)
        Assert.assertEquals(fileItemRepository.findByVirtualPathAndOwnerNameAndIsAvailable(testFileItem.virtualPath, testFileItem.ownerName, true), testFileItem)
        fileItemRepository.delete(testFileItem)
        Assert.assertTrue(fileItemRepository.countByVirtualPathAndOwnerNameAndIsAvailable(testFileItem.virtualPath, testFileItem.ownerName, true) == 0L)
    }

    @Test
    fun testUpdate(){
        val testFileItem = FileItem(
                ownerName = "Lunzi",
                realPath = (FileItem.rootPath + "root/test"),
                virtualPath = ("Lunzi/test"),
                virtualName = "test",
                isDictionary = false,
                isUserRootPath = false,
                children = null,
                isPublic = true,
                lastModified = Date()
        )
        fileItemRepository.save(testFileItem)
        Assert.assertTrue(fileItemRepository.countByVirtualPathAndIsAvailable(testFileItem.virtualPath, true) > 0)
        Assert.assertEquals(fileItemRepository.findByVirtualPathAndOwnerNameAndIsAvailable(testFileItem.virtualPath, testFileItem.ownerName, true), testFileItem)

        val newFileItem = testFileItem.copy(ownerName = "QAQ")
        fileItemRepository.save(newFileItem)
        Assert.assertTrue(fileItemRepository.countByVirtualPathAndIsAvailable(testFileItem.virtualPath, true) == 1L)
        Assert.assertEquals(fileItemRepository.findByVirtualPathAndIsAvailable(testFileItem.virtualPath, true), newFileItem)

        fileItemRepository.delete(testFileItem)
    }
}