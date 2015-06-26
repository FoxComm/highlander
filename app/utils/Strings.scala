package utils

import scala.language.implicitConversions

object Strings {
  class EnrichedString(val s: String) {
    def lowerCaseFirstLetter = s(0).toLower + s.substring(1)

    def upperCaseFirstLetter = s(0).toUpper + s.substring(1)
  }

  implicit def stringToEnrichedString(s: String): EnrichedString = new EnrichedString(s)
}

