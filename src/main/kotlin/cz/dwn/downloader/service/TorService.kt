package cz.dwn.downloader.service

import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils
import org.berndpruenster.netlayer.tor.Tor
import org.berndpruenster.netlayer.tor.TorController
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.io.IOException
import java.lang.reflect.Field
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration


@Service
class TorService(private val tor: Tor, @Qualifier("torHttpClient") private val torHttpClient: CloseableHttpClient) {

    private val torController: TorController

    init {
        val torControllerField: Field = Tor::class.java.getDeclaredField("torController")
        torControllerField.isAccessible = true
        torController = torControllerField.get(tor) as TorController
    }

    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup() {
        test()
    }

    fun Tor.reload() {
        torController.setConf("DisableNetwork", "1")
        torController.setConf("DisableNetwork", "0")
    }

    fun test() {
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

            val body = Flux.interval(Duration.ofSeconds(10))
                .flatMap { _: Long? ->
                    try {
                        torHttpClient.execute(request).use { response ->
                            val entity: String = EntityUtils.toString(response!!.entity, StandardCharsets.UTF_8)
                            return@flatMap Flux.just(entity)
                        }
                    } catch (e: IOException) {
                        print("Exception while polling for ${url}: ${e.message}")
                        return@flatMap Flux.empty<String>()
                    }
                }
                .blockFirst(Duration.ofMinutes(3))

            print(body)
            tor.reload()
            Thread.sleep(2000)
        }
    }
}
