package utils

import models.{CreditCardCharge, GiftCard, GiftCardAdjustment, Order, OrderLineItem,
Shipment, StoreCredit, StoreCreditAdjustment}
import models.rules.{Condition, QueryStatement}
import org.json4s.ext.DateTimeSerializer
import org.json4s.jackson
import responses.CountryWithRegions

object JsonFormatters {
  val serialization = jackson.Serialization

  val DefaultFormats = org.json4s.DefaultFormats + DateTimeSerializer + Money.jsonFormat

  val phoenixFormats = DefaultFormats +
    Order.Status.jsonFormat +
    OrderLineItem.Status.jsonFormat +
    Shipment.Status.jsonFormat +
    GiftCard.OriginType.jsonFormat +
    GiftCard.Status.jsonFormat +
    GiftCardAdjustment.Status.jsonFormat +
    StoreCredit.Status.jsonFormat +
    StoreCreditAdjustment.Status.jsonFormat +
    CreditCardCharge.Status.jsonFormat +
    CountryWithRegions.jsonFormat +
    QueryStatement.Comparison.jsonFormat +
    Condition.Operator.jsonFormat
}

