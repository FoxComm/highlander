package responses

import scala.concurrent.ExecutionContext

import models.{Customer, GiftCard}
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardResponse {
  final val message = "Not implemented yet"

  final case class Root(
    id: Int,
    createdAt: DateTime,
    code: String,
    `type`: String,
    status: models.GiftCard.Status,
    originalBalance: Int,
    availableBalance: Int,
    currentBalance: Int,
    customer: Option[Customer],
    message: String)

  def build(gc: GiftCard, customer: Option[Customer] = None)(implicit db: Database, ec: ExecutionContext): Root =
    Root(id = gc.id, createdAt = gc.createdAt, code = gc.code, `type` = gc.originType, status = gc.status,
      originalBalance = gc.originalBalance, availableBalance = gc.availableBalance, currentBalance = gc.currentBalance,
      customer = customer, message = message)
}
