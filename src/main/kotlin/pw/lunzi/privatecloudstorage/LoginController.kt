package pw.lunzi.privatecloudstorage

import org.springframework.security.access.prepost.PreAuthorize
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

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping("/")
    fun hello() = "hello world"

    @RequestMapping("/api/admin/e")
    fun helloe() = "hello world"
}
