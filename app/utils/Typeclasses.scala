package utils

import scalaz._
import Strings._
import org.json4s.JsonAST.JString
import org.json4s.{jackson, CustomSerializer, DefaultFormats}
import org.json4s.jackson.Serialization.{write ⇒ jsonWrite}
import slick.driver.PostgresDriver.api._

trait Read[F] { self ⇒
  def read(f: String): Option[F]
}

trait ADT[F] extends Read[F] with Show[F] { self ⇒
  implicit val jsonFormats = DefaultFormats
  implicit val serialization = jackson.Serialization

  def types: Set[F]

  val typeMap: Map[String, F] =
    types.foldLeft(Map[String, F]()) { case (m, f) ⇒ m.updated(shows(f), f) }

  def read(s: String) = typeMap.get(s)

  override def shows(f: F): String = f.toString.lowerCaseFirstLetter

  def jsonFormat(implicit m: Manifest[F]): CustomSerializer[F] = new CustomSerializer[F](format => ({
    case JString(str) ⇒ read(str).get // if we cannot deserialize then we throw. Yes, I know it's not *pure*.
  }, {
    case f: F ⇒ JString(shows(f))
  }))

  def slickColumn(implicit m: Manifest[F]) = MappedColumnType.base[F, String]({
    case f ⇒ shows(f)
  },{
    case f ⇒ read(f).get
  })
}


