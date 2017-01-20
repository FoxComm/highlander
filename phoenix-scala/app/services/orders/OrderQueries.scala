package services.orders

import cats.implicits._
import failures.NotFoundFailure404
import models.account.{User, Users}
import models.cord._
import responses.TheResponse
import responses.cord.{AllOrders, OrderResponse}
import services.CordQueries
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object OrderQueries extends CordQueries {

  def findAllByQuery(query: Orders.QuerySeq = Orders)(
      implicit ec: EC): DbResultT[TheResponse[Seq[AllOrders.Root]]] = {

    def build(order: Order, customer: User) =
      for {
        paymentState ← * <~ getCordPaymentState(order.refNum)
      } yield AllOrders.build(order, customer.some, paymentState.some)

    for {
      ordersCustomers ← * <~ query.join(Users).on(_.accountId === _.id).result
      response        ← * <~ ordersCustomers.map((build _).tupled)
    } yield TheResponse.build(response)
  }

  def findOne(refNum: String, grouped: Boolean = true)(
      implicit ec: EC,
      db: DB,
      ctx: OC): DbResultT[TheResponse[OrderResponse]] =
    for {
      order    ← * <~ Orders.mustFindByRefNum(refNum)
      response ← * <~ OrderResponse.fromOrder(order, grouped)
    } yield TheResponse.build(response)

  def findAllByUser(customer: User, grouped: Boolean = true)(
      implicit ec: EC,
      db: DB,
      ctx: OC): DbResultT[TheResponse[Seq[AllOrders.Root]]] =
    for {
      response ← * <~ findAllByQuery(Orders.findByAccountId(customer.accountId))
    } yield response

  def findOneByUser(refNum: String, customer: User, grouped: Boolean = true)(
      implicit ec: EC,
      db: DB,
      ctx: OC): DbResultT[TheResponse[OrderResponse]] =
    for {
      order ← * <~ Orders
               .findByRefNumAndAccountId(refNum, customer.accountId)
               .mustFindOneOr(NotFoundFailure404(Orders, refNum))
      response ← * <~ OrderResponse.fromOrder(order, grouped)
    } yield TheResponse.build(response)

  private def buildResponse(order: Order, grouped: Boolean)(implicit ec: EC,
                                                            db: DB,
                                                            ctx: OC): DbResultT[OrderResponse] =
    for {
      response ← * <~ OrderResponse.fromOrder(order, grouped)
    } yield response
}
