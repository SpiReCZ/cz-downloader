package cz.dwn.downloader.config

import cz.dwn.downloader.tor.obtainFreePorts
import org.berndpruenster.netlayer.tor.TorCtlException
import org.berndpruenster.netlayer.tor.Torrc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException
import java.util.*


@Configuration
class TorConfig @Throws(TorCtlException::class) constructor() {

    @Bean
    @Throws(IOException::class)
    fun torrc(): Torrc {
        val ports = obtainFreePorts()
        val overrides = LinkedHashMap<String, String>()
        // It is required to set ports manually
        overrides["ControlPort"] = ports.first.toString()
        overrides["SOCKSPort"] = ports.second.toString()
        // We cannot run as Daemon
        overrides["RunAsDaemon"] = "0"
        return Torrc(overrides)
    }

}