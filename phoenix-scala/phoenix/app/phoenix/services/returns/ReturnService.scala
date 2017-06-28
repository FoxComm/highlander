package phoenix.services.returns

import cats.implicits._
import core.db._
import phoenix.failures.InvalidCancellationReasonFailure
import phoenix.failures.ReturnFailures.OrderMustBeShippedForReturn
import phoenix.models.Reason.Cancellation
import phoenix.models.account._
import phoenix.models.admin.AdminsData
import phoenix.models.cord.{Order, Orders}
import phoenix.models.customer.CustomersData
import phoenix.models.returns._
import phoenix.models.{Reason, Reasons}
import phoenix.payloads.ReturnPayloads._
import phoenix.responses.ReturnResponse
import phoenix.responses.ReturnResponse._
import phoenix.responses.users._
import phoenix.services.LogActivity
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import slick.jdbc.PostgresProfile.api._

object ReturnService {

  def updateMessageToCustomer(refNum: String, payload: ReturnMessageToCustomerPayload)(
      implicit ec: EC,
      db: DB): DbResultT[Root] =
    for {
      rma ← * <~ Returns.mustFindActiveByRefNum404(refNum)
      newMessage = if (payload.message.length > 0) Some(payload.message) else None
      _        ← * <~ Returns.update(rma, rma.copy(messageToAccount = newMessage))
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  def updateStateByCsr(refNum: String, payload: ReturnUpdateStatePayload)(implicit ec: EC,
                                                                          db: DB,
                                                                          au: AU,
                                                                          ac: AC,
                                                                          apis: Apis): DbResultT[Root] =
    for {
      rma      ← * <~ Returns.mustFindByRefNum(refNum)
      _        ← * <~ rma.transitionState(payload.state)
      reason   ← * <~ payload.reasonId.flatTraverse(Reasons.findOneById(_).dbresult)
      _        ← * <~ reason.map(r ⇒ failIfNot(r.reasonType == Cancellation, InvalidCancellationReasonFailure))
      _        ← * <~ update(rma, reason, payload)
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
      customer ← * <~ Users.mustFindByAccountId(rma.accountId)
      _ ← * <~ when(rma.state != payload.state,
                    LogActivity().returnStateChanged(customer, response, payload.state).void)
    } yield response

  private def update(rma: Return,
                     reason: Option[Reason],
                     payload: ReturnUpdateStatePayload)(implicit ec: EC, db: DB, au: AU, ac: AC, apis: Apis) =
    for {
      rma ← * <~ Returns
             .update(rma, rma.copy(state = payload.state, canceledReasonId = reason.map(_.id)))
      _ ← * <~ when(rma.state == Return.Complete, ReturnPaymentManager.issueRefunds(rma))
      _ ← * <~ when(rma.state == Return.Canceled, ReturnPaymentManager.cancelRefunds(rma))
    } yield rma

  // todo should be available for non-admin as well
  def createByAdmin(admin: User,
                    payload: ReturnCreatePayload)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      order        ← * <~ Orders.mustFindByRefNum(payload.cordRefNum)
      _            ← * <~ failIf(order.state != Order.Shipped, OrderMustBeShippedForReturn(order.refNum, order.state))
      rma          ← * <~ Returns.create(Return.build(order, admin, payload.returnType))
      customer     ← * <~ Users.mustFindByAccountId(order.accountId)
      custData     ← * <~ CustomersData.mustFindByAccountId(order.accountId)
      adminData    ← * <~ AdminsData.mustFindByAccountId(admin.accountId)
      organization ← * <~ Organizations.mustFindByAccountId(admin.accountId)
      adminResponse    = Some(StoreAdminResponse.build(admin, adminData, organization))
      customerResponse = CustomerResponse.build(customer, custData)
      response         = build(rma, Some(customerResponse), adminResponse)
      _ ← * <~ LogActivity().returnCreated(admin, response)
    } yield response

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
