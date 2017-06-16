package services

import objectframework.models.{ObjectContext, ObjectContexts}
import phoenix.models.cord.lineitems._
import phoenix.models.product.{Mvp, SimpleContext, SimpleProductData}
import phoenix.payloads.LineItemPayloads.{UpdateLineItemsPayload ⇒ Payload}
import phoenix.utils.aliases._
import phoenix.utils.seeds.Factories
import testutils._
import testutils.fixtures.BakedFixtures
import core.db._
import phoenix.services.carts.CartLineItemUpdater

class LineItemUpdaterTest
    extends IntegrationTestBase
    with TestObjectContext
    with TestActivityContext.AdminAC
    with BakedFixtures {

  def createProducts(num: Int)(implicit au: AU): DbResultT[(ObjectContext, Seq[SimpleProductData])] =
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
        Payload(sku = "1", quantity = 3),
        Payload(sku = "2", quantity = 0)
      )

      val root =
        CartLineItemUpdater.updateQuantitiesOnCart(storeAdmin, cart.refNum, payload).gimme.result
      root.lineItems.skus.count(_.sku == "1") must be(1)
      root.lineItems.skus.count(_.sku == "2") must be(0)

      root.lineItems.skus.find(_.sku === "1") match {
        case Some(s) ⇒
          s.quantity must be(3)
        case None ⇒
          fail("Should have found sku 1")
      }

      root.lineItems.skus.map(_.quantity).sum must === (CartLineItems.size.gimme)
    }

    "Updates line items when the Sku already is in cart" in new Fixture {
      val (context, products) = createProducts(3).gimme
      val seedItems = Seq(1, 1, 1, 1, 1, 1, 2, 3, 3).map { idx ⇒
        CartLineItem(cordRef = cart.refNum, skuId = products(idx - 1).skuId)
      }

      CartLineItems.createAll(seedItems).gimme

      val payload = Seq[Payload](
        Payload(sku = "1", quantity = 3),
        Payload(sku = "2", quantity = 0),
        Payload(sku = "3", quantity = 1)
      )

      val root =
        CartLineItemUpdater.updateQuantitiesOnCart(storeAdmin, cart.refNum, payload).gimme.result
      root.lineItems.skus.count(_.sku == "1") must be(1)
      root.lineItems.skus.count(_.sku == "2") must be(0)
      root.lineItems.skus.count(_.sku == "3") must be(1)

      root.lineItems.skus.find(_.sku === "1") match {
        case Some(s) ⇒
          s.quantity must be(3)
        case None ⇒
          fail("Should have found sku 1")
      }

      root.lineItems.skus.map(_.quantity).sum must === (CartLineItems.gimme.size)
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed
}
