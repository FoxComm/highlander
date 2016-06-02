package payloads

import utils.aliases._

object CustomerGroupPayloads {

  case class CustomerDynamicGroupPayload(
      name: String, clientState: Json, elasticRequest: Json, customersCount: Option[Int])
}
