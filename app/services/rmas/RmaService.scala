package services.rmas

import scala.concurrent.{Future, ExecutionContext}

import models._
import payloads._
import responses.{CustomerResponse, StoreAdminResponse, AllRmas}
import services._
import responses.RmaResponse._
import services.rmas.Helpers._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.DbResult
import utils.Slick.implicits._

object RmaService {
  def updateMessageToCustomer(refNum: String, payload: RmaMessageToCustomerPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    _         ← * <~ payload.validate.toXor
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    newMessage = if (payload.message.length > 0) Some(payload.message) else None
    update    ← * <~ Rmas.update(rma, rma.copy(messageToCustomer = newMessage))
    response  ← * <~ fullRma(Rmas.findByRefNum(refNum)).toXor
  } yield response).runT()

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
