package phoenix.utils

import akka.NotUsed
import akka.http.scaladsl.model.{ContentType, ContentTypes}
import akka.http.scaladsl.server.{PathMatcher, PathMatcher1}
import akka.stream.scaladsl.{Concat, Source}
import akka.util.ByteString
import cats.Show
import core.utils.Strings._
import core.utils._
import org.json4s.JsonAST.JString
import org.json4s.jackson.compactJson
import org.json4s.{CustomKeySerializer, CustomSerializer, Formats}
import phoenix.utils.aliases._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

import scala.collection.immutable.TreeMap
import scala.language.implicitConversions

trait Read[F] { self ⇒
  def read(f: String): Option[F]
}

trait ADT[F] extends Read[F] with Show[F] { self ⇒
  implicit lazy val jsonFormats: Formats = JsonFormatters.DefaultFormats

  def types: Set[F]

  val typeMap: Map[String, F] = types.map(f ⇒ show(f) → f).toMap

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

  lazy val pathMatcher: PathMatcher1[F] = PathMatcher(TreeMap(typeMap.toList: _*)(Ordering[String].reverse))
}
object ADT {
  @inline def apply[T](implicit adt: ADT[T]): ADT[T] = adt

  implicit def pathMatcher[T](adt: ADT[T]): PathMatcher1[T] = adt.pathMatcher
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
      val elSep          = ByteString(",")
      val streamStart    = Source.single(ByteString("""{"result": ["""))
      val streamElements = super.bytes(s).grouped(2).map(_.reduceLeft(_ ++ elSep ++ _))
      val streamEnd      = Source.single(ByteString("]}"))

      Source.combine(streamStart, streamElements, streamEnd)(Concat(_))
    }

    def contentType: ContentType = ContentTypes.`application/json`
  }

  def csvChunkable(fields: List[String]): Chunkable[CsvData] =
    new Chunkable[CsvData] {
      private[this] val fieldsSet = fields.toSet

      def bytes(t: CsvData): ByteString =
        ByteString(
          t.collect {
              case (field, value) if fieldsSet.contains(field) ⇒ value
            }
            .mkString(","))

      override def bytes(s: Source[CsvData, NotUsed]): Source[ByteString, NotUsed] = {
        val elSep          = ByteString("\n")
        val streamStart    = Source.single(ByteString(fields.mkString(",")) ++ elSep)
        val streamElements = super.bytes(s).map(_ ++ elSep)

        Source.combine(streamStart, streamElements)(Concat(_))
      }

      def contentType: ContentType = ContentTypes.`text/csv(UTF-8)`
    }
}
