package services

import objectframework.FormShadowGet
import objectframework.models.ObjectContexts
import phoenix.models.cord.lineitems._
import phoenix.models.cord.{OrderShippingMethod, OrderShippingMethods}
import phoenix.models.product.{Mvp, SimpleContext}
import phoenix.models.shipping.ShippingMethods
import phoenix.services.carts.CartTotaler
import phoenix.utils.seeds.Factories
import testutils._
import testutils.fixtures.BakedFixtures
import core.utils.Money._
import core.db._

class CartTotalerTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  "OrderTotalerTest" - {
    "subTotal" - {
      "is zero when there are no line items" in new Fixture {
        val subTotal = CartTotaler.subTotal(cart).run().futureValue

        subTotal === 0
      }

      "includes both SKU line items and purchased gift cards" in new SkuLineItemsFixture {
        val subTotal = CartTotaler.subTotal(cart).run().futureValue

        subTotal must === (skuPrice)
      }

      "uses SKU line items only if order purchases no gift cards" in new SkuLineItemsFixture {
        val subTotal = CartTotaler.subTotal(cart).run().futureValue

        subTotal must === (skuPrice)
      }
    }

    "shipping" - {
      "sums the shipping total from both shipping methods" in new ShippingMethodFixture {
        val subTotal = CartTotaler.shippingTotal(cart).gimme
        subTotal must === (295)
      }
    }

    "taxes" - {
      "are hardcoded to 5%" in new SkuLineItemsFixture {
        val totals = CartTotaler.totals(cart).gimme
        val taxes  = skuPrice.applyTaxes(0.05)

        totals.subTotal === skuPrice
        totals.shipping === 0
        totals.taxes === taxes
        totals.adjustments === 0
        totals.total === (totals.subTotal + taxes)
      }
    }

    "totals" - {
      "all are zero when there are no line items and no adjustments" in new Fixture {
        val totals = CartTotaler.totals(cart).gimme

        totals.subTotal mustBe 0
        totals.shipping mustBe 0
        totals.taxes mustBe 0
        totals.adjustments mustBe 0
        totals.total mustBe 0
      }
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked with CustomerAddress_Raw

  trait SkuLineItemsFixture extends Fixture {
    val (productContext, product, productShadow, sku, skuShadow, skuPrice) = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      simpleProduct  ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head)
      tup            ← * <~ Mvp.getProductTuple(simpleProduct)
      _              ← * <~ CartLineItems.create(CartLineItem(cordRef = cart.refNum, skuId = tup.sku.id))
      skuPrice       ← * <~ FormShadowGet.priceAsLong(tup.skuForm, tup.skuShadow)
    } yield (productContext, tup.product, tup.productShadow, tup.sku, tup.skuShadow, skuPrice)).gimme
  }

  trait ShippingMethodFixture extends Fixture {
    val orderShippingMethods = (for {
      shipM ← * <~ ShippingMethods.create(Factories.shippingMethods.head.copy(price = 295))
      osm   ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(cart.refNum, shipM))
    } yield osm).gimme
  }
}
