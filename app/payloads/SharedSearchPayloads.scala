package payloads

import models.sharedsearch.SharedSearch
import org.json4s.JsonAST.JValue

object SharedSearchPayloads {

  case class SharedSearchPayload(
      title: String, query: JValue, rawQuery: JValue, scope: SharedSearch.Scope)

  case class SharedSearchAssociationPayload(associates: Seq[Int])
}
