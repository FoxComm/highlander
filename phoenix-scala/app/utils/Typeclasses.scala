package utils

import cats.Show
import cats.implicits._
import io.circe._
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

  implicit val decodeADT: Decoder[F] = Decoder.decodeString.emap(str ⇒
        read(str).map(Either.right(_)).getOrElse(Either.left(s"No such element: $str")))

  implicit val decodeADTKey: KeyDecoder[F] = new KeyDecoder[F] {
    def apply(key: String): Option[F] = read(key)
  }

  implicit val encodeADT: Encoder[F] = Encoder.encodeString.contramap(show)

  implicit val encodeADTKey: KeyEncoder[F] = KeyEncoder.encodeKeyString.contramap(show)
}
object ADT {
  @inline def apply[T](implicit adt: ADT[T]): ADT[T] = adt
}
