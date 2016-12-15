package services.product

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import cats.data._
import cats.implicits._
import cats.data.ValidatedNel
import cats.instances.map
import failures._
import failures.ArchiveFailures._
import failures.ProductFailures._
import models.image.{AlbumImageLinks, Albums}
import models.inventory._
import models.objects._
import models.product._
import models.account._
import models.cord.lineitems.CartLineItems
import payloads.ImagePayloads.{AlbumPayload, UpdateAlbumPositionPayload}
import payloads.ProductPayloads._
import payloads.ProductVariantPayloads._
import payloads.ProductOptionPayloads._
import responses.AlbumResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import responses.AlbumResponses._
import responses.ImageResponses.ImageResponse
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductResponses._
import responses.ProductVariantResponses._
import responses.ProductOptionResponses.IlluminatedProductOptionResponse
import services.image.ImageManager
import services.inventory.ProductVariantManager
import services.objects.ObjectManager
import services.variant.ProductOptionManager
import services.variant.ProductOptionManager._
import slick.driver.PostgresDriver.api._
import utils.Validation._
import utils.aliases._
import utils.db._
import org.json4s._
import org.json4s.JsonDSL._
import services.LogActivity
import services.taxonomy.TaxonomyManager
import services.image.ImageManager.FullAlbumWithImages

object ProductManager {

  def createProduct(admin: User, payload: CreateProductPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      oc: OC,
      au: AU): DbResultT[ProductResponse.Root] = {

    val form            = ObjectForm.fromPayload(Product.kind, payload.attributes)
    val shadow          = ObjectShadow.fromPayload(payload.attributes)
    val variantPayloads = payload.variants.getOrElse(Seq.empty)
    val hasVariants     = variantPayloads.nonEmpty
    val albumPayloads   = payload.albums.getOrElse(Seq.empty)

    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      _     ← * <~ validateCreate(payload)
      ins   ← * <~ ObjectUtils.insert(form, shadow, payload.schema)
      product ← * <~ Products.create(
                   Product(scope = scope,
                           contextId = oc.id,
                           formId = ins.form.id,
                           shadowId = ins.shadow.id,
                           commitId = ins.commit.id))

      albums         ← * <~ findOrCreateAlbumsForProduct(product, albumPayloads)
      productSkus    ← * <~ findOrCreateVariantsForProduct(product, payload.skus, !hasVariants)
      variants       ← * <~ findOrCreateVariantsForProduct(product, variantPayloads)
      variantAndSkus ← * <~ getVariantsWithRelatedSkus(variants)
      (variantSkus, variantResponses) = variantAndSkus
      taxons ← * <~ TaxonomyManager.getAssignedTaxons(product)
      response = ProductResponse.build(
          IlluminatedProduct.illuminate(oc, product, ins.form, ins.shadow),
          albums.map(AlbumResponse.build),
          if (hasVariants) variantSkus else productSkus,
          variantResponses,
          taxons)
      _ ← * <~ LogActivity
           .fullProductCreated(Some(admin), response, ObjectContextResponse.build(oc))
    } yield response

  }

  def getProduct(
      productId: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[ProductResponse.Root] =
    for {
      oldProduct ← * <~ mustFindFullProductByFormId(productId)
      albums     ← * <~ ImageManager.getAlbumsForProduct(oldProduct.form.id)

      fullSkus    ← * <~ ProductVariantLinks.queryRightByLeft(oldProduct.model)
      productSkus ← * <~ fullSkus.map(ProductVariantManager.illuminateVariant)

      variants     ← * <~ ProductOptionLinks.queryRightByLeft(oldProduct.model)
      fullVariants ← * <~ variants.map(ProductOptionManager.zipVariantWithValues)

      hasVariants = variants.nonEmpty

      variantAndSkus ← * <~ getVariantsWithRelatedSkus(fullVariants)
      (variantSkus, variantResponses) = variantAndSkus

      taxons ← * <~ TaxonomyManager.getAssignedTaxons(oldProduct.model)
    } yield
      ProductResponse.build(
          IlluminatedProduct.illuminate(oc, oldProduct.model, oldProduct.form, oldProduct.shadow),
          albums,
          if (hasVariants) variantSkus else productSkus,
          variantResponses,
          taxons)

  def updateProduct(admin: User, productId: Int, payload: UpdateProductPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      oc: OC,
      au: AU): DbResultT[ProductResponse.Root] = {

    val formAndShadow = FormAndShadow.fromPayload(Product.kind, payload.attributes)

    val payloadSkus = payload.skus.getOrElse(Seq.empty)

    for {
      oldProduct ← * <~ mustFindFullProductByFormId(productId)

      _ ← * <~ skusToBeUnassociatedMustNotBePresentInCarts(oldProduct.model.id, payloadSkus)

      mergedAttrs = oldProduct.shadow.attributes.merge(formAndShadow.shadow.attributes)
      updated ← * <~ ObjectUtils.update(oldProduct.form.id,
                                        oldProduct.shadow.id,
                                        formAndShadow.form.attributes,
                                        mergedAttrs,
                                        force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(oldProduct.model, updated.shadow, commit)

      albums ← * <~ updateAssociatedAlbums(updatedHead, payload.albums)

      variantLinks ← * <~ ProductOptionLinks.filterLeft(oldProduct.model).result

      hasVariants = variantLinks.nonEmpty || payload.variants.exists(_.nonEmpty)

      updatedSkus ← * <~ findOrCreateVariantsForProduct(oldProduct.model,
                                                        payloadSkus,
                                                        !hasVariants)

      variants ← * <~ updateAssociatedVariants(updatedHead, payload.variants)
      _        ← * <~ validateUpdate(updatedSkus, variants)

      variantAndSkus ← * <~ getVariantsWithRelatedSkus(variants)
      (variantSkus, variantResponses) = variantAndSkus
      taxons ← * <~ TaxonomyManager.getAssignedTaxons(oldProduct.model)
      response = ProductResponse.build(
          IlluminatedProduct.illuminate(oc, updatedHead, updated.form, updated.shadow),
          albums,
          if (hasVariants) variantSkus else updatedSkus,
          variantResponses,
          taxons)
      _ ← * <~ LogActivity
           .fullProductUpdated(Some(admin), response, ObjectContextResponse.build(oc))
    } yield response

  }

  def archiveByContextAndId(
      productId: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[ProductResponse.Root] = {
    val payload = Map("activeFrom" → (("v" → JNull) ~ ("type" → JString("datetime"))),
                      "activeTo" → (("v" → JNull) ~ ("type" → JString("datetime"))))

    val newFormAttrs   = ObjectForm.fromPayload(Product.kind, payload).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(payload).attributes

    for {
      productObject ← * <~ mustFindFullProductByFormId(productId)
      _             ← * <~ productObject.model.mustNotBePresentInCarts
      mergedAttrs = productObject.shadow.attributes.merge(newShadowAttrs)
      inactive ← * <~ ObjectUtils.update(productObject.form.id,
                                         productObject.shadow.id,
                                         newFormAttrs,
                                         mergedAttrs,
                                         force = true)
      commit      ← * <~ ObjectUtils.commit(inactive)
      updatedHead ← * <~ updateHead(productObject.model, inactive.shadow, commit)

      archiveResult ← * <~ Products.update(updatedHead,
                                           updatedHead.copy(archivedAt = Some(Instant.now)))

      albumLinks ← * <~ ProductAlbumLinks.filter(_.leftId === archiveResult.id).result
      _ ← * <~ albumLinks.map { link ⇒
           ProductAlbumLinks.deleteById(link.id,
                                        DbResultT.unit,
                                        id ⇒ NotFoundFailure400(ProductAlbumLinks, id))
         }
      albums   ← * <~ ImageManager.getAlbumsForProduct(inactive.form.id)
      skuLinks ← * <~ ProductVariantLinks.filter(_.leftId === archiveResult.id).result
      _ ← * <~ skuLinks.map { link ⇒
           ProductVariantLinks.deleteById(link.id,
                                          DbResultT.unit,
                                          id ⇒ NotFoundFailure400(ProductVariantLink, id))
         }
      updatedSkus  ← * <~ ProductVariantLinks.queryRightByLeft(archiveResult)
      skus         ← * <~ updatedSkus.map(ProductVariantManager.illuminateVariant)
      variantLinks ← * <~ ProductOptionLinks.filter(_.leftId === archiveResult.id).result
      _ ← * <~ variantLinks.map { link ⇒
           ProductOptionLinks.deleteById(link.id,
                                         DbResultT.unit,
                                         id ⇒ NotFoundFailure400(ProductVariantLink, link.id))
         }
      updatedVariants ← * <~ ProductOptionLinks.queryRightByLeft(archiveResult)
      variants        ← * <~ updatedVariants.map(ProductOptionManager.zipVariantWithValues)
      variantAndSkus  ← * <~ getVariantsWithRelatedSkus(variants)
      (variantSkus, variantResponses) = variantAndSkus
      taxons ← * <~ TaxonomyManager.getAssignedTaxons(productObject.model)
    } yield
      ProductResponse.build(
          product =
            IlluminatedProduct.illuminate(oc, archiveResult, inactive.form, inactive.shadow),
          albums = albums,
          if (variantLinks.nonEmpty) variantSkus else skus,
          variantResponses,
          taxons
      )
  }

  private def getVariantsWithRelatedSkus(
      variants: Seq[FullProductOption])(implicit ec: EC, db: DB, oc: OC)
    : DbResultT[(Seq[ProductVariantResponse.Root], Seq[IlluminatedProductOptionResponse.Root])] = {
    val variantValueIds = variants.flatMap { case (_, variantValue) ⇒ variantValue }
      .map(_.model.id)
    for {
      variantValueSkuCodes ← * <~ ProductOptionManager.getProductValueSkuCodes(variantValueIds)
      variantValueSkuCodesSet = variantValueSkuCodes.values.toSeq.flatten.distinct
      variantSkus ← * <~ variantValueSkuCodesSet.map(skuCode ⇒
                         ProductVariantManager.getBySkuCode(skuCode))
      illuminated = variants.map {
        case (fullVariant, values) ⇒
          val variant = IlluminatedProductOption.illuminate(oc, fullVariant)
          IlluminatedProductOptionResponse.buildLite(variant, values, variantValueSkuCodes)
      }
    } yield (variantSkus, illuminated)
  }

  private def validateCreate(
      payload: CreateProductPayload): ValidatedNel[Failure, CreateProductPayload] = {
    val maxSkus = payload.variants
      .getOrElse(Seq.empty)
      .map(_.values.getOrElse(Seq.empty).length.max(1))
      .product

    (notEmpty(payload.skus, "SKUs") |@| lesserThanOrEqual(payload.skus.length,
                                                          maxSkus,
                                                          "number of SKUs")).map {
      case _ ⇒ payload
    }
  }

  private def validateUpdate(
      skus: Seq[ProductVariantResponse.Root],
      variants: Seq[(FullObject[ProductOption], Seq[FullObject[ProductValue]])])
    : ValidatedNel[Failure, Unit] = {
    val maxSkus = variants.map { case (_, values) ⇒ values.length.max(1) }.product

    lesserThanOrEqual(skus.length, maxSkus, "number of SKUs for given variants").map {
      case _ ⇒ Unit
    }
  }

  private def updateAssociatedVariants(product: Product,
                                       variantsPayload: Option[Seq[ProductOptionPayload]])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[FullProductOption]] =
    variantsPayload match {
      case Some(payloads) ⇒
        findOrCreateVariantsForProduct(product, payloads)
      case None ⇒
        for {
          variants     ← * <~ ProductOptionLinks.queryRightByLeft(product)
          fullVariants ← * <~ variants.map(ProductOptionManager.zipVariantWithValues)
        } yield fullVariants
    }

  private def updateAssociatedAlbums(product: Product, albumsPayload: Option[Seq[AlbumPayload]])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[AlbumRoot]] =
    albumsPayload match {
      case Some(payloads) ⇒
        for {
          albums ← * <~ findOrCreateAlbumsForProduct(product, payloads)
        } yield albums.map { case (album, images) ⇒ AlbumResponse.build(album, images) }
      case None ⇒
        ImageManager.getAlbumsForProductInner(product)
    }

  private def updateHead(product: Product,
                         shadow: ObjectShadow,
                         maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Product] =
    maybeCommit match {
      case Some(commit) ⇒
        Products.update(product, product.copy(shadowId = shadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.good(product)
    }

  private def findOrCreateVariantsForProduct(
      product: Product,
      variantPayloads: Seq[ProductVariantPayload],
      createLinks: Boolean = true)(implicit ec: EC, db: DB, oc: OC, au: AU) =
    variantPayloads.map { payload ⇒
      val albumPayloads = payload.albums.getOrElse(Seq.empty)

      for {
        code    ← * <~ ProductVariantManager.mustGetSkuCode(payload)
        variant ← * <~ ProductVariants.filterByContextAndCode(oc.id, code).one.dbresult
        up ← * <~ variant.map { foundVariant ⇒
              if (foundVariant.archivedAt.isEmpty) {
                for {
                  existingVariant ← * <~ ProductVariantManager.updateInner(foundVariant, payload)
                  _ ← * <~ ProductVariantManager
                       .findOrCreateAlbumsForVariant(existingVariant.model, albumPayloads)
                  _ ← * <~ ProductVariantLinks.syncLinks(product,
                                                         if (createLinks)
                                                           Seq(existingVariant.model)
                                                         else Seq.empty)
                } yield existingVariant
              } else {
                DbResultT.failure(LinkArchivedSkuFailure(Product, product.id, code))
              }
            }.getOrElse {
              for {
                newSku ← * <~ ProductVariantManager.createInner(oc, payload)
                _ ← * <~ ProductVariantManager.findOrCreateAlbumsForVariant(newSku.model,
                                                                            albumPayloads)
                _ ← * <~ ProductVariantLinks.syncLinks(product,
                                                       if (createLinks) Seq(newSku.model)
                                                       else Seq.empty)
              } yield newSku
            }
        albums ← * <~ ImageManager.getAlbumsForVariantInner(code, oc)
      } yield ProductVariantResponse.buildLite(IlluminatedVariant.illuminate(oc, up), albums)
    }

  private def findOrCreateVariantsForProduct(product: Product, payload: Seq[ProductOptionPayload])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[FullProductOption]] =
    for {
      variants ← * <~ payload.map(ProductOptionManager.updateOrCreate(oc, _))
      _ ← * <~ ProductOptionLinks.syncLinks(product, variants.map {
           case (variant, values) ⇒ variant.model
         })
    } yield variants

  private def findOrCreateAlbumsForProduct(product: Product, payload: Seq[AlbumPayload])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[FullAlbumWithImages]] =
    for {
      albums ← * <~ payload.map(ImageManager.updateOrCreateAlbum)
      _ ← * <~ ProductAlbumLinks.syncLinks(product, albums.map {
           case (fullAlbum, _) ⇒ fullAlbum.model
         })
    } yield albums

  def mustFindProductByContextAndFormId404(contextId: Int, formId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[Product] =
    Products
      .filter(_.contextId === contextId)
      .filter(_.formId === formId)
      .mustFindOneOr(ProductFormNotFoundForContext(formId, contextId))

  def getContextsForProduct(formId: Int)(implicit ec: EC,
                                         db: DB): DbResultT[Seq[ObjectContextResponse.Root]] =
    for {
      products   ← * <~ Products.filterByFormId(formId).result
      contextIds ← * <~ products.map(_.contextId)
      contexts   ← * <~ ObjectContexts.filter(_.id.inSet(contextIds)).sortBy(_.id).result
    } yield contexts.map(ObjectContextResponse.build)

  def mustFindFullProductByFormId(
      productId: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[FullObject[Product]] =
    ObjectManager.getFullObject(mustFindProductByContextAndFormId404(oc.id, productId))

  def mustFindFullProductById(productId: Int)(implicit ec: EC,
                                              db: DB): DbResultT[FullObject[Product]] =
    ObjectManager.getFullObject(Products.mustFindById404(productId))

  // This is an inefficient intensely quering method that does the trick
  private def skusToBeUnassociatedMustNotBePresentInCarts(
      productId: Int,
      payloadSkus: Seq[ProductVariantPayload])(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      skuIdsForProduct ← * <~ ProductVariantLinks.filter(_.leftId === productId).result.flatMap {
                          case links @ Seq(_) ⇒
                            lift(links.map(_.rightId))
                          case _ ⇒
                            for {
                              variantLinks ← ProductOptionLinks
                                              .filter(_.leftId === productId)
                                              .result
                              variantIds = variantLinks.map(_.rightId)
                              valueLinks ← ProductOptionValueLinks
                                            .filter(_.leftId.inSet(variantIds))
                                            .result
                              valueIds = valueLinks.map(_.rightId)
                              skuLinks ← ProductValueVariantLinks
                                          .filter(_.leftId.inSet(valueIds))
                                          .result
                            } yield skuLinks.map(_.rightId)
                        }
      skuCodesForProduct ← * <~ ProductVariants
                            .filter(_.id.inSet(skuIdsForProduct))
                            .map(_.code)
                            .result
      skuCodesFromPayload = payloadSkus
        .map(ps ⇒ ProductVariantManager.getSkuCode(ps.attributes))
        .flatten
      skuCodesToBeGone = skuCodesForProduct.diff(skuCodesFromPayload)
      _ ← * <~ (skuCodesToBeGone.map { codeToUnassociate ⇒
               for {
                 skuToUnassociate ← * <~ ProductVariants.mustFindByCode(codeToUnassociate)
                 _                ← * <~ skuToUnassociate.mustNotBePresentInCarts
               } yield {}
             })
    } yield {}
}
