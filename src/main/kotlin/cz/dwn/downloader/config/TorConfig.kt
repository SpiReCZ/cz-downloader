package cz.dwn.downloader.config

import cz.dwn.downloader.tor.DisposableTorFactory
import org.berndpruenster.netlayer.tor.NativeTor
import org.berndpruenster.netlayer.tor.Tor
import org.berndpruenster.netlayer.tor.Torrc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.tbk.tor.NativeTorFactory
import org.tbk.tor.TorFactory
import org.tbk.tor.spring.config.TorAutoConfigProperties
import java.io.File
import java.io.IOException
import java.util.*

@Configuration
class TorConfig(properties: TorAutoConfigProperties) {

    private val properties: TorAutoConfigProperties

    init {
        this.properties = Objects.requireNonNull(properties)
    }

    @Bean(destroyMethod = "shutdown")
    fun nativeTor(nativeTorFactory: TorFactory<NativeTor?>): NativeTor? {
        val tor = nativeTorFactory.create()
            .blockOptional(properties.startupTimeout)
            .orElseThrow { IllegalStateException("Could not start tor") }
        if (Tor.default == null) {
            // set default instance, so it can be omitted whenever creating Tor (Server)Sockets
            Tor.default = tor
        }
        return tor
    }

    @Bean
    fun nativeTorFactory(torrc: Torrc?): TorFactory<NativeTor?>? {
        val workingDirectory = File(properties.workingDirectory)
        return NativeTorFactory(workingDirectory, torrc)
    }

    @Bean
    fun disposableTorFactory(tor: Tor): DisposableTorFactory {
        val cookieFile = File(properties.workingDirectory, ".tor/control_auth_cookie")
        return DisposableTorFactory(44443, cookieFile)
    }

    @Bean
    @Throws(IOException::class)
    fun torrc(): Torrc {
        val torrcEntries: LinkedHashMap<String, String> = LinkedHashMap<String, String>()
        torrcEntries["ControlPort"] = "44443"
        torrcEntries["SOCKSPort"] = "44444"
        torrcEntries["CookieAuthentication"] = "1"
        torrcEntries["AvoidDiskWrites"] = "1"
        torrcEntries["RunAsDaemon"] = "0"
        torrcEntries["DisableNetwork"] = "0"
        return Torrc(torrcEntries)
    }
}