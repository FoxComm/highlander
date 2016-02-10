package services

import scala.concurrent.ExecutionContext

import models.activity.ActivityContext
import models.traits.{Originator, CustomerOriginator, AdminOriginator}
import models.order.{Order, Orders}
import responses.{AllOrders, TheResponse}
import slick.driver.PostgresDriver.api._
import services.CartFailures.CustomerHasNoActiveOrder
import utils.Slick.DbResult
import utils.Slick.implicits._

package object orders {
  type BulkOrderUpdateResponse = TheResponse[Seq[AllOrders.Root]]

  def getCartByOriginator(originer: Originator, refNum: Option[String])
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): DbResult[Order] = (originer, refNum) match {
    case (CustomerOriginator(customer), _) ⇒
      Orders.findActiveOrderByCustomer(customer).one.mustFindOr(CustomerHasNoActiveOrder(customer.id))
    case (AdminOriginator(_), Some(ref)) ⇒
      Orders.mustFindByRefNum(ref)
    case _ ⇒
      DbResult.failure(EmptyRefNumFailure)
  }
}
