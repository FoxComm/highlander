package phoenix.responses

import utils.Money.Currency

case class CaptureResponse(order: String,
                           captured: Long,
                           external: Long,
                           internal: Long,
                           lineItems: Long,
                           taxes: Long,
                           shipping: Long,
                           currency: Currency)
    extends ResponseItem
