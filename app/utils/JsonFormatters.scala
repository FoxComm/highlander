package utils

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models.{GiftCardPaymentStatus, CreditCardPaymentStatus, Order}
import com.pellucid.sealerate
import org.json4s.JsonAST.JString
import org.json4s.{jackson, CustomSerializer, DefaultFormats}

object JsonFormatters {
  val serialization = jackson.Serialization

  val phoenixFormats = DefaultFormats +
    Order.Status.jsonFormat +
    GiftCardPaymentStatus.jsonFormat +
    CreditCardPaymentStatus.jsonFormat
}

