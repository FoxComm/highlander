package foxcomm.agni.dsl

import cats.data.NonEmptyList
import cats.implicits._
import io.circe._
import io.circe.generic.extras.auto._

object aggregations {
  sealed trait AggregationFunction {
    def name: String

    def `type`: String

    final def tpe: String = `type`
  }
  object AggregationFunction {
    sealed trait WithMetadata { this: AggregationFunction ⇒
      def meta: Option[JsonObject]
    }

    final case class raw private (name: String, `type`: String, meta: Option[JsonObject], value: JsonObject)
        extends AggregationFunction
        with WithMetadata

    implicit val decodeAggFunction: Decoder[AggregationFunction] = {
      val all: Map[String, Decoder[_ <: AggregationFunction]] = Map.empty.withDefaultValue(Decoder[raw])

      Decoder.instance { hc ⇒
        val c   = hc.downField(Discriminator)
        val tpe = c.focus.flatMap(_.asString)
        tpe match {
          case Some(t) ⇒ all(t).tryDecode(hc)
          case None    ⇒ Either.left(DecodingFailure("Unknown aggregation function type", c.history))
        }
      }
    }
  }

  final case class FCAggregation(aggs: Option[NonEmptyList[AggregationFunction]])
  object FCAggregation {
    implicit val decodeFCAggregation: Decoder[FCAggregation] = {
      Decoder
        .decodeOption(Decoder.decodeNonEmptyList[AggregationFunction].emap { aggs ⇒
          aggs
            .foldM[Either[String, ?], Set[String]](Set.empty[String])(
              (defined, agg) ⇒
                if (defined.contains(agg.name))
                  Either.left(
                    s"Cannot have multiple aggregations with the same name: ${agg.name} is defined twice")
                else Either.right(defined))
            .right
            .map(_ ⇒ aggs)
        } or Decoder[AggregationFunction].map(NonEmptyList.of(_)))
        .map(FCAggregation(_))
    }
  }
}
