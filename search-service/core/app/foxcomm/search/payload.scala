package foxcomm.search

import cats.data.NonEmptyList
import io.circe.JsonObject

final case class SearchQuery(query: JsonObject, fields: Option[NonEmptyList[String]])
