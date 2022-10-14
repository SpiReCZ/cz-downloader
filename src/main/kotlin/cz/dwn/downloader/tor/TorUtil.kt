package cz.dwn.downloader.tor

import org.berndpruenster.netlayer.tor.TorCtlException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

@Throws(TorCtlException::class)
fun obtainFreePorts(): Pair<Int, Int> {
    val initialPortValue = 0
    val port1 = AtomicInteger(initialPortValue)
    val port2 = AtomicInteger(initialPortValue)

    fun assignPortIfAvailable(target: AtomicInteger, value: Int): Boolean {
        if (isTcpPortAvailable(value)) {
            target.set(value)
            return true
        }
        return false
    }

    // attempt to obtain 2 ports from client port range
    for (int in 49152..65535) {
        if (port1.get() == initialPortValue) {
            assignPortIfAvailable(port1, int)
        } else {
            assignPortIfAvailable(port2, int + 1)
            break
        }
    }

    if (port1.get() == initialPortValue || port2.get() == initialPortValue) {
        throw TorCtlException("Unable to obtain Tor Control port or Socks port")
    }

    return Pair(port1.get(), port2.get())
}

fun isTcpPortAvailable(port: Int): Boolean {
    try {
        ServerSocket().use { serverSocket ->
            // setReuseAddress(false) is required only on macOS,
            // otherwise the code will not work correctly on that platform
            serverSocket.reuseAddress = false
            serverSocket.bind(InetSocketAddress(InetAddress.getByName("localhost"), port), 1)
            return true
        }
    } catch (ex: Exception) {
        return false
    }
}