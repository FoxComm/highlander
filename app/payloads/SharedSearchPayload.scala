package payloads

import models.sharedsearch.SharedSearch
import org.json4s.JsonAST.JValue

final case class SharedSearchPayload(title: String, query: JValue, scope: SharedSearch.Scope)

final case class SharedSearchAssociationPayload(associates: Seq[Int])