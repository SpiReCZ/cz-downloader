package cz.dwn.downloader

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DownloaderApplication

fun main(args: Array<String>) {
    runApplication<DownloaderApplication>(*args)
}
