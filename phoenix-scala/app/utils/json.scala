package utils

import cats.implicits._
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.tminglei.slickpg.LTree
import com.ning.http.client.Response
import io.circe.jackson.CirceJsonModule
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json, parser}
import models.returns._
import payloads.ReturnPayloads.ReturnLineItemPayload
import scala.annotation.tailrec
import scala.reflect.{ClassTag, classTag}
import utils.aliases.Json
import utils.db.ExPostgresDriver.api._
import utils.time.JavaInstantJsonCodec

/**
  * [[TypeHints]] implementation for json4s that supports
  * discriminating of trait/class hierarchies based on [[ADT]] typeclass.
  *
  * Example:
  * {{{
  * ADTTypeHints(
  *   Map(
  *     ReturnLineItem.ShippingCost → classOf[ReturnShippingCostLineItemPayload],
  *     ReturnLineItem.SkuItem      → classOf[ReturnSkuLineItemPayload]
  * ))
  * }}}
  * It will allow to deserialize [[ReturnLineItemPayload]] based on [[ReturnLineItem.OriginType]] ADT.
  * Every json payload with [[ReturnLineItemPayload]] should then contain additional field [[ExtraJsonCodecs.TypeHintFieldName]]
  * with type hint that is string representation of specific [[ReturnLineItem.OriginType]].
  *
  * Note that you are fully responsible for providing exclusive 1:1 mapping from ADT element to class.
  * If more than one class is assigned to ADT element, then only single one will be serializable to JSON
  * and which one be will depend on underlying [[Map]] implementation.
  */
//case class ADTTypeHints[T: ADT](adtHints: Map[T, Class[_]]) extends TypeHints {
//  @inline private def adt = implicitly[ADT[T]]
//
//  private[this] lazy val reversed = adtHints.map(_.swap)
//
//  lazy val hints: List[Class[_]] = adtHints.valuesIterator.toList
//
//  def hintFor(clazz: Class[_]): String =
//    reversed.get(clazz).fold(sys.error(s"No hint defined for ${clazz.getName}"))(adt.show(_))
//
//  def classFor(hint: String): Option[Class[_]] = adt.typeMap.get(hint).flatMap(adtHints.get)
//}

object json {
  private[this] val mapper = new ObjectMapper().registerModule(CirceJsonModule)

  implicit class RichJson(val json: Json) extends AnyVal {
    def asJsonNode: JsonNode = mapper.valueToTree[JsonNode](json)

    def transformField(f: PartialFunction[(String, Json), (String, Json)]): Json =
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

    // port of json4s '\'
    def \(key: String): Option[Json] = {
      @tailrec
      def findRec(toProcess: Vector[Json], acc: Vector[Json]): Vector[Json] = toProcess match {
        case j +: js =>
          j.asArray match {
            case Some(values) => findRec(js ++ values, acc)
            case None => findRec(js, acc ++ j.asObject.flatMap(_(key)))
          }
        case js if js.isEmpty => acc
      }

      json.arrayOrObject(
        None,
        values => {
          val vs = findRec(values, Vector.empty)
          if (vs.nonEmpty) Some(Json.arr(vs: _*)) else None
        },
        _(key)
      )
    }
  }

  // Even if you feel yolo please do not use it.
  // Except seeds, tests and very few rare cases in prod code it shouldn't be used
  object yolo {
    implicit class RichYoloJson(val json: Json) extends AnyVal {
      // yep, this reproduces behaviour of json4s's `\` which returns array if no value is found under `key`
      def \(key: String): Json = (new RichJson(json) \ key).getOrElse(Json.arr())

      def transformField(f: PartialFunction[(String, Json), (String, Json)]): Json = new RichJson(json).transformField(f)

      def extract[T: ClassTag: Decoder]: T =
        json
          .as[T]
          .getOrElse(
            sys.error(s"Could not decode '$json' to ${classTag[T].runtimeClass.getSimpleName}"))
    }

    def parse(json: String): Json =
      parser.parse(json).getOrElse(sys.error(s"Cannot parse '$json' as json"))
  }

  def dbJsonColumn[A: ClassTag: Decoder: Encoder]: BaseColumnType[A] = {
    import yolo._

    MappedColumnType.base[A, Json](_.asJson, _.extract[A])
  }

  object asJson extends (Response ⇒ Json) {
    def apply(v1: Response): Json = ???
  }
}

object ExtraJsonCodecs extends JavaInstantJsonCodec {
  val TypeHintFieldName = "payloadType"

  implicit val decodeLTree: Decoder[LTree] =
    Decoder.decodeString.map(LTree(_)).or(Decoder.decodeNone.map(_ ⇒ LTree("")))
  implicit val encodeLTree: Encoder[LTree] = Encoder.encodeString.contramap(_.toString())
}
