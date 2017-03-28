package services.activity

import models.account.User
import models.customer.CustomerGroup

object CustomerGroupsTailored {

  case class CustomerGroupCreated(customerGroup: CustomerGroup, admin: User)
      extends ActivityBase[CustomerGroupCreated]

  case class CustomerGroupUpdated(customerGroup: CustomerGroup, admin: User)
      extends ActivityBase[CustomerGroupUpdated]

  case class CustomerGroupArchived(customerGroup: CustomerGroup, admin: User)
      extends ActivityBase[CustomerGroupArchived]

}
