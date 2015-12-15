package payloads

import org.json4s.JsonAST.{JValue ⇒ Json}

final case class CustomerDynamicGroupPayload(
  val name: String,
  val clientState: Json,
  val elasticRequest: Json,
  val customersCount: Option[Int])

