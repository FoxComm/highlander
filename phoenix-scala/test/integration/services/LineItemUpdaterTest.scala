package services

import models.cord.lineitems._
import models.objects._
import models.product.{Mvp, SimpleContext, SimpleProductData}
import payloads.LineItemPayloads.{UpdateLineItemsPayload ⇒ Payload}
import testutils._
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api._
import utils.MockedApis
import utils.aliases._
import utils.db._
import utils.seeds.Seeds.Factories

class LineItemUpdaterTest
    extends IntegrationTestBase
    with TestObjectContext
    with TestActivityContext.AdminAC
    with MockedApis
    with BakedFixtures {

  def createProducts(num: Int)(
      implicit au: AU): DbResultT[(ObjectContext, Seq[SimpleProductData])] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      products ← * <~ Mvp.insertProducts((1 to num).map { i ⇒
                  Factories.products.head.copy(code = i.toString, price = 5)
                }, context.id)
    } yield (context, products)

  "LineItemUpdater" - {

    "Adds line items when the sku doesn't exist in cart" in new Fixture {
      val (context, products) = createProducts(2).gimme

      val payload = Seq[Payload](
          Payload(productVariantId = TEMPORARY_skuCodeToVariantFormId("1"), quantity = 3),
          Payload(productVariantId = TEMPORARY_skuCodeToVariantFormId("2"), quantity = 0)
      )

      val root =
        LineItemUpdater.updateQuantitiesOnCart(storeAdmin, cart.refNum, payload).gimme.result

      import root.lineItems.skus
      skus.count(_.sku == "1") must be(1)
      skus.count(_.sku == "2") must be(0)
      // TODO: check if *variant* IDs match?

      skus.find(_.sku === "1").map(_.quantity) must be (Some(3))

      skus.map(_.quantity).sum must === (CartLineItems.size.gimme)
    }

    "Updates line items when the ProductVariant already is in cart" in new Fixture {
      val (context, products) = createProducts(3).gimme
      val seedItems = Seq(1, 1, 1, 1, 1, 1, 2, 3, 3).map { skuId ⇒
        CartLineItem(cordRef = cart.refNum, productVariantId = skuId)
      }

      CartLineItems.createAll(seedItems).gimme

      val payload = Seq[Payload](
          Payload(productVariantId = TEMPORARY_skuCodeToVariantFormId("1"), quantity = 3),
          Payload(productVariantId = TEMPORARY_skuCodeToVariantFormId("2"), quantity = 0),
          Payload(productVariantId = TEMPORARY_skuCodeToVariantFormId("3"), quantity = 1)
      )

      val root =
        LineItemUpdater.updateQuantitiesOnCart(storeAdmin, cart.refNum, payload).gimme.result

      import root.lineItems.skus
      skus.count(_.sku == "1") must be(1)
      skus.count(_.sku == "2") must be(0)
      skus.count(_.sku == "3") must be(1)

      skus.find(_.sku === "1").map(_.quantity) must be (Some(3))

      skus.map(_.quantity).sum must === (CartLineItems.gimme.size)
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed
}
