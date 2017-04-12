package utils

import akka.NotUsed
import akka.http.scaladsl.model.{ContentType, ContentTypes}
import akka.stream.scaladsl.{Concat, Source}
import akka.util.ByteString
import cats.Show
import org.json4s.JsonAST.JString
import org.json4s.jackson.compactJson
import org.json4s.jackson.Serialization.{write ⇒ jsonWrite}
import org.json4s.{CustomKeySerializer, CustomSerializer, DefaultFormats, Formats, jackson}
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.Strings._
import utils.aliases.Json

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

trait Chunkable[T] {
  def bytes(t: T): ByteString

  def bytes(s: Source[T, NotUsed]): Source[ByteString, NotUsed] = s.map(bytes)

  def contentType: ContentType
}
object Chunkable {
  @inline def apply[T]()(implicit c: Chunkable[T]): Chunkable[T] = c

  implicit val jsonChunkable: Chunkable[Json] = new Chunkable[Json] {
    def bytes(t: Json): ByteString = ByteString(compactJson(t))

    override def bytes(s: Source[Json, NotUsed]): Source[ByteString, NotUsed] = {
      val sep            = ByteString(",")
      val streamStart    = Source.single(ByteString("""{"result": ["""))
      val streamElements = super.bytes(s).grouped(2).map(_.reduceLeft(_ ++ sep ++ _))
      val streamEnd      = Source.single(ByteString("]}"))

      Source.combine(streamStart, streamElements, streamEnd)(Concat(_))
    }

    def contentType: ContentType = ContentTypes.`application/json`
  }
}
