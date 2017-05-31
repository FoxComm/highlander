package phoenix.payloads;

import java.time.Instant

import core.utils.Money.Currency

import com.github.tminglei.slickpg.LTree

object AmazonOrderPayloads {
  case class CreateAmazonOrderPayload(amazonOrderId: String,
                                      orderTotal: Int,
                                      paymentMethodDetail: String,
                                      orderType: String,
                                      currency: Currency = Currency.USD,
                                      orderStatus: String,
                                      scope: LTree,
                                      customerEmail: String,
                                      purchaseDate: Instant)

  case class UpdateAmazonOrderPayload(orderStatus: String)
}
