package responses

import scala.concurrent.ExecutionContext

import models.GiftCard
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardResponse {
  final val mockMessage = "Not implemented yet"

  final case class Root(
    id: Int,
    createdAt: DateTime,
    code: String,
    `type`: String,
    status: GiftCard.Status,
    originalBalance: Int,
    availableBalance: Int,
    currentBalance: Int,
    createdBy: Option[Any],
    message: String)

  def build(gc: GiftCard, createdBy: Option[Any] = None): Root =
    Root(id = gc.id, createdAt = gc.createdAt, code = gc.code, `type` = gc.originType, status = gc.status,
      originalBalance = gc.originalBalance, availableBalance = gc.availableBalance, currentBalance = gc.currentBalance,
      createdBy = createdBy, message = mockMessage)
}
