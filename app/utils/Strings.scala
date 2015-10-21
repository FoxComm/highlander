package utils

import scala.language.implicitConversions

object Strings {
  final implicit class EnrichedString(val s: String) extends AnyVal {
    def lowerCaseFirstLetter = s.headOption.map(_.toLower.toString).getOrElse("") + s.drop(1)
    def upperCaseFirstLetter = s.capitalize
    def underscoreToCamel = "_([a-z])".r.replaceAllIn(s, _.group(1).toUpperCase())
  }
}

