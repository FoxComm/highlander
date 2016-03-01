package models.product

import models.inventory.{Skus, Sku, SkuShadow, SkuShadows}

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
  val name =  "default"
  val variant = name
  val id = 1
  def create: ProductContext = 
    ProductContext(
      id = id,
      name = SimpleContext.variant,
      attributes = parse(s"""
      {
        "modality" : "desktop",
        "language" : "EN"
      }
      """))
}

object SimpleProductDefaults {
  val imageUrl = "http://lorempixel.com/75/75/fashion/"
}

final case class SimpleProduct(title: String, description: String, image: String,
  code: String, isActive: Boolean) {

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
          "${SimpleContext.variant}" : "$code"
        }"""),
        isActive = isActive)
}

final case class SimpleProductShadow(productContextId: Int, productId: Int) { 

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

final case class SimpleSku(productId: Int, code: String, title: String, price: Int,
  currency: Currency, isActive: Boolean, isHazardous: Boolean) {

    def create : Sku = 
      Sku(
        code = code,
        productId = productId,
        attributes = parse(s"""
        {
          "title" : {
            "type" : "string",
            "${SimpleContext.variant}" : "$title"
          },
          "price" : {
            "type" : "price",
            "${SimpleContext.variant}" : {
              "value" : $price,
              "currency" : "${currency.getCode}"
            }
          }
        }"""),
      isHazardous = isHazardous,
      isActive = isActive)
}

final case class SimpleSkuShadow(productContextId: Int, skuId: Int) { 

    def create : SkuShadow = 
      SkuShadow(
        productContextId = productContextId,
        skuId = skuId,
        attributes = parse(s"""
        {
          "title" : "${SimpleContext.variant}",
          "price" : "${SimpleContext.variant}"
        }""")) }

final case class SimpleProductData(productId : Int = 0, productShadowId: Int = 0,
  skuId: Int = 0, skuShadowId: Int = 0, title: String, description: String,
  image: String = SimpleProductDefaults.imageUrl, code: String, price: Int,
  currency: Currency = Currency.USD, isActive: Boolean = true, isHazardous: Boolean = false)

final case class SimpleProductTuple(product: Product, productShadow: ProductShadow,
  sku: Sku, skuShadow: SkuShadow)

object Mvp { 

  def insertProduct(contextId: Int, p: SimpleProductData)(implicit db: Database): 
  DbResultT[SimpleProductData] = for {
    simpleProduct ← * <~ SimpleProduct(p.title, p.description, p.image, p.code, p.isActive)
    product ← * <~ Products.create(simpleProduct.create)
    simpleShadow ← * <~ SimpleProductShadow(contextId, product.id)
    productShadow ← * <~ ProductShadows.create(simpleShadow.create)
    simpleSku ← * <~ SimpleSku(product.id, p.code, p.title, p.price, p.currency, p.isActive, p.isHazardous)
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

  def getProductTuple(d: SimpleProductData)(implicit db: Database): 
    DbResultT[SimpleProductTuple] = for {
      product ← * <~ Products.mustFindById404(d.productId)
      productShadow ← * <~ ProductShadows.mustFindById404(d.productShadowId)
      sku ← * <~ Skus.mustFindById404(d.skuId)
      skuShadow ← * <~ SkuShadows.mustFindById404(d.skuShadowId)
    } yield (SimpleProductTuple(product, productShadow, sku, skuShadow))

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
  def priceAsInt(s: Sku, ss: SkuShadow) : Int = 
    price(s, ss).getOrElse((0, Currency.USD))._1

  def updatePrice(s: Sku, ss: SkuShadow, price: Int) : Json = {
    ss.attributes \ "price" match {
      case JString(key) ⇒  
        s.attributes merge parse(
        s"""
        {
          "price" : {
            "type" : "price",
            "${key}" : {
              "value" : $price
            }
          }
        }
        """)
      case _ ⇒  s.attributes
    }
  }

  def nameFromJson(form: Json, shadow: Json) : Option[String] = {
    shadow \ "title" match {
      case JString(key) ⇒  form \ "title" \ key match { 
        case JString(name) ⇒ Some(name)
        case _ ⇒ None
      }
      case _ ⇒ None
    }
  }

  def name(p: Product, ps: ProductShadow) : Option[String] = {
    nameFromJson(p.attributes, ps.attributes)
  }

  def name(s: Sku, ss: SkuShadow) : Option[String] = {
    nameFromJson(s.attributes, ss.attributes)
  }
}
