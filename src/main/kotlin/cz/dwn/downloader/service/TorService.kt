package cz.dwn.downloader.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils
import org.berndpruenster.netlayer.tor.Tor
import org.berndpruenster.netlayer.tor.TorController
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.io.IOException
import java.lang.reflect.Field
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration


@Service
class TorService(tor: Tor, @Qualifier("torHttpClient") private val torHttpClient: CloseableHttpClient) {

    private val log = LoggerFactory.getLogger("tor-service")
    private val torController: TorController

    init {
        val torControllerField: Field = Tor::class.java.getDeclaredField("torController")
        torControllerField.isAccessible = true
        torController = torControllerField.get(tor) as TorController
    }

    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup() {
        runBlocking {
            test()
        }
    }

    suspend fun test() {
        val url: URI = URIBuilder()
            .setScheme("https")
            .setHost("api.myip.com")
            .setPort(443)
            .setPath("")
            .build()

        val requestConfig: RequestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Duration.ofSeconds(10).toMillis().toInt())
            .setConnectTimeout(Duration.ofSeconds(10).toMillis().toInt())
            .setSocketTimeout(Duration.ofSeconds(10).toMillis().toInt())
            .build()

        while (true) {

            val request = HttpGet(url)
            request.config = requestConfig

            makeRequest(request).single().use {
                print(EntityUtils.toString(it.entity, StandardCharsets.UTF_8))
            }

            reloadNetwork()
        }
    }

    @Throws(IOException::class, ClientProtocolException::class)
    fun makeRequest(request: HttpRequestBase): Flow<CloseableHttpResponse> {
        return flowOf(torHttpClient.execute(request)).flowOn(Dispatchers.IO).retryWhen { cause, attempt ->
            log.warn("Retrying Tor network request..")
            delay(1000)
            attempt < 3 || cause is IOException
        }
    }

    suspend fun reloadNetwork() {
        disableNetwork()
        delay(2000)
        enableNetwork()
    }

    suspend fun disableNetwork() {
        torController.setConf(DISABLE_NETWORK_KEY, "1")
    }

    suspend fun enableNetwork() {
        torController.setConf(DISABLE_NETWORK_KEY, "0")
    }

    companion object {
        private const val DISABLE_NETWORK_KEY = "DisableNetwork"
    }

}
