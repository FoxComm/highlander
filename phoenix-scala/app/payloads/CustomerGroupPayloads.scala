package payloads

import models.customer.CustomerGroup._
import utils.aliases._

object CustomerGroupPayloads {
  
  case class CustomerGroupPayload(scope: Option[String] = None,
                                  name: String,
                                  clientState: Json,
                                  elasticRequest: Json,
                                  customersCount: Int = 0,
                                  templateId: Option[Int] = None,
                                  `type`: GroupType = Dynamic)

  case class CustomerGroupMemberSyncPayload(customers: Seq[Int])
}
