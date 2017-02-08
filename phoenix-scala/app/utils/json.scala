package utils

import models.admin.AdminData
import models.auth.Identity.IdentityKind
import models.cord.{CordPaymentState, Order}
import models.cord.lineitems._
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
import org.json4s.{TypeHints, jackson}
import payloads.AuthPayload
import payloads.ReturnPayloads.ReturnLineItemPayload
import responses.PublicResponses.CountryWithRegions

case class ADTTypeHints[T: ADT](adtHints: Map[T, Class[_]]) extends TypeHints {
  @inline def adt = implicitly[ADT[T]]

  private[this] lazy val reversed = adtHints.map(_.swap)

  lazy val hints: List[Class[_]] = adtHints.valuesIterator.toList

  def hintFor(clazz: Class[_]): String =
    reversed.get(clazz).fold(sys.error(s"No hint defined for ${clazz.getName}"))(adt.show(_))

  def classFor(hint: String): Option[Class[_]] = adt.typeMap.get(hint).flatMap(adtHints.get)
}

object JsonFormatters {
  val serialization = jackson.Serialization

  val DefaultFormats =
    org.json4s.DefaultFormats + time.JavaTimeJson4sSerializer.jsonFormat + Money.jsonFormat

  val TypeHintFieldName = "payloadType"

  val phoenixFormats =
    DefaultFormats.withTypeHintFieldName(TypeHintFieldName) +
      Note.ReferenceType.jsonFormat + QualifierType.jsonFormat +
      OfferType.jsonFormat + Assignment.AssignmentType.jsonFormat +
      Assignment.ReferenceType.jsonFormat + Order.State.jsonFormat + Promotion.ApplyType.jsonFormat +
      OrderLineItem.State.jsonFormat + OrderLineItemAdjustment.AdjustmentType.jsonFormat +
      Shipment.State.jsonFormat + GiftCard.OriginType.jsonFormat + GiftCard.State.jsonFormat +
      InStorePaymentStates.State.jsonFormat + CordPaymentState.State.jsonFormat + StoreCredit.State.jsonFormat +
      StoreCredit.OriginType.jsonFormat +
      Reason.ReasonType.jsonFormat + ReturnReason.ReasonType.jsonFormat +
      Return.State.jsonFormat + Return.ReturnType.jsonFormat +
      ReturnLineItem.InventoryDisposition.jsonFormat + ReturnLineItem.OriginType.jsonFormat +
      CreditCardCharge.State.jsonFormat + CountryWithRegions.jsonFormat +
      QueryStatement.Comparison.jsonFormat + Condition.Operator.jsonFormat +
      PaymentMethod.Type.jsonFormat + SkuType.jsonFormat + SharedSearch.Scope.jsonFormat +
      IdentityKind.jsonFormat + AdminData.State.jsonFormat + PluginSettings.SettingType.jsonFormat +
      AuthPayload.JwtClaimsSerializer + ReturnLineItemPayload.typeHints
}
