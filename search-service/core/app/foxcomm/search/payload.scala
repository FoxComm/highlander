package foxcomm.search

import cats.data.NonEmptyList
import io.circe.JsonObject

sealed trait SearchQuery {
  def fields: Option[NonEmptyList[String]]
}
object SearchQuery {
  final case class es(query: JsonObject, fields: Option[NonEmptyList[String]]) extends SearchQuery
  final case class fc(query: FCQuery, fields: Option[NonEmptyList[String]])    extends SearchQuery
}
