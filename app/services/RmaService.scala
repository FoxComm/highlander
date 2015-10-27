package services

import scala.concurrent.ExecutionContext

import models._
import responses.RmaResponse._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.DbResult

object RmaService {
  def getByRefNum(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    val finder = Rmas.findByRefNum(refNum)

    finder.selectOne({
      rma ⇒ DbResult.fromDbio(fromRma(rma))
    }, checks = finder.checks, notFoundFailure = NotFoundFailure404(Rma, refNum))
  }
}