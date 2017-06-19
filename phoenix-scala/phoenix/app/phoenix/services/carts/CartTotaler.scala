package phoenix.services.carts

import core.db._
import core.db.ExPostgresDriver.api._
import objectframework.DbObjectUtils._
import phoenix.models.inventory.Skus
import objectframework.models.{ObjectForms, ObjectShadows}
import phoenix.models.cord._
import phoenix.models.cord.lineitems._
import phoenix.utils.FoxConfig.config
import core.utils.Money._
import phoenix.models.location.Addresses

// TODO: Use utils.Money
object CartTotaler {

  case class Totals(subTotal: Long, taxes: Long, shipping: Long, adjustments: Long, total: Long)

  object Totals {
    def build(subTotal: Long, shipping: Long, adjustments: Long, taxes: Long): Totals =
      Totals(subTotal = subTotal,
             taxes = taxes,
             shipping = shipping,
             adjustments = adjustments,
             total = (subTotal + taxes + shipping - adjustments).zeroIfNegative)

    def empty: Totals = Totals(0, 0, 0, 0, 0)
  }

  val defaultTaxRate = 0.0

  def subTotal(cart: Cart)(implicit ec: EC): DBIO[Long] =
    skuSubTotalForCart(cart)

  def skuSubTotalForCart(cart: Cart)(implicit ec: EC): DBIO[Long] =
    (for {
      lineItem ← CartLineItems if lineItem.cordRef === cart.refNum
      sku      ← Skus if sku.id === lineItem.skuId
      form     ← ObjectForms if form.id === sku.formId
      shadow   ← ObjectShadows if shadow.id === sku.shadowId
      illuminated = (form, shadow)
      salePrice   = ((illuminated |→ "salePrice") +>> "value").asColumnOf[Long]
    } yield salePrice).sum.getOrElse(0L).result

  def shippingTotal(cart: Cart)(implicit ec: EC): DbResultT[Long] =
    for {
      orderShippingMethods ← * <~ OrderShippingMethods.findByOrderRef(cart.refNum).result
      sum = orderShippingMethods.map(_.price).sum
    } yield sum

  def adjustmentsTotal(cart: Cart)(implicit ec: EC): DbResultT[Long] =
    for {
      lineItemAdjustments ← * <~ CartLineItemAdjustments.filter(_.cordRef === cart.refNum).result
      sum = lineItemAdjustments.map(_.subtract).sum
    } yield sum

  def taxesTotal(cordRef: String, subTotal: Long, shipping: Long, adjustments: Long)(
      implicit ec: EC): DbResultT[Long] =
    for {
      maybeAddress ← * <~ Addresses.findByCordRef(cordRef).one
      optionalCustomRate = for {
        address     ← maybeAddress
        taxRegionId ← config.taxRules.regionId
        taxRate     ← config.taxRules.rate
      } yield if (address.regionId == taxRegionId) taxRate / 100 else defaultTaxRate
      taxRate = optionalCustomRate.getOrElse(defaultTaxRate)
    } yield (subTotal - adjustments + shipping).applyTaxes(taxRate)

  def totals(cart: Cart)(implicit ec: EC): DbResultT[Totals] =
    for {
      sub  ← * <~ subTotal(cart)
      ship ← * <~ shippingTotal(cart)
      adj  ← * <~ adjustmentsTotal(cart)
      tax  ← * <~ taxesTotal(cordRef = cart.refNum, subTotal = sub, shipping = ship, adjustments = adj)
    } yield Totals.build(subTotal = sub, shipping = ship, adjustments = adj, taxes = tax)

  def saveTotals(cart: Cart)(implicit ec: EC): DbResultT[Cart] =
    for {
      t ← * <~ totals(cart)
      withTotals = cart.copy(subTotal = t.subTotal,
                             shippingTotal = t.shipping,
                             adjustmentsTotal = t.adjustments,
                             taxesTotal = t.taxes,
                             grandTotal = t.total)
      updated ← * <~ Carts.update(cart, withTotals)
    } yield updated
}
