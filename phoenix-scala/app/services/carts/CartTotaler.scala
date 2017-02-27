package services.carts

import models.cord.lineitems._
import models.cord._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.FoxConfig.config

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
    skuSubTotalForCart(cart)

  def skuSubTotalForCart(cart: Cart)(implicit ec: EC): DBIO[Int] =
    sql"""select count(*), sum(coalesce(cast(sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value' as integer), 0)) as sum
       |	from cart_line_items sli
       |	left outer join skus sku on (sku.id = sli.sku_id)
       |	left outer join object_forms sku_form on (sku_form.id = sku.form_id)
       |	left outer join object_shadows sku_shadow on (sku_shadow.id = sku.shadow_id)
       |
       |	where sli.cord_ref = ${cart.refNum}
       | """.stripMargin.as[(Int, Int)].headOption.map {
      case Some((count, total)) if count > 0 ⇒ total
      case _                                 ⇒ 0
    }

  def shippingTotal(cart: Cart)(implicit ec: EC): DbResultT[Int] =
    for {
      orderShippingMethods ← * <~ OrderShippingMethods.findByOrderRef(cart.refNum).result
      sum = orderShippingMethods.foldLeft(0)(_ + _.price)
    } yield sum

  def adjustmentsTotal(cart: Cart)(implicit ec: EC): DbResultT[Int] =
    for {
      lineItemAdjustments ← * <~ OrderLineItemAdjustments.filter(_.cordRef === cart.refNum).result
      sum = lineItemAdjustments.foldLeft(0)(_ + _.subtract)
    } yield sum

  def taxesTotal(cordRef: String, subTotal: Int, shipping: Int, adjustments: Int)(
      implicit ec: EC): DbResultT[Int] =
    for {
      maybeAddress ← * <~ OrderShippingAddresses.findByOrderRef(cordRef).one
      optionalCustomRate = for {
        address     ← maybeAddress
        taxRegionId ← config.taxRules.regionId
        taxRate     ← config.taxRules.rate
      } yield if (address.regionId == taxRegionId) taxRate / 100 else defaultTaxRate
      taxRate = optionalCustomRate.getOrElse(defaultTaxRate)
    } yield ((subTotal - adjustments + shipping) * taxRate).toInt

  def totals(cart: Cart)(implicit ec: EC): DbResultT[Totals] =
    for {
      sub  ← * <~ subTotal(cart)
      ship ← * <~ shippingTotal(cart)
      adj  ← * <~ adjustmentsTotal(cart)
      tax ← * <~ taxesTotal(cordRef = cart.refNum,
                            subTotal = sub,
                            shipping = ship,
                            adjustments = adj)
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
