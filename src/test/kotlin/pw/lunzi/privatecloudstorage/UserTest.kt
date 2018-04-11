package pw.lunzi.privatecloudstorage

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.junit4.SpringRunner

/**
 * ***********************************************
 * Created by Lunzi on 4/5/2018.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */

@RunWith(SpringRunner::class)
@DataMongoTest
class UserTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun testUserRepo(){
        val testuser = User("Lunzi", "test")
        userRepository.save(testuser)
        Assert.assertTrue(userRepository.countByUsername(testuser.username) > 0)
        Assert.assertEquals(userRepository.findByUsername(testuser.username), testuser)
        userRepository.delete(testuser)
        Assert.assertTrue(userRepository.countByUsername(testuser.username) == 0L)
    }
}