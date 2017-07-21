package phoenix.responses.cord

import java.time.Instant

import phoenix.models.cord.AmazonOrder
import core.utils.Money.Currency
import phoenix.responses._
import com.github.tminglei.slickpg.LTree

case class AmazonOrderResponse(id: Int,
                               amazonOrderId: String,
                               orderTotal: Long,
                               paymentMethodDetail: String,
                               orderType: String,
                               currency: Currency,
                               orderStatus: String,
                               purchaseDate: Instant,
                               scope: LTree,
                               accountId: Int,
                               createdAt: Instant = Instant.now,
                               updatedAt: Instant = Instant.now)
    extends ResponseItem

object AmazonOrderResponse {
  def build(amazonOrder: AmazonOrder): AmazonOrderResponse =
    AmazonOrderResponse(
      id = amazonOrder.id,
      amazonOrderId = amazonOrder.amazonOrderId,
      orderTotal = amazonOrder.orderTotal,
      paymentMethodDetail = amazonOrder.paymentMethodDetail,
      orderType = amazonOrder.orderType,
      currency = amazonOrder.currency,
      orderStatus = amazonOrder.orderStatus,
      purchaseDate = amazonOrder.purchaseDate,
      scope = amazonOrder.scope,
      accountId = amazonOrder.accountId,
      createdAt = amazonOrder.createdAt,
      updatedAt = amazonOrder.updatedAt
    )
}
