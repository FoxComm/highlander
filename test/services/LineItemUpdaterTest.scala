package services

import models.{LineItems, LineItem, Cart}
import payloads.{UpdateLineItemsPayload => Payload}

import org.scalactic.{Good, Bad, ErrorMessage, Or}
import org.scalatest.{BeforeAndAfter, FreeSpec, MustMatchers, FunSuite}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

class LineItemUpdaterTest extends FreeSpec
  with util.DbTestSupport
  with MustMatchers
  with ScalaFutures
  with BeforeAndAfter
  with IntegrationPatience {

  import api._
  import concurrent.ExecutionContext.Implicits.global

  val lineItems = TableQuery[LineItems]

  def createLineItems(items: Seq[LineItem]): Unit = {
    val insert = lineItems ++= items
    db.run(insert).futureValue
  }

  "LineItemUpdater" - {

    "Adds line_items when the sku doesn't exist in cart" in {
      val cart = Cart(id = 1, accountId = None)
      val payload = Seq[Payload](
        Payload(skuId = 1, quantity = 3),
        Payload(skuId = 2, quantity = 0)
      )

      LineItemUpdater(cart, payload).futureValue match {
        case Good(items) =>
          items.count(_.skuId == 1) must be(3)
          items.count(_.skuId == 2) must be(0)

          val allRecords = db.run(lineItems.result).futureValue

          items must be (allRecords)

        case Bad(s) => fail(s.mkString(";"))
      }
    }

    "Updates line_items when the Sku already is in cart" in {
      val seedItems = Seq(1, 1, 1, 1, 1, 1, 2, 3, 3).map { skuId => LineItem(id = 0, cartId = 1, skuId = skuId) }
      createLineItems(seedItems)

      val cart = Cart(id = 1, accountId = None)
      val payload = Seq[Payload](
        Payload(skuId = 1, quantity = 3),
        Payload(skuId = 2, quantity = 0),
        Payload(skuId = 3, quantity = 1)
      )

      LineItemUpdater(cart, payload).futureValue match {
        case Good(items) =>
          items.count(_.skuId == 1) must be(3)
          items.count(_.skuId == 2) must be(0)
          items.count(_.skuId == 3) must be(1)

          val allRecords = db.run(lineItems.result).futureValue

          items must be (allRecords)

        case Bad(s) => fail(s.mkString(";"))
      }
    }
  }
}
