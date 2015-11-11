package services.rmas

import scala.concurrent.{Future, ExecutionContext}

import models._
import payloads._
import responses.{CustomerResponse, StoreAdminResponse, AllRmas}
import services._
import responses.RmaResponse._
import slick.driver.PostgresDriver.api._
import slick.jdbc.TransactionIsolation
import utils.CustomDirectives
import utils.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.DbResult
import utils.Slick.implicits._

object RmaService {
  def createByAdmin(admin: StoreAdmin, payload: RmaCreatePayload)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = {

    val finder = Orders.findByRefNum(payload.orderRefNum)
    finder.selectOne({ order ⇒

      val future = createActions(order, admin, payload.rmaType).run().flatMap {
        case (rma, customer) ⇒
          val adminResponse = Some(StoreAdminResponse.build(admin))
          val customerResponse = customer.map(CustomerResponse.build(_))
          Future.successful(build(rma, customerResponse, adminResponse))
      }

      DbResult.fromFuture(future)
    })
  }

  def createActions(order: Order, admin: StoreAdmin, rmaType: Rma.RmaType)
    (implicit db: Database, ec: ExecutionContext): DBIO[(Rma, Option[Customer])] = for {
      rma ← Rmas.saveNew(Rma.build(order, admin, rmaType))
      customer ← Customers.findOneById(order.customerId)
  } yield (rma, customer)

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