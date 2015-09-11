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
    status: String,
    originalBalance: Int,
    availableBalance: Int,
    currentBalance: Int,
    customer: Option[CustomerResponse.Root],
    storeAdmin: Option[StoreAdminResponse.Root],
    message: String)

  def build(gc: GiftCard, customer: Option[CustomerResponse.Root] = None, admin: Option[StoreAdminResponse.Root] = None):
  Root =
    Root(id = gc.id, createdAt = gc.createdAt, code = gc.code, `type` = gc.originType.toString, status = gc.status.toString,
      originalBalance = gc.originalBalance, availableBalance = gc.availableBalance, currentBalance = gc.currentBalance,
      customer = customer, storeAdmin = admin, message = mockMessage)
}
