package foxcomm.agni

import io.circe.JsonObject

final case class SearchResult(result: List[JsonObject], pagination: SearchPagination, maxScore: Float)

final case class SearchPagination(total: Long)
