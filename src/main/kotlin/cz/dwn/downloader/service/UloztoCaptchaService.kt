package cz.dwn.downloader.service

import ai.djl.inference.Predictor
import ai.djl.modality.cv.Image
import ai.djl.ndarray.NDList
import ai.djl.ndarray.types.DataType
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import ai.djl.training.util.ProgressBar
import ai.djl.translate.Batchifier
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class UloztoCaptchaService {
    private val log = LoggerFactory.getLogger("captcha-breaker")

    /*
    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup() {
        val captchaImage = BufferedImageFactory.getInstance()
            .fromFile(Paths.get("image2.jpg"))
        val captchaValue = runBlocking {
            solve(captchaImage)
        }
        log.info("CAPTCHA auto solved as '${captchaValue}'")
    }

     */

    suspend fun solve(captchaImage: Image): String {
        with(loadModel()) {
            val predictor: Predictor<Image, String> = this.newPredictor()
            val captchaValue = predictor.predict(captchaImage)
            log.info("CAPTCHA auto solved as '${captchaValue}'")
            return captchaValue
        }
    }

    private suspend fun loadModel(): ZooModel<Image, String> {
        val modelUrl = "https://github.com/JanPalasek/ulozto-captcha-breaker/releases/download/v2.2/model.zip";
        val criteria: Criteria<Image, String> = Criteria.builder()
            .setTypes(Image::class.java, String::class.java)
            .optModelUrls(modelUrl)
            .optTranslator(UloztoCaptchaTranslator())
            .optProgress(ProgressBar())
            .build()
        return criteria.loadModel()
    }
}

class UloztoCaptchaTranslator : Translator<Image, String> {
    private val availableChars = "abcdefghijklmnopqrstuvwxyz"

    override fun processInput(ctx: TranslatorContext?, input: Image?): NDList {
        val manager = ctx!!.ndManager
        // convert to grayscale
        var array = input!!.toNDArray(manager, Image.Flag.GRAYSCALE)
        // normalize to [0...1]
        array = array.toType(DataType.FLOAT32, true).div(255.0f)
        // input has now shape of (70, 175, 1)
        // we modify dimensions to match model's input
        array = array.expandDims(0)
        // input is now shape of (batch_size, 70, 175, 1)
        // output will have shape (batch_size, 4, 26)
        return NDList(array)
    }

    override fun processOutput(ctx: TranslatorContext?, list: NDList?): String {
        val probabilities = list!!.singletonOrThrow()
        val indices = probabilities.argMax(2)
        return buildString {
            for (l in indices.toLongArray()) {
                this.append(availableChars[l.toInt()])
            }
        }
    }

    override fun getBatchifier(): Batchifier? {
        // Batchifier would normally modify array to be 4 dimensional, but we do this manually
        //return Batchifier.STACK
        return null
    }
}
