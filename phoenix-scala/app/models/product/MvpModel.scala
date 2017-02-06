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

case class SimpleVariant(code: String,
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

case class SimpleVariantShadow(s: SimpleVariant) {

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

case class SimpleProductOption(name: String) {
  val (keyMap, form) = ObjectUtils.createForm(parse(s"""{ "name": "$name" }"""))

  def create: ObjectForm = ObjectForm(kind = ProductOption.kind, attributes = form)

  def update(oldForm: ObjectForm): ObjectForm =
    oldForm.copy(attributes = oldForm.attributes merge form)
}

case class SimpleOptionShadow(v: SimpleProductOption) {
  val shadow =
    ObjectUtils.newShadow(parse("""{ "name": { "type": "string", "ref": "name" } }"""), v.keyMap)

  def create: ObjectShadow = ObjectShadow(attributes = shadow)
}

case class SimpleProductOptionValue(name: String, swatch: String, skus: Seq[String] = Seq.empty) {
  val (keyMap, form) =
    ObjectUtils.createForm(parse(s"""{ "name": "$name", "swatch": "$swatch" }"""))

  def create: ObjectForm = ObjectForm(kind = ProductOptionValue.kind, attributes = form)

  def update(oldForm: ObjectForm): ObjectForm =
    oldForm.copy(attributes = oldForm.attributes merge form)
}

case class SimpleProductOptionValueShadow(v: SimpleProductOptionValue) {
  val shadow = ObjectUtils.newShadow(parse("""
      {
        "name": { "type": "string", "ref": "name" },
        "swatch": { "type": "string", "ref": "swatch" }
      }
    """),
                                     v.keyMap)

  def create: ObjectShadow = ObjectShadow(attributes = shadow)
}

case class SimpleCompleteOption(option: SimpleProductOption,
                                productValues: Seq[SimpleProductOptionValue])

case class SimpleProductData(productId: Int = 0,
                             variantId: Int = 0,
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
                              variant: ProductVariant,
                              productForm: ObjectForm,
                              variantForm: ObjectForm,
                              productShadow: ObjectShadow,
                              variantShadow: ObjectShadow)

case class SimpleOptionData(optionId: Int,
                            optionFormId: Int,
                            productShadowId: Int,
                            optionShadowId: Int,
                            name: String)

case class SimpleProductOptionValueData(id: Int,
                                        optionShadowId: Int,
                                        shadowId: Int,
                                        name: String,
                                        swatch: String)

case class SimpleCompleteOptionData(option: SimpleOptionData,
                                    optionValues: Seq[SimpleProductOptionValueData])

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

      variant ← * <~ ProductVariants
                 .filter(_.id === link.rightId)
                 .mustFindOneOr(ProductVariantNotFound(link.rightId))

      simpleVariant  ← * <~ SimpleVariant(p.code, p.title, p.price, p.currency, p.active, p.tags)
      oldVariantForm ← * <~ ObjectForms.mustFindById404(variant.formId)
      variantForm    ← * <~ ObjectForms.update(oldVariantForm, simpleVariant.update(oldVariantForm))

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
                                        variantForm,
                                        albumForm,
                                        simpleProduct,
                                        simpleVariant,
                                        simpleAlbum,
                                        p)
    } yield r

  def insertProduct(contextId: Int, p: SimpleProductData)(implicit db: DB,
                                                          au: AU): DbResultT[SimpleProductData] =
    for {
      simpleProduct ← * <~ SimpleProduct(p.title, p.description, p.active, p.tags)
      productForm   ← * <~ ObjectForms.create(simpleProduct.create)
      simpleVariant ← * <~ SimpleVariant(p.code, p.title, p.price, p.currency, p.active, p.tags)
      variantForm   ← * <~ ObjectForms.create(simpleVariant.create)
      simpleAlbum   ← * <~ new SimpleAlbum(p.title, p.image)
      albumForm     ← * <~ ObjectForms.create(simpleAlbum.create)
      insertedProduct ← * <~ insertProductIntoContext(contextId,
                                                      productForm,
                                                      variantForm,
                                                      albumForm,
                                                      simpleProduct,
                                                      simpleVariant,
                                                      simpleAlbum,
                                                      p)
    } yield insertedProduct

  def insertProductWithExistingVariants(scope: LTree,
                                        contextId: Int,
                                        productData: SimpleProductData,
                                        variants: Seq[ProductVariant]): DbResultT[Product] =
    for {
      simpleProduct ← * <~ SimpleProduct(productData.title,
                                         productData.description,
                                         productData.active,
                                         productData.tags)
      result ← * <~ insertProductWithExistingVariants(scope, contextId, simpleProduct, variants)
    } yield result

  def insertProductWithExistingVariants(scope: LTree,
                                        contextId: Int,
                                        simpleProduct: SimpleProduct,
                                        variants: Seq[ProductVariant]): DbResultT[Product] =
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

      _ ← * <~ variants.map(variant ⇒ linkProductAndVariant(product, variant))
    } yield product

  // Temporary convenience method to use until ObjectLink is replaced.
  private def linkProductAndVariant(product: Product, variant: ProductVariant)(implicit ec: EC) =
    ProductVariantLinks.create(ProductVariantLink(leftId = product.id, rightId = variant.id)).meh

  def insertProductVariant(scope: LTree,
                           contextId: Int,
                           simpleVariant: SimpleVariant): DbResultT[ProductVariant] =
    for {
      form          ← * <~ ObjectForms.create(simpleVariant.create)
      variantSchema ← * <~ ObjectFullSchemas.findOneByName("variant")
      shadow ← * <~ ObjectShadows.create(
                  SimpleVariantShadow(simpleVariant).create
                    .copy(formId = form.id, jsonSchema = variantSchema.map(_.name)))
      commit ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      variant ← * <~ ProductVariants.create(
                   ProductVariant(scope = scope,
                                  contextId = contextId,
                                  code = simpleVariant.code,
                                  formId = form.id,
                                  shadowId = shadow.id,
                                  commitId = commit.id))
    } yield variant

  def insertProductVariants(scope: LTree,
                            contextId: Int,
                            variants: Seq[SimpleVariant]): DbResultT[Seq[ProductVariant]] =
    DbResultT.sequence(variants.map(variant ⇒ insertProductVariant(scope, contextId, variant)))

  def insertProductOption(scope: LTree,
                          contextId: Int,
                          productOption: SimpleProductOption,
                          product: Product): DbResultT[SimpleOptionData] =
    for {
      form ← * <~ ObjectForms.create(productOption.create)
      shadow ← * <~ ObjectShadows.create(
                  SimpleOptionShadow(productOption).create.copy(formId = form.id))
      commit ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      option ← * <~ ProductOptions.create(
                  ProductOption(scope = scope,
                                contextId = contextId,
                                formId = form.id,
                                shadowId = shadow.id,
                                commitId = commit.id))
      _ ← * <~ ProductOptionLinks.create(
             ProductOptionLink(leftId = product.id, rightId = option.id))
    } yield
      SimpleOptionData(optionId = option.id,
                       optionFormId = option.formId,
                       productShadowId = product.shadowId,
                       optionShadowId = option.shadowId,
                       name = productOption.name)

  def insertProductOptionValue(
      scope: LTree,
      contextId: Int,
      simpleOptionValue: SimpleProductOptionValue,
      optionShadowId: Int,
      optionId: ProductOption#Id): DbResultT[SimpleProductOptionValueData] =
    for {
      form ← * <~ ObjectForms.create(simpleOptionValue.create)
      shadow ← * <~ ObjectShadows.create(
                  SimpleProductOptionValueShadow(simpleOptionValue).create.copy(formId = form.id))
      commit ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      optionValue ← * <~ ProductOptionValues.create(
                       ProductOptionValue(scope = scope,
                                          contextId = contextId,
                                          formId = form.id,
                                          shadowId = shadow.id,
                                          commitId = commit.id))
      _ ← * <~ ProductOptionValueLinks.create(
             ProductOptionValueLink(leftId = optionId, rightId = optionValue.id))
      productVariants ← * <~ simpleOptionValue.skus.map(code ⇒
                             ProductVariantManager.mustFindByContextAndCode(contextId, code))
      _ ← * <~ productVariants.map { variant ⇒
           ProductValueVariantLinks.create(
               ProductValueVariantLink(leftId = optionValue.id, rightId = variant.id))
         }
    } yield
      SimpleProductOptionValueData(id = optionValue.id,
                                   optionShadowId = optionShadowId,
                                   shadowId = shadow.id,
                                   name = simpleOptionValue.name,
                                   swatch = simpleOptionValue.swatch)

  def insertProductOptionWithValues(
      scope: LTree,
      contextId: Int,
      product: Product,
      simpleCompleteOption: SimpleCompleteOption): DbResultT[SimpleCompleteOptionData] =
    for {
      option ← * <~ insertProductOption(scope, contextId, simpleCompleteOption.option, product)
      values ← * <~ simpleCompleteOption.productValues.map { optionValue ⇒
                insertProductOptionValue(scope = scope,
                                         contextId = contextId,
                                         simpleOptionValue = optionValue,
                                         optionShadowId = option.optionShadowId,
                                         optionId = option.optionId)
              }
    } yield SimpleCompleteOptionData(option, values)

  def insertProductIntoContext(
      contextId: Int,
      productForm: ObjectForm,
      variantForm: ObjectForm,
      albumForm: ObjectForm,
      simpleProduct: SimpleProduct,
      simpleVariant: SimpleVariant,
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

      simpleVariantShadow ← * <~ SimpleVariantShadow(simpleVariant)
      variantSchema       ← * <~ ObjectFullSchemas.findOneByName("variant")
      variantShadow ← * <~ ObjectShadows.create(
                         simpleVariantShadow.create.copy(formId = variantForm.id,
                                                         jsonSchema = variantSchema.map(_.name)))

      variantCommit ← * <~ ObjectCommits.create(
                         ObjectCommit(formId = variantForm.id, shadowId = variantShadow.id))

      variant ← * <~ ProductVariants.create(
                   ProductVariant(scope = scope,
                                  contextId = contextId,
                                  code = p.code,
                                  formId = variantForm.id,
                                  shadowId = variantShadow.id,
                                  commitId = variantCommit.id))

      _ ← * <~ linkProductAndVariant(product, variant)

      context ← * <~ ObjectContexts.mustFindById404(contextId)
      album   ← * <~ insertAlbumIntoContext(context, simpleAlbum, albumForm, productShadow, product)

    } yield p.copy(productId = product.id, variantId = variant.id, albumId = album.id)

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

  def getPrice(variantId: Int)(implicit db: DB): DbResultT[Int] =
    for {
      variant ← * <~ ProductVariants.mustFindById404(variantId)
      form    ← * <~ ObjectForms.mustFindById404(variant.formId)
      shadow  ← * <~ ObjectShadows.mustFindById404(variant.shadowId)
      price   ← * <~ priceAsInt(form, shadow)
    } yield price

  def getProductTuple(d: SimpleProductData)(implicit db: DB): DbResultT[SimpleProductTuple] =
    for {
      product       ← * <~ Products.mustFindById404(d.productId)
      productForm   ← * <~ ObjectForms.mustFindById404(product.formId)
      productShadow ← * <~ ObjectShadows.mustFindById404(product.shadowId)
      variant       ← * <~ ProductVariants.mustFindById404(d.variantId)
      variantForm   ← * <~ ObjectForms.mustFindById404(variant.formId)
      variantShadow ← * <~ ObjectShadows.mustFindById404(variant.shadowId)
    } yield
      SimpleProductTuple(product, variant, productForm, variantForm, productShadow, variantShadow)

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
