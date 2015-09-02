package utils

import scala.language.implicitConversions

object Strings {
  final implicit class EnrichedString(val s: String) extends AnyVal {
    def lowerCaseFirstLetter = s(0).toLower + s.substring(1)
    def upperCaseFirstLetter = s(0).toUpper + s.substring(1)
  }
}

