package services

import scala.concurrent.ExecutionContext

import models.{RmaReasons, RmaReason, Reasons, Reason}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._

object ReasonService {
  def listReasons(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage):
  ResultWithMetadata[Seq[Reason]] = {
    Reasons.queryAll.result
  }

  def listRmaReasons(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage):
  ResultWithMetadata[Seq[RmaReason]] = {
    RmaReasons.queryAll.result
  }
}
