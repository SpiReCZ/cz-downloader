package cz.dwn.downloader.props

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("app.downloader.model")
class AppModelProps {
    var ulozto: String = "model.zip"
}