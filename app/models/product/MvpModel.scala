package models.product

import models.inventory._
import models.objects._
import models.Aliases.Json
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money.Currency
import utils.Slick.implicits._
import failures.ProductFailures._
import failures.ObjectFailures._

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonAST.{JValue, JString, JObject, JField, JNothing}

import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import cats.implicits._
import java.time.Instant
import java.security.MessageDigest

object SimpleContext { 
  val id = 1
  val ruId = 2
  val default = "default"
  val ru = "ru"

  def create(id: Int = 0, name: String = "default", lang: String = "en", modality: String = "desktop"): 
  ObjectContext = 
    ObjectContext(
      id = id,
      name = name,
      attributes = parse(s"""
      {
        "modality" : "$modality",
        "lang" : "$lang"
      }
      """))
}

object SimpleProductDefaults {

  val imageUrl = "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/no_image.jpg"
}

final case class SimpleProduct(title: String, description: String, image: String,
  code: String, active: Boolean = false) {
    val activeFrom = if(active) s""""${Instant.now}"""" else "null";

    val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "title" : "$title",
      "description" : "$description",
      "images" : ["$image"],
      "variants" : {},
      "skus" : {"$code" : {}},
      "activeFrom" : $activeFrom
    }"""))

    def create : ObjectForm = ObjectForm(kind = Product.kind, attributes = form)
    def update(oldForm: ObjectForm) : ObjectForm = 
      oldForm.copy(attributes = oldForm.attributes merge form)
}

final case class SimpleProductShadow(p: SimpleProduct) { 

    val shadow = ObjectUtils.newShadow(parse(
      s"""
        {
          "title" : {"type": "string", "ref": "title"},
          "description" : {"type": "richText", "ref": "description"},
          "images" : {"type": "images", "ref": "images"},
          "variants" : {"type": "variants", "ref": "variants"},
          "skus" : {"type": "skus", "ref": "skus"},
          "activeFrom" : {"type": "date", "ref": "activeFrom"}
        }"""), 
      p.keyMap)

    def create : ObjectShadow = 
      ObjectShadow(attributes = shadow)
}

final case class SimpleSku(code: String, title: String, image: String, 
  price: Int, currency: Currency, active: Boolean = false) {

    val activeFrom = if(active) s""""${Instant.now}"""" else "null";

    val (keyMap, form) =  ObjectUtils.createForm(parse(
      s""" 
      {
        "title" : "$title",
        "images" : ["$image"],
        "retailPrice" : {
          "value" : ${price + 500},
          "currency" : "${currency.getCode}" 
        },
        "salePrice" : {
          "value" : $price,
          "currency" : "${currency.getCode}" 
        },
        "activeFrom" : $activeFrom
      } """))

    def create : ObjectForm = 
      ObjectForm(kind = Sku.kind, attributes = form) 
    def update(oldForm: ObjectForm) : ObjectForm = 
      oldForm.copy(attributes = oldForm.attributes merge form)
}

final case class SimpleSkuShadow(s: SimpleSku) { 

    val shadow = ObjectUtils.newShadow(parse(
      s"""
        {
          "title" : {"type": "string", "ref": "title"},
          "images" : {"type": "images", "ref": "images"},
          "retailPrice" : {"type": "price", "ref": "retailPrice"},
          "salePrice" : {"type": "price", "ref": "salePrice"},
          "activeFrom" : {"type": "date", "ref": "activeFrom"}
        }"""), 
      s.keyMap) 

    def create : ObjectShadow = 
      ObjectShadow(attributes = shadow)
}

final case class SimpleProductData(productId: Int = 0, skuId: Int = 0, title: String, 
  description: String, image: String = SimpleProductDefaults.imageUrl, code: String, 
  price: Int, currency: Currency = Currency.USD, active: Boolean = false)

final case class SimpleProductTuple(product: Product, sku: Sku, 
  productForm: ObjectForm, skuForm: ObjectForm, productShadow: ObjectShadow, 
  skuShadow: ObjectShadow)

object Mvp { 
  def insertProductNewContext(oldContextId: Int, contextId: Int, p: SimpleProductData)(implicit db: Database): 
  DbResultT[SimpleProductData] = for {
    simpleProduct   ← * <~ SimpleProduct(p.title, p.description, p.image, p.code, p.active)
    //find product form other context, get old form and merge with new
    product       ← * <~ Products.filter(_.contextId === oldContextId).filter(_.id === p.productId).one.
        mustFindOr(ProductNotFoundForContext(p.productId, oldContextId)) 
    oldForm         ← * <~ ObjectForms.mustFindById404(product.formId)
    productForm     ← * <~ ObjectForms.update(oldForm, simpleProduct.update(oldForm))

    //find sku form for the product and update it with new sku
    link ← * <~ ObjectLinks.filter(_.leftId === product.shadowId).one.
      mustFindOr(ObjectLeftLinkCannotBeFound(product.shadowId))
    sku ← * <~ Skus.filter(_.contextId === oldContextId).
      filter(_.shadowId === link.rightId).one.
        mustFindOr(SkuWithShadowNotFound(link.rightId))

    simpleSku  ← * <~ SimpleSku(p.code, p.title, p.image, p.price, p.currency, p.active)
    oldSkuForm ← * <~ ObjectForms.mustFindById404(sku.formId)
    skuForm    ← * <~ ObjectForms.update(oldSkuForm, simpleSku.update(oldSkuForm))

    r ← * <~ insertProductIntoContext(contextId, productForm, skuForm,
      simpleProduct, simpleSku, p)

  } yield r

  def insertProduct(contextId: Int, p: SimpleProductData)(implicit db: Database): 
  DbResultT[SimpleProductData] = for {
    simpleProduct   ← * <~ SimpleProduct(p.title, p.description, p.image, p.code, p.active)
    productForm     ← * <~ ObjectForms.create(simpleProduct.create)
    simpleSku       ← * <~ SimpleSku(p.code, p.title, p.image, p.price, p.currency, p.active)
    skuForm             ← * <~ ObjectForms.create(simpleSku.create)
    r ← * <~ insertProductIntoContext(contextId, productForm, skuForm,
      simpleProduct, simpleSku, p)
  } yield r

  def insertProductIntoContext(contextId: Int, productForm: ObjectForm, 
    skuForm: ObjectForm, simpleProduct: SimpleProduct, simpleSku: SimpleSku, 
    p: SimpleProductData)
  (implicit db: Database): DbResultT[SimpleProductData] = for {

    simpleShadow    ← * <~ SimpleProductShadow(simpleProduct)
    productShadow   ← * <~ ObjectShadows.create(simpleShadow.create.
        copy(formId = productForm.id))

    productCommit   ← * <~ ObjectCommits.create(
      ObjectCommit(formId = productForm.id, shadowId = productShadow.id))

    product   ← * <~ Products.create(
      Product(contextId = contextId, formId = productForm.id, 
        shadowId = productShadow.id, commitId = productCommit.id))

    simpleSkuShadow ← * <~ SimpleSkuShadow(simpleSku)
    skuShadow       ← * <~ ObjectShadows.create(simpleSkuShadow.create.
        copy(formId = skuForm.id))

    link            ← * <~ ObjectLinks.create(ObjectLink(
      leftId = productShadow.id, rightId = skuShadow.id))

    skuCommit       ← * <~ ObjectCommits.create(
      ObjectCommit(formId = skuForm.id, shadowId = skuShadow.id))

    sku   ← * <~ Skus.create(Sku(contextId = contextId, code = p.code, 
      formId = skuForm.id, shadowId = skuShadow.id, commitId = 
          skuCommit.id))

  } yield p.copy(productId = product.id, skuId = sku.id)

  def getPrice(skuId: Int)(implicit db: Database): DbResultT[Int] = for {
    sku       ← * <~ Skus.mustFindById404(skuId)
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

  def insertProductsNewContext(oldContextId: Int, contextId: Int, ps : Seq[SimpleProductData])
    (implicit db: Database): DbResultT[Seq[SimpleProductData]] = for {
    results ← * <~ DbResultT.sequence(ps.map { p ⇒ insertProductNewContext(oldContextId, contextId, p) } )
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

  def firstImage(f: ObjectForm, s: ObjectShadow) : Option[String] = {
    ObjectUtils.get("images", f, s) match {
      case JArray(images) ⇒ images.headOption.flatMap { 
        case JString(image) ⇒ image.some
        case _ ⇒ None
      }
      case _ ⇒ None
    }
  }
}
