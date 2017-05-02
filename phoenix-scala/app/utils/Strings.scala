package utils

import scala.language.implicitConversions
import mojolly.inflector.InflectorImports._

object Strings {
  final implicit class EnrichedString(val s: String) extends AnyVal {
    def lowerCaseFirstLetter = s.headOption.map(_.toLower.toString).getOrElse("") + s.drop(1)
    def upperCaseFirstLetter = s.capitalize
    def underscoreToCamel    = "_([a-z])".r.replaceAllIn(s, _.group(1).toUpperCase)
    def tableNameToCamel     = s.underscoreToCamel.singularize
  }
}