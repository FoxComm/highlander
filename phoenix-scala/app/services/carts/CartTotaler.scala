package services.carts

import scala.util.Try

import models.cord.lineitems._
import models.cord._
import models.inventory.ProductVariants
import models.objects.{ObjectForms, ObjectShadows}
import utils.aliases._
import utils.db._
import utils.FoxConfig._
import utils.db.ExPostgresDriver.api._
import utils.db.DbObjectUtils._

// TODO: Use utils.Money
object CartTotaler {

  case class Totals(subTotal: Int, taxes: Int, shipping: Int, adjustments: Int, total: Int)

  object Totals {
    def build(subTotal: Int, shipping: Int, adjustments: Int, taxes: Int): Totals = {
      Totals(subTotal = subTotal,
             taxes = taxes,
             shipping = shipping,
             adjustments = adjustments,
             total = (adjustments - (subTotal + taxes + shipping)).abs)
    }

    def empty: Totals = Totals(0, 0, 0, 0, 0)
  }

  val defaultTaxRate = 0.0

  def subTotal(cart: Cart)(implicit ec: EC): DBIO[Int] =
    variantSubTotalForCart(cart)

  def variantSubTotalForCart(cart: Cart)(implicit ec: EC): DBIO[Int] =
    (for {
      lineItem ← CartLineItems if lineItem.cordRef === cart.refNum
      variant  ← ProductVariants if variant.id === lineItem.productVariantId
      form     ← ObjectForms if form.id === variant.formId
      shadow   ← ObjectShadows if shadow.id === variant.shadowId
      illuminated = (form, shadow)
      salePrice   = ((illuminated |→ "salePrice") +>> "value").asColumnOf[Int]
    } yield salePrice).sum.result.map(_.getOrElse(0))

  def shippingTotal(cart: Cart)(implicit ec: EC): DbResultT[Int] =
    for {
      orderShippingMethods ← * <~ OrderShippingMethods.findByOrderRef(cart.refNum).result
      sum = orderShippingMethods.map(_.price).sum
    } yield sum

  def adjustmentsTotal(cart: Cart)(implicit ec: EC): DbResultT[Int] =
    for {
      lineItemAdjustments ← * <~ OrderLineItemAdjustments.filter(_.cordRef === cart.refNum).result
      sum = lineItemAdjustments.map(_.subtract).sum
    } yield sum

  def taxesTotal(cart: Cart, subTotal: Int, shipping: Int, adjustments: Int)(
      implicit ec: EC): DbResultT[Int] =
    for {
      maybeAddress ← * <~ OrderShippingAddresses.findByOrderRef(cart.refNum).one
      optionalCustomRate = for {
        address        ← maybeAddress
        cfgTaxRegionId ← config.getOptString("tax_rules.region_id")
        cfgTaxRate     ← config.getOptString("tax_rules.rate")
        taxRegionId    ← Try(cfgTaxRegionId.toInt).toOption
        taxRate        ← Try(cfgTaxRate.toDouble).toOption
      } yield if (address.regionId == taxRegionId) taxRate / 100 else defaultTaxRate
      taxRate = optionalCustomRate.getOrElse(defaultTaxRate)
    } yield ((subTotal - adjustments + shipping) * taxRate).toInt

  def totals(cart: Cart)(implicit ec: EC): DbResultT[Totals] =
    for {
      sub  ← * <~ subTotal(cart)
      ship ← * <~ shippingTotal(cart)
      adj  ← * <~ adjustmentsTotal(cart)
      tax  ← * <~ taxesTotal(cart = cart, subTotal = sub, shipping = ship, adjustments = adj)
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
