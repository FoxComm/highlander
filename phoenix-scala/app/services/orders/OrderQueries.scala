package services.orders

import cats.implicits._
import models.cord._
import models.customer.{Customer, Customers}
import models.payment.PaymentMethod
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import responses.TheResponse
import responses.cord.{AllOrders, OrderResponse}
import services.CordQueries
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object OrderQueries extends CordQueries {

  def findAllByQuery(query: Orders.QuerySeq = Orders)(
      implicit ec: EC): DbResultT[TheResponse[Seq[AllOrders.Root]]] = {

    def build(order: Order, customer: Customer) =
      for {
        paymentState ← * <~ getPaymentState(order.refNum)
      } yield AllOrders.build(order, customer.some, paymentState.some)

    for {
      ordersCustomers ← * <~ query.join(Customers).on(_.customerId === _.id).result
      response        ← * <~ ordersCustomers.map((build _).tupled)
    } yield TheResponse.build(response)
  }

  def findOne(
      refNum: String)(implicit ec: EC, db: DB, ctx: OC): DbResultT[TheResponse[OrderResponse]] =
    for {
      order    ← * <~ Orders.mustFindByRefNum(refNum)
      response ← * <~ OrderResponse.fromOrder(order)
    } yield TheResponse.build(response)

}
