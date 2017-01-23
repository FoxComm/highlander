package payloads

import utils.aliases._

object CustomerGroupPayloads {

  case class CustomerDynamicGroupPayload(scope: Option[String] = None,
                                         name: String,
                                         clientState: Json,
                                         elasticRequest: Json,
                                         customersCount: Option[Int],
                                         templateId: Option[Int] = None)

  case class CustomerGroupMemberSyncPayload(customers: Seq[Int])
}
