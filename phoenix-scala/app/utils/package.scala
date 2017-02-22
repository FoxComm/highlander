import scala.language.implicitConversions

import utils.Strings._
import utils.aliases.{SF, SL}

package object utils {
  def generateUuid: String = java.util.UUID.randomUUID.toString

  def friendlyClassName[A](a: A): String =
    a.getClass.getSimpleName.replaceAll("""\$""", "").lowerCaseFirstLetter

  def snakeCaseName[A](a: A): String =
    camelToUnderscores(a.getClass.getSimpleName.replaceAll("""\$""", "")).replaceFirst("_", "")

  def camelToUnderscores(name: String) =
    "[A-Z\\d]".r.replaceAllIn(name, { m ⇒
      "_" + m.group(0).toLowerCase
    })

  def codeSource(implicit line: SL, file: SF): String =
    s"""${file.value.split("/").last}:${line.value}"""

  implicit class OptionError[A](val o: Option[A]) extends AnyVal {
    def getOrError(text: String): A = o.getOrElse {
      throw new NoSuchElementException(text)
    }
  }
}
