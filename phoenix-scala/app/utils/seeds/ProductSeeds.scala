package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.objects.ObjectContexts
import models.product.{Mvp, SimpleContext, SimpleProductData}
import utils.Money.Currency
import utils.aliases._
import utils.db._

trait ProductSeeds extends {

  type SeedProducts = (SimpleProductData,
                       SimpleProductData,
                       SimpleProductData,
                       SimpleProductData,
                       SimpleProductData,
                       SimpleProductData,
                       SimpleProductData)

  def createProducts(implicit db: DB, au: AU): DbResultT[SeedProducts] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      ps      ← * <~ Mvp.insertProducts(products, context.id)
      (p1 :: p2 :: s3 :: p4 :: p5 :: p6 :: p7 :: Nil) = ps
    } yield (p1, p2, s3, p4, p5, p6, p7)

  def products: Seq[SimpleProductData] =
    Seq(
        SimpleProductData(
            skuCode = "SKU-YAX",
            title = "Donkey",
            description = "A styled fit for the donkey life.",
            price =
              3300,
            image =
              "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Granger_Three_Quarter.jpg",
            active = true,
            tags =
              Seq("eyeglasses", "readers")),
        SimpleProductData(skuCode = "SKU-BRO",
                          title =
                            "Shark",
                          description =
                            "Sharks come with balanced framing.",
                          price = 15300,
                          image =
                            "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Festival_Three_Quarter.jpg",
                          active = true,
                          tags = Seq("sunglasses", "readers")),
        SimpleProductData(skuCode =
                            "SKU-ABC",
                          title = "Sharkling",
                          description =
                            "Designed for the beach.",
                          price = 4500,
                          image =
                            "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Gold_Blue_Three_Quarter.jpg",
                          active = true,
                          tags = Seq("sunglasses")),
        SimpleProductData(
            skuCode =
              "SKU-SHH",
            title =
              "Duck",
            description = "The yellow trim accentuates deep pond style.",
            price = 1500,
            image = "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Emma_Top_Front.jpg",
            active =
              true,
            tags = Seq("sunglasses")),
        SimpleProductData(
            skuCode = "SKU-ZYA",
            title = "Duckling",
            description =
              "A fit for a smaller face.",
            price = 8800,
            image =
              "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg",
            active = true,
            tags = Seq("sunglasses", "readers")),
        SimpleProductData(skuCode = "SKU-MRP",
                          title = "Chicken",
                          description = "Cross the road in these with confidence.",
                          price = 7700,
                          image =
                            "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Round_Readers_Top_Front.jpg",
                          active = true,
                          tags = Seq("eyeglasses")),
        SimpleProductData(
            skuCode = "SKU-TRL",
            title = "Fox",
            description = "Stylish fit, stylish finish.",
            price = 10000,
            image =
              "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
            active = true,
            tags = Seq("sunglasses")))

  def createRuProducts(products: SeedProducts)(implicit db: DB, au: AU): DbResultT[SeedProducts] = {
    val p1 = products._1.copy(title = "осел",
                              description = "Стилизированный, пригодный для жизни осла.",
                              price = 3300,
                              currency = Currency.RUB)
    val p2 = products._2.copy(title = "Акула",
                              description = "Акулы приходят со сбалансированным обрамления.",
                              price = 15300,
                              currency = Currency.RUB)
    val p3 = products._3.copy(title = "Малые Акулы",
                              description = "Предназначен для пляжа.",
                              price = 4500,
                              currency = Currency.RUB)
    val p4 = products._4.copy(title = "Утка",
                              description = "Желтый отделка подчеркивает глубокий пруд стиль.",
                              price = 1500,
                              currency = Currency.RUB)
    val p5 = products._5.copy(title = "Утенок",
                              description = "Подходят для меньшего лица.",
                              price = 8800,
                              currency = Currency.RUB)
    val p6 = products._6.copy(title = "Курица",
                              description = "Перейдя дорогу в них с уверенностью.",
                              price = 7700,
                              currency = Currency.RUB)
    val p7 = products._7.copy(title = "Лиса",
                              description = "Стильный подходит, стильный отделка.",
                              price = 10000,
                              currency = Currency.RUB)

    val ruProducts = Seq(p1, p2, p3, p4, p5, p6, p7)

    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.ruId)
      ps      ← * <~ Mvp.insertProductsNewContext(SimpleContext.id, SimpleContext.ruId, ruProducts)
      (p1 :: p2 :: s3 :: p4 :: p5 :: p6 :: p7 :: Nil) = ps
    } yield (p1, p2, s3, p4, p5, p6, p7)
  }
}
