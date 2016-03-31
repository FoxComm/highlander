package models.product

import models.inventory._
import models.objects._
import models.Aliases.Json
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money.Currency

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonAST.{JValue, JString, JObject, JField, JNothing}

import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import cats.implicits._
import java.time.Instant
import java.security.MessageDigest

object SimpleContext { 
  val name =  "default"
  val variant = name
  val id = 1
  def create: ObjectContext = 
    ObjectContext(
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
  code: String) {

    val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "title" : "$title",
      "description" : "$description",
      "images" : ["$image"],
      "variants" : {},
      "skus" : {"$code" : {}}
    }"""))

    def create : ObjectForm = ObjectForm(kind = Product.kind, attributes = form)
}

final case class SimpleProductShadow(p: SimpleProduct) { 

    val shadow = ObjectUtils.newShadow(parse(
      s"""
        {
          "title" : {"type": "string", "ref": "title"},
          "description" : {"type": "richText", "ref": "description"},
          "images" : {"type": "images", "ref": "images"},
          "variants" : {"type": "variants", "ref": "variants"},
          "skus" : {"type": "skus", "ref": "skus"}
        }"""), 
      p.keyMap)

    def create : ObjectShadow = 
      ObjectShadow(attributes = shadow)
}

final case class SimpleSku(code: String, title: String, 
  price: Int, currency: Currency) {

    val (keyMap, form) =  ObjectUtils.createForm(parse(
      s""" 
      {
        "title" : "$title",
        "retailPrice" : {
          "value" : ${price + 500},
          "currency" : "${currency.getCode}" 
        },
        "salePrice" : {
          "value" : $price,
          "currency" : "${currency.getCode}" 
        }
      } """))

    def create : ObjectForm = 
      ObjectForm(kind = Sku.kind, attributes = form) 
}

final case class SimpleSkuShadow(s: SimpleSku) { 

    val shadow = ObjectUtils.newShadow(parse(
      s"""
        {
          "title" : {"type": "string", "ref": "title"},
          "retailPrice" : {"type": "price", "ref": "retailPrice"},
          "salePrice" : {"type": "price", "ref": "salePrice"}
        }"""), 
      s.keyMap) 

    def create : ObjectShadow = 
      ObjectShadow(attributes = shadow)
}

final case class SimpleProductData(productId: Int = 0, skuId: Int = 0, title: String, 
  description: String, image: String = SimpleProductDefaults.imageUrl, code: String, 
  price: Int, currency: Currency = Currency.USD)

final case class SimpleProductTuple(product: Product, sku: Sku, 
  productForm: ObjectForm, skuForm: ObjectForm, productShadow: ObjectShadow, 
  skuShadow: ObjectShadow)

object Mvp { 

  def insertProduct(contextId: Int, p: SimpleProductData)(implicit db: Database): 
  DbResultT[SimpleProductData] = for {
    simpleProduct   ← * <~ SimpleProduct(p.title, p.description, p.image, p.code)
    simpleShadow    ← * <~ SimpleProductShadow(simpleProduct)
    productIns ← * <~ ObjectUtils.insert(simpleProduct.create, simpleShadow.create)
    product   ← * <~ Products.create(
      Product(contextId = contextId, formId = productIns.form.id, 
        shadowId = productIns.shadow.id, commitId = productIns.commit.id))
    simpleSku       ← * <~ SimpleSku(p.code, p.title, p.price, p.currency)
    simpleSkuShadow ← * <~ SimpleSkuShadow(simpleSku)
    skuIns ← * <~ ObjectUtils.insert(simpleSku.create, simpleSkuShadow.create)
    link   ← * <~ ObjectLinks.create(ObjectLink(leftId = productIns.shadow.id, 
      rightId = skuIns.shadow.id))
    sku   ← * <~ Skus.create(Sku(contextId = contextId, code = p.code, 
      formId = skuIns.form.id, shadowId = skuIns.shadow.id, commitId = skuIns.commit.id))
  } yield p.copy(productId = product.id, skuId = sku.id)

  def getPrice(product: SimpleProductData)(implicit db: Database): DbResultT[Int] = for {
    sku       ← * <~ Skus.mustFindById404(product.skuId)
    form      ← * <~ ObjectForms.mustFindById404(sku.formId)
    shadow    ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    p         ← * <~ priceAsInt(form, shadow)
  } yield p

  def getProductTuple(d: SimpleProductData)(implicit db: Database): DbResultT[SimpleProductTuple] = for {
      product       ← * <~ Products.mustFindById404(d.productId)
      productForm   ← * <~ ObjectForms.mustFindById404(product.formId)
      productShadow ← * <~ ObjectShadows.mustFindById404(product.shadowId)
      sku           ← * <~ Skus.mustFindById404(d.skuId)
      skuForm       ← * <~ ObjectForms.mustFindById404(sku.formId)
      skuShadow     ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    } yield SimpleProductTuple(product, sku, productForm, skuForm, productShadow, skuShadow)

  def insertProducts(ps : Seq[SimpleProductData], contextId: Int)
    (implicit db: Database): DbResultT[Seq[SimpleProductData]] = for {
    results ← * <~ DbResultT.sequence(ps.map { p ⇒ insertProduct(contextId, p) } )
  } yield results

  def priceFromJson(p: JValue) : Option[(Int, Currency)] = {
    val price = for {
      JInt(value) ← p \ "value"
      JString(currency) ← p \ "currency"
    } yield (value.toInt, Currency(currency))
    if (price.isEmpty) None else price.headOption
  }

  def price(f: ObjectForm, s: ObjectShadow) : Option[(Int, Currency)] = {
    ObjectUtils.get("salePrice", f, s) match {
      case JNothing ⇒ None
      case v ⇒  priceFromJson(v)
    }
  }

  def priceAsInt(f: ObjectForm, s: ObjectShadow) : Int = 
    price(f, s).getOrElse((0, Currency.USD))._1

  def name(f: ObjectForm, s: ObjectShadow) : Option[String] = {
    ObjectUtils.get("title", f, s) match {
      case JString(title) ⇒ title.some
      case _ ⇒ None
    }
  }
}
