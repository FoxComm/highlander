package foxcomm.search

import cats.data.NonEmptyList
import foxcomm.search.dsl.query._
import io.circe.JsonObject

sealed trait SearchPayload {
  def fields: Option[NonEmptyList[String]]
}
object SearchPayload {
  final case class es(query: JsonObject, fields: Option[NonEmptyList[String]])      extends SearchPayload
  final case class fc(query: Option[FCQuery], fields: Option[NonEmptyList[String]]) extends SearchPayload
}
