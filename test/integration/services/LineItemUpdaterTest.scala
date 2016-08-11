package services

import scala.concurrent.ExecutionContext.Implicits.global

import models.StoreAdmins
import models.activity.ActivityContext
import models.cord.Carts
import models.cord.lineitems._
import models.customer.Customers
import models.objects._
import models.product.{Mvp, SimpleContext, SimpleProductData}
import payloads.LineItemPayloads.{UpdateLineItemsPayload ⇒ Payload}
import util._
import utils.db._
import utils.seeds.Seeds.Factories

class LineItemUpdaterTest
    extends IntegrationTestBase
    with TestObjectContext
    with TestActivityContext.AdminAC {

  import api._

  implicit val activityContext = ActivityContext(userId = 1, userType = "b", transactionId = "c")
  implicit val elaticsearchApi = utils.ElasticsearchApi.default()

  def createProducts(num: Int): DbResultT[(ObjectContext, Seq[SimpleProductData])] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      products ← * <~ Mvp.insertProducts((1 to num).map { i ⇒
                  Factories.products.head.copy(code = i.toString, price = 5)
                }, context.id)
    } yield (context, products)

  def createLineItems(items: Seq[OrderLineItem]) = OrderLineItems.createAll(items).gimme

  "LineItemUpdater" - {

    "Adds line items when the sku doesn't exist in cart" in new Fixture {
      val (context, products) = createProducts(2).gimme

      val payload = Seq[Payload](
          Payload(sku = "1", quantity = 3),
          Payload(sku = "2", quantity = 0)
      )

      val root = LineItemUpdater.updateQuantitiesOnCart(admin, cart.refNum, payload).gimme.result
      root.lineItems.skus.count(_.sku == "1") must be(1)
      root.lineItems.skus.count(_.sku == "2") must be(0)

      root.lineItems.skus.find(_.sku === "1") match {
        case Some(s) ⇒
          s.quantity must be(3)
        case None ⇒
          assert(false, "Should have found sku 1")
      }

      val allRecords = OrderLineItems.gimme
      root.lineItems.skus.foldLeft(0)((a, b) ⇒ a + b.quantity) must === (allRecords.size)

      OrderLineItemSkus.gimme.size must === (2)
    }

    "Updates line items when the Sku already is in cart" in new Fixture {
      val (context, products) = createProducts(3).gimme
      val seedItems = Seq(1, 1, 1, 1, 1, 1, 2, 3, 3).map { linkId ⇒
        OrderLineItem(id = 0,
                      cordRef = cart.refNum,
                      originId = linkId,
                      originType = OrderLineItem.SkuItem)
      }
      createLineItems(seedItems)

      val payload = Seq[Payload](
          Payload(sku = "1", quantity = 3),
          Payload(sku = "2", quantity = 0),
          Payload(sku = "3", quantity = 1)
      )

      val root = LineItemUpdater.updateQuantitiesOnCart(admin, cart.refNum, payload).gimme.result
      root.lineItems.skus.count(_.sku == "1") must be(1) //Because same skus are grouped
      root.lineItems.skus.count(_.sku == "2") must be(0)
      root.lineItems.skus.count(_.sku == "3") must be(1)

      root.lineItems.skus.find(_.sku === "1") match {
        case Some(s) ⇒
          s.quantity must be(3)
        case None ⇒
          assert(false, "Should have found sku 1")
      }

      root.lineItems.skus.foldLeft(0)((a, b) ⇒ a + b.quantity) must === (OrderLineItems.gimme.size)
    }
  }

  trait Fixture {
    val (cart, admin) = (for {
      _     ← * <~ Customers.create(Factories.customer)
      cart  ← * <~ Carts.create(Factories.cart)
      admin ← * <~ StoreAdmins.create(Factories.storeAdmin)
    } yield (cart, admin)).gimme
  }
}
