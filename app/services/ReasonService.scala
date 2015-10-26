package services

import scala.concurrent.{ExecutionContext, Future}

import models.{Reasons, Reason}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._

object ReasonService {

  type QuerySeq = Reasons.QuerySeq

  def listAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Reason]] = {
    Reasons.queryAll.result
  }
}
