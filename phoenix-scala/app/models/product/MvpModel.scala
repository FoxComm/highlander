package models.product

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import failures.ImageFailures._
import failures.ObjectFailures._
import failures.ProductFailures._
import models.image._
import models.inventory._
import models.objects._
import models.account._
import org.json4s.JsonAST.{JNothing, JString}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import payloads.ImagePayloads._
import services.image.ImageManager
import services.inventory.ProductVariantManager
import com.github.tminglei.slickpg.LTree
import slick.driver.PostgresDriver.api._
import utils.Money.Currency
import utils.aliases._
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

case class SimpleProduct(title: String,
                         description: String,
                         active: Boolean = false,
                         tags: Seq[String] = Seq.empty) {
  val activeFrom = if (active) s""""${Instant.now}"""" else "null";
  val ts: String = compact(render(JArray(tags.map(t ⇒ JString(t)).toList)))

  val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "title" : "$title",
      "description" : "$description",
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
          "activeFrom" : {"type": "date", "ref": "activeFrom"},
          "tags" : {"type": "tags", "ref": "tags"}
        }"""),
      p.keyMap)

  def create: ObjectShadow =
    ObjectShadow(attributes = shadow)
}

case class SimpleAlbum(payload: AlbumPayload) {

  def this(name: String, image: String) =
    this(
        AlbumPayload(name = Some(name),
                     position = Some(1),
                     images =
                       Seq(ImagePayload(src = image, title = image.some, alt = image.some)).some))

  val (keyMap, form) = ObjectUtils.createForm(payload.formAndShadow.form.attributes)

  def create: ObjectForm = ObjectForm(kind = Album.kind, attributes = form)

  def update(oldForm: ObjectForm): ObjectForm =
    oldForm.copy(attributes = oldForm.attributes.merge(create.attributes))
}

case class SimpleAlbumShadow(album: SimpleAlbum) {

  val shadow = ObjectUtils.newShadow(album.payload.formAndShadow.shadow.attributes, album.keyMap)

  def create: ObjectShadow = ObjectShadow(attributes = shadow)
}

case class SimpleSku(code: String,
                     title: String,
                     price: Int,
                     currency: Currency = Currency.USD,
                     active: Boolean = false,
                     tags: Seq[String] = Seq.empty) {

  val activeFrom = if (active) s""""${Instant.now}"""" else "null";
  val ts: String = compact(render(JArray(tags.map(t ⇒ JString(t)).toList)))

  val (keyMap, form) = ObjectUtils.createForm(parse(s"""
      {
        "code": "$code",
        "title" : "$title",
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
    ObjectForm(kind = ProductVariant.kind, attributes = form)
  def update(oldForm: ObjectForm): ObjectForm =
    oldForm.copy(attributes = oldForm.attributes merge form)
}

case class SimpleSkuShadow(s: SimpleSku) {

  val shadow = ObjectUtils.newShadow(parse("""
        {
          "code" : {"type": "string", "ref": "code"},
          "title" : {"type": "string", "ref": "title"},
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

  def create: ObjectForm = ObjectForm(kind = ProductOption.kind, attributes = form)

  def update(oldForm: ObjectForm): ObjectForm =
    oldForm.copy(attributes = oldForm.attributes merge form)
}

case class SimpleVariantShadow(v: SimpleVariant) {
  val shadow =
    ObjectUtils.newShadow(parse("""{ "name": { "type": "string", "ref": "name" } }"""), v.keyMap)

  def create: ObjectShadow = ObjectShadow(attributes = shadow)
}

case class SimpleVariantValue(name: String, swatch: String, skuCodes: Seq[String] = Seq.empty) {
  val (keyMap, form) =
    ObjectUtils.createForm(parse(s"""{ "name": "$name", "swatch": "$swatch" }"""))

  def create: ObjectForm = ObjectForm(kind = ProductValue.kind, attributes = form)

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
                             albumId: Int = 0,
                             title: String,
                             description: String,
                             image: String,
                             code: String,
                             price: Int,
                             currency: Currency = Currency.USD,
                             active: Boolean = false,
                             tags: Seq[String] = Seq.empty)

case class SimpleProductTuple(product: Product,
                              sku: ProductVariant,
                              productForm: ObjectForm,
                              skuForm: ObjectForm,
                              productShadow: ObjectShadow,
                              skuShadow: ObjectShadow)

case class SimpleVariantData(variantId: Int,
                             variantFormId: Int,
                             productShadowId: Int,
                             shadowId: Int,
                             name: String)

case class SimpleVariantValueData(valueId: Int,
                                  variantShadowId: Int,
                                  shadowId: Int,
                                  name: String,
                                  swatch: String)

case class SimpleCompleteVariantData(variant: SimpleVariantData,
                                     variantValues: Seq[SimpleVariantValueData])

object Mvp {

  def insertProductNewContext(oldContextId: Int, contextId: Int, p: SimpleProductData)(
      implicit db: DB,
      au: AU): DbResultT[SimpleProductData] =
    for {
      simpleProduct ← * <~ SimpleProduct(p.title, p.description, p.active, p.tags)
      //find product form other context, get old form and merge with new
      product ← * <~ Products
                 .filter(_.contextId === oldContextId)
                 .filter(_.id === p.productId)
                 .mustFindOneOr(ProductNotFoundForContext(p.productId, oldContextId))
      oldForm     ← * <~ ObjectForms.mustFindById404(product.formId)
      productForm ← * <~ ObjectForms.update(oldForm, simpleProduct.update(oldForm))

      //find variant form for the product and update it with new variant
      link ← * <~ ProductVariantLinks
              .filterLeft(product)
              .mustFindOneOr(ObjectLeftLinkCannotBeFound(product.shadowId))

      sku ← * <~ ProductVariants
             .filter(_.id === link.rightId)
             .mustFindOneOr(ProductVariantNotFound(link.rightId))

      simpleSku  ← * <~ SimpleSku(p.code, p.title, p.price, p.currency, p.active, p.tags)
      oldSkuForm ← * <~ ObjectForms.mustFindById404(sku.formId)
      skuForm    ← * <~ ObjectForms.update(oldSkuForm, simpleSku.update(oldSkuForm))

      //find album form for the product and update it
      albumLink ← * <~ ProductAlbumLinks
                   .filterLeft(product)
                   .mustFindOneOr(NoAlbumsFoundForProduct(product.id))
      album ← * <~ Albums
               .filter(_.id === albumLink.rightId)
               .mustFindOneOr(AlbumNotFoundForContext(albumLink.rightId, oldContextId))

      simpleAlbum  ← * <~ new SimpleAlbum(p.title, p.image)
      oldAlbumForm ← * <~ ObjectForms.mustFindById404(album.formId)
      albumForm    ← * <~ ObjectForms.update(oldAlbumForm, simpleAlbum.update(oldAlbumForm))

      r ← * <~ insertProductIntoContext(contextId,
                                        productForm,
                                        skuForm,
                                        albumForm,
                                        simpleProduct,
                                        simpleSku,
                                        simpleAlbum,
                                        p)
    } yield r

  def insertProduct(contextId: Int, p: SimpleProductData)(implicit db: DB,
                                                          au: AU): DbResultT[SimpleProductData] =
    for {
      simpleProduct ← * <~ SimpleProduct(p.title, p.description, p.active, p.tags)
      productForm   ← * <~ ObjectForms.create(simpleProduct.create)
      simpleSku     ← * <~ SimpleSku(p.code, p.title, p.price, p.currency, p.active, p.tags)
      skuForm       ← * <~ ObjectForms.create(simpleSku.create)
      simpleAlbum   ← * <~ new SimpleAlbum(p.title, p.image)
      albumForm     ← * <~ ObjectForms.create(simpleAlbum.create)
      r ← * <~ insertProductIntoContext(contextId,
                                        productForm,
                                        skuForm,
                                        albumForm,
                                        simpleProduct,
                                        simpleSku,
                                        simpleAlbum,
                                        p)
    } yield r

  def insertProductWithExistingSkus(scope: LTree,
                                    contextId: Int,
                                    productData: SimpleProductData,
                                    skus: Seq[ProductVariant]): DbResultT[Product] =
    for {
      simpleProduct ← * <~ SimpleProduct(productData.title,
                                         productData.description,
                                         productData.active,
                                         productData.tags)
      result ← * <~ insertProductWithExistingSkus(scope, contextId, simpleProduct, skus)
    } yield result

  def insertProductWithExistingSkus(scope: LTree,
                                    contextId: Int,
                                    simpleProduct: SimpleProduct,
                                    skus: Seq[ProductVariant]): DbResultT[Product] =
    for {
      productForm   ← * <~ ObjectForms.create(simpleProduct.create)
      simpleShadow  ← * <~ SimpleProductShadow(simpleProduct)
      productSchema ← * <~ ObjectFullSchemas.findOneByName("product")
      productShadow ← * <~ ObjectShadows.create(
                         simpleShadow.create.copy(formId = productForm.id,
                                                  jsonSchema = productSchema.map(_.name)))

      productCommit ← * <~ ObjectCommits.create(
                         ObjectCommit(formId = productForm.id, shadowId = productShadow.id))

      product ← * <~ Products.create(
                   Product(scope = scope,
                           contextId = contextId,
                           formId = productForm.id,
                           shadowId = productShadow.id,
                           commitId = productCommit.id))

      _ ← * <~ skus.map(sku ⇒ linkProductAndSku(product, sku))
    } yield product

  // Temporary convenience method to use until ObjectLink is replaced.
  private def linkProductAndSku(product: Product, sku: ProductVariant)(implicit ec: EC) =
    for {
      _ ← * <~ ProductVariantLinks.create(
             ProductVariantLink(leftId = product.id, rightId = sku.id))
    } yield {}

  def insertSku(scope: LTree, contextId: Int, s: SimpleSku): DbResultT[ProductVariant] =
    for {
      form          ← * <~ ObjectForms.create(s.create)
      sShadow       ← * <~ SimpleSkuShadow(s)
      variantSchema ← * <~ ObjectFullSchemas.findOneByName("variant")
      shadow ← * <~ ObjectShadows.create(
                  sShadow.create.copy(formId = form.id, jsonSchema = variantSchema.map(_.name)))
      commit ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      sku ← * <~ ProductVariants.create(
               ProductVariant(scope = scope,
                              contextId = contextId,
                              code = s.code,
                              formId = form.id,
                              shadowId = shadow.id,
                              commitId = commit.id))
    } yield sku

  def insertSkus(scope: LTree,
                 contextId: Int,
                 ss: Seq[SimpleSku]): DbResultT[Seq[ProductVariant]] =
    for {
      skus ← * <~ ss.map(s ⇒ insertSku(scope, contextId, s))
    } yield skus

  def insertVariant(scope: LTree,
                    contextId: Int,
                    v: SimpleVariant,
                    product: Product): DbResultT[SimpleVariantData] =
    for {
      form    ← * <~ ObjectForms.create(v.create)
      sShadow ← * <~ SimpleVariantShadow(v)
      shadow  ← * <~ ObjectShadows.create(sShadow.create.copy(formId = form.id))
      commit  ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      variant ← * <~ ProductOptions.create(
                   ProductOption(scope = scope,
                                 contextId = contextId,
                                 formId = form.id,
                                 shadowId = shadow.id,
                                 commitId = commit.id))
      _ ← * <~ ProductOptionLinks.create(
             ProductOptionLink(leftId = product.id, rightId = variant.id))
    } yield
      SimpleVariantData(variantId = variant.id,
                        variantFormId = variant.formId,
                        productShadowId = product.shadowId,
                        shadowId = shadow.id,
                        name = v.name)

  def insertVariantValue(scope: LTree,
                         contextId: Int,
                         v: SimpleVariantValue,
                         variantShadowId: Int,
                         variantId: ProductOption#Id): DbResultT[SimpleVariantValueData] =
    for {
      form    ← * <~ ObjectForms.create(v.create)
      sShadow ← * <~ SimpleVariantValueShadow(v)
      shadow  ← * <~ ObjectShadows.create(sShadow.create.copy(formId = form.id))
      commit  ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      value ← * <~ ProductValues.create(
                 ProductValue(scope = scope,
                              contextId = contextId,
                              formId = form.id,
                              shadowId = shadow.id,
                              commitId = commit.id))
      _ ← * <~ ProductOptionValueLinks.create(
             ProductOptionValueLink(leftId = variantId, rightId = value.id))
      skuCodes ← * <~ v.skuCodes.map(code ⇒
                      ProductVariantManager.mustFindByContextAndCode(contextId, code))
      _ ← * <~ skuCodes.map(
             s ⇒
               ProductValueVariantLinks.create(
                   ProductValueVariantLink(leftId = value.id, rightId = s.id)))
    } yield
      SimpleVariantValueData(valueId = value.id,
                             variantShadowId = variantShadowId,
                             shadowId = shadow.id,
                             name = v.name,
                             swatch = v.swatch)

  def insertVariantWithValues(
      scope: LTree,
      contextId: Int,
      product: Product,
      simpleCompleteVariant: SimpleCompleteVariant): DbResultT[SimpleCompleteVariantData] =
    for {
      variant ← * <~ insertVariant(scope, contextId, simpleCompleteVariant.variant, product)
      values ← * <~ simpleCompleteVariant.variantValues.map(
                  variantValue ⇒
                    insertVariantValue(scope,
                                       contextId,
                                       variantValue,
                                       variant.shadowId,
                                       variant.variantId))
    } yield SimpleCompleteVariantData(variant, values)

  def insertProductIntoContext(
      contextId: Int,
      productForm: ObjectForm,
      skuForm: ObjectForm,
      albumForm: ObjectForm,
      simpleProduct: SimpleProduct,
      simpleSku: SimpleSku,
      simpleAlbum: SimpleAlbum,
      p: SimpleProductData)(implicit db: DB, au: AU): DbResultT[SimpleProductData] =
    for {
      scope         ← * <~ Scope.resolveOverride(None)
      simpleShadow  ← * <~ SimpleProductShadow(simpleProduct)
      productSchema ← * <~ ObjectFullSchemas.findOneByName("product")
      productShadow ← * <~ ObjectShadows.create(
                         simpleShadow.create.copy(formId = productForm.id,
                                                  jsonSchema = productSchema.map(_.name)))

      productCommit ← * <~ ObjectCommits.create(
                         ObjectCommit(formId = productForm.id, shadowId = productShadow.id))

      product ← * <~ Products.create(
                   Product(scope = scope,
                           contextId = contextId,
                           formId = productForm.id,
                           shadowId = productShadow.id,
                           commitId = productCommit.id))

      simpleSkuShadow ← * <~ SimpleSkuShadow(simpleSku)
      variantSchema   ← * <~ ObjectFullSchemas.findOneByName("variant")
      skuShadow ← * <~ ObjectShadows.create(
                     simpleSkuShadow.create.copy(formId = skuForm.id,
                                                 jsonSchema = variantSchema.map(_.name)))

      skuCommit ← * <~ ObjectCommits.create(
                     ObjectCommit(formId = skuForm.id, shadowId = skuShadow.id))

      sku ← * <~ ProductVariants.create(
               ProductVariant(scope = scope,
                              contextId = contextId,
                              code = p.code,
                              formId = skuForm.id,
                              shadowId = skuShadow.id,
                              commitId = skuCommit.id))

      _ ← * <~ linkProductAndSku(product, sku)

      context ← * <~ ObjectContexts.mustFindById404(contextId)
      album   ← * <~ insertAlbumIntoContext(context, simpleAlbum, albumForm, productShadow, product)

    } yield p.copy(productId = product.id, skuId = sku.id, albumId = album.id)

  def insertAlbumIntoContext(context: ObjectContext,
                             simpleAlbum: SimpleAlbum,
                             albumForm: ObjectForm,
                             productShadow: ObjectShadow,
                             product: Product)(implicit db: DB, au: AU): DbResultT[Album] = {
    for {
      scope       ← * <~ Scope.resolveOverride(None)
      albumSchema ← * <~ ObjectFullSchemas.findOneByName("album")
      albumShadow ← * <~ ObjectShadows.create(
                       SimpleAlbumShadow(simpleAlbum).create
                         .copy(formId = albumForm.id, jsonSchema = albumSchema.map(_.name)))
      albumCommit ← * <~ ObjectCommits.create(
                       ObjectCommit(formId = albumForm.id, shadowId = albumShadow.id))

      album ← * <~ Albums.create(
                 Album(scope = scope,
                       contextId = context.id,
                       formId = albumForm.id,
                       shadowId = albumShadow.id,
                       commitId = albumCommit.id))
      albumLink ← * <~ ProductAlbumLinks.create(
                     ProductAlbumLink(leftId = product.id, rightId = album.id))
      _ ← * <~ ImageManager
           .createImagesForAlbum(album, simpleAlbum.payload.images.toSeq.flatten, context)
    } yield album
  }

  def getPrice(skuId: Int)(implicit db: DB): DbResultT[Int] =
    for {
      sku    ← * <~ ProductVariants.mustFindById404(skuId)
      form   ← * <~ ObjectForms.mustFindById404(sku.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      p      ← * <~ priceAsInt(form, shadow)
    } yield p

  def getProductTuple(d: SimpleProductData)(implicit db: DB): DbResultT[SimpleProductTuple] =
    for {
      product       ← * <~ Products.mustFindById404(d.productId)
      productForm   ← * <~ ObjectForms.mustFindById404(product.formId)
      productShadow ← * <~ ObjectShadows.mustFindById404(product.shadowId)
      sku           ← * <~ ProductVariants.mustFindById404(d.skuId)
      skuForm       ← * <~ ObjectForms.mustFindById404(sku.formId)
      skuShadow     ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    } yield SimpleProductTuple(product, sku, productForm, skuForm, productShadow, skuShadow)

  def insertProducts(ps: Seq[SimpleProductData],
                     contextId: Int)(implicit db: DB, au: AU): DbResultT[Seq[SimpleProductData]] =
    for {
      results ← * <~ ps.map(p ⇒ insertProduct(contextId, p))
    } yield results

  def insertProductsNewContext(oldContextId: Int, contextId: Int, ps: Seq[SimpleProductData])(
      implicit db: DB,
      au: AU): DbResultT[Seq[SimpleProductData]] =
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

  def title(f: ObjectForm, s: ObjectShadow): Option[String] = {
    ObjectUtils.get("title", f, s) match {
      case JString(title) ⇒ title.some
      case _              ⇒ None
    }
  }

  def externalId(f: ObjectForm, s: ObjectShadow): Option[String] = {
    ObjectUtils.get("externalId", f, s) match {
      case JString(externalId) ⇒ externalId.some
      case _                   ⇒ None
    }
  }

  def trackInventory(f: ObjectForm, s: ObjectShadow): Boolean = {
    ObjectUtils.get("trackInventory", f, s) match {
      case JBool(trackInventory) ⇒ trackInventory
      case _                     ⇒ true
    }
  }
}
