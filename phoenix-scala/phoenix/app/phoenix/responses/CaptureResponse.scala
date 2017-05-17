package phoenix.responses

import utils.Money.Currency

case class CaptureResponse(order: String,
                           captured: Int,
                           external: Int,
                           internal: Int,
                           lineItems: Int,
                           taxes: Int,
                           shipping: Int,
                           currency: Currency)
    extends ResponseItem
