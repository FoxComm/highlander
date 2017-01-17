package utils.time

import java.time.Instant
import java.time.format.DateTimeFormatter

import org.json4s._
import org.json4s.JsonAST.JString

/** Since this works on instants, the time zone information is honored, but thrown away. */
object JavaTimeJson4sSerializer {
  val formatter = DateTimeFormatter.ISO_INSTANT

  object jsonFormat
      extends CustomSerializer[Instant](format ⇒
        ({
          case JString(s) ⇒ Instant.from(formatter.parse(s))
          case JNull      ⇒ null
        }, {
          case d: Instant ⇒ JString(formatter.format(d))
        }))
}
