package cz.dwn.downloader

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.tbk.tor.spring.config.TorHiddenServiceAutoConfiguration

@SpringBootApplication(
    exclude = [TorHiddenServiceAutoConfiguration::class]
)
class DownloaderApplication

fun main(args: Array<String>) {
    runApplication<DownloaderApplication>(*args)
}
