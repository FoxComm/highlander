package services.returns

import failures.InvalidCancellationReasonFailure
import models.customer.Customers
import models.order.Orders
import models.returns.Return.Canceled
import models.returns._
import models.{Reason, Reasons, StoreAdmin}
import payloads.ReturnPayloads._
import responses.ReturnResponse._
import responses.{CustomerResponse, ReturnResponse, StoreAdminResponse}
import services.Result
import services.returns.Helpers._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object ReturnService {
  def updateMessageToCustomer(refNum: String, payload: ReturnMessageToCustomerPayload)(
      implicit ec: EC,
      db: DB): Result[Root] =
    (for {
      _   ← * <~ payload.validate.toXor
      rma ← * <~ mustFindPendingReturnByRefNum(refNum)
      newMessage = if (payload.message.length > 0) Some(payload.message) else None
      update   ← * <~ Returns.update(rma, rma.copy(messageToCustomer = newMessage))
      updated  ← * <~ Returns.refresh(rma).toXor
      response ← * <~ ReturnResponse.fromRma(updated).toXor
    } yield response).runTxn()

  def updateStateByCsr(refNum: String, payload: ReturnUpdateStatePayload)(implicit ec: EC,
                                                                          db: DB): Result[Root] =
    (for {
      _        ← * <~ payload.validate.toXor
      rma      ← * <~ Returns.mustFindByRefNum(refNum)
      reason   ← * <~ payload.reasonId.map(Reasons.findOneById).getOrElse(lift(None)).toXor
      _        ← * <~ cancelOrUpdate(rma, reason, payload)
      updated  ← * <~ Returns.refresh(rma).toXor
      response ← * <~ ReturnResponse.fromRma(updated).toXor
    } yield response).runTxn()

  private def cancelOrUpdate(rma: Return,
                             reason: Option[Reason],
                             payload: ReturnUpdateStatePayload)(implicit ec: EC) = {
    (payload.state, reason) match {
      case (Canceled, Some(r)) ⇒
        Returns.update(rma, rma.copy(state = payload.state, canceledReason = Some(r.id)))
      case (Canceled, None) ⇒
        DbResult.failure(InvalidCancellationReasonFailure)
      case (_, _) ⇒
        Returns.update(rma, rma.copy(state = payload.state))
    }
  }

  def createByAdmin(admin: StoreAdmin, payload: ReturnCreatePayload)(implicit ec: EC,
                                                                     db: DB): Result[Root] =
    (for {
      order    ← * <~ Orders.mustFindByRefNum(payload.orderRefNum)
      rma      ← * <~ Returns.create(Return.build(order, admin, payload.returnType))
      customer ← * <~ Customers.findOneById(order.customerId).toXor
      adminResponse    = Some(StoreAdminResponse.build(admin))
      customerResponse = customer.map(CustomerResponse.build(_))
    } yield build(rma, customerResponse, adminResponse)).runTxn()

  def getByRefNum(refNum: String)(implicit ec: EC, db: DB): Result[Root] =
    (for {
      rma      ← * <~ Returns.mustFindByRefNum(refNum)
      response ← * <~ fromRma(rma).toXor
    } yield response).run()

  def getExpandedByRefNum(refNum: String)(implicit ec: EC, db: DB): Result[RootExpanded] =
    (for {
      rma      ← * <~ Returns.mustFindByRefNum(refNum)
      response ← * <~ fromRmaExpanded(rma).toXor
    } yield response).run()
}
