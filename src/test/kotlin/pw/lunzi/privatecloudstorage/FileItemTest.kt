package pw.lunzi.privatecloudstorage

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataMongoTest
class FileItemTest {
    @Autowired
    lateinit var fileItemRepository: FileItemRepository

    @Test
    fun testSave() {
        val testFileItem = FileItem(
                ownerName = "Lunzi",
                realPath = "root/test",
                virtualPath ="Lunzi/",
                virtualName = "test",
                isDictionary = false,
                isUserRootPath = false,
                isPublic = true
        )
        fileItemRepository.save(testFileItem)
        Assert.assertEquals(fileItemRepository.findByVirtualPathAndVirtualName(testFileItem.virtualPath, testFileItem.virtualName), testFileItem)
        fileItemRepository.delete(testFileItem)
        Assert.assertTrue(fileItemRepository.countByVirtualPathAndVirtualNameAndOwnerName(testFileItem.virtualPath, testFileItem.virtualName, testFileItem.ownerName) == 0L)
    }

    @Test
    fun testUpdate(){
        val testFileItem = FileItem(
                ownerName = "Lunzi",
                realPath = "root/test",
                virtualPath = ("Lunzi/"),
                virtualName = "test",
                isDictionary = false,
                isUserRootPath = false,
                isPublic = true
        )
        fileItemRepository.save(testFileItem)
        Assert.assertEquals(fileItemRepository.findByVirtualPathAndVirtualName(testFileItem.virtualPath, testFileItem.virtualName), testFileItem)

        val newFileItem = testFileItem.copy(ownerName = "QAQ")
        fileItemRepository.save(newFileItem)
        Assert.assertEquals(fileItemRepository.findByVirtualPathAndVirtualName(testFileItem.virtualPath, testFileItem.virtualName), newFileItem)

        fileItemRepository.delete(testFileItem)
    }
}