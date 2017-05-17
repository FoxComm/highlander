package phoenix.utils

import cats.Show
import org.json4s.JsonAST.JString
import org.json4s.{CustomKeySerializer, CustomSerializer, Formats}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import utils.Strings._
import utils._

trait Read[F] { self ⇒
  def read(f: String): Option[F]
}

trait ADT[F] extends Read[F] with Show[F] { self ⇒
  implicit lazy val jsonFormats: Formats = JsonFormatters.DefaultFormats

  def types: Set[F]

  val typeMap: Map[String, F] = types.foldLeft(Map[String, F]()) {
    case (m, f) ⇒ m.updated(show(f), f)
  }

  def read(s: String): Option[F] = typeMap.get(s)

  override def show(f: F): String = f.toString.lowerCaseFirstLetter

  /**
    * Json4s works by matching types against Any at runtime so we need to support these features.
    */
  def jsonFormat(implicit m: Manifest[F]): CustomSerializer[F] =
    new CustomSerializer[F](format ⇒
          ({
        case JString(str) ⇒
          read(str)
            .getOrError(s"No such element: $str") // if we cannot deserialize then we throw. Yes, I know it's not *pure*.
      }, {
        case f: F ⇒ JString(show(f))
      }))

  // let's play find the difference game
  // but you know, `KeySerializer` does not extend `Serializer`, so it's completely different thing
  // thanks json4s!
  def jsonKeyFormat(implicit m: Manifest[F]): CustomKeySerializer[F] =
    new CustomKeySerializer[F](format ⇒
          ({
        case str ⇒
          read(str)
            .getOrError(s"No such element: $str") // if we cannot deserialize then we throw. Yes, I know it's not *pure*.
      }, {
        case f: F ⇒ show(f)
      }))

  def slickColumn(implicit m: Manifest[F]): JdbcType[F] with BaseTypedType[F] =
    MappedColumnType.base[F, String]({
      case f ⇒ show(f)
    }, {
      case f ⇒ read(f).getOrError(s"No such element: $f")
    })
}
object ADT {
  @inline def apply[T](implicit adt: ADT[T]): ADT[T] = adt
}
