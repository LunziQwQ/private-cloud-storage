package pw.lunzi.privatecloudstorage

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PrivateCloudStorageApplication

fun main(args: Array<String>) {
    runApplication<PrivateCloudStorageApplication>(*args)
}
