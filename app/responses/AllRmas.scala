package responses

import scala.concurrent.{ExecutionContext, Future}

import models._
import slick.driver.PostgresDriver.api._

object AllRmas {
  type Response = Future[Seq[Root]]

  final case class Root(
    referenceNumber: String,
    status: Rma.Status,
    total: Option[Int] = None
    ) extends ResponseItem

  def build(rma: Rma)(implicit ec: ExecutionContext): DBIO[Root] = {
    DBIO.successful(
      Root(
        referenceNumber = rma.referenceNumber,
        status = rma.status
      )
    )
  }
}