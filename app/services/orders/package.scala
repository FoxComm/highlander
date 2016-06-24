package services

import failures.CartFailures.CustomerHasNoActiveOrder
import failures.OrderFailures.EmptyRefNumFailure
import models.traits.{AdminOriginator, CustomerOriginator, Originator}
import models.order.{Order, Orders}
import utils.db._
import utils.aliases._

package object orders {
  def getCartByOriginator(originator: Originator,
                          refNum: Option[String])(implicit ec: EC, db: DB): DbResult[Order] =
    (originator, refNum) match {
      case (CustomerOriginator(customer), _) ⇒
        Orders
          .findActiveOrderByCustomer(customer)
          .mustFindOneOr(CustomerHasNoActiveOrder(customer.id))
      case (AdminOriginator(_), Some(ref)) ⇒
        Orders.mustFindByRefNum(ref)
      case _ ⇒
        DbResult.failure(EmptyRefNumFailure)
    }
}
