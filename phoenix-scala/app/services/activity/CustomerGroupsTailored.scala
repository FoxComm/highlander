package services.activity

import models.account.User
import models.customer.CustomerDynamicGroup

object CustomerGroupsTailored {

  case class CustomerGroupCreated(customerGroup: CustomerDynamicGroup, admin: User)
      extends ActivityBase[CustomerGroupCreated]

  case class CustomerGroupUpdated(customerGroup: CustomerDynamicGroup, admin: User)
      extends ActivityBase[CustomerGroupUpdated]

  case class CustomerGroupArchived(customerGroup: CustomerDynamicGroup, admin: User)
      extends ActivityBase[CustomerGroupArchived]

}
