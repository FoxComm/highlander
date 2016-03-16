package services.rmas

import models.customer.Customers
import models.order.Orders
import models.rma.{Rmas, Rma}
import Rma.Canceled
import models.{Reason, Reasons, StoreAdmin}
import payloads.{RmaCreatePayload, RmaMessageToCustomerPayload, RmaUpdateStatePayload}
import responses.RmaResponse._
import responses.{AllRmas, RmaResponse, CustomerResponse, StoreAdminResponse, BatchResponse}
import services.rmas.Helpers._
import services.{InvalidCancellationReasonFailure, Result}
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.{DbResult, _}
import utils.Slick.implicits._
import utils.aliases._

object RmaService {
  def updateMessageToCustomer(refNum: String, payload: RmaMessageToCustomerPayload)
    (implicit ec: EC, db: DB): Result[Root] = (for {
    _         ← * <~ payload.validate.toXor
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    newMessage = if (payload.message.length > 0) Some(payload.message) else None
    update    ← * <~ Rmas.update(rma, rma.copy(messageToCustomer = newMessage))
    updated   ← * <~ Rmas.refresh(rma).toXor
    response  ← * <~ RmaResponse.fromRma(updated).toXor
  } yield response).runTxn()

  def updateStateByCsr(refNum: String, payload: RmaUpdateStatePayload)(implicit ec: EC, db: DB): Result[Root] = (for {
    _         ← * <~ payload.validate.toXor
    rma       ← * <~ Rmas.mustFindByRefNum(refNum)
    reason    ← * <~ payload.reasonId.map(Reasons.findOneById).getOrElse(lift(None)).toXor
    _         ← * <~ cancelOrUpdate(rma, reason, payload)
    updated   ← * <~ Rmas.refresh(rma).toXor
    response  ← * <~ RmaResponse.fromRma(updated).toXor
  } yield response).runTxn()

  private def cancelOrUpdate(rma: Rma, reason: Option[Reason], payload: RmaUpdateStatePayload)(implicit ec: EC, db: DB) = {
    (payload.state, reason) match {
      case (Canceled, Some(r)) ⇒
        Rmas.update(rma, rma.copy(state = payload.state, canceledReason = Some(r.id)))
      case (Canceled, None) ⇒
        DbResult.failure(InvalidCancellationReasonFailure)
      case (_, _) ⇒
        Rmas.update(rma, rma.copy(state = payload.state))
    }
  }

  def createByAdmin(admin: StoreAdmin, payload: RmaCreatePayload)(implicit ec: EC, db: DB): Result[Root] = (for {
    order    ← * <~ Orders.mustFindByRefNum(payload.orderRefNum)
    rma      ← * <~ Rmas.create(Rma.build(order, admin, payload.rmaType))
    customer ← * <~ Customers.findOneById(order.customerId).toXor
    adminResponse = Some(StoreAdminResponse.build(admin))
    customerResponse = customer.map(CustomerResponse.build(_))
  } yield build(rma, customerResponse, adminResponse)).runTxn()

  def getByRefNum(refNum: String)(implicit ec: EC, db: DB): Result[Root] = (for {
    rma      ← * <~ Rmas.mustFindByRefNum(refNum)
    response ← * <~ fromRma(rma).toXor
  } yield response).run()

  def getExpandedByRefNum(refNum: String)(implicit ec: EC, db: DB): Result[RootExpanded] = (for {
    rma      ← * <~ Rmas.mustFindByRefNum(refNum)
    response ← * <~ fromRmaExpanded(rma).toXor
  } yield response).run()

  def findByOrderRef(refNum: String)
    (implicit ec: EC, db: DB, sortAndPage: SortAndPage): Result[BatchResponse[AllRmas.Root]] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    rmas  ← * <~ RmaQueries.findAllDbio(Rmas.findByOrderRefNum(refNum))
  } yield rmas).run()

  def findByCustomerId(customerId: Int)
    (implicit ec: EC, db: DB, sortAndPage: SortAndPage): Result[BatchResponse[AllRmas.Root]] = (for {
    _    ← * <~ Customers.mustFindById404(customerId)
    rmas ← * <~ RmaQueries.findAllDbio(Rmas.findByCustomerId(customerId))
  } yield rmas).run()
}
