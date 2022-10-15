package cz.dwn.downloader.service

import ai.djl.inference.Predictor
import ai.djl.modality.cv.Image
import ai.djl.ndarray.NDList
import ai.djl.ndarray.types.DataType
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import ai.djl.translate.Batchifier
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import cz.dwn.downloader.props.AppModelProps
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class UloztoCaptchaService(models: AppModelProps) {

    private val log = LoggerFactory.getLogger("captcha-breaker")

    private val model: ZooModel<Image, String>
    private val predictor: Predictor<Image, String>

    init {
        model = loadModel(models.ulozto)
        predictor = model.newPredictor()
    }

    suspend fun solve(captchaImage: Image): String {
        val captchaValue = predictor.predict(captchaImage)
        log.info("CAPTCHA auto solved as '${captchaValue}'")
        return captchaValue
    }

    private fun loadModel(modelUris: String): ZooModel<Image, String> {
        val criteria: Criteria<Image, String> = Criteria.builder()
            .setTypes(Image::class.java, String::class.java)
            .optModelUrls(modelUris)
            .optTranslator(UloztoCaptchaTranslator)
            //.optProgress(ProgressBar())
            .build()
        return criteria.loadModel()
    }
}

object UloztoCaptchaTranslator : Translator<Image, String> {
    private const val availableChars = "abcdefghijklmnopqrstuvwxyz"

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
            for (charPosition in indices.toLongArray()) {
                this.append(availableChars[charPosition.toInt()])
            }
        }
    }

    /**
     * Batchifier would normally modify array to be 4 dimensional, but we do this manually
     */
    override fun getBatchifier(): Batchifier? = null
}
