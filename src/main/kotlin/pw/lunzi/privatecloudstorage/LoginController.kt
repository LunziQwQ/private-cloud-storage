package pw.lunzi.privatecloudstorage

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * ***********************************************
 * Created by Lunzi on 3/27/2018.
 * Just presonal practice.
 * Not allowed to copy without permission.
 * ***********************************************
 */
@RestController
class LoginController {
    @RequestMapping("/")
    fun hello() = "hello world"
}
