package payloads

import models.Order.Status

final case class UpdateOrderPayload(status: Status)

final case class BulkUpdateOrdersPayload(referenceNumbers: Seq[String], status: Status)

final case class CreateOrderNotePayload(body: String)

final case class CreateShippingAddress(addressId: Option[Int] = None, address: Option[CreateAddressPayload] = None)

final case class UpdateShippingAddress(addressId: Option[Int] = None, address: Option[UpdateAddressPayload] = None)

final case class CreateOrder(customerId: Int)

