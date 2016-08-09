package utils.time

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.ZoneId

object JavaInstantFormatters {
  private val formatterPattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  private val formatter = DateTimeFormatter
    .ofPattern(formatterPattern)
    .withLocale(Locale.UK)
    .withZone(ZoneId.systemDefault())

  implicit class EnrichedInstant4Formatters(val instant: Instant) extends AnyVal {
    def forEs(): String = formatter.format(instant)
  }

}
