package responses

import io.circe.syntax._
import utils.Money.Currency
import utils.aliases._
import utils.json.codecs._

case class CaptureResponse(order: String,
                           captured: Int,
                           external: Int,
                           internal: Int,
                           lineItems: Int,
                           taxes: Int,
                           shipping: Int,
                           currency: Currency)
    extends ResponseItem {
  def json: Json = this.asJson
}
