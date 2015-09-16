package services

import scala.concurrent.{ExecutionContext, Future}

import models.{Reasons, Reason}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object ReasonService {
  def listAll(implicit db: Database, ec: ExecutionContext): Future[Seq[Reason]] = {
    Reasons.sortBy(_.id.asc).result.run()
  }
}
