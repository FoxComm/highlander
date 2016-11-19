package services

import models.cord.lineitems._
import models.objects._
import models.product.{Mvp, SimpleContext, SimpleProductData}
import services.inventory.SkuManager
import payloads.LineItemPayloads.{UpdateLineItemsPayload ⇒ Payload}
import testutils._
import testutils.fixtures.BakedFixtures
import utils.MockedApis
import utils.aliases._
import utils.db._
import utils.seeds.Seeds.Factories

import scala.collection.immutable.::

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
                  Factories.products.head.copy(skuCode = i.toString, price = 5)
                }, context.id)
    } yield (context, products)

  "LineItemUpdater" - {

    def skuFormId =
      (contextId: Int) ⇒
        (code: String) ⇒ SkuManager.mustFindSkuByContextAndCode(contextId, code).gimme.formId

    "Adds line items when the sku doesn't exist in cart" in new Fixture {
      val (context, _) = createProducts(2).gimme
      val skuId        = skuFormId(context.id)

      val payload = Seq[Payload](
          Payload(skuId = skuId("1"), quantity = 3),
          Payload(skuId = skuId("2"), quantity = 0)
      )

      val root =
        LineItemUpdater.updateQuantitiesOnCart(storeAdmin, cart.refNum, payload).gimme.result
      root.lineItems.skus.count(_.skuCode == "1") must be(1)
      root.lineItems.skus.count(_.skuCode == "2") must be(0)

      root.lineItems.skus.find(_.skuCode === "1") match {
        case Some(s) ⇒
          s.quantity must be(3)
        case None ⇒
          fail("Should have found sku 1")
      }

      root.lineItems.skus.foldLeft(0)(_ + _.quantity) must === (CartLineItems.size.gimme)
    }

    "Updates line items when the Sku already is in cart" in new Fixture {
      val (context, _) = createProducts(3).gimme
      val skuId        = skuFormId(context.id)
      val seedItems = Seq(1, 1, 1, 1, 1, 1, 2, 3, 3).map { skuId ⇒
        CartLineItem(cordRef = cart.refNum, skuId = skuId)
      }

      CartLineItems.createAll(seedItems).gimme

      val payload = Seq[Payload](
          Payload(skuId = skuId("1"), quantity = 3),
          Payload(skuId = skuId("2"), quantity = 0),
          Payload(skuId = skuId("3"), quantity = 1)
      )

      val root =
        LineItemUpdater.updateQuantitiesOnCart(storeAdmin, cart.refNum, payload).gimme.result
      root.lineItems.skus.count(_.skuCode == "1") must be(1)
      root.lineItems.skus.count(_.skuCode == "2") must be(0)
      root.lineItems.skus.count(_.skuCode == "3") must be(1)

      root.lineItems.skus.find(_.skuCode === "1") match {
        case Some(s) ⇒
          s.quantity must be(3)
        case None ⇒
          fail("Should have found sku 1")
      }

      root.lineItems.skus.foldLeft(0)(_ + _.quantity) must === (CartLineItems.gimme.size)
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed
}
