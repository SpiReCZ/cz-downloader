package cz.dwn.downloader.service

import ai.djl.modality.cv.BufferedImageFactory
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource

@SpringBootTest
internal class UloztoCaptchaServiceTest {

    @Autowired
    private lateinit var captchaService: UloztoCaptchaService

    @ParameterizedTest
    @ValueSource(strings = ["ovnh", "pkqt"])
    fun solve(captchaValue: String) {
        val captchaImage = with(ClassPathResource("captchas/${captchaValue}.jpg").inputStream) {
            BufferedImageFactory.getInstance().fromInputStream(this)
        }
        val computedCaptchaValue = runBlocking {
            captchaService.solve(captchaImage)
        }
        assertThat(computedCaptchaValue).isEqualTo(captchaValue)
    }
}
