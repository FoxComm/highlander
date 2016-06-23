package services

import scala.concurrent.ExecutionContext.Implicits.global

import models.StoreAdmins
import models.activity.ActivityContext
import models.objects._
import models.order.lineitems._
import models.order.{Order, Orders}
import models.product.{Mvp, SimpleContext, SimpleProductData}
import payloads.LineItemPayloads.{UpdateLineItemsPayload ⇒ Payload}
import util.IntegrationTestBase
import utils.db.DbResultT._
import utils.db._
import utils.seeds.Seeds.Factories

class LineItemUpdaterTest extends IntegrationTestBase {

  import api._

  implicit val activityContext = ActivityContext(userId = 1, userType = "b", transactionId = "c")
  implicit val elaticsearchApi = utils.ElasticsearchApi.default()

  val lineItems    = TableQuery[OrderLineItems]
  val lineItemSkus = TableQuery[OrderLineItemSkus]

  def createProducts(num: Int): DbResultT[(ObjectContext, Seq[SimpleProductData])] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      products ← * <~ Mvp.insertProducts((1 to num).map { i ⇒
                  Factories.products.head.copy(code = i.toString, price = 5)
                }, context.id)
    } yield (context, products)

  def createLineItems(items: Seq[OrderLineItem]): Unit = {
    OrderLineItems.createAll(items).gimme
  }

  "LineItemUpdater" - {

    "Adds line items when the sku doesn't exist in order" in new Fixture {
      val (context, products) = createProducts(2).gimme
      val order               = Orders.create(Order(customerId = 1, contextId = context.id)).gimme

      val payload = Seq[Payload](
          Payload(sku = "1", quantity = 3),
          Payload(sku = "2", quantity = 0)
      )

      val root = LineItemUpdater.updateQuantitiesOnOrder(admin, order.refNum, payload).gimme.result
      root.lineItems.skus.count(_.sku == "1") must be(3)
      root.lineItems.skus.count(_.sku == "2") must be(0)

      val allRecords = lineItems.gimme
      root.lineItems.skus.size must ===(allRecords.size)

      val allRelations = lineItemSkus.gimme
      allRelations.size must ===(2)
    }

    "Updates line items when the Sku already is in order" in new Fixture {
      val (context, products) = createProducts(3).gimme
      val order               = Orders.create(Order(customerId = 1, contextId = context.id)).gimme
      val seedItems = Seq(1, 1, 1, 1, 1, 1, 2, 3, 3).map { linkId ⇒
        OrderLineItem(id = 0,
                      orderId = 1,
                      originId = linkId,
                      originType = OrderLineItem.SkuItem)
      }
      createLineItems(seedItems)

      val payload = Seq[Payload](
          Payload(sku = "1", quantity = 3),
          Payload(sku = "2", quantity = 0),
          Payload(sku = "3", quantity = 1)
      )

      val root = LineItemUpdater.updateQuantitiesOnOrder(admin, order.refNum, payload).gimme.result
      root.lineItems.skus.count(_.sku == "1") must be(3)
      root.lineItems.skus.count(_.sku == "2") must be(0)
      root.lineItems.skus.count(_.sku == "3") must be(1)

      val allRecords = db.run(lineItems.result).futureValue
      root.lineItems.skus.size must ===(allRecords.size)
    }
  }

  trait Fixture {
    val admin = StoreAdmins.create(Factories.storeAdmin).gimme
  }
}
