package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global

import scala.io.Source
import scala.util.Random

import faker._
import models.objects.ObjectContexts
import models.product.{Mvp, SimpleContext, SimpleProductData}
import org.conbere.markov.MarkovChain
import utils.aliases._
import utils.db._

object ProductGenerator {

  val sampleImages = Seq(
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Emma_Detail.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Emma_Side.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Emma_Three_Quarter.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Emma_Top_Front.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Festival_Detail.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Festival_Side.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Festival_Three_Quarter.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Festival_Top_Front.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Gold_Blue_Back.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Gold_Blue_Front.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Gold_Blue_Three_Quarter_Back.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Gold_Blue_Three_Quarter.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Gold_Blue_Three_Quarter_Top.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Granger_Three_Back.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Granger_Three_Front.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Granger_Three_Quarter.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/index.html",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Detail.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Side.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Three_Quarter.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Detail.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Three_Quarter.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Top_Front.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Round_Readers_Detail.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Round_Readers_Side.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Round_Readers_Three_Quarter.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Round_Readers_Top_Front.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Flex_Metal_Back.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Flex_Metal_Front.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Flex_Metal_Three_Quarter.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Flex_Metal_Three_Quarter_Top.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Off_World_Back.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Off_World_Front.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Off_World_Three_Quarter.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Off_World_Three_Quarter_Top.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Post_Punk_Back.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Post_Punk_Front.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Post_Punk_Three_Quarter_Back.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Post_Punk_Three_Quarter.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Post_Punk_Three_Quarter_Top.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Technotronics_Back.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Technotronics_Front.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Technotronics_Three_Quarters.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Spitfire_Technotronics_Three_Quarters_Top.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Vivien_Back.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Vivien_Front.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Vivien_Three_Quarter_Back.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Vivien_Three_Quarter.jpg",
      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Vivien_Three_Quarter_Top.jpg")

  def randomImage = sampleImages(Random.nextInt(sampleImages.length))
}

trait ProductGenerator {

  val stop  = "\u0002"
  val start = "\u0003"
  val nameGenerator = Source
    .fromURL(getClass.getResource("/product_titles.txt"), "UTF-8")
    .getLines
    .map(_.grouped(2)) //group characters in line into sets of 2
    .foldLeft(new MarkovChain[String](start, stop))((acc, wordChunks) ⇒
          acc.insert(wordChunks.map(_.toLowerCase).toList))

  def generateProduct: SimpleProductData = {
    val base  = new Base {}
    val code  = base.letterify("???-???")
    val title = nameGenerator.generate(Math.max(5, Random.nextInt(20))).mkString("")
    Console.err.println(s"product: ${code} ${title}")
    SimpleProductData(code = code,
                      title = title,
                      description = title,
                      price = Random.nextInt(10000),
                      image = ProductGenerator.randomImage)
  }

  def generateProducts(data: Seq[SimpleProductData])(implicit db: DB, au: AU) =
    for {
      context  ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      products ← * <~ Mvp.insertProducts(data, context.id)
    } yield products
}
