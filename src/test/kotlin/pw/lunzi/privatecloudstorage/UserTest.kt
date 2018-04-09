package pw.lunzi.privatecloudstorage

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

/**
 * ***********************************************
 * Created by Lunzi on 4/5/2018.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */

@RunWith(SpringRunner::class)
@SpringBootTest
class UserTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun testDBConnection(){
        userRepository.save(User("lunzi","123"))

//        println("userRepositoryCount = ${userRepository.count()}")
    }
}