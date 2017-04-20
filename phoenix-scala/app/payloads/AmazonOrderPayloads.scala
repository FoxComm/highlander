package payloads

import utils.Money.Currency
import java.time.Instant

object AmazonOrderPayloads {
  case class CreateAmazonOrderPayload(amazonOrderId: String = "",
                                      orderTotal: Int,
                                      paymentMethodDetail: String,
                                      orderType: String = "",
                                      currency: Currency = Currency.USD,
                                      orderStatus: String = "",
                                      purchaseDate: Instant)
}
