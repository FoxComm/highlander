package payloads

import models.account.User
import utils.aliases._

object CustomerGroupPayloads {

  case class CustomerDynamicGroupPayload(scope: Option[String] = None,
                                         name: String,
                                         clientState: Json,
                                         elasticRequest: Json,
                                         customersCount: Int = 0,
                                         templateId: Option[Int] = None)

  case class CustomerGroupMemberSyncPayload(customers: Seq[Int])
}
