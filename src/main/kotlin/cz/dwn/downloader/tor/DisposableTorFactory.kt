package cz.dwn.downloader.tor

import org.berndpruenster.netlayer.tor.ExternalTor
import org.berndpruenster.netlayer.tor.TorCtlException
import org.tbk.tor.TorFactory
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import java.io.File

class DisposableTorFactory(private val port: Int, private val cookieFile: File) : TorFactory<ExternalTor> {
    override fun create(): Mono<ExternalTor> {
        return Mono.create { fluxSink: MonoSink<ExternalTor> ->
            try {
                fluxSink.success(ExternalTor(port, cookieFile, true))
            } catch (e: TorCtlException) {
                fluxSink.error(e)
            }
        }
    }
}