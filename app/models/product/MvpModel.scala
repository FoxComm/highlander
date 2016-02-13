package models.product

import Aliases.Json
import utils.Money.Currency
import utils.DbResultT._
import utils.DbResultT.implicits._

import org.json4s._
import org.json4s.jackson.JsonMethods._

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
  skuType: Sku.Type) {

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
  isActive: Boolean = true)

object Mvp { 

  def priceFromJson(p: JValue) : Option[(Int, Currency)] = {
    val price = for {
      JInt(value) ← p \ "value"
      JString(currency) ← p \ "currency"
    } yield (value.toInt, Currency(currency))
    if (price.isEmpty) None else Some(price.head)
  }

  def price(s: Sku, ss: SkuShadow) : Option[(Int, Currency)] = {
    ss.attributes \ "price" match {
      case JString(key) ⇒  priceFromJson(s.attributes \ "price" \ key)
      case _ ⇒ None
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
