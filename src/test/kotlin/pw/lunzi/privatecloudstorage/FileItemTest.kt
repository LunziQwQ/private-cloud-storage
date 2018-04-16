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
        Assert.assertTrue(fileItemRepository.countByVirtualPathAndOwnerName(testFileItem.virtualPath, testFileItem.ownerName) > 0)
        Assert.assertEquals(fileItemRepository.findByVirtualPathAndOwnerName(testFileItem.virtualPath, testFileItem.ownerName), testFileItem)
        fileItemRepository.delete(testFileItem)
        Assert.assertTrue(fileItemRepository.countByVirtualPathAndOwnerName(testFileItem.virtualPath, testFileItem.ownerName) == 0L)
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
        Assert.assertTrue(fileItemRepository.countByVirtualPath(testFileItem.virtualPath) > 0)
        Assert.assertEquals(fileItemRepository.findByVirtualPathAndOwnerName(testFileItem.virtualPath, testFileItem.ownerName), testFileItem)

//        val newFileItem = testFileItem.copy(ownerName = "QAQ")
//        fileItemRepository.save(newFileItem)
//        Assert.assertTrue(fileItemRepository.countByVirtualPath(testFileItem.virtualPath) == 1L)
//        Assert.assertEquals(fileItemRepository.findByVirtualPath(testFileItem.virtualPath), newFileItem)
//
//        fileItemRepository.delete(testFileItem)
    }
}