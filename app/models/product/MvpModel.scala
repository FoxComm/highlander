package models.product

import Aliases.Json
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money.Currency

import org.json4s._
import org.json4s.jackson.JsonMethods._

import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

object SimpleContext { 
  val variant =  "default"
  def create: ProductContext = 
    ProductContext(
      name = SimpleContext.variant,
      context = parse(s"""
      {
        "modality" : "desktop",
        "language" : "EN"
      }
      """))
}

object SimpleProductDefaults {
  val imageUrl = "http://lorempixel.com/75/75/fashion/"
}

final case class SimpleProduct(
  title: String,
  description: String,
  image: String,
  sku: String,
  isActive: Boolean) {

    def create : Product = 
      Product(
        attributes = parse(s"""
        {
          "title" : {
            "type" : "string",
            "${SimpleContext.variant}" : "$title"
          },
          "description" : {
            "type" : "string",
            "${SimpleContext.variant}" : "$description"
          },
          "images" : {
            "type" : "images",
            "${SimpleContext.variant}" : ["$image"]
          }
        }"""),
        variants = parse(s"""
        {
          "${SimpleContext.variant}" : "$sku"
        }"""),
        isActive = isActive)
}

final case class SimpleProductShadow(
  productContextId: Int,
  productId: Int) { 

    def create : ProductShadow = 
      ProductShadow(
        productContextId = productContextId,
        productId = productId,
        attributes = parse(s"""
        {
          "title" : "${SimpleContext.variant}",
          "description" : "${SimpleContext.variant}",
          "images" : "${SimpleContext.variant}"
        }"""))
}

final case class SimpleSku(
  productId: Int,
  sku: String,
  price: Int,
  currency: Currency,
  skuType: Sku.Type,
  isHazardous: Boolean) {

    def create : Sku = 
      Sku(
        sku = sku,
        productId = productId,
        attributes = parse(s"""
        {
          "price" : {
            "type" : "price",
            "${SimpleContext.variant}" : {
              "value" : $price,
              "currency" : "${currency.getCode}"
            }
          }
        }"""),
      isHazardous = isHazardous,
      `type` = skuType)
}

final case class SimpleSkuShadow(
  productContextId: Int,
  skuId: Int) { 

    def create : SkuShadow = 
      SkuShadow(
        productContextId = productContextId,
        skuId = skuId,
        attributes = parse(s"""
        {
          "price" : "${SimpleContext.variant}"
        }""")) }

final case class SimpleProductData(
  productId : Int = 0,
  productShadowId: Int = 0,
  skuId: Int = 0,
  skuShadowId: Int = 0,
  title: String,
  description: String,
  image: String = SimpleProductDefaults.imageUrl,
  sku: String,
  skuType: Sku.Type = Sku.Sellable,
  price: Int,
  currency: Currency = Currency.USD,
  isActive: Boolean = true,
  isHazardous: Boolean = false)

object Mvp { 

  def insertProduct(contextId: Int, p: SimpleProductData)(implicit db: Database): 
  DbResultT[SimpleProductData] = for {
    simpleProduct ← * <~ SimpleProduct(p.title, p.description, p.image, p.sku, p.isActive)
    product ← * <~ Products.create(simpleProduct.create)
    simpleShadow ← * <~ SimpleProductShadow(contextId, product.id)
    productShadow ← * <~ ProductShadows.create(simpleShadow.create)
    simpleSku ← * <~ SimpleSku(product.id, p.sku, p.price, p.currency, p.skuType, p.isHazardous)
    sku ← * <~ Skus.create(simpleSku.create)
    simpleSkuShadow ← * <~ SimpleSkuShadow(contextId, sku.id)
    skuShadow ← * <~ SkuShadows.create(simpleSkuShadow.create)
  } yield p.copy(
    productId = product.id,
    productShadowId = productShadow.id,
    skuId = sku.id,
    skuShadowId = skuShadow.id)

  def getPrice(product: SimpleProductData)(implicit db: Database): 
  DbResultT[Int] = for {
    sku ← * <~ Skus.mustFindById404(product.skuId)
    skuShadow ← * <~ SkuShadows.mustFindById404(product.skuShadowId)
    p ← * <~ price(sku, skuShadow).getOrElse((0, Currency.USD))
  } yield p._1

  def insertProducts(ps : Seq[SimpleProductData], contextId: Int)(implicit db: Database) : 
  DbResultT[Seq[SimpleProductData]] = for {
    results ← * <~ DbResultT.sequence(ps.map { p ⇒ insertProduct(contextId, p) } )
  } yield results

  def priceFromJson(p: JValue) : Option[(Int, Currency)] = {
    val price = for {
      JInt(value) ← p \ "value"
      JString(currency) ← p \ "currency"
    } yield (value.toInt, Currency(currency))
    if (price.isEmpty) None else price.headOption
  }

  def price(s: Sku, ss: SkuShadow) : Option[(Int, Currency)] = {
    ss.attributes \ "price" match {
      case JString(key) ⇒  priceFromJson(s.attributes \ "price" \ key)
      case _ ⇒ None
    }
  }

  def updatePrice(s: Sku, ss: SkuShadow, price: Int) : Json = {
    ss.attributes \ "price" match {
      case JString(key) ⇒  
        s.attributes merge parse(
        s"""
        {
          "price" : {
            "type" : "price",
            "${key}" : {
              "value" : $price,
            }
          }
        }
        """)
      case _ ⇒  s.attributes
    }
  }

  def name(p: Product, ps: ProductShadow) : Option[String] = {
    ps.attributes \ "name" match {
      case JString(key) ⇒  p.attributes \ "name" \ key match { 
        case JString(name) ⇒ Some(name)
        case _ ⇒ None
      }
      case _ ⇒ None
    }
  }
}
