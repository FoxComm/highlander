package payloads

import utils.Money.Currency
import java.time.Instant

object ChannelPayloads {
  case class CreateAmazonOrderPayload(amazonOrderId: Option[Int] = None,
                                      orderTotal: Option[Int] = None,
                                      paymentMethodDetail: String,
                                      orderType: String,
                                      currency: Currency,
                                      orderStatus: Instant)
}