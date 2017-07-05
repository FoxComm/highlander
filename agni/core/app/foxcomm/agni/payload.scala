package foxcomm.agni

import cats.data.NonEmptyList
import foxcomm.agni.dsl.query._
import foxcomm.agni.dsl.sort.FCSort
import io.circe.JsonObject

sealed trait SearchPayload {
  def fields: Option[NonEmptyList[String]]
}
object SearchPayload {
  final case class es(query: JsonObject, fields: Option[NonEmptyList[String]]) extends SearchPayload
  final case class fc(query: FCQuery, sort: FCSort, fields: Option[NonEmptyList[String]])
      extends SearchPayload
}
