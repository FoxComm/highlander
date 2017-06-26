package core.utils

import mojolly.inflector.InflectorImports._

object Strings {
  final implicit class EnrichedString(val s: String) extends AnyVal {
    def lowerCaseFirstLetter: String =
      s.headOption.map(_.toLower.toString).getOrElse("") + s.drop(1)
    def underscoreToCamel: String = "_([a-z])".r.replaceAllIn(s, _.group(1).toUpperCase)
    def tableNameToCamel: String  = s.underscoreToCamel.singularize
    def underscore: String =
      s.flatMap {
          case c if c.isUpper ⇒ s"_${c.toLower}"
          case c              ⇒ s"$c"
        }
        .stripPrefix("_")
    def prettify: String = s.split("(?=\\p{Upper})").mkString(" ")
    def quote(escapeChar: Char = '\\'): String = {
      val escaped =
        if (escapeChar == '"') s else s.replace(s"$escapeChar", s"$escapeChar$escapeChar")

      "\"" + escaped.replace("\"", s"""$escapeChar"""") + "\""
    }
  }
}
