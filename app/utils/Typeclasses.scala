package utils

import cats.Show
import org.json4s.JsonAST.JString
import org.json4s.jackson.Serialization.{write ⇒ jsonWrite}
import org.json4s.{CustomSerializer, DefaultFormats, jackson}
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.Strings._

trait Read[F] { self ⇒
  def read(f: String): Option[F]
}

trait ADT[F] extends Read[F] with Show[F] { self ⇒
  implicit val jsonFormats = JsonFormatters.DefaultFormats

  def types: Set[F]

  val typeMap: Map[String, F] =
    types.foldLeft(Map[String, F]()) { case (m, f) ⇒ m.updated(show(f), f) }

  def read(s: String): Option[F] = typeMap.get(s)

  override def show(f: F): String = f.toString.lowerCaseFirstLetter

  /**
   * Json4s works by matching types against Any at runtime so we need to support these features.
   */
  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Any", "org.brianmckenna.wartremover.warts.IsInstanceOf"))
  def jsonFormat(implicit m: Manifest[F]): CustomSerializer[F] = new CustomSerializer[F](format => ({
    case JString(str) ⇒ read(str).get // if we cannot deserialize then we throw. Yes, I know it's not *pure*.
  }, {
    case f: F ⇒ JString(show(f))
  }))

  def slickColumn(implicit m: Manifest[F]): JdbcType[F] with BaseTypedType[F] = MappedColumnType.base[F, String]({
    case f ⇒ show(f)
  },{
    case f ⇒ read(f).get
  })
}


