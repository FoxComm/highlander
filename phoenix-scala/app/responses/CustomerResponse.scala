package responses

import java.time.Instant

import models.account._
import models.customer.CustomerRank
import models.customer.CustomerUser
import models.location.Region

object CustomerResponse {
  case class Root(id: Int = 0,
                  email: Option[String] = None,
                  name: Option[String] = None,
                  phoneNumber: Option[String] = None,
                  createdAt: Instant,
                  disabled: Boolean,
                  isGuest: Boolean,
                  isBlacklisted: Boolean,
                  rank: Option[Int] = None,
                  totalSales: Int = 0,
                  numOrders: Option[Int] = None,
                  billingRegion: Option[Region] = None,
                  shippingRegion: Option[Region] = None,
                  lastOrderDays: Option[Long] = None)
      extends ResponseItem

  def build(customer: User,
            customerUser: CustomerUser,
            shippingRegion: Option[Region] = None,
            billingRegion: Option[Region] = None,
            numOrders: Option[Int] = None,
            rank: Option[CustomerRank] = None,
            lastOrderDays: Option[Long] = None): Root = {

    require(customerUser.userId == customer.id)
    require(customerUser.accountId == customer.accountId)

    Root(id = customer.accountId,
         email = customer.email,
         name = customer.name,
         phoneNumber = customer.phoneNumber,
         createdAt = customer.createdAt,
         isGuest = customerUser.isGuest,
         disabled = customer.isDisabled,
         isBlacklisted = customer.isBlacklisted,
         rank = rank.flatMap(_.rank),
         totalSales = rank.map(_.revenue).getOrElse(0),
         numOrders = numOrders,
         billingRegion = billingRegion,
         shippingRegion = shippingRegion,
         lastOrderDays = lastOrderDays)
  }
}
