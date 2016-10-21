package testutils

import org.json4s.{DefaultFormats, Extraction}
import utils.aliases._

object PayloadHelpers {

  implicit lazy val formats = DefaultFormats

  implicit class AttributesJsonifyValues(val attrs: Map[String, Any]) extends AnyVal {
    def jsonifyValues: Map[String, Json] =
      attrs.mapValues(Extraction.decompose)
  }

}
