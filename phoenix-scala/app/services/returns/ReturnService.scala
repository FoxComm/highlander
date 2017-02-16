package services.returns

import failures.InvalidCancellationReasonFailure
import models.Reason.Cancellation
import models.account._
import models.admin.AdminsData
import models.cord.Orders
import models.customer.CustomersData
import models.returns.Return.Canceled
import models.returns._
import models.{Reason, Reasons}
import payloads.ReturnPayloads._
import responses.ReturnResponse._
import responses.{CustomerResponse, ReturnResponse, StoreAdminResponse}
import services.returns.Helpers._
import slick.driver.PostgresDriver.api._
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
      update   ← * <~ Returns.update(rma, rma.copy(messageToAccount = newMessage))
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  def updateStateByCsr(refNum: String, payload: ReturnUpdateStatePayload)(
      implicit ec: EC,
      db: DB): DbResultT[Root] =
    for {
      rma    ← * <~ Returns.mustFindByRefNum(refNum)
      _      ← * <~ rma.transitionState(payload.state)
      reason ← * <~ payload.reasonId.map(Reasons.findOneById).getOrElse(lift(None))
      _ ← * <~ reason.map(r ⇒
               failIfNot(r.reasonType == Cancellation, InvalidCancellationReasonFailure))
      _        ← * <~ update(rma, reason, payload)
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  private def update(rma: Return, reason: Option[Reason], payload: ReturnUpdateStatePayload)(
      implicit ec: EC) = {
    Returns.update(rma, rma.copy(state = payload.state, canceledReasonId = reason.map(_.id)))
  }

  // todo should be available for non-admin as well
  def createByAdmin(admin: User, payload: ReturnCreatePayload)(implicit ec: EC,
                                                               db: DB): DbResultT[Root] =
    for {
      order     ← * <~ Orders.mustFindByRefNum(payload.cordRefNum)
      rma       ← * <~ Returns.create(Return.build(order, admin, payload.returnType))
      customer  ← * <~ Users.mustFindByAccountId(order.accountId)
      custData  ← * <~ CustomersData.mustFindByAccountId(order.accountId)
      adminData ← * <~ AdminsData.mustFindByAccountId(admin.accountId)
      adminResponse    = Some(StoreAdminResponse.build(admin, adminData))
      customerResponse = CustomerResponse.build(customer, custData)
    } yield build(rma, Some(customerResponse), adminResponse)

  def list(implicit ec: EC, db: DB): DbResultT[Seq[Root]] =
    for {
      rma      ← * <~ Returns.result
      response ← * <~ rma.map(fromRma)
    } yield response

  def getByCustomer(customerId: Int)(implicit ec: EC, db: DB): DbResultT[Seq[Root]] =
    for {
      _        ← * <~ Accounts.mustFindById404(customerId)
      rma      ← * <~ Returns.findByAccountId(customerId).result
      response ← * <~ rma.map(fromRma)
    } yield response

  def getByOrder(refNum: String)(implicit ec: EC, db: DB): DbResultT[Seq[Root]] =
    for {
      _        ← * <~ Orders.mustFindByRefNum(refNum)
      rma      ← * <~ Returns.findByOrderRefNum(refNum).result
      response ← * <~ rma.map(fromRma)
    } yield response

  def getByRefNum(refNum: String)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      rma      ← * <~ Returns.mustFindByRefNum(refNum)
      response ← * <~ fromRma(rma)
    } yield response

}
