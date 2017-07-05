package foxcomm.agni.dsl

import cats.data.NonEmptyList
import io.circe._
import io.circe.generic.extras.semiauto._
import shapeless._

object sort {
  sealed trait SortFunction
  object SortFunction {
    type RawSortValue = JsonObject :+: String :+: CNil
    implicit val decodeRawSortValue: Decoder[RawSortValue] = decodeCoproduct[JsonObject, String :+: CNil]
      .withErrorMessage("Raw sort value must be either a string or an object")

    final case class raw private (value: RawSortValue) extends SortFunction
    object raw {
      implicit val decodeRaw: Decoder[raw] = deriveDecoder[raw]
    }

    implicit val decodeSortFunction: Decoder[SortFunction] =
      Decoder[raw].map(identity[SortFunction](_))
  }

  final case class FCSort(sort: Option[NonEmptyList[SortFunction]])
  object FCSort {
    implicit val decodeFCQuery: Decoder[FCSort] = {
      Decoder
        .decodeOption(
          Decoder.decodeNonEmptyList[SortFunction] or
            Decoder[SortFunction].map(NonEmptyList.of(_)))
        .map(FCSort(_))
    }
  }
}
