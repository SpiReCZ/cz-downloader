package cz.dwn.downloader.config

import cz.dwn.downloader.tor.obtainFreePorts
import org.apache.http.impl.client.CloseableHttpClient
import org.berndpruenster.netlayer.tor.Tor
import org.berndpruenster.netlayer.tor.TorCtlException
import org.berndpruenster.netlayer.tor.Torrc
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.tbk.tor.http.SimpleTorHttpClientBuilder
import org.tbk.tor.spring.config.TorHttpClientAutoConfiguration.TorHttpClientBuilderCustomizer
import java.io.IOException
import java.util.*


@Configuration
class TorConfig {

    @Bean(name = ["torHttpClient"]/*, destroyMethod = "close"*/)
    // Spring AOT issues
    //@Throws(TorCtlException::class)
    fun torHttpClient(
        tor: Tor?,
        torHttpClientBuilderCustomizers: ObjectProvider<TorHttpClientBuilderCustomizer>
    ): CloseableHttpClient? {
        val torHttpClientBuilder = SimpleTorHttpClientBuilder.tor(tor)
        torHttpClientBuilderCustomizers.orderedStream().forEach { customizer: TorHttpClientBuilderCustomizer ->
            customizer.customize(
                torHttpClientBuilder
            )
        }
        return torHttpClientBuilder.build()
    }

    @Bean
    @Throws(IOException::class)
    fun torrc(): Torrc {
        val ports = obtainFreePorts()
        val overrides = LinkedHashMap<String, String>()
        // It is required to set ports manually
        overrides["ControlPort"] = ports.first.toString()
        overrides["SOCKSPort"] = ports.second.toString()
        return Torrc(overrides)
    }

}