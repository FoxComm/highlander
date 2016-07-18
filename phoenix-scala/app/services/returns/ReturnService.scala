package services.returns

import failures.InvalidCancellationReasonFailure
import models.cord.Orders
import models.customer.Customers
import models.returns.Return.Canceled
import models.returns._
import models.{Reason, Reasons, StoreAdmin}
import payloads.ReturnPayloads._
import responses.ReturnResponse._
import responses.{CustomerResponse, ReturnResponse, StoreAdminResponse}
import services.returns.Helpers._
import utils.aliases._
import utils.db._

object ReturnService {
  def updateMessageToCustomer(refNum: String, payload: ReturnMessageToCustomerPayload)(
      implicit ec: EC,
      db: DB): DbResultT[Root] =
    for {
      _   ← * <~ payload.validate
      rma ← * <~ mustFindPendingReturnByRefNum(refNum)
      newMessage = if (payload.message.length > 0) Some(payload.message) else None
      update   ← * <~ Returns.update(rma, rma.copy(messageToCustomer = newMessage))
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  def updateStateByCsr(refNum: String, payload: ReturnUpdateStatePayload)(
      implicit ec: EC,
      db: DB): DbResultT[Root] =
    for {
      _        ← * <~ payload.validate
      rma      ← * <~ Returns.mustFindByRefNum(refNum)
      reason   ← * <~ payload.reasonId.map(Reasons.findOneById).getOrElse(lift(None))
      _        ← * <~ cancelOrUpdate(rma, reason, payload)
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  private def cancelOrUpdate(rma: Return,
                             reason: Option[Reason],
                             payload: ReturnUpdateStatePayload)(implicit ec: EC) = {
    (payload.state, reason) match {
      case (Canceled, Some(r)) ⇒
        Returns.update(rma, rma.copy(state = payload.state, canceledReason = Some(r.id)))
      case (Canceled, None) ⇒
        DbResultT.failure(InvalidCancellationReasonFailure)
      case (_, _) ⇒
        Returns.update(rma, rma.copy(state = payload.state))
    }
  }

  def createByAdmin(admin: StoreAdmin, payload: ReturnCreatePayload)(implicit ec: EC,
                                                                     db: DB): DbResultT[Root] =
    for {
      order    ← * <~ Orders.mustFindByRefNum(payload.cordRefNum)
      rma      ← * <~ Returns.create(Return.build(order, admin, payload.returnType))
      customer ← * <~ Customers.findOneById(order.customerId)
      adminResponse    = Some(StoreAdminResponse.build(admin))
      customerResponse = customer.map(CustomerResponse.build(_))
    } yield build(rma, customerResponse, adminResponse)

  def getByRefNum(refNum: String)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      rma      ← * <~ Returns.mustFindByRefNum(refNum)
      response ← * <~ fromRma(rma)
    } yield response

  def getExpandedByRefNum(refNum: String)(implicit ec: EC, db: DB): DbResultT[RootExpanded] =
    for {
      rma      ← * <~ Returns.mustFindByRefNum(refNum)
      response ← * <~ fromRmaExpanded(rma)
    } yield response
}
