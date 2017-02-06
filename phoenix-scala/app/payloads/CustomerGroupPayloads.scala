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
                                  groupType: GroupType = Dynamic)

  case class CustomerGroupMemberServiceSyncPayload(customers: Seq[Int])

  case class CustomerGroupMemberSyncPayload(toAdd: Seq[Int], toDelete: Seq[Int])

  case class AddCustomerToGroups(groups: Seq[Int])
}
