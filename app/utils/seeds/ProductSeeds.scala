package utils.seeds

import utils.seeds.generators.ProductGenerator
import models.product.{SimpleContext, SimpleProductData, Mvp}
import models.objects.ObjectContexts
import utils.DbResultT._
import utils.DbResultT.implicits._

import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

trait ProductSeeds extends  {

  type SeedProducts = (SimpleProductData, SimpleProductData, SimpleProductData, 
    SimpleProductData, SimpleProductData, SimpleProductData, SimpleProductData, SimpleProductData)

  def createProducts(implicit db: Database) : DbResultT[SeedProducts] = for {
    context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
    ps ← * <~ Mvp.insertProducts(products, context.id)
    } yield ps match {
      case p1 :: p2 :: s3 :: p4 :: p5 :: p6 :: p7 :: p8 :: Nil ⇒ 
        ( p1, p2, s3, p4, p5, p6, p7, p8)
      case other ⇒  ???
    }

  def products: Seq[SimpleProductData] = Seq(
    SimpleProductData(code = "SKU-YAX", title = "Donkey", description = "A styled fit for the donkey life.", price = 3300, 
      image = "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Granger_Three_Quarter.jpg"),
    SimpleProductData(code = "SKU-BRO", title = "Shark", description = "Sharks come with balanced framing.", price = 15300, 
      image = "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Festival_Three_Quarter.jpg" ),
    SimpleProductData(code = "SKU-ABC", title = "Sharkling", description = "Designed for the beach.", price = 4500, 
      image =  "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Gold_Blue_Three_Quarter.jpg"),
    SimpleProductData(code = "SKU-SHH", title = "Duck", description = "The yellow trim accentuates deep pond style.", price = 1500, 
      image = "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Emma_Top_Front.jpg"),
    SimpleProductData(code = "SKU-ZYA", title = "Duckling", description = "A fit for a smaller face.", price = 8800, 
      image ="https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg"),
    SimpleProductData(code = "SKU-MRP", title = "Chicken", description = "Cross the road in these with confidence.", price = 7700, 
      image ="https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Round_Readers_Top_Front.jpg"),
    // Why beetle? Cuz it"s probably a bug. FIXME: add validation!!!
    SimpleProductData(code = "SKU-TRL", title = "Fox", description = "Stylish fit, stylish finish.", price = -100, 
      image ="https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg"),
    SimpleProductData(code = "SKU-TRL", title = "Free Fox", description = "Free stylish fox", price = 0, 
      image ="https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg"))
}
