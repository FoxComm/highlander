package services

import java.time.Instant

import models.activity.ActivityContext
import models.inventory._
import models.order.lineitems._
import models.order.{Orders, Order}
import models.StoreAdmins
import models.product.{Mvp, ProductContext, ProductContexts, SimpleContext, SimpleProductData}
import payloads.{UpdateLineItemsPayload => Payload}
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.Seeds.Factories

class LineItemUpdaterTest extends IntegrationTestBase {
  import api._

  import concurrent.ExecutionContext.Implicits.global

  implicit val ac = ActivityContext(userId = 1, userType = "b", transactionId = "c")

  val lineItems = TableQuery[OrderLineItems]
  val lineItemSkus = TableQuery[OrderLineItemSkus]

  def createProducts(num: Int): DbResultT[(ProductContext, Seq[SimpleProductData])] = for {
    productContext ← * <~ ProductContexts.mustFindById404(SimpleContext.id)
    products ← * <~ Mvp.insertProducts((1 to num).map { i ⇒
      Factories.products.head.copy(code = i.toString, price = 5)
    }, productContext.id)
  } yield (productContext, products)

  def createLineItems(items: Seq[OrderLineItem]): Unit = {
    OrderLineItems.createAll(items).run().futureValue.rightVal
  }

  def createDefaultWarehouse() : Warehouse =
    Warehouses.create(Factories.warehouse).run().futureValue.rightVal

  "LineItemUpdater" - {

    "Adds line items when the sku doesn't exist in order" in new Fixture {
      val (productContext, products) = createProducts(2).run().futureValue.rightVal
      val order = Orders.create(Order(customerId = 1, productContextId = productContext.id)).run().futureValue.rightVal
      //      val warehouse = createDefaultWarehouse()
      //      createDefaultWarehouse()
      
      //      createInventory(warehouse.id, 1, 100)
      //      createInventory(warehouse.id, 2, 100)

      val payload = Seq[Payload](
        Payload(sku = "1", quantity = 3),
        Payload(sku = "2", quantity = 0)
      )

      val root = LineItemUpdater.updateQuantitiesOnOrder(admin, order.refNum, payload).futureValue.rightVal.result
      root.lineItems.skus.count(_.sku == "1") must be(3)
      root.lineItems.skus.count(_.sku == "2") must be(0)

      val allRecords = db.run(lineItems.result).futureValue
      root.lineItems.skus.size must ===(allRecords.size)

      val allRelations = db.run(lineItemSkus.result).futureValue
      allRelations.size must ===(2)
    }

    "Updates line items when the Sku already is in order" in new Fixture {
      val (productContext, products) = createProducts(3).run().futureValue.rightVal
      val order = Orders.create(Order(customerId = 1, productContextId = productContext.id)).run().futureValue.rightVal
      //      val warehouse = createDefaultWarehouse()
      //      createInventory(warehouse.id, 1, 100)
      //      createInventory(warehouse.id, 2, 100)
      //      createInventory(warehouse.id, 3, 100)
      val seedItems = Seq(1, 1, 1, 1, 1, 1, 2, 3, 3).map { linkId ⇒
        OrderLineItem(id = 0, orderId = 1, originId = linkId, originType = OrderLineItem.SkuItem)
      }
      createLineItems(seedItems)

      val payload = Seq[Payload](
        Payload(sku = "1", quantity = 3),
        Payload(sku = "2", quantity = 0),
        Payload(sku = "3", quantity = 1)
      )

      val root = LineItemUpdater.updateQuantitiesOnOrder(admin, order.refNum, payload).futureValue.rightVal.result
      root.lineItems.skus.count(_.sku == "1") must be(3)
      root.lineItems.skus.count(_.sku == "2") must be(0)
      root.lineItems.skus.count(_.sku == "3") must be(1)

      val allRecords = db.run(lineItems.result).futureValue
      root.lineItems.skus.size must ===(allRecords.size)
    }
  }

  // if we've asked for more than available we will "reserve" up to available_on_hand in Skus
  "Adds line_items up to availableOnHand" in (pending)

  trait Fixture {
    val (admin) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
    } yield admin).runTxn().futureValue.rightVal
  }
}
