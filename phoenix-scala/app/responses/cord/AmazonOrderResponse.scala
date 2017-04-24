package responses.cord

import java.time.Instant
import models.cord.AmazonOrder
import utils.Money.Currency
import responses._
import com.github.tminglei.slickpg.LTree

case class AmazonOrderResponse(id: Int,
                               amazonOrderId: String = "",
                               orderTotal: Int = 0,
                               paymentMethodDetail: String = "",
                               orderType: String = "",
                               currency: Currency = Currency.USD,
                               orderStatus: String = "",
                               purchaseDate: Instant,
                               scope: LTree,
                               accountId: Int,
                               createdAt: Instant = Instant.now,
                               updatedAt: Instant = Instant.now)
    extends ResponseItem

object AmazonOrderResponse {

  case class Root(id: Int,
                  amazonOrderId: String,
                  orderTotal: Int,
                  paymentMethodDetail: String,
                  orderType: String,
                  currency: Currency,
                  orderStatus: String,
                  purchaseDate: Instant,
                  scope: LTree,
                  accountId: Int,
                  createdAt: Instant,
                  updatedAt: Instant)
      extends ResponseItem

  def build(amazonOrder: AmazonOrder): Root =
    Root(id = amazonOrder.id,
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
         updatedAt = amazonOrder.updatedAt)
}
