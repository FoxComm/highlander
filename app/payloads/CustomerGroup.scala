package payloads

import org.json4s.JsonAST.{JValue ⇒ Json}

final case class CustomerDynamicGroupPayload(
  name: String,
  clientState: Json,
  elasticRequest: Json,
  customersCount: Option[Int])

