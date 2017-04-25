package utils.time

import io.circe.{Decoder, Encoder}
import java.time.Instant
import java.time.format.DateTimeFormatter

/** Since this works on instants, the time zone information is honored, but thrown away. */
trait JavaInstantJsonCodec {
  val formatter = DateTimeFormatter.ISO_INSTANT

  implicit val decodeInstant: Decoder[Instant] =
    Decoder.decodeString.map(s ⇒ Instant.from(formatter.parse(s)))
  implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap(formatter.format)
}
