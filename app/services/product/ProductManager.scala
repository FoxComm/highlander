package services.product

import cats.data._
import cats.implicits._
import failures.{GeneralFailure, Failures, Failure}
import failures.ProductFailures._
import models.image._
import models.inventory._
import models.objects._
import models.product._
import payloads.ProductPayloads._
import payloads.SkuPayloads
import payloads.SkuPayloads._
import payloads.VariantPayloads._
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductResponses._
import responses.SkuResponses._
import services.image.ImageManager
import services.inventory.SkuManager
import services.objects.ObjectManager
import services.variant.VariantManager
import slick.driver.PostgresDriver.api._
import utils.Validation._
import utils.aliases._
import utils.db._

object ProductManager {

  private def productSkusAsVariants(
      skus: Seq[SkuPayloads.SkuPayload],
      variantPayload: Seq[VariantPayload]): Failures Xor Seq[VariantPayload] = {
    if (variantPayload.nonEmpty) Xor.right(variantPayload)
    else {
      val skuCodes   = skus.map(sku ⇒ SkuManager.getSkuCode(sku.attributes))
      val hasFailure = skuCodes.contains(None)

      if (hasFailure) {
        Xor.left(GeneralFailure("SKU code not found in payload").single)
      } else {
        val values = skuCodes.map(sku ⇒ VariantValuePayload(None, None, None, Seq(sku.get)))
        Xor.right(Seq(VariantPayload(None, Map(), Some(values))))
      }
    }
  }

  def createProduct(payload: CreateProductPayload)(implicit ec: EC,
                                                   db: DB,
                                                   oc: OC): DbResultT[ProductResponse.Root] = {

    val form            = ObjectForm.fromPayload(Product.kind, payload.attributes)
    val shadow          = ObjectShadow.fromPayload(payload.attributes)
    val variantPayloads = payload.variants.getOrElse(Seq.empty)
    val hasVariants     = variantPayloads.nonEmpty

    for {
      _   ← * <~ validateCreate(payload)
      ins ← * <~ ObjectUtils.insert(form, shadow)
      product ← * <~ Products.create(
                   Product(contextId = oc.id,
                           formId = ins.form.id,
                           shadowId = ins.shadow.id,
                           commitId = ins.commit.id))

      productSkus ← * <~ payload.skus.map(sku ⇒
                         findOrCreateSkuForProduct(product, sku, !hasVariants))
      /*productSkuCodes ← * <~ payload.skus.map(
                           sku ⇒
                             SkuManager
                               .getSkuCode(sku.attributes)
                               .ensuring(_.isDefined, s"Cannot find sku code in payload")
                               .get)*/

      variants ← * <~ variantPayloads.map(variant ⇒
                      findOrCreateVariantForProduct(product, variant))
      variantValueIds = variants.flatMap { case (_, variantValue) ⇒ variantValue }.map(_.model.id)
      variantValueSkuCodes ← * <~ VariantManager.getVariantValueSkuCodes(variantValueIds)
      variantOnlySkuCodes = variantValueSkuCodes.values.toSeq.flatten.distinct
      //.filter(!productSkuCodes.contains(_))
      variantSkus ← * <~ variantOnlySkuCodes.map(skuCode ⇒ SkuManager.getSku(oc.name, skuCode))
    } yield
      ProductResponse.build(product =
                              IlluminatedProduct.illuminate(oc, product, ins.form, ins.shadow),
                            albums = Seq.empty,
                            skus = if (hasVariants) variantSkus else productSkus,
                            variants = variants.map {
                              case (fullVariant, values) ⇒
                                (IlluminatedVariant.illuminate(oc, fullVariant), values)
                            },
                            variantValueSkuCodeLinks = variantValueSkuCodes)
  }

  def getProduct(
      productId: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[ProductResponse.Root] =
    for {
      oldProduct ← * <~ mustFindFullProductById(productId)
      albums     ← * <~ ImageManager.getAlbumsForProduct(oldProduct.form.id)

      skuLinks    ← * <~ ProductSkuLinks.filter(_.leftId === oldProduct.model.id).result
      productSkus ← * <~ skuLinks.map(link ⇒ SkuManager.mustFindIlluminatedSkuById(link.rightId))

      variantLinks ← * <~ ProductVariantLinks.filter(_.leftId === oldProduct.model.id).result

      hasVariants = variantLinks.nonEmpty

      variants ← * <~ variantLinks.map(link ⇒
                      VariantManager.mustFindFullVariantWithValuesById(link.rightId))

      variantValueIds = variants.flatMap { case (_, variantValue) ⇒ variantValue }.map(_.model.id)
      variantValueSkuCodes ← * <~ VariantManager.getVariantValueSkuCodes(variantValueIds)
      variantSkuCodes = variantValueSkuCodes.values.toSeq.flatten.distinct
      variantSkus ← * <~ variantSkuCodes.map(skuCode ⇒ SkuManager.getSku(oc.name, skuCode))
    } yield
      ProductResponse.build(
          product = IlluminatedProduct
            .illuminate(oc, oldProduct.model, oldProduct.form, oldProduct.shadow),
          albums = albums,
          skus = if (hasVariants) variantSkus else productSkus,
          variants = variants.map {
            case (fullVariant, values) ⇒
              (IlluminatedVariant.illuminate(oc, fullVariant), values)
          },
          variantValueSkuCodeLinks = variantValueSkuCodes)

  def updateProduct(productId: Int, payload: UpdateProductPayload)(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[ProductResponse.Root] = {

    val newFormAttrs                 = ObjectForm.fromPayload(Product.kind, payload.attributes).attributes
    val newShadowAttrs               = ObjectShadow.fromPayload(payload.attributes).attributes
    val payloadSkus: Seq[SkuPayload] = payload.skus.getOrElse(Seq.empty)

    for {
      oldProduct ← * <~ mustFindFullProductById(productId)

      mergedAttrs = oldProduct.shadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils.update(oldProduct.form.id,
                                        oldProduct.shadow.id,
                                        newFormAttrs,
                                        mergedAttrs,
                                        force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(oldProduct.model, updated.shadow, commit)

      albums ← * <~ ImageManager.getAlbumsForProduct(updated.form.id)

      skuLinks ← * <~ ProductSkuLinks.filterLeft(oldProduct.model.id).result
      variantLinks ← * <~ ObjectLinks
                      .findByLeftAndType(oldProduct.shadow.id, ObjectLink.ProductVariant)
                      .result

      hasVariants = variantLinks.nonEmpty || payload.variants.isDefined

      deleted ← * <~ (if (skuLinks.nonEmpty && hasVariants)
                        ProductSkuLinks
                          .filterLeft(oldProduct.model.id)
                          .deleteAll(
                              DbResultT.unit,
                              DbResultT.failure(new GeneralFailure(
                                      s"Cannot delete PproductSku links for product ${oldProduct.model.id}")))
                      else DbResultT.unit)

      updatedSkus ← * <~ payloadSkus.map(sku ⇒
                         findOrCreateSkuForProduct(oldProduct.model, sku, !hasVariants))

      variants ← * <~ updateAssociatedVariants(updatedHead, oldProduct.shadow.id, payload.variants)

      _ ← * <~ updateAssociatedAlbums(updatedHead, oldProduct.shadow.id)

      fullProduct = FullObject(updatedHead, updated.form, updated.shadow)
      _ ← * <~ validateUpdate(updatedSkus, variants)

      variantValueIds = variants.flatMap { case (_, variantValue) ⇒ variantValue }.map(_.model.id)
      variantValueSkuCodes ← * <~ VariantManager.getVariantValueSkuCodes(variantValueIds)
      variantValueSkuCodesSet = variantValueSkuCodes.values.toSeq.flatten.distinct
      variantSkus ← * <~ variantValueSkuCodesSet.map(skuCode ⇒ SkuManager.getSku(oc.name, skuCode))
    } yield
      ProductResponse.build(
          product = IlluminatedProduct.illuminate(oc, updatedHead, updated.form, updated.shadow),
          albums = albums,
          skus = if (hasVariants) variantSkus else updatedSkus,
          variants = variants.map {
            case (fullVariant, values) ⇒
              (IlluminatedVariant.illuminate(oc, fullVariant), values)
          },
          variantValueSkuCodeLinks = variantValueSkuCodes)
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

    lesserThanOrEqual(skus.length, maxSkus, "number of SKUs").map {
      case _ ⇒ Unit
    }
  }

  private def updateAssociatedSkus(product: Product, skusPayload: Option[Seq[SkuPayload]])(
      implicit ec: EC,
      db: DB,
      oc: OC) = {

    skusPayload match {
      case Some(payloads) ⇒
        DbResultT.sequence(payloads.map(payload ⇒ findOrCreateSkuForProduct(product, payload)))

      case None ⇒
        for {
          links ← * <~ ProductSkuLinks.filter(_.leftId === product.id).result
          skus  ← * <~ links.map(link ⇒ SkuManager.mustFindIlluminatedSkuById(link.rightId))
        } yield skus
    }
  }

  private def updateAssociatedVariants(
      product: Product,
      shadowId: Int,
      variantsPayload: Option[Seq[VariantPayload]])(implicit ec: EC, db: DB, oc: OC) = {

    variantsPayload match {
      case Some(payloads) ⇒
        DbResultT.sequence(payloads.map(payload ⇒ findOrCreateVariantForProduct(product, payload)))

      case None ⇒
        for {
          links ← * <~ ObjectLinks.findByLeftAndType(shadowId, ObjectLink.ProductVariant).result

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
      variant ← * <~ VariantManager.mustFindFullVariantWithValuesByShadowId(link.rightId)
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
        DbResultT.good(product)
    }

  private def findOrCreateSkuForProduct(
      product: Product,
      payload: SkuPayload,
      createLinks: Boolean = true)(implicit ec: EC, db: DB, oc: OC) = {
    for {
      code ← * <~ SkuManager.mustGetSkuCode(payload)
      sku ← * <~ Skus.filterByContextAndCode(oc.id, code).one.toXor.flatMap {
             case Some(sku) ⇒
               for {
                 existingSku ← * <~ SkuManager.updateSkuInner(sku, payload)
                 link = ProductSkuLink(leftId = product.id, rightId = existingSku.model.id)
                 _ ← * <~ ProductSkuLinks.insertOrUpdate(link)
               } yield existingSku
             case None ⇒
               for {
                 newSku ← * <~ SkuManager.createSkuInner(oc, payload)
                 _ ← * <~ (if (createLinks)
                             ProductSkuLinks.create(
                                 ProductSkuLink(leftId = product.id, rightId = newSku.model.id))
                           else DbResultT.unit)
               } yield newSku)
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

  def mustFindProductByContextAndFormId404(contextId: Int, formId: Int)(
      implicit ec: EC, db: DB): DbResultT[Product] =
    Products
      .filter(_.contextId === contextId)
      .filter(_.formId === formId)
      .mustFindOneOr(ProductFormNotFoundForContext(formId, contextId))

  def mustFindProductByContextAndId404(contextId: Int, productId: Int)(
    implicit ec: EC, db: DB): DbResultT[Product] =
    Products
      .filter(_.contextId === contextId)
      .filter(_.id === productId)
      .mustFindOneOr(ProductNotFoundForContext(productId, contextId))

  def getContextsForProduct(formId: Int)(implicit ec: EC,
                                         db: DB): DbResultT[Seq[ObjectContextResponse.Root]] =
    for {
      products   ← * <~ Products.filterByFormId(formId).result
      contextIds ← * <~ products.map(_.contextId)
      contexts   ← * <~ ObjectContexts.filter(_.id.inSet(contextIds)).sortBy(_.id).result
    } yield contexts.map(ObjectContextResponse.build)

  def mustFindFullProductById(productId: Int)(implicit ec: EC, db: DB, oc: OC) =
    ObjectManager.getFullObject(mustFindProductByContextAndFormId404(oc.id, productId))
}
