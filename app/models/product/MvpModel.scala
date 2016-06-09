package models.product

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import failures.ObjectFailures._
import failures.ProductFailures._
import models.inventory._
import models.objects._
import org.json4s.JsonAST.{JNothing, JString}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import slick.driver.PostgresDriver.api._
import utils.Money.Currency
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object SimpleContext {
  val id      = 1
  val ruId    = 2
  val default = "default"
  val ru      = "ru"

  def create(id: Int = 0,
             name: String = "default",
             lang: String = "en",
             modality: String = "desktop"): ObjectContext =
    ObjectContext(id = id, name = name, attributes = ("modality" → modality) ~ ("lang" → lang))
}

object SimpleProductDefaults {

  val imageUrl =
    "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/no_image.jpg"
}

case class SimpleProduct(title: String,
                         description: String,
                         image: String,
                         code: String,
                         active: Boolean = false,
                         tags: Seq[String] = Seq.empty) {
  val activeFrom = if (active) s""""${Instant.now}"""" else "null";
  val ts: String = compact(render(JArray(tags.map(t ⇒ JString(t)).toList)))

  val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "title" : "$title",
      "description" : "$description",
      "images" : ["$image"],
      "variants" : {},
      "skus" : {"$code" : {}},
      "activeFrom" : $activeFrom,
      "tags" : $ts
    }"""))

  def create: ObjectForm = ObjectForm(kind = Product.kind, attributes = form)
  def update(oldForm: ObjectForm): ObjectForm =
    oldForm.copy(attributes = oldForm.attributes merge form)
}

case class SimpleProductShadow(p: SimpleProduct) {

  val shadow = ObjectUtils.newShadow(
      parse("""
        {
          "title" : {"type": "string", "ref": "title"},
          "description" : {"type": "richText", "ref": "description"},
          "images" : {"type": "images", "ref": "images"},
          "variants" : {"type": "variants", "ref": "variants"},
          "skus" : {"type": "skus", "ref": "skus"},
          "activeFrom" : {"type": "date", "ref": "activeFrom"},
          "tags" : {"type": "tags", "ref": "tags"}
        }"""),
      p.keyMap)

  def create: ObjectShadow =
    ObjectShadow(attributes = shadow)
}

case class SimpleSku(code: String,
                     title: String,
                     image: String,
                     price: Int,
                     currency: Currency,
                     active: Boolean = false,
                     tags: Seq[String] = Seq.empty) {

  val activeFrom = if (active) s""""${Instant.now}"""" else "null";
  val ts: String = compact(render(JArray(tags.map(t ⇒ JString(t)).toList)))

  val (keyMap, form) = ObjectUtils.createForm(parse(s"""
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
        "activeFrom" : $activeFrom,
        "tags" : $ts
      } """))

  def create: ObjectForm =
    ObjectForm(kind = Sku.kind, attributes = form)
  def update(oldForm: ObjectForm): ObjectForm =
    oldForm.copy(attributes = oldForm.attributes merge form)
}

case class SimpleSkuShadow(s: SimpleSku) {

  val shadow = ObjectUtils.newShadow(parse("""
        {
          "title" : {"type": "string", "ref": "title"},
          "images" : {"type": "images", "ref": "images"},
          "retailPrice" : {"type": "price", "ref": "retailPrice"},
          "salePrice" : {"type": "price", "ref": "salePrice"},
          "activeFrom" : {"type": "date", "ref": "activeFrom"},
          "tags" : {"type": "tags", "ref": "tags"}
        }"""),
                                     s.keyMap)

  def create: ObjectShadow =
    ObjectShadow(attributes = shadow)
}

case class SimpleVariant(name: String) {
  val (keyMap, form) = ObjectUtils.createForm(parse(s"""{ "name": "$name" }"""))

  def create: ObjectForm = ObjectForm(kind = Variant.kind, attributes = form)

  def update(oldForm: ObjectForm): ObjectForm =
    oldForm.copy(attributes = oldForm.attributes merge form)
}

case class SimpleVariantShadow(v: SimpleVariant) {
  val shadow =
    ObjectUtils.newShadow(parse("""{ "name": { "type": "string", "ref": "name" } }"""), v.keyMap)

  def create: ObjectShadow = ObjectShadow(attributes = shadow)
}

case class SimpleVariantValue(name: String, swatch: String) {
  val (keyMap, form) =
    ObjectUtils.createForm(parse(s"""{ "name": "$name", "swatch": "$swatch" }"""))

  def create: ObjectForm = ObjectForm(kind = VariantValue.kind, attributes = form)

  def update(oldForm: ObjectForm): ObjectForm =
    oldForm.copy(attributes = oldForm.attributes merge form)
}

case class SimpleVariantValueShadow(v: SimpleVariantValue) {
  val shadow = ObjectUtils.newShadow(parse("""
      {
        "name": { "type": "string", "ref": "name" },
        "swatch": { "type": "string", "ref": "swatch" }
      }
    """),
                                     v.keyMap)

  def create: ObjectShadow = ObjectShadow(attributes = shadow)
}

case class SimpleCompleteVariant(variant: SimpleVariant, variantValues: Seq[SimpleVariantValue])

case class SimpleProductData(productId: Int = 0,
                             skuId: Int = 0,
                             title: String,
                             description: String,
                             image: String = SimpleProductDefaults.imageUrl,
                             code: String,
                             price: Int,
                             currency: Currency = Currency.USD,
                             active: Boolean = false,
                             tags: Seq[String] = Seq.empty)

case class SimpleProductTuple(product: Product,
                              sku: Sku,
                              productForm: ObjectForm,
                              skuForm: ObjectForm,
                              productShadow: ObjectShadow,
                              skuShadow: ObjectShadow)

case class SimpleVariantData(variantId: Int, productShadowId: Int, shadowId: Int, name: String)

case class SimpleVariantValueData(
    valueId: Int, variantShadowId: Int, shadowId: Int, name: String, swatch: String)

case class SimpleCompleteVariantData(
    variant: SimpleVariantData, variantValues: Seq[SimpleVariantValueData])

object Mvp {
  def insertProductNewContext(oldContextId: Int, contextId: Int, p: SimpleProductData)(
      implicit db: Database): DbResultT[SimpleProductData] =
    for {
      simpleProduct ← * <~ SimpleProduct(p.title, p.description, p.image, p.code, p.active, p.tags)
      //find product form other context, get old form and merge with new
      product ← * <~ Products
                 .filter(_.contextId === oldContextId)
                 .filter(_.id === p.productId)
                 .mustFindOneOr(ProductNotFoundForContext(p.productId, oldContextId))
      oldForm     ← * <~ ObjectForms.mustFindById404(product.formId)
      productForm ← * <~ ObjectForms.update(oldForm, simpleProduct.update(oldForm))

      //find sku form for the product and update it with new sku
      link ← * <~ ObjectLinks
              .findByLeftAndType(product.shadowId, ObjectLink.ProductSku)
              .mustFindOneOr(ObjectLeftLinkCannotBeFound(product.shadowId))
      sku ← * <~ Skus
             .filter(_.contextId === oldContextId)
             .filter(_.shadowId === link.rightId)
             .mustFindOneOr(SkuWithShadowNotFound(link.rightId))

      simpleSku  ← * <~ SimpleSku(p.code, p.title, p.image, p.price, p.currency, p.active, p.tags)
      oldSkuForm ← * <~ ObjectForms.mustFindById404(sku.formId)
      skuForm    ← * <~ ObjectForms.update(oldSkuForm, simpleSku.update(oldSkuForm))

      r ← * <~ insertProductIntoContext(
             contextId, productForm, skuForm, simpleProduct, simpleSku, p)
    } yield r

  def insertProduct(contextId: Int, p: SimpleProductData): DbResultT[SimpleProductData] =
    for {
      simpleProduct ← * <~ SimpleProduct(p.title, p.description, p.image, p.code, p.active, p.tags)
      productForm   ← * <~ ObjectForms.create(simpleProduct.create)
      simpleSku     ← * <~ SimpleSku(p.code, p.title, p.image, p.price, p.currency, p.active, p.tags)
      skuForm       ← * <~ ObjectForms.create(simpleSku.create)
      r ← * <~ insertProductIntoContext(
             contextId, productForm, skuForm, simpleProduct, simpleSku, p)
    } yield r

  def insertProductWithExistingSkus(
      contextId: Int, productData: SimpleProductData, skus: Seq[Sku]): DbResultT[Product] =
    for {
      simpleProduct ← * <~ SimpleProduct(productData.title,
                                         productData.description,
                                         productData.image,
                                         productData.code,
                                         productData.active,
                                         productData.tags)
      productForm   ← * <~ ObjectForms.create(simpleProduct.create)
      simpleShadow  ← * <~ SimpleProductShadow(simpleProduct)
      productShadow ← * <~ ObjectShadows.create(simpleShadow.create.copy(formId = productForm.id))

      productCommit ← * <~ ObjectCommits.create(
                         ObjectCommit(formId = productForm.id, shadowId = productShadow.id))

      product ← * <~ Products.create(
                   Product(contextId = contextId,
                           formId = productForm.id,
                           shadowId = productShadow.id,
                           commitId = productCommit.id))

      _ ← * <~ skus.map(sku ⇒ linkProductAndSku(product, sku))
    } yield product

  // Temporary convenience method to use until ObjectLink is replaced.
  private def linkProductAndSku(product: Product, sku: Sku)(implicit ec: EC) =
    for {
      _ ← * <~ ObjectLinks.create(ObjectLink(leftId = product.shadowId,
                                             rightId = sku.shadowId,
                                             linkType = ObjectLink.ProductSku))
      _ ← * <~ ProductSkuLinks.create(ProductSkuLink(leftId = product.id, rightId = sku.id))
    } yield {}

  def insertSku(contextId: Int, s: SimpleSku): DbResultT[Sku] =
    for {
      form    ← * <~ ObjectForms.create(s.create)
      sShadow ← * <~ SimpleSkuShadow(s)
      shadow  ← * <~ ObjectShadows.create(sShadow.create.copy(formId = form.id))
      commit  ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      sku ← * <~ Skus.create(
               Sku(contextId = contextId,
                   code = s.code,
                   formId = form.id,
                   shadowId = shadow.id,
                   commitId = commit.id))
    } yield sku

  def insertSkus(contextId: Int, ss: Seq[SimpleSku]): DbResultT[Seq[Sku]] =
    for {
      skus ← * <~ ss.map(s ⇒ insertSku(contextId, s))
    } yield skus

  def insertVariant(
      contextId: Int, v: SimpleVariant, productShadowId: Int): DbResultT[SimpleVariantData] =
    for {
      form    ← * <~ ObjectForms.create(v.create)
      sShadow ← * <~ SimpleVariantShadow(v)
      shadow  ← * <~ ObjectShadows.create(sShadow.create.copy(formId = form.id))
      commit  ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      variant ← * <~ Variants.create(
                   Variant(contextId = contextId,
                           formId = form.id,
                           shadowId = shadow.id,
                           commitId = commit.id))
      _ ← * <~ ObjectLinks.create(ObjectLink(leftId = productShadowId,
                                             rightId = shadow.id,
                                             linkType = ObjectLink.ProductVariant))
    } yield
      SimpleVariantData(variantId = variant.formId,
                        productShadowId = productShadowId,
                        shadowId = shadow.id,
                        name = v.name)

  def insertVariantValue(contextId: Int,
                         v: SimpleVariantValue,
                         variantShadowId: Int): DbResultT[SimpleVariantValueData] =
    for {
      form    ← * <~ ObjectForms.create(v.create)
      sShadow ← * <~ SimpleVariantValueShadow(v)
      shadow  ← * <~ ObjectShadows.create(sShadow.create.copy(formId = form.id))
      commit  ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      value ← * <~ VariantValues.create(
                 VariantValue(contextId = contextId,
                              formId = form.id,
                              shadowId = shadow.id,
                              commitId = commit.id))
      _ ← * <~ ObjectLinks.create(ObjectLink(leftId = variantShadowId,
                                             rightId = shadow.id,
                                             linkType = ObjectLink.VariantValue))
    } yield
      SimpleVariantValueData(valueId = value.id,
                             variantShadowId = variantShadowId,
                             shadowId = shadow.id,
                             name = v.name,
                             swatch = v.swatch)

  def insertVariantWithValues(
      contextId: Int,
      productShadowId: Int,
      simpleCompleteVariant: SimpleCompleteVariant): DbResultT[SimpleCompleteVariantData] =
    for {
      variant ← * <~ insertVariant(contextId, simpleCompleteVariant.variant, productShadowId)
      values ← * <~ simpleCompleteVariant.variantValues.map(variantValue ⇒
                    insertVariantValue(contextId, variantValue, variant.shadowId))
    } yield SimpleCompleteVariantData(variant, values)

  def insertProductIntoContext(contextId: Int,
                               productForm: ObjectForm,
                               skuForm: ObjectForm,
                               simpleProduct: SimpleProduct,
                               simpleSku: SimpleSku,
                               p: SimpleProductData): DbResultT[SimpleProductData] =
    for {

      simpleShadow  ← * <~ SimpleProductShadow(simpleProduct)
      productShadow ← * <~ ObjectShadows.create(simpleShadow.create.copy(formId = productForm.id))

      productCommit ← * <~ ObjectCommits.create(
                         ObjectCommit(formId = productForm.id, shadowId = productShadow.id))

      product ← * <~ Products.create(
                   Product(contextId = contextId,
                           formId = productForm.id,
                           shadowId = productShadow.id,
                           commitId = productCommit.id))

      simpleSkuShadow ← * <~ SimpleSkuShadow(simpleSku)
      skuShadow       ← * <~ ObjectShadows.create(simpleSkuShadow.create.copy(formId = skuForm.id))

      link ← * <~ ObjectLinks.create(ObjectLink(leftId = productShadow.id,
                                                rightId = skuShadow.id,
                                                linkType = ObjectLink.ProductSku))

      skuCommit ← * <~ ObjectCommits.create(
                     ObjectCommit(formId = skuForm.id, shadowId = skuShadow.id))

      sku ← * <~ Skus.create(
               Sku(contextId = contextId,
                   code = p.code,
                   formId = skuForm.id,
                   shadowId = skuShadow.id,
                   commitId = skuCommit.id))
    } yield p.copy(productId = product.id, skuId = sku.id)

  def getPrice(skuId: Int)(implicit db: Database): DbResultT[Int] =
    for {
      sku    ← * <~ Skus.mustFindById404(skuId)
      form   ← * <~ ObjectForms.mustFindById404(sku.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      p      ← * <~ priceAsInt(form, shadow)
    } yield p

  def getProductTuple(d: SimpleProductData)(implicit db: Database): DbResultT[SimpleProductTuple] =
    for {
      product       ← * <~ Products.mustFindById404(d.productId)
      productForm   ← * <~ ObjectForms.mustFindById404(product.formId)
      productShadow ← * <~ ObjectShadows.mustFindById404(product.shadowId)
      sku           ← * <~ Skus.mustFindById404(d.skuId)
      skuForm       ← * <~ ObjectForms.mustFindById404(sku.formId)
      skuShadow     ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    } yield SimpleProductTuple(product, sku, productForm, skuForm, productShadow, skuShadow)

  def insertProducts(
      ps: Seq[SimpleProductData], contextId: Int): DbResultT[Seq[SimpleProductData]] =
    for {
      results ← * <~ ps.map(p ⇒ insertProduct(contextId, p))
    } yield results

  def insertProductsNewContext(oldContextId: Int, contextId: Int, ps: Seq[SimpleProductData])(
      implicit db: Database): DbResultT[Seq[SimpleProductData]] =
    for {
      results ← * <~ ps.map(p ⇒ insertProductNewContext(oldContextId, contextId, p))
    } yield results

  def priceFromJson(p: Json): Option[(Int, Currency)] = {
    val price = for {
      JInt(value)       ← p \ "value"
      JString(currency) ← p \ "currency"
    } yield (value.toInt, Currency(currency))
    if (price.isEmpty) None else price.headOption
  }

  def price(f: ObjectForm, s: ObjectShadow): Option[(Int, Currency)] = {
    ObjectUtils.get("salePrice", f, s) match {
      case JNothing ⇒ None
      case v        ⇒ priceFromJson(v)
    }
  }

  def priceAsInt(f: ObjectForm, s: ObjectShadow): Int =
    price(f, s).map { case (value, _) ⇒ value }.getOrElse(0)

  def name(f: ObjectForm, s: ObjectShadow): Option[String] = {
    ObjectUtils.get("title", f, s) match {
      case JString(title) ⇒ title.some
      case _              ⇒ None
    }
  }

  def firstImage(f: ObjectForm, s: ObjectShadow): Option[String] = {
    ObjectUtils.get("images", f, s) match {
      case JArray(images) ⇒
        images.headOption.flatMap {
          case JString(image) ⇒ image.some
          case _              ⇒ None
        }
      case _ ⇒ None
    }
  }
}
