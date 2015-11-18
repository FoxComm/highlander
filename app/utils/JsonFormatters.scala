package utils

import models._
import models.rules.{Condition, QueryStatement}
import org.json4s.ext.DateTimeSerializer
import org.json4s.jackson
import responses.CountryWithRegions

object JsonFormatters {
  val serialization = jackson.Serialization

  val DefaultFormats = org.json4s.DefaultFormats + time.JavaTimeJson4sSerializer.jsonFormat + Money.jsonFormat

  val phoenixFormats = DefaultFormats +
    Order.Status.jsonFormat +
    OrderLineItem.Status.jsonFormat +
    Shipment.Status.jsonFormat +
    GiftCard.OriginType.jsonFormat +
    GiftCard.Status.jsonFormat +
    GiftCardAdjustment.Status.jsonFormat +
    StoreCredit.Status.jsonFormat +
    StoreCredit.OriginType.jsonFormat +
    StoreCreditAdjustment.Status.jsonFormat +
    Rma.Status.jsonFormat +
    Rma.RmaType.jsonFormat +
    RmaLineItem.InventoryDisposition.jsonFormat +
    RmaReason.ReasonType.jsonFormat +
    CreditCardCharge.Status.jsonFormat +
    CountryWithRegions.jsonFormat +
    QueryStatement.Comparison.jsonFormat +
    Condition.Operator.jsonFormat +
    PaymentMethod.Type.jsonFormat
}

