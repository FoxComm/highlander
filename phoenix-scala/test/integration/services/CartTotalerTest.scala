package services

import models.cord.lineitems._
import models.cord.{OrderShippingMethod, OrderShippingMethods}
import models.objects._
import models.product.{Mvp, SimpleContext}
import models.shipping.ShippingMethods
import services.carts.CartTotaler
import testutils._
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class CartTotalerTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  "OrderTotalerTest" - {
    "subTotal" - {
      "is zero when there are no line items" in new Fixture {
        CartTotaler.subTotal(cart).gimme must === (0)
      }

      "includes both SKU line items and purchased gift cards" in new LineItemsFixture {
        CartTotaler.subTotal(cart).gimme must === (variantPrice)
      }

      "uses SKU line items only if order purchases no gift cards" in new LineItemsFixture {
        CartTotaler.subTotal(cart).gimme must === (variantPrice)
      }
    }

    "shipping" - {
      "sums the shipping total from both shipping methods" in new ShippingMethodFixture {
        CartTotaler.shippingTotal(cart).gimme must === (295)
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

  trait LineItemsFixture extends Fixture {
    val (productContext, product, productShadow, variant, variantShadow, variantPrice) = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      simpleProduct  ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head)
      tup            ← * <~ Mvp.getProductTuple(simpleProduct)
      _ ← * <~ CartLineItems.create(
             CartLineItem(cordRef = cart.refNum, productVariantId = tup.variant.id))
      variantPrice ← * <~ Mvp.priceAsInt(tup.variantForm, tup.variantShadow)
    } yield
      (productContext,
       tup.product,
       tup.productShadow,
       tup.variant,
       tup.variantShadow,
       variantPrice)).gimme
  }

  trait ShippingMethodFixture extends Fixture {
    val orderShippingMethods = (for {
      shipM ← * <~ ShippingMethods.create(Factories.shippingMethods.head.copy(price = 295))
      osm   ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(cart.refNum, shipM))
    } yield osm).gimme
  }
}
