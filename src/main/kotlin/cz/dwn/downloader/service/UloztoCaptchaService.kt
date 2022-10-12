package cz.dwn.downloader.service

import ai.djl.inference.*
import ai.djl.modality.*
import ai.djl.modality.cv.*
import ai.djl.modality.cv.util.*
import ai.djl.ndarray.*
import ai.djl.ndarray.types.DataType
import ai.djl.repository.zoo.*
import ai.djl.training.util.*
import ai.djl.translate.*
import ai.djl.util.*
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.net.*
import java.nio.file.*
import java.util.*


@Service
class UloztoCaptchaService {
    private val log = LoggerFactory.getLogger("captcha-breaker")

    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup() {
        test()
    }

    private fun test() {
        val captchaImage = BufferedImageFactory.getInstance()
            .fromFile(Paths.get("image2.jpg"))
        with(loadModel()) {
            val predictor: Predictor<Image, String> = this.newPredictor()
            val captcheValue = predictor.predict(captchaImage)
            log.info("CAPTCHA auto solved as '${captcheValue}'")
        }
    }

    private fun loadModel(): ZooModel<Image, String> {
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
        return decode(indices)
    }

    private fun decode(indexes: NDArray): String {
        return buildString {
            for (i: Long in indexes.toLongArray()) {
                this.append(availableChars[i.toInt()])
            }
        }
    }

    override fun getBatchifier(): Batchifier? {
        // stack would
        //return Batchifier.STACK
        return null
    }
}