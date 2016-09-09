package responses

import java.time.Instant

import models.account._
import models.location.Region

object CustomerResponse {
  case class Root(id: Int = 0,
                  email: Option[String] = None,
                  name: Option[String] = None,
                  phoneNumber: Option[String] = None,
                  location: Option[String] = None,
                  modality: Option[String] = None,
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
            isGuest: Boolean,
            shippingRegion: Option[Region] = None,
            billingRegion: Option[Region] = None,
            numOrders: Option[Int] = None,
            rank: Option[CustomerRank] = None,
            lastOrderDays: Option[Long] = None): Root =
    Root(id = customer.id,
         email = customer.email,
         name = customer.name,
         phoneNumber = customer.phoneNumber,
         location = customer.location,
         modality = customer.modality,
         createdAt = customer.createdAt,
         isGuest = isGuest,
         disabled = customer.isDisabled,
         isBlacklisted = customer.isBlacklisted,
         rank = rank.flatMap(_.rank),
         totalSales = rank.map(_.revenue).getOrElse(0),
         numOrders = numOrders,
         billingRegion = billingRegion,
         shippingRegion = shippingRegion,
         lastOrderDays = lastOrderDays)

  case class ResetPasswordSendAnswer(status: String)

  case class ResetPasswordDoneAnswer(status: String)

}
