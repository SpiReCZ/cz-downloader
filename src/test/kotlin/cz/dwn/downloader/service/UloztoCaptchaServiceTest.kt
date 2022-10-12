package cz.dwn.downloader.service

import ai.djl.modality.cv.BufferedImageFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.nio.file.Paths

@SpringBootTest
internal class UloztoCaptchaServiceTest {

    @Autowired
    private lateinit var captchaService: UloztoCaptchaService

    @Test
    fun solve() {
        val captchaImage = BufferedImageFactory.getInstance()
            .fromFile(Paths.get("image2.jpg"))
        fun solve() = runBlocking {
            captchaService.solve(captchaImage)
        }
        val captchaValue = solve()
        println(captchaValue)
    }
}
