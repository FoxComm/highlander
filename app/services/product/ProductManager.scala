package services.product

import cats.data._
import failures.Failure
import failures.ProductFailures._
import models.image._
import models.inventory._
import models.objects._
import models.product._
import payloads.ProductPayloads._
import payloads.SkuPayloads._
import payloads.VariantPayloads._
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductResponses._
import responses.SkuResponses._
import services.Result
import services.image.ImageManager
import services.inventory.SkuManager
import services.variant.VariantManager
import slick.driver.PostgresDriver.api._
import utils.Validation._
import utils.aliases._
import utils.db._

object ProductManager {

  def createProduct(payload: CreateProductPayload)(implicit ec: EC,
                                                   db: DB,
                                                   oc: OC): DbResultT[ProductResponse.Root] = {

    val form            = ObjectForm.fromPayload(Product.kind, payload.attributes)
    val shadow          = ObjectShadow.fromPayload(payload.attributes)
    val variantPayloads = payload.variants.getOrElse(Seq.empty)

    for {
      _   ← * <~ payload.validate
      ins ← * <~ ObjectUtils.insert(form, shadow)
      product ← * <~ Products.create(
                   Product(contextId = oc.id,
                           formId = ins.form.id,
                           shadowId = ins.shadow.id,
                           commitId = ins.commit.id))

      skus ← * <~ payload.skus.map(sku ⇒ findOrCreateSkuForProduct(product, sku))
      variants ← * <~ variantPayloads.map(variant ⇒
                      findOrCreateVariantForProduct(product, variant))
    } yield
      ProductResponse.build(product =
                              IlluminatedProduct.illuminate(oc, product, ins.form, ins.shadow),
                            albums = Seq.empty,
                            skus = skus,
                            variants = variants.map {
                              case (fullVariant, values) ⇒
                                (IlluminatedVariant.illuminate(oc, fullVariant), values)
                            },
                            variantMap = Map.empty)
  }

  def getProduct(
      productId: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[ProductResponse.Root] =
    for {
      product ← * <~ mustFindProductByContextAndId404(oc.id, productId)
      form    ← * <~ ObjectForms.mustFindById404(product.formId)
      shadow  ← * <~ ObjectShadows.mustFindById404(product.shadowId)
      albums  ← * <~ ImageManager.getAlbumsForProduct(form.id)

      skuLinks ← * <~ ProductSkuLinks.filter(_.leftId === product.id).result
      skus     ← * <~ skuLinks.map(link ⇒ mustFindFullSkuById(link.rightId))

      variantLinks ← * <~ ProductVariantLinks.filter(_.leftId === product.id).result
      variants     ← * <~ variantLinks.map(link ⇒ mustFindFullVariantById(link.rightId))
    } yield
      ProductResponse.build(product = IlluminatedProduct.illuminate(oc, product, form, shadow),
                            albums = albums,
                            skus = skus,
                            variants = variants.map {
                              case (fullVariant, values) ⇒
                                (IlluminatedVariant.illuminate(oc, fullVariant), values)
                            },
                            variantMap = Map.empty)

  def updateProduct(productId: Int, payload: UpdateProductPayload)(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[ProductResponse.Root] = {

    val newFormAttrs   = ObjectForm.fromPayload(Product.kind, payload.attributes).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(payload.attributes).attributes

    for {
      product   ← * <~ mustFindProductByContextAndId404(oc.id, productId)
      oldForm   ← * <~ ObjectForms.mustFindById404(product.formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(product.shadowId)

      mergedAttrs = oldShadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils
                 .update(oldForm.id, oldShadow.id, newFormAttrs, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(product, updated.shadow, commit)

      albums ← * <~ ImageManager.getAlbumsForProduct(updated.form.id)

      skus ← * <~ updateAssociatedSkus(updatedHead, oldShadow.id, payload.skus)

      variants ← * <~ updateAssociatedVariants(updatedHead, oldShadow.id, payload.variants)

      _ ← * <~ updateAssociatedAlbums(updatedHead, oldShadow.id)

      fullProduct = FullObject(updatedHead, updated.form, updated.shadow)
      _ ← * <~ validateUpdate(fullProduct, skus, variants)
    } yield
      ProductResponse.build(
          product = IlluminatedProduct.illuminate(oc, updatedHead, updated.form, updated.shadow),
          albums = albums,
          skus = skus,
          variants = variants.map {
            case (fullVariant, values) ⇒
              (IlluminatedVariant.illuminate(oc, fullVariant), values)
          },
          variantMap = Map.empty)
  }

  private def validateUpdate(product: FullObject[Product],
                             skus: Seq[SkuResponse.Root],
                             variants: Seq[(FullObject[Variant], Seq[FullObject[VariantValue]])])
    : ValidatedNel[Failure, Unit] = {

    val maxSkus = variants.foldLeft(1) {
      case (acc, (_, values)) ⇒
        if (values.nonEmpty) acc * values.length
        else acc
    }

    lesserThanOrEqual(skus.length, maxSkus, "number of SKUs").map {
      case _ ⇒ Unit
    }
  }

  private def updateAssociatedSkus(
      product: Product,
      oldProductShadowId: Int,
      skusPayload: Option[Seq[SkuPayload]])(implicit ec: EC, db: DB, oc: OC) = {

    skusPayload match {
      case Some(payloads) ⇒
        DbResultT.sequence(payloads.map(payload ⇒ findOrCreateSkuForProduct(product, payload)))

      case None ⇒
        for {
          links ← * <~ ProductSkuLinks.filter(_.leftId === product.id).result
          skus  ← * <~ links.map(link ⇒ mustFindFullSkuById(link.rightId))
        } yield skus
    }
  }

  private def updateAssociatedVariants(
      product: Product,
      oldProductShadowId: Int,
      variantsPayload: Option[Seq[VariantPayload]])(implicit ec: EC, db: DB, oc: OC) = {

    variantsPayload match {
      case Some(payloads) ⇒
        DbResultT.sequence(payloads.map(payload ⇒ findOrCreateVariantForProduct(product, payload)))

      case None ⇒
        for {
          links ← * <~ ObjectLinks
                   .findByLeftAndType(oldProductShadowId, ObjectLink.ProductVariant)
                   .result

          variants ← * <~ links.map(link ⇒ updateAssociatedVariant(product.shadowId, link))
        } yield variants
    }
  }

  private def updateAssociatedVariant(newShadowId: Int,
                                      variantLink: ObjectLink)(implicit ec: EC, db: DB, oc: OC) =
    for {
      link ← * <~ ObjectUtils.updateAssociatedRight(Variants, variantLink, newShadowId)
      valLinks ← * <~ ObjectLinks
                  .findByLeftAndType(variantLink.rightId, ObjectLink.VariantValue)
                  .result
      _       ← * <~ ObjectUtils.updateAssociatedRights(VariantValues, valLinks, link.rightId)
      variant ← * <~ mustFindFullVariantByShadowId(link.rightId)
    } yield variant

  private def updateAssociatedAlbums(product: Product,
                                     oldProductShadowId: Int)(implicit ec: EC, db: DB, oc: OC) = {

    for {
      existingLinks ← * <~ ObjectLinks
                       .findByLeftAndType(oldProductShadowId, ObjectLink.ProductAlbum)
                       .result
      _ ← * <~ ObjectUtils.updateAssociatedRights(Albums, existingLinks, product.shadowId)
    } yield {}
  }

  private def updateHead(product: Product,
                         shadow: ObjectShadow,
                         maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Product] =
    maybeCommit match {
      case Some(commit) ⇒
        Products.update(product, product.copy(shadowId = shadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.rightLift(product)
    }

  private def findOrCreateSkuForProduct(product: Product,
                                        payload: SkuPayload)(implicit ec: EC, db: DB, oc: OC) = {

    for {
      code ← * <~ SkuManager.mustGetSkuCode(payload)
      sku ← * <~ Skus.filterByContextAndCode(oc.id, code).one.flatMap {
             case Some(sku) ⇒
               SkuManager.updateSkuInner(sku, payload).value
             case None ⇒
               (for {
                 newSku ← * <~ SkuManager.createSkuInner(oc, payload)
                 _ ← * <~ ProductSkuLinks.create(
                        ProductSkuLink(leftId = product.id, rightId = newSku.model.id))
               } yield newSku).value
           }
      albums ← * <~ ImageManager.getAlbumsForSkuInner(code, oc)
    } yield SkuResponse.buildLite(IlluminatedSku.illuminate(oc, sku), albums)
  }

  private def findOrCreateVariantForProduct(product: Product, payload: VariantPayload)(
      implicit ec: EC,
      db: DB,
      oc: OC) = {

    for {
      variant ← * <~ VariantManager.updateOrCreateVariant(oc, payload)
      (fullVariant, _) = variant
      _ ← * <~ ObjectLinks.create(
             ObjectLink(leftId = product.shadowId,
                        rightId = fullVariant.shadow.id,
                        linkType = ObjectLink.ProductVariant))
    } yield variant
  }

  private def mustFindFullSkuById(
      id: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[SkuResponse.Root] =
    for {
      sku    ← * <~ Skus.filter(_.id === id).mustFindOneOr(SkuNotFound(id))
      shadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      form   ← * <~ ObjectForms.mustFindById404(sku.formId)
      fullSku = FullObject(sku, form, shadow)
      albums ← * <~ ImageManager.getAlbumsForSkuInner(sku.code, oc)
    } yield SkuResponse.buildLite(IlluminatedSku.illuminate(oc, fullSku), albums)

  private def mustFindFullVariantById(id: Int)(implicit ec: EC, db: DB, oc: OC) =
    for {
      variant ← * <~ Variants.filter(_.id === id).mustFindOneOr(VariantNotFound(id))
      shadow  ← * <~ ObjectShadows.mustFindById404(variant.shadowId)
      form    ← * <~ ObjectForms.mustFindById404(variant.formId)
      fullVariant = FullObject(variant, form, shadow)
      links ← * <~ ObjectLinks.findByLeftAndType(shadow.id, ObjectLink.VariantValue).result
      values ← * <~ links.map(link ⇒
                    VariantManager.mustFindVariantValueByContextAndShadow(oc.id, link.rightId))
    } yield (fullVariant, values)

  private def mustFindFullVariantByShadowId(shadowId: Int)(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[(FullObject[Variant], Seq[FullObject[VariantValue]])] =
    for {
      variant ← * <~ VariantManager.mustFindVariantByContextAndShadow(oc.id, shadowId)
      links   ← * <~ ObjectLinks.findByLeftAndType(shadowId, ObjectLink.VariantValue).result
      values ← * <~ links.map(link ⇒
                    VariantManager.mustFindVariantValueByContextAndShadow(oc.id, link.rightId))
    } yield (variant, values)

  def mustFindProductByContextAndId404(contextId: Int, productId: Int)(implicit ec: EC,
                                                                       db: DB): DbResult[Product] =
    Products
      .filter(_.contextId === contextId)
      .filter(_.formId === productId)
      .mustFindOneOr(ProductNotFoundForContext(productId, contextId))

  def getContextsForProduct(formId: Int)(implicit ec: EC,
                                         db: DB): Result[Seq[ObjectContextResponse.Root]] =
    (for {
      products   ← * <~ Products.filterByFormId(formId).result
      contextIds ← * <~ products.map(_.contextId)
      contexts   ← * <~ ObjectContexts.filter(_.id.inSet(contextIds)).sortBy(_.id).result
    } yield contexts.map(ObjectContextResponse.build)).run()
}
