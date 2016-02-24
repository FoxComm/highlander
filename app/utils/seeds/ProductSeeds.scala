package utils.seeds

import models.inventory.{Sku, InventorySummaries, InventorySummary, Warehouse,
  Warehouses} 
import models.product.{SimpleContext, SimpleProductData, Mvp, ProductContexts}
import utils.Money.Currency
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._

import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

trait ProductSeeds {

  type SeedProducts = (SimpleProductData, SimpleProductData, SimpleProductData, 
    SimpleProductData, SimpleProductData, SimpleProductData, SimpleProductData)

  def createProducts(implicit db: Database) : DbResultT[SeedProducts] = for {
    productContext ← * <~ ProductContexts.mustFindById404(SimpleContext.id)
    ps ← * <~ Mvp.insertProducts(products, productContext.id)
    } yield ps match {
      case p1 :: p2 :: s3 :: p4 :: p5 :: p6 :: p7 :: Nil ⇒ 
        ( p1, p2, s3, p4, p5, p6, p7)
      case other ⇒  ???
    }

  def products: Seq[SimpleProductData] = Seq(
    SimpleProductData(sku = "SKU-YAX", title = "Flonkey", description = "Best in Class Flonkey", price = 3300),
    SimpleProductData(sku = "SKU-BRO", title = "Bronkey", description = "Bronze Bronkey", price = 15300),
    SimpleProductData(sku = "SKU-ABC", title = "Shark", description = "Dangerious Shark Pets", price = 4500, skuType = Sku.Preorder),
    SimpleProductData(sku = "SKU-SHH", title = "Sharkling", description = "Smaller Shark", price = 1500, skuType = Sku.Preorder),
    SimpleProductData(sku = "SKU-ZYA", title = "Dolphin", description = "A Dog named Dolphin", price = 8800, skuType = Sku.Backorder),
    SimpleProductData(sku = "SKU-MRP", title = "Morphin", description = "Power Ranger", price = 7700),
    // Why beetle? Cuz it's probably a bug. FIXME: add validation!!!
    SimpleProductData(sku = "SKU-TRL", title = "Beetle", description = "Music Album", price = -100, skuType = Sku.NonSellable, isActive = false))
}
