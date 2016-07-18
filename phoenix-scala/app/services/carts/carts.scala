package services

import failures.CartFailures.CustomerHasNoCart
import failures.OrderFailures.EmptyRefNumFailure
import models.cord._
import models.traits.{AdminOriginator, CustomerOriginator, Originator}
import utils.aliases._
import utils.db._

package object carts {

  def getCartByOriginator(originator: Originator,
                          refNum: Option[String])(implicit ec: EC, db: DB): DbResultT[Cart] =
    (originator, refNum) match {
      case (CustomerOriginator(customer), _) ⇒
        Carts.findByCustomer(customer).mustFindOneOr(CustomerHasNoCart(customer.id))

      case (AdminOriginator(_), Some(ref)) ⇒
        Carts.mustFindByRefNum(ref)

      case (AdminOriginator(_), _) ⇒
        DbResultT.failure(EmptyRefNumFailure)
    }
}
