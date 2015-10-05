import scala.concurrent.Future
import scala.language.implicitConversions

import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._
import utils.Strings._

package object utils {
  def friendlyClassName[A](a: A): String = a.getClass.getSimpleName.replaceAll("""\$""", "").lowerCaseFirstLetter

  implicit def caseClassToMap(cc: Product): Map[String, Any] = {
    val values = cc.productIterator
    cc.getClass.getDeclaredFields.map( _.getName -> values.next ).toMap
  }

  implicit class OptionError[A](val o: Option[A]) extends AnyVal {
    def getOrError(text: String): A = o.getOrElse {
      throw new NoSuchElementException(text)
    }
  }
}
