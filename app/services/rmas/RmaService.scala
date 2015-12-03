package services.rmas

import models.Rma.Canceled
import models.{Customer, Customers, Order, Orders, Reason, Reasons, Rma, Rmas, StoreAdmin}
import payloads.{RmaCreatePayload, RmaMessageToCustomerPayload, RmaUpdateStatusPayload}
import responses.RmaResponse._
import responses.{AllRmas, CustomerResponse, StoreAdminResponse}
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
    response  ← * <~ fullRma(Rmas.findByRefNum(refNum)).toXor
  } yield response).runT()

  def updateStatusByCsr(refNum: String, payload: RmaUpdateStatusPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    _         ← * <~ payload.validate.toXor
    rma       ← * <~ mustFindRmaByRefNum(refNum)
    reason    ← * <~ payload.reasonId.map(Reasons.findOneById).getOrElse(lift(None)).toXor
    _         ← * <~ cancelOrUpdate(rma, reason, payload)
    response  ← * <~ fullRma(Rmas.findByRefNum(refNum)).toXor
  } yield response).runT()

  private def cancelOrUpdate(rma: Rma, reason: Option[Reason], payload: RmaUpdateStatusPayload)
    (implicit ec: ExecutionContext, db: Database) = {

    (payload.status, reason) match {
      case (Canceled, Some(r)) ⇒
        Rmas.update(rma, rma.copy(status = payload.status, canceledReason = Some(r.id)))
      case (Canceled, None) ⇒
        DbResult.failure(InvalidCancellationReasonFailure)
      case (_, _) ⇒
        Rmas.update(rma, rma.copy(status = payload.status))
    }
  }

  def createByAdmin(admin: StoreAdmin, payload: RmaCreatePayload)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = {

    val finder = Orders.findByRefNum(payload.orderRefNum)
    finder.selectOne({ order ⇒

      createActions(order, admin, payload.rmaType).map { _.map {
        case (rma, customer) ⇒
          val adminResponse = Some(StoreAdminResponse.build(admin))
          val customerResponse = customer.map(CustomerResponse.build(_))
          build(rma, customerResponse, adminResponse)
      }}
    })
  }

  def createActions(order: Order, admin: StoreAdmin, rmaType: Rma.RmaType)
    (implicit db: Database, ec: ExecutionContext): DbResult[(Rma, Option[Customer])] = for {
        rmaXor ← Rmas.create(Rma.build(order, admin, rmaType))
        customer ← Customers.findOneById(order.customerId)
      } yield rmaXor.map(rma ⇒ (rma, customer))

  def getByRefNum(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    val finder = Rmas.findByRefNum(refNum)
    finder.selectOne(rma ⇒ DbResult.fromDbio(fromRma(rma)))
  }

  def getExpandedByRefNum(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[RootExpanded] = {
    val finder = Rmas.findByRefNum(refNum)
    finder.selectOne(rma ⇒ DbResult.fromDbio(fromRmaExpanded(rma)))
  }

  def findByOrderRef(refNum: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Future[ResultWithMetadata[Seq[AllRmas.Root]]] = {

    val finder = Orders.findByRefNum(refNum)
    finder.selectOneWithMetadata({
      order ⇒ RmaQueries.findAll(Rmas.findByOrderRefNum(refNum))
    })
  }

  def findByCustomerId(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Future[ResultWithMetadata[Seq[AllRmas.Root]]] = {

    val finder = Customers.filter(_.id === customerId)
    finder.selectOneWithMetadata({
      customer ⇒ RmaQueries.findAll(Rmas.findByCustomerId(customerId))
    })
  }
}
