package services

import models.traits.{Originator, CustomerOriginator, AdminOriginator}
import models.order.{Order, Orders}

import services.CartFailures.CustomerHasNoActiveOrder
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.aliases._

package object orders {
  def getCartByOriginator(originer: Originator, refNum: Option[String])
    (implicit ec: EC, db: DB, ac: AC): DbResult[Order] = (originer, refNum) match {
    case (CustomerOriginator(customer), _) ⇒
      Orders.findActiveOrderByCustomer(customer).one.mustFindOr(CustomerHasNoActiveOrder(customer.id))
    case (AdminOriginator(_), Some(ref)) ⇒
      Orders.mustFindByRefNum(ref)
    case _ ⇒
      DbResult.failure(EmptyRefNumFailure)
  }
}
