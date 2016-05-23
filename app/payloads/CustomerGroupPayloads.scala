package payloads

import org.json4s.JsonAST.{JValue ⇒ Json}

object CustomerGroupPayloads {

  case class CustomerDynamicGroupPayload(name: String, clientState: Json, elasticRequest: Json,
    customersCount: Option[Int])

}
