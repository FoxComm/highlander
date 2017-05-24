package phoenix.payloads

object CapturePayloads {

  case class CaptureLineItem(ref: String, sku: String);
  case class ShippingCost(total: Int, currency: String);
  case class Capture(order: String, items: Seq[CaptureLineItem], shipping: ShippingCost)

}
