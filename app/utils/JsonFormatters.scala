package utils

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import com.pellucid.sealerate
import org.json4s.JsonAST.JString

import models.{StoreCreditAdjustment, GiftCardAdjustment, CreditCardCharge, GiftCard, Order, OrderLineItem, Shipment,
StoreCredit}
import org.json4s.ext.DateTimeSerializer
import org.json4s.{DefaultFormats, jackson}
import responses.CountryWithRegions

object JsonFormatters {
  val serialization = jackson.Serialization

  val DefaultFormats = org.json4s.DefaultFormats + DateTimeSerializer + Money.jsonFormat

  val phoenixFormats = DefaultFormats +
    Order.Status.jsonFormat +
    OrderLineItem.Status.jsonFormat +
    Shipment.Status.jsonFormat +
    GiftCard.Status.jsonFormat +
    GiftCardAdjustment.Status.jsonFormat +
    StoreCredit.Status.jsonFormat +
    StoreCreditAdjustment.Status.jsonFormat +
    CreditCardCharge.Status.jsonFormat +
    CountryWithRegions.jsonFormat
}

