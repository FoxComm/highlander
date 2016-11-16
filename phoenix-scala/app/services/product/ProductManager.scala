package services.product

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import cats.data._
import cats.implicits._
import cats.data.ValidatedNel
import cats.std.map
import failures._
import failures.ArchiveFailures._
import failures.ProductFailures._
import models.image.{AlbumImageLinks, Albums}
import models.inventory._
import models.objects._
import models.product._
import models.account._
import models.cord.lineitems.CartLineItems
import payloads.ImagePayloads.UpdateAlbumPositionPayload
import payloads.ProductPayloads._
import payloads.SkuPayloads._
import payloads.VariantPayloads._
import responses.ImageResponses.ImageResponse
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductResponses._
import responses.SkuResponses._
import responses.VariantResponses.IlluminatedVariantResponse
import services.image.ImageManager
import services.inventory.SkuManager
import services.objects.ObjectManager
import services.variant.VariantManager
import services.variant.VariantManager._
import slick.driver.PostgresDriver.api._
import utils.Validation._
import utils.aliases._
import utils.db._
import org.json4s._
import org.json4s.JsonDSL._
import services.LogActivity

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

    for {
      _   ← * <~ validateCreate(payload)
      ins ← * <~ ObjectUtils.insert(form, shadow, payload.schema)
      product ← * <~ Products.create(
                   Product(scope = LTree(au.token.scope),
                           contextId = oc.id,
                           formId = ins.form.id,
                           shadowId = ins.shadow.id,
                           commitId = ins.commit.id))

      productSkus    ← * <~ findOrCreateSkusForProduct(product, payload.skus, !hasVariants)
      variants       ← * <~ findOrCreateVariantsForProduct(product, variantPayloads)
      variantAndSkus ← * <~ getVariantsWithRelatedSkus(variants)
      (variantSkus, variantResponses) = variantAndSkus
      response = ProductResponse.build(
          IlluminatedProduct.illuminate(oc, product, ins.form, ins.shadow),
          Seq.empty,
          if (hasVariants) variantSkus else productSkus,
          variantResponses)
      _ ← * <~ LogActivity
           .fullProductCreated(Some(admin), response, ObjectContextResponse.build(oc))
    } yield response

  }

  def getProduct(
      productId: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[ProductResponse.Root] =
    for {
      oldProduct ← * <~ mustFindFullProductByFormId(productId)
      albums     ← * <~ ImageManager.getAlbumsForProduct(oldProduct.form.id)

      fullSkus    ← * <~ ProductSkuLinks.queryRightByLeft(oldProduct.model)
      productSkus ← * <~ fullSkus.map(SkuManager.illuminateSku)

      variants     ← * <~ ProductVariantLinks.queryRightByLeft(oldProduct.model)
      fullVariants ← * <~ variants.map(VariantManager.zipVariantWithValues)

      hasVariants = variants.nonEmpty

      variantAndSkus ← * <~ getVariantsWithRelatedSkus(fullVariants)
      (variantSkus, variantResponses) = variantAndSkus
    } yield
      ProductResponse.build(
          IlluminatedProduct.illuminate(oc, oldProduct.model, oldProduct.form, oldProduct.shadow),
          albums,
          if (hasVariants) variantSkus else productSkus,
          variantResponses)

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

      albums ← * <~ ImageManager.getAlbumsForProduct(updated.form.id)

      variantLinks ← * <~ ProductVariantLinks.filterLeft(oldProduct.model).result

      hasVariants = variantLinks.nonEmpty || payload.variants.exists(_.nonEmpty)

      updatedSkus ← * <~ findOrCreateSkusForProduct(oldProduct.model, payloadSkus, !hasVariants)

      variants ← * <~ updateAssociatedVariants(updatedHead, payload.variants)
      _        ← * <~ validateUpdate(updatedSkus, variants)

      variantAndSkus ← * <~ getVariantsWithRelatedSkus(variants)
      (variantSkus, variantResponses) = variantAndSkus
      response = ProductResponse.build(
          IlluminatedProduct.illuminate(oc, updatedHead, updated.form, updated.shadow),
          albums,
          if (hasVariants) variantSkus else updatedSkus,
          variantResponses)
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
      skuLinks ← * <~ ProductSkuLinks.filter(_.leftId === archiveResult.id).result
      _ ← * <~ skuLinks.map { link ⇒
           ProductSkuLinks.deleteById(link.id,
                                      DbResultT.unit,
                                      id ⇒ NotFoundFailure400(ProductSkuLink, id))
         }
      updatedSkus  ← * <~ ProductSkuLinks.queryRightByLeft(archiveResult)
      skus         ← * <~ updatedSkus.map(SkuManager.illuminateSku)
      variantLinks ← * <~ ProductVariantLinks.filter(_.leftId === archiveResult.id).result
      _ ← * <~ variantLinks.map { link ⇒
           ProductVariantLinks.deleteById(link.id,
                                          DbResultT.unit,
                                          id ⇒ NotFoundFailure400(ProductSkuLink, link.id))
         }
      updatedVariants ← * <~ ProductVariantLinks.queryRightByLeft(archiveResult)
      variants        ← * <~ updatedVariants.map(VariantManager.zipVariantWithValues)
      variantAndSkus  ← * <~ getVariantsWithRelatedSkus(variants)
      (variantSkus, variantResponses) = variantAndSkus
    } yield
      ProductResponse.build(
          product =
            IlluminatedProduct.illuminate(oc, archiveResult, inactive.form, inactive.shadow),
          albums = albums,
          if (variantLinks.nonEmpty) variantSkus else skus,
          variantResponses
      )
  }

  private def getVariantsWithRelatedSkus(variants: Seq[FullVariant])(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[(Seq[SkuResponse.Root], Seq[IlluminatedVariantResponse.Root])] = {
    val variantValueIds = variants.flatMap { case (_, variantValue) ⇒ variantValue }
      .map(_.model.id)
    for {
      variantValueSkuCodes ← * <~ VariantManager.getVariantValueSkuCodes(variantValueIds)
      variantValueSkuCodesSet = variantValueSkuCodes.values.toSeq.flatten.distinct
      variantSkus ← * <~ variantValueSkuCodesSet.map(skuCode ⇒ SkuManager.getSku(skuCode))
      illuminated = variants.map {
        case (fullVariant, values) ⇒
          val variant = IlluminatedVariant.illuminate(oc, fullVariant)
          IlluminatedVariantResponse.buildLite(variant, values, variantValueSkuCodes)
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

  private def validateUpdate(skus: Seq[SkuResponse.Root],
                             variants: Seq[(FullObject[Variant], Seq[FullObject[VariantValue]])])
    : ValidatedNel[Failure, Unit] = {
    val maxSkus = variants.map { case (_, values) ⇒ values.length.max(1) }.product

    lesserThanOrEqual(skus.length, maxSkus, "number of SKUs for given variants").map {
      case _ ⇒ Unit
    }
  }

  private def updateAssociatedVariants(product: Product,
                                       variantsPayload: Option[Seq[VariantPayload]])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[FullVariant]] =
    variantsPayload match {
      case Some(payloads) ⇒
        findOrCreateVariantsForProduct(product, payloads)
      case None ⇒
        for {
          variants     ← * <~ ProductVariantLinks.queryRightByLeft(product)
          fullVariants ← * <~ variants.map(VariantManager.zipVariantWithValues)
        } yield fullVariants
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

  private def findOrCreateSkusForProduct(
      product: Product,
      skuPayloads: Seq[SkuPayload],
      createLinks: Boolean = true)(implicit ec: EC, db: DB, oc: OC, au: AU) =
    skuPayloads.map { payload ⇒
      for {
        code ← * <~ SkuManager.mustGetSkuCode(payload)
        sku  ← * <~ Skus.filterByContextAndCode(oc.id, code).one.dbresult
        up ← * <~ sku.map { foundSku ⇒
              if (foundSku.archivedAt.isEmpty) {
                for {
                  existingSku ← * <~ SkuManager.updateSkuInner(foundSku, payload)
                  _ ← * <~ ProductSkuLinks.syncLinks(product,
                                                     if (createLinks) Seq(existingSku.model)
                                                     else Seq.empty)
                } yield existingSku
              } else {
                DbResultT.failure(LinkArchivedSkuFailure(Product, product.id, code))
              }
            }.getOrElse {
              for {
                newSku ← * <~ SkuManager.createSkuInner(oc, payload)
                _ ← * <~ ProductSkuLinks.syncLinks(product,
                                                   if (createLinks) Seq(newSku.model)
                                                   else Seq.empty)
              } yield newSku
            }
        albums ← * <~ ImageManager.getAlbumsForSkuInner(up.model.formId, oc)
      } yield SkuResponse.buildLite(IlluminatedSku.illuminate(oc, up), albums)
    }

  private def findOrCreateVariantsForProduct(product: Product, payload: Seq[VariantPayload])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[FullVariant]] =
    for {
      variants ← * <~ payload.map(VariantManager.updateOrCreateVariant(oc, _))
      _ ← * <~ ProductVariantLinks.syncLinks(product, variants.map {
           case (variant, values) ⇒ variant.model
         })
    } yield variants

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
      payloadSkus: Seq[SkuPayload])(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      skuIdsForProduct ← * <~ ProductSkuLinks.filter(_.leftId === productId).result.flatMap {
                          case links @ Seq(_) ⇒
                            lift(links.map(_.rightId))
                          case _ ⇒
                            for {
                              variantLinks ← ProductVariantLinks
                                              .filter(_.leftId === productId)
                                              .result
                              variantIds = variantLinks.map(_.rightId)
                              valueLinks ← VariantValueLinks
                                            .filter(_.leftId.inSet(variantIds))
                                            .result
                              valueIds = valueLinks.map(_.rightId)
                              skuLinks ← VariantValueSkuLinks
                                          .filter(_.leftId.inSet(valueIds))
                                          .result
                            } yield skuLinks.map(_.rightId)
                        }
      skuCodesForProduct ← * <~ Skus.filter(_.id.inSet(skuIdsForProduct)).map(_.code).result
      skuCodesToBeGone = skuCodesForProduct.diff(
          payloadSkus.map(ps ⇒ SkuManager.getSkuCode(ps.attributes)).filter(_.nonEmpty))
      _ ← * <~ (skuCodesToBeGone.map { codeToUnassociate ⇒
               for {
                 skuToUnassociate ← * <~ Skus.mustFindByCode(codeToUnassociate)
                 _                ← * <~ skuToUnassociate.mustNotBePresentInCarts
               } yield {}
             })
    } yield {}
}
