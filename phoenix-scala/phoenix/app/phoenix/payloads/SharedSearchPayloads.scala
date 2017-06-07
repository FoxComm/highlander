package phoenix.payloads

import phoenix.models.sharedsearch.SharedSearch
import phoenix.utils.aliases._

object SharedSearchPayloads {

  case class SharedSearchPayload(title: String, query: Json, rawQuery: Json, scope: SharedSearch.Scope)

  case class SharedSearchAssociationPayload(associates: Seq[Int])
}
