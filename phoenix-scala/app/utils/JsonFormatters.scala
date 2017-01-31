package utils

import models.admin.AdminData
import models.auth.Identity.IdentityKind
import models.cord.{CordPaymentState, Order}
import models.cord.lineitems._
import models.customer.CustomerGroup
import models.discount.offers.OfferType
import models.discount.qualifiers.QualifierType
import models.inventory.SkuType
import models.payment.creditcard.CreditCardCharge
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit
import models.payment.{InStorePaymentStates, PaymentMethod}
import models.plugins.PluginSettings
import models.promotion.Promotion
import models.returns._
import models.rules.{Condition, QueryStatement}
import models.sharedsearch.SharedSearch
import models.shipping.Shipment
import models.{Assignment, Note, Reason}
import org.json4s.jackson
import payloads.AuthPayload
import responses.PublicResponses.CountryWithRegions

object JsonFormatters {
  val serialization = jackson.Serialization

  val DefaultFormats =
    org.json4s.DefaultFormats + time.JavaTimeJson4sSerializer.jsonFormat + Money.jsonFormat

  val phoenixFormats =
    DefaultFormats + Note.ReferenceType.jsonFormat + QualifierType.jsonFormat +
      OfferType.jsonFormat + Assignment.AssignmentType.jsonFormat +
      Assignment.ReferenceType.jsonFormat + Order.State.jsonFormat + Promotion.ApplyType.jsonFormat +
      OrderLineItem.State.jsonFormat + OrderLineItemAdjustment.AdjustmentType.jsonFormat +
      Shipment.State.jsonFormat + GiftCard.OriginType.jsonFormat + GiftCard.State.jsonFormat +
      InStorePaymentStates.State.jsonFormat + CordPaymentState.State.jsonFormat + StoreCredit.State.jsonFormat +
      StoreCredit.OriginType.jsonFormat +
      Reason.ReasonType.jsonFormat + Return.State.jsonFormat + Return.ReturnType.jsonFormat +
      ReturnLineItem.InventoryDisposition.jsonFormat + ReturnReason.ReasonType.jsonFormat +
      CreditCardCharge.State.jsonFormat + CountryWithRegions.jsonFormat +
      QueryStatement.Comparison.jsonFormat + Condition.Operator.jsonFormat +
      PaymentMethod.Type.jsonFormat + SkuType.jsonFormat + SharedSearch.Scope.jsonFormat +
      IdentityKind.jsonFormat + AdminData.State.jsonFormat + PluginSettings.SettingType.jsonFormat +
      AuthPayload.JwtClaimsSerializer + CustomerGroup.GroupType.jsonFormat
}
