package phoenix.responses.users

import java.time.Instant

import phoenix.models.account._
import phoenix.models.customer.{CustomerData, CustomerRank}
import phoenix.models.location.Region
import phoenix.responses.StoreCreditResponse.Totals
import phoenix.responses.{GroupResponses, ResponseItem}

case class CustomerResponse(id: Int = 0,
                            email: Option[String] = None,
                            name: Option[String] = None,
                            phoneNumber: Option[String] = None,
                            createdAt: Instant,
                            disabled: Boolean,
                            isGuest: Boolean,
                            isBlacklisted: Boolean,
                            rank: Option[Int] = None,
                            totalSales: Long = 0,
                            storeCreditTotals: Totals,
                            numOrders: Option[Int] = None,
                            billingRegion: Option[Region] = None,
                            shippingRegion: Option[Region] = None,
                            lastOrderDays: Option[Long] = None,
                            groups: Seq[GroupResponses.CustomerGroupResponse.Root])
    extends ResponseItem

object CustomerResponse {
  def build(customer: User,
            customerData: CustomerData,
            shippingRegion: Option[Region] = None,
            billingRegion: Option[Region] = None,
            numOrders: Option[Int] = None,
            rank: Option[CustomerRank] = None,
            lastOrderDays: Option[Long] = None,
            scTotals: Option[Totals] = None,
            groups: Seq[GroupResponses.CustomerGroupResponse.Root] = Seq.empty): CustomerResponse = {

    require(customerData.userId == customer.id)
    require(customerData.accountId == customer.accountId)

    CustomerResponse(
      id = customer.accountId,
      email = customer.email,
      name = customer.name,
      phoneNumber = customer.phoneNumber,
      createdAt = customer.createdAt,
      isGuest = customerData.isGuest,
      disabled = customer.isDisabled,
      isBlacklisted = customer.isBlacklisted,
      rank = rank.flatMap(_.rank),
      totalSales = rank.map(_.revenue).getOrElse(0L),
      numOrders = numOrders,
      storeCreditTotals = scTotals.getOrElse(Totals(0, 0)),
      billingRegion = billingRegion,
      shippingRegion = shippingRegion,
      lastOrderDays = lastOrderDays,
      groups = groups
    )
  }
}
