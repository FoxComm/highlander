package payloads

import org.json4s.JsonAST.JValue

final case class SharedSearchPayload(title: String, query: JValue, scope: models.SharedSearch.Scope)

final case class SharedSearchAssociationPayload(associates: Seq[Int])