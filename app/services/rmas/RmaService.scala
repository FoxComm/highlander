package services.rmas

import scala.concurrent.{Future, ExecutionContext}

import models._
import services._
import responses.RmaResponse._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives
import utils.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.DbResult
import utils.Slick.implicits._

object RmaService {
  def getByRefNum(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    val finder = Rmas.findByRefNum(refNum)
    finder.selectOne(rma ⇒ DbResult.fromDbio(fromRma(rma)))
  }

  def getExpandedByRefNum(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[RootExpanded] = {
    val finder = Rmas.findByRefNum(refNum)
    finder.selectOne(rma ⇒ DbResult.fromDbio(fromRmaExpanded(rma)))
  }

  def findAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Root]] = {
    Rmas.queryAll.result.map(_.map(build(_)))
  }

  def findByOrderRef(refNum: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Future[ResultWithMetadata[Seq[Root]]] = {

    val finder = Orders.findByRefNum(refNum)
    finder.selectOneWithMetadata({
      order ⇒ Rmas.queryByOrderRefNum(refNum).result.map(_.map(build(_)))
    })
  }

  def findByCustomerId(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Future[ResultWithMetadata[Seq[Root]]] = {

    val finder = Customers.filter(_.id === customerId)
    finder.selectOneWithMetadata({
      customer ⇒ Rmas.queryByCustomerId(customerId).result.map(_.map(build(_)))
    })
  }
}