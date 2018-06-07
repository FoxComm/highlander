package foxcomm.agni.dsl

import cats.data.NonEmptyList
import io.circe.Decoder

package object query extends QueryData with QueryFunctions {
  final case class FCQuery(query: Option[NonEmptyList[QueryFunction]])
  object FCQuery {
    implicit val decodeFCQuery: Decoder[FCQuery] =
      Decoder
        .decodeOption(
          Decoder.decodeNonEmptyList[QueryFunction] or
            Decoder[QueryFunction].map(NonEmptyList.of(_)))
        .map(FCQuery(_))
  }

  implicit class RichQueryValue[T](val qv: QueryValue[T]) extends AnyVal {
    def toNEL: NonEmptyList[T] = qv.eliminate(NonEmptyList.of(_), _.eliminate(identity, _.impossible))

    def toList: List[T] = toNEL.toList
  }

  implicit class RichCompoundValue(val cv: CompoundValue) extends AnyVal {
    def toNEL: NonEmptyList[AnyRef] = cv.eliminate(_.toNEL, _.eliminate(_.toNEL, _.impossible))

    def toList: List[AnyRef] = toNEL.toList
  }
}
