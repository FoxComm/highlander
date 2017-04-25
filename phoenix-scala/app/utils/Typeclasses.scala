package utils

import cats.Show
import cats.implicits._
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.Strings._

trait Read[F] { self ⇒
  def read(f: String): Option[F]
}

trait ADT[F] extends Read[F] with Show[F] { self ⇒
  def types: Set[F]

  val typeMap: Map[String, F] = types.foldLeft(Map[String, F]()) {
    case (m, f) ⇒ m.updated(show(f), f)
  }

  def read(s: String): Option[F] = typeMap.get(s)

  override def show(f: F): String = f.toString.lowerCaseFirstLetter

  def slickColumn(implicit m: Manifest[F]): JdbcType[F] with BaseTypedType[F] =
    MappedColumnType.base[F, String](show, f ⇒ read(f).getOrError(s"No such element: $f"))
}
object ADT {
  @inline def apply[T](implicit adt: ADT[T]): ADT[T] = adt

  implicit def adtDecoder[T: ADT]: Decoder[T] =
    Decoder.decodeString.emap(str ⇒
          ADT[T].read(str).map(Either.right(_)).getOrElse(Either.left(s"No such element: $str")))

  implicit def adtEncoder[T: ADT]: Encoder[T] = Encoder.encodeString.contramap(ADT[T].show(_))

  implicit def adtKeyDecoder[T: ADT]: KeyDecoder[T] = new KeyDecoder[T] {
    def apply(key: String): Option[T] = ADT[T].read(key)
  }

  implicit def adtKeyEncoder[T: ADT]: KeyEncoder[T] = new KeyEncoder[T] {
    def apply(key: T): String = ADT[T].show(key)
  }
}
