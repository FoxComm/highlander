package phoenix.utils

import com.github.tminglei.slickpg.LTree
import core.utils.Money
import org.json4s.JsonAST.JString
import org.json4s.{jackson, CustomSerializer, Formats, JNull, TypeHints}
import phoenix.models.admin.AdminData
import phoenix.models.auth.Identity.IdentityKind
import phoenix.models.cord.lineitems._
import phoenix.models.cord.{CordPaymentState, Order}
import phoenix.models.customer.CustomerGroup
import phoenix.models.discount.offers.OfferType
import phoenix.models.discount.qualifiers.QualifierType
import phoenix.models.inventory.SkuType
import phoenix.models.payment.creditcard.CreditCardCharge
import phoenix.models.payment.giftcard.GiftCard
import phoenix.models.payment.storecredit.StoreCredit
import phoenix.models.payment.{ExternalCharge, InStorePaymentStates, PaymentMethod}
import phoenix.models.plugins.PluginSettings
import phoenix.models.promotion.Promotion
import phoenix.models.returns._
import phoenix.models.rules.{Condition, QueryStatement}
import phoenix.models.sharedsearch.SharedSearch
import phoenix.models.shipping.Shipment
import phoenix.models.{Assignment, Note, Reason}
import phoenix.payloads.AuthPayload
import phoenix.payloads.EntityExportPayloads._
import phoenix.payloads.ReturnPayloads.ReturnLineItemPayload
import phoenix.responses.PublicResponses.CountryWithRegions

/**
  * [[TypeHints]] implementation for json4s that supports
  * discriminating of trait/class hierarchies based on [[ADT]] typeclass.
  *
  * Example:
  * {{{
  * ADTTypeHints(
  *   Map(
  *     ReturnLineItem.ShippingCost → classOf[ReturnShippingCostLineItemPayload],
  *     ReturnLineItem.SkuItem      → classOf[ReturnSkuLineItemPayload]
  * ))
  * }}}
  * It will allow to deserialize [[ReturnLineItemPayload]] based on [[ReturnLineItem.OriginType]] ADT.
  * Every json payload with [[ReturnLineItemPayload]] should then contain additional field [[JsonFormatters.TypeHintFieldName]]
  * with type hint that is string representation of specific [[ReturnLineItem.OriginType]].
  *
  * Note that you are fully responsible for providing exclusive 1:1 mapping from ADT element to class.
  * If more than one class is assigned to ADT element, then only single one will be serializable to JSON
  * and which one be will depend on underlying [[Map]] implementation.
  */
case class ADTTypeHints[T: ADT](adtHints: Map[T, Class[_]]) extends TypeHints {
  @inline private def adt = implicitly[ADT[T]]

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

  val phoenixFormats: Formats =
    DefaultFormats.withTypeHintFieldName(TypeHintFieldName) +
      Note.ReferenceType.jsonFormat + QualifierType.jsonFormat +
      OfferType.jsonFormat + Assignment.AssignmentType.jsonFormat +
      Assignment.ReferenceType.jsonFormat + Order.State.jsonFormat + Promotion.ApplyType.jsonFormat +
      OrderLineItem.State.jsonFormat + CartLineItemAdjustment.AdjustmentType.jsonFormat +
      Shipment.State.jsonFormat + GiftCard.OriginType.jsonFormat + GiftCard.State.jsonFormat +
      InStorePaymentStates.State.jsonFormat + CordPaymentState.State.jsonFormat + StoreCredit.State.jsonFormat +
      StoreCredit.OriginType.jsonFormat +
      Reason.ReasonType.jsonFormat + ReturnReason.ReasonType.jsonFormat +
      Return.State.jsonFormat + Return.ReturnType.jsonFormat +
      ReturnLineItem.OriginType.jsonFormat +
      ExternalCharge.State.jsonFormat + CountryWithRegions.jsonFormat +
      QueryStatement.Comparison.jsonFormat + Condition.Operator.jsonFormat +
      PaymentMethod.Type.jsonFormat + SkuType.jsonFormat + SharedSearch.Scope.jsonFormat +
      IdentityKind.jsonFormat + AdminData.State.jsonFormat + PluginSettings.SettingType.jsonFormat +
      CustomerGroup.GroupType.jsonFormat +
      AuthPayload.JwtClaimsSerializer + LTreeFormat +
      ReturnLineItemPayload.typeHints + PaymentMethod.Type.jsonKeyFormat +
      ExportableEntity.jsonFormat + ExportEntity.Type.jsonFormat +
      ExportEntity.typeHints + RawSortDefinition.jsonFormat

  object LTreeFormat
      extends CustomSerializer[LTree](format ⇒
        ({
          case JString(s)      ⇒ LTree(s)
          case JNull           ⇒ LTree("")
        }, { case value: LTree ⇒ JString(value.toString) }))
}
