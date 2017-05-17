package payloads

import models.sharedsearch.SharedSearch
import utils.aliases._

object SharedSearchPayloads {

  case class SharedSearchPayload(title: String,
                                 query: Json,
                                 rawQuery: Json,
                                 scope: SharedSearch.Scope)

  case class SharedSearchAssociationPayload(associates: Seq[Int])
}
