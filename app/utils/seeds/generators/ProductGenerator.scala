package utils.seeds.generators


import GeneratorUtils.randomString
import models.inventory.{InventorySummary, InventorySummaries, Warehouse, Warehouses}
import models.product.{SimpleProductData, Mvp, ProductContexts, SimpleContext}
import scala.util.Random
import utils.DbResultT._
import utils.DbResultT.implicits._

import faker._;
import java.time.{Instant, ZoneId}
import org.conbere.markov.MarkovChain
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import slick.driver.PostgresDriver.api._

trait ProductGenerator {

  val stop = "\u0002"
  val start = "\u0003"
  def nameGenerator = Source.fromURL(getClass.getResource("/product_titles.txt"), "UTF-8").getLines
    .map(_.grouped(2)) //group characters in line into sets of 2
    .foldLeft(new MarkovChain[String](start, stop))((acc, wordChunks) => 
        acc.insert(wordChunks.map(_.toLowerCase).toList))

  def generateProduct: SimpleProductData = {
    val base = new Base{}
    val sku = base.letterify("???-???") 
    val title = nameGenerator.generate(Math.max(5, Random.nextInt(20))).mkString("")
    Console.err.println(s"product: ${sku} ${title}")
    SimpleProductData(
      sku = sku, 
      title = title,
      description = title,
      price = Random.nextInt(10000))
  }

  def generateProducts(data: Seq[SimpleProductData])(implicit db: Database) = for {
    context ← * <~ ProductContexts.create(SimpleContext.create)
    products ← * <~ Mvp.insertProducts(data, context.id)
  } yield products

}
