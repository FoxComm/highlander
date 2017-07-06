package foxcomm.agni.dsl

import cats.data.NonEmptyList
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
