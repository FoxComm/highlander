package utils.seeds

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
    SimpleProductData(code = "SKU-YAX", title = "Flonkey", description = "Best in Class Flonkey", price = 3300),
    SimpleProductData(code = "SKU-BRO", title = "Bronkey", description = "Bronze Bronkey", price = 15300),
    SimpleProductData(code = "SKU-ABC", title = "Shark", description = "Dangerious Shark Pets", price = 4500),
    SimpleProductData(code = "SKU-SHH", title = "Sharkling", description = "Smaller Shark", price = 1500),
    SimpleProductData(code = "SKU-ZYA", title = "Dolphin", description = "A Dog named Dolphin", price = 8800),
    SimpleProductData(code = "SKU-MRP", title = "Morphin", description = "Power Ranger", price = 7700),
    // Why beetle? Cuz it's probably a bug. FIXME: add validation!!!
    SimpleProductData(code = "SKU-TRL", title = "Beetle", description = "Music Album", price = -100, isActive = false))
}
