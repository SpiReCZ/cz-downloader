package cz.dwn.downloader.config

import org.berndpruenster.netlayer.tor.Torrc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException
import java.util.*

@Configuration
class TorConfig {

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