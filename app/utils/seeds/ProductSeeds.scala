package utils.seeds

import utils.seeds.generators.ProductGenerator
import models.product.{SimpleContext, SimpleProductData, Mvp, ProductContexts}
import utils.DbResultT._
import utils.DbResultT.implicits._

import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

trait ProductSeeds extends  {

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
    SimpleProductData(code = "SKU-YAX", title = "Flonkey", description = "Best in Class Flonkey", price = 3300, image = ProductGenerator.randomImage),
    SimpleProductData(code = "SKU-BRO", title = "Bronkey", description = "Bronze Bronkey", price = 15300, image = ProductGenerator.randomImage),
    SimpleProductData(code = "SKU-ABC", title = "Shark", description = "Dangerious Shark Pets", price = 4500, image = ProductGenerator.randomImage),
    SimpleProductData(code = "SKU-SHH", title = "Sharkling", description = "Smaller Shark", price = 1500, image = ProductGenerator.randomImage),
    SimpleProductData(code = "SKU-ZYA", title = "Dolphin", description = "A Dog named Dolphin", price = 8800, image = ProductGenerator.randomImage),
    SimpleProductData(code = "SKU-MRP", title = "Morphin", description = "Power Ranger", price = 7700, image = ProductGenerator.randomImage),
    // Why beetle? Cuz it's probably a bug. FIXME: add validation!!!
    SimpleProductData(code = "SKU-TRL", title = "Beetle", description = "Music Album", price = -100, image = ProductGenerator.randomImage))
}
