package foxcomm.agni.dsl

import cats.data.NonEmptyList
import cats.implicits._
import io.circe._
import io.circe.generic.extras.auto._
import shapeless._

object sort {
  type RawSortValue = JsonObject :+: String :+: CNil
  implicit val decodeRawSortValue: Decoder[RawSortValue] = decodeCoproduct[JsonObject, String :+: CNil]
    .withErrorMessage("Raw sort value must be either a string or an object")

  sealed trait SortFunction
  object SortFunction {
    final case class raw private (value: RawSortValue) extends SortFunction

    implicit val decodeSortFunction: Decoder[SortFunction] = {
      val all: Map[String, Decoder[_ <: SortFunction]] = Map.empty.withDefaultValue(Decoder[raw])

      Decoder.instance { hc ⇒
        val c   = hc.downField(Discriminator)
        val tpe = c.focus.flatMap(_.asString)
        tpe match {
          case Some(t) ⇒ all(t).tryDecode(hc)
          case None    ⇒ Either.left(DecodingFailure("Unknown sort function type", c.history))
        }
      }
    }
  }

  final case class FCSort(sorts: Option[NonEmptyList[SortFunction]])
  object FCSort {
    implicit val decodeFCSort: Decoder[FCSort] = {
      Decoder
        .decodeOption(
          Decoder.decodeNonEmptyList[SortFunction] or
            Decoder[SortFunction].map(NonEmptyList.of(_)))
        .map(FCSort(_))
    }
  }
}
