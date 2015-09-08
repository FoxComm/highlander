package responses

import scala.concurrent.{ExecutionContext, Future}
import models.{GiftCard, GiftCardAdjustment}
import org.joda.time.DateTime

object GiftCardResponse {
  final case class Root(
    id: Int,
    createdAt: DateTime,
    code: String,
    `type`: String,
    status: models.GiftCard.Status,
    originalBalance: Int,
    availableBalance: Int,
    currentBalance: Int)

  def _build(gc: GiftCard): Root =
    Root(id = gc.id, createdAt = gc.createdAt, code = gc.code, `type` = gc.originType, status = gc.status,
      originalBalance = gc.originalBalance, availableBalance = gc.availableBalance, currentBalance = gc.currentBalance)

  def build(gc: GiftCard): Root = _build(gc)
}
