package services.rmas

import models.Rma.Canceled
import models.{Customers, Orders, Reason, Reasons, Rma, Rmas, StoreAdmin}
import payloads.{RmaCreatePayload, RmaMessageToCustomerPayload, RmaUpdateStatePayload}
import responses.RmaResponse._
import responses.{RmaResponse, CustomerResponse, StoreAdminResponse}
import services.rmas.Helpers._
import services.{InvalidCancellationReasonFailure, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.{DbResult, _}
import utils.Slick.implicits._

import scala.concurrent.{ExecutionContext, Future}

object RmaService {
  def updateMessageToCustomer(refNum: String, payload: RmaMessageToCustomerPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    _         ← * <~ payload.validate.toXor
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    newMessage = if (payload.message.length > 0) Some(payload.message) else None
    update    ← * <~ Rmas.update(rma, rma.copy(messageToCustomer = newMessage))
    updated   ← * <~ Rmas.refresh(rma).toXor
    response  ← * <~ RmaResponse.fromRma(updated).toXor
  } yield response).runTxn()

  def updateStateByCsr(refNum: String, payload: RmaUpdateStatePayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    _         ← * <~ payload.validate.toXor
    rma       ← * <~ Rmas.mustFindByRefNum(refNum)
    reason    ← * <~ payload.reasonId.map(Reasons.findOneById).getOrElse(lift(None)).toXor
    _         ← * <~ cancelOrUpdate(rma, reason, payload)
    updated   ← * <~ Rmas.refresh(rma).toXor
    response  ← * <~ RmaResponse.fromRma(updated).toXor
  } yield response).runTxn()

  private def cancelOrUpdate(rma: Rma, reason: Option[Reason], payload: RmaUpdateStatePayload)
    (implicit ec: ExecutionContext, db: Database) = {

    (payload.state, reason) match {
      case (Canceled, Some(r)) ⇒
        Rmas.update(rma, rma.copy(state = payload.state, canceledReason = Some(r.id)))
      case (Canceled, None) ⇒
        DbResult.failure(InvalidCancellationReasonFailure)
      case (_, _) ⇒
        Rmas.update(rma, rma.copy(state = payload.state))
    }
  }

  def createByAdmin(admin: StoreAdmin, payload: RmaCreatePayload)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = (for {
    order    ← * <~ Orders.mustFindByRefNum(payload.orderRefNum)
    rma      ← * <~ Rmas.create(Rma.build(order, admin, payload.rmaType))
    customer ← * <~ Customers.findOneById(order.customerId).toXor
    adminResponse = Some(StoreAdminResponse.build(admin))
    customerResponse = customer.map(CustomerResponse.build(_))
  } yield build(rma, customerResponse, adminResponse)).runTxn()

  def getByRefNum(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = (for {
    rma      ← * <~ Rmas.mustFindByRefNum(refNum)
    response ← * <~ fromRma(rma).toXor
  } yield response).run()

  def getExpandedByRefNum(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[RootExpanded] = (for {
    rma      ← * <~ Rmas.mustFindByRefNum(refNum)
    response ← * <~ fromRmaExpanded(rma).toXor
  } yield response).run()

  def findByOrderRef(refNum: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[BulkRmaUpdateResponse] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    rmas  ← * <~ RmaQueries.findAllDbio(Rmas.findByOrderRefNum(refNum))
  } yield rmas).run()

  def findByCustomerId(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[BulkRmaUpdateResponse] = (for {
    _    ← * <~ Customers.mustFindById404(customerId)
    rmas ← * <~ RmaQueries.findAllDbio(Rmas.findByCustomerId(customerId))
  } yield rmas).run()
}
