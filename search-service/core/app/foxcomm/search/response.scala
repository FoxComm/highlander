package foxcomm.search

import io.circe.JsonObject

final case class SearchResult(result: List[JsonObject], pagination: SearchPagination)

final case class SearchPagination(total: Long)
