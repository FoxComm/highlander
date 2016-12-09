package responses

import java.time.Instant

import models.account._
import models.customer.CustomerRank
import models.customer.CustomerData
import models.location.Region
import responses.StoreCreditResponse.Totals

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
                  storeCreditTotals: Totals,
                  numOrders: Option[Int] = None,
                  billingRegion: Option[Region] = None,
                  shippingRegion: Option[Region] = None,
                  lastOrderDays: Option[Long] = None)
      extends ResponseItem

  def build(customer: User,
            customerData: CustomerData,
            shippingRegion: Option[Region] = None,
            billingRegion: Option[Region] = None,
            numOrders: Option[Int] = None,
            rank: Option[CustomerRank] = None,
            lastOrderDays: Option[Long] = None,
            scTotals: Option[Totals] = None
            ): Root = {

    require(customerData.userId == customer.id)
    require(customerData.accountId == customer.accountId)

    Root(id = customer.accountId,
         email = customer.email,
         name = customer.name,
         phoneNumber = customer.phoneNumber,
         createdAt = customer.createdAt,
         isGuest = customerData.isGuest,
         disabled = customer.isDisabled,
         isBlacklisted = customer.isBlacklisted,
         rank = rank.flatMap(_.rank),
         totalSales = rank.map(_.revenue).getOrElse(0),
         numOrders = numOrders,
         storeCreditTotals = scTotals.getOrElse(Totals(0, 0)),
         billingRegion = billingRegion,
         shippingRegion = shippingRegion,
         lastOrderDays = lastOrderDays)
  }
}
