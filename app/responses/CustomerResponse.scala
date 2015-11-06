package responses

import java.time.Instant

import models.{Region, Customer, CustomerRank}

object CustomerResponse {
  final case class Root(
    id: Int = 0,
    email: String,
    name: Option[String] = None,
    phoneNumber: Option[String] = None,
    location: Option[String] = None,
    modality: Option[String] = None,
    createdAt: Instant,
    disabled: Boolean,
    isGuest: Boolean,
    blacklisted: Boolean,
    rank: Option[Int] = None,
    numOrders: Option[Int] = None,
    billingRegion: Option[String] = None,
    shippingRegion: Option[String] = None) extends ResponseItem

  def build(customer: Customer,
    shippingRegion: Option[Region] = None,
    billingRegion: Option[Region] = None,
    numOrders: Option[Int] = None,
    rank: Option[CustomerRank] = None): Root =
    Root(id = customer.id,
      email = customer.email,
      name = customer.name,
      phoneNumber = customer.phoneNumber,
      location = customer.location,
      modality = customer.modality,
      createdAt = customer.createdAt,
      isGuest = customer.isGuest,
      disabled = customer.isDisabled,
      blacklisted = customer.isBlacklisted,
      rank = rank.map(_.rank),
      numOrders = numOrders,
      billingRegion = billingRegion.map(_.name),
      shippingRegion = shippingRegion.map(_.name))
}
