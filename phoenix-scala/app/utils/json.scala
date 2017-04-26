package utils

import cats.implicits._
import cats.kernel.Monoid
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.tminglei.slickpg.LTree
import com.ning.http.client.Response
import io.circe.generic.extras.{AutoDerivation, Configuration}
import io.circe.jackson.CirceJsonModule
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json, parser}
import scala.annotation.tailrec
import scala.reflect.{ClassTag, classTag}
import utils.Money.Currency
import utils.aliases.Json
import utils.db.ExPostgresDriver.api._
import utils.time.JavaInstantJsonCodec

object json {
  type JsonField = (String, Json)

  private[this] val mapper = new ObjectMapper().registerModule(CirceJsonModule)

  implicit class RichJson(val json: Json) extends AnyVal {
    def asJsonNode: JsonNode = mapper.valueToTree[JsonNode](json)

    // port of json4s '\'
    def \(key: String): Option[Json] = {
      @tailrec
      def findRec(toProcess: Vector[Json], acc: Vector[Json]): Vector[Json] = toProcess match {
        case j +: js ⇒
          j.asArray match {
            case Some(values) ⇒ findRec(js ++ values, acc)
            case None         ⇒ findRec(js, acc ++ j.asObject.flatMap(_ (key)))
          }
        case js if js.isEmpty ⇒ acc
      }

      json.arrayOrObject(
          None,
          values ⇒ {
            val vs = findRec(values, Vector.empty)
            if (vs.nonEmpty) Some(Json.arr(vs: _*)) else None
          },
          _ (key)
      )
    }

    def foldField[A](z: A)(f: (A, JsonField) ⇒ A): A = {
      def rec(acc: A, v: Json) = {
        v.arrayOrObject(
            acc,
            _.foldLeft(acc)((a, e) ⇒ e.foldField(a)(f)),
            _.toVector.foldLeft(acc) {
              case (a, field @ (_, value)) ⇒ value.foldField(f(a, field))(f)
            }
        )
      }

      rec(z, json)
    }

    def transformField(f: PartialFunction[JsonField, JsonField]): Json =
      json.hcursor
        .withFocus(
            jv ⇒
              jv.arrayOrObject(
                  jv,
                  jarr ⇒ Json.fromValues(jarr.map(_.transformField(f))),
                  jobj ⇒
                    Json.fromFields(jobj.toMap.map {
                  case (k, v) ⇒
                    val kv = k → v.transformField(f)
                    if (f.isDefinedAt(kv)) f(kv) else kv
                })
            ))
        .focus
        .getOrElse(json)
  }

  def dbJsonColumn[A: ClassTag: Decoder: Encoder]: BaseColumnType[A] = {
    import yolo._

    MappedColumnType.base[A, Json](_.asJson, _.extract[A])
  }

  object asJson extends (Response ⇒ Json) {
    def apply(v1: Response): Json = ???
  }

  object codecs extends AutoDerivation with JavaInstantJsonCodec {
    implicit val configuration: Configuration =
      Configuration.default.withDefaults.withDiscriminator("payloadType")

    implicit val decodeCurrency: Decoder[Currency] =
      Decoder.decodeString.map(s ⇒ Currency(s.toUpperCase()))
    implicit val encodeCurrency: Encoder[Currency] = Encoder.encodeString.contramap(_.getCode)

    implicit val decodeLTree: Decoder[LTree] =
      Decoder.decodeString.map(LTree(_)).or(Decoder.decodeNone.map(_ ⇒ LTree("")))
    implicit val encodeLTree: Encoder[LTree] = Encoder.encodeString.contramap(_.toString())
  }

  // It is intended to be used as a bridge between json4s and circe - to be removed eventually.
  // Except seeds, tests and very few rare cases in prod code it shouldn't be used.
  object yolo {
    implicit class RichYoloJson(val json: Json) extends AnyVal {
      // yep, this reproduces behaviour of json4s's `\` which returns array if no value is found under `key`
      def \(key: String): Json = (new RichJson(json) \ key).getOrElse(Json.arr())

      def extract[T: ClassTag: Decoder]: T =
        json
          .as[T]
          .getOrElse(
              sys.error(s"Could not decode '$json' to ${classTag[T].runtimeClass.getSimpleName}"))

      def foldField[A](z: A)(f: (A, JsonField) ⇒ A): A = new RichJson(json).foldField(z)(f)

      def transformField(f: PartialFunction[JsonField, JsonField]): Json =
        new RichJson(json).transformField(f)
    }

    // absolutely unlawful, but hey it's yolo module
    implicit val monoidInstance: Monoid[Json] = new Monoid[Json] {
      // Circe json fortunately does not have JNothing,
      // which is not a valid json anyway, but empty string.
      // So we model empty value as an empty json object.
      def empty: Json = Json.obj()

      def combine(x: Json, y: Json): Json = x.deepMerge(y)
    }

    def parse(json: String): Json =
      parser.parse(json).getOrElse(sys.error(s"Cannot parse '$json' as json"))
  }
}
