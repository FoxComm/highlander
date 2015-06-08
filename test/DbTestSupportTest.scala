import org.scalactic.{Bad, Good}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, FreeSpec}
import util.DbTestSupport

import scala.concurrent.ExecutionContext

class DbTestSupportTest extends FreeSpec
  with MustMatchers
  with DbTestSupport
  with ScalaFutures {

  /** Slick import is still necessary, but this saves you some typing */
  import api._

  implicit val ec: ExecutionContext = concurrent.ExecutionContext.global

  val lineItems = TableQuery[LineItems]
  val carts = TableQuery[Carts]

  def createLineItems(items: Seq[LineItem]): Unit = {
    val insert = lineItems ++= items
    db.run(insert).futureValue
  }

  "DB Test Support" - {

    "LineItemUpdater" - {

      "Adds line_items when the sku doesn't exist in cart" in {
        val cart = Cart(id = 1, accountId = None)
        val payload = Seq[LineItemsPayload](
          LineItemsPayload(skuId = 1, quantity = 3),
          LineItemsPayload(skuId = 2, quantity = 0)
        )

        LineItemUpdater(db, cart, payload).futureValue match {
          case Good(items) =>
            items.filter(_.skuId == 1).length must be(3)
            items.filter(_.skuId == 2).length must be(0)

            val allRecords = db.run(lineItems.result).futureValue

            items must be (allRecords)

          case Bad(s) => fail(s.mkString(";"))
        }
      }

      // TODO: move me to model spec
      "Updates line_items when the Sku already is in cart" in {
        val seedItems = Seq(1, 1, 1, 1, 1, 1, 2, 3, 3).map { skuId => LineItem(id = 0, cartId = 1, skuId = skuId) }
        createLineItems(seedItems)

        val cart = Cart(id = 1, accountId = None)
        val payload = Seq[LineItemsPayload](
          LineItemsPayload(skuId = 1, quantity = 3),
          LineItemsPayload(skuId = 2, quantity = 0),
          LineItemsPayload(skuId = 3, quantity = 1)
        )

        LineItemUpdater(db, cart, payload).futureValue match {
          case Good(items) =>
            items.filter(_.skuId == 1).length must be(3)
            items.filter(_.skuId == 2).length must be(0)
            items.filter(_.skuId == 3).length must be(1)

            val allRecords = db.run(lineItems.result).futureValue

            items must be (allRecords)

          case Bad(s) => fail(s.mkString(";"))
        }
      }
    }

    "allows access to the data base" in {
      val findById = carts.findBy(_.id)
      val insert = carts.returning(carts.map(_.id)) += Cart(0, Some(42))
      val insertedID = db.run(insert).futureValue

      val found = db.run(findById(insertedID).result).futureValue
      found.head.accountId must contain (42)
    }
  }
}
