package concepts.discounts.qualifiers

import cats.data.Xor
import failures.Failures
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod
import cats.data.NonEmptyList
import cats.std.list._

case class AndQualifier(qualifiers: Seq[Qualifier]) extends Qualifier {

  def check(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): Xor[Failures, Unit] = {

    val checks = qualifiers.map(_.check(order, lineItems, shippingMethod))
    val failures = checks.flatMap(_.fold(fs ⇒ fs.unwrap, q ⇒ Seq.empty))

    failures match {
      case head :: tail ⇒ Xor.Left(NonEmptyList(head, tail))
      case Nil          ⇒ Xor.Right(Unit)
    }
  }
}
