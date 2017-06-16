package core

import core.utils.Strings._

import scala.language.implicitConversions

package object utils {
  def generateUuid: String = java.util.UUID.randomUUID.toString // FIXME: please… @michalrus

  def friendlyClassName[A](a: A): String =
    a.getClass.getSimpleName.replaceAll("""\$""", "").lowerCaseFirstLetter

  def snakeCaseName[A](a: A): String =
    camelToUnderscores(a.getClass.getSimpleName.replaceAll("""\$""", "")).replaceFirst("_", "")

  def camelToUnderscores(name: String) =
    "[A-Z\\d]".r.replaceAllIn(name, { m ⇒
      "_" + m.group(0).toLowerCase
    })

  implicit class OptionError[A](val o: Option[A]) extends AnyVal {
    def getOrError(text: String): A =
      o.getOrElse { // FIXME: PLEASE. Prove statically at usage sites. @michalrus
        throw new NoSuchElementException(text)
      }
  }
}
