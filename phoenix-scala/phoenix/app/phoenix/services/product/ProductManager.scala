package phoenix.services.product

import java.time.Instant

import cats._
import cats.data._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import core.failures._
import objectframework.ObjectResponses.ObjectContextResponse
import objectframework.ObjectUtils
import objectframework.models._
import objectframework.services.ObjectManager
import org.json4s.JsonDSL._
import org.json4s._
import phoenix.failures.ArchiveFailures._
import phoenix.failures.ProductFailures
import phoenix.failures.ProductFailures._
import phoenix.models.account._
import phoenix.models.inventory._
import phoenix.models.objects._
import phoenix.models.product._
import phoenix.payloads.ImagePayloads.AlbumPayload
import phoenix.payloads.ProductPayloads._
import phoenix.payloads.SkuPayloads._
import phoenix.payloads.VariantPayloads._
import phoenix.responses.AlbumResponses.AlbumResponse
import phoenix.responses.AlbumResponses._
import phoenix.responses.ProductResponses._
import phoenix.responses.SkuResponses._
import phoenix.responses.VariantResponses.IlluminatedVariantResponse
import phoenix.services.LogActivity
import phoenix.services.image.ImageManager
import phoenix.services.image.ImageManager.FullAlbumWithImages
import phoenix.services.inventory.SkuManager
import phoenix.services.taxonomy.TaxonomyManager
import phoenix.services.variant.VariantManager
import phoenix.services.variant.VariantManager._
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._
import core.utils.Validation._
import core.db._
import phoenix.utils.apis.Apis

object ProductManager extends LazyLogging {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def createProduct(admin: User, payload: CreateProductPayload)(implicit ec: EC,
                                                                db: DB,
                                                                ac: AC,
                                                                oc: OC,
                                                                au: AU,
                                                                apis: Apis): DbResultT[ProductResponse] = {

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
                         slug = payload.slug,
                         contextId = oc.id,
                         formId = ins.form.id,
                         shadowId = ins.shadow.id,
                         commitId = ins.commit.id))

      albums         ← * <~ findOrCreateAlbumsForProduct(product, albumPayloads)
      productSkus    ← * <~ findOrCreateSkusForProduct(product, payload.skus, !hasVariants)
      variants       ← * <~ findOrCreateVariantsForProduct(product, variantPayloads)
      variantAndSkus ← * <~ getVariantsWithRelatedSkus(variants)
      (variantSkus, variantResponses) = variantAndSkus
      taxons ← * <~ TaxonomyManager.getAssignedTaxons(product)
      response = ProductResponse.build(
        IlluminatedProduct.illuminate(oc, product, ins.form, ins.shadow),
        albums.map(AlbumResponse.build),
        if (hasVariants) variantSkus else productSkus,
        variantResponses,
        taxons
      )
      _ ← * <~ LogActivity()
           .withScope(scope)
           .fullProductCreated(Some(admin), response, ObjectContextResponse.build(oc))
    } yield response

  }

  def getProduct(productId: ProductReference,
                 checkActive: Boolean)(implicit ec: EC, db: DB, oc: OC): DbResultT[ProductResponse] =
    for {
      oldProduct ← * <~ Products.mustFindFullByReference(productId)
      illuminated = IlluminatedProduct
        .illuminate(oc, oldProduct.model, oldProduct.form, oldProduct.shadow)
      _ ← when(
           checkActive, {
             illuminated.mustBeActive match {
               case Left(err) ⇒ {
                 logger.warn(err.toString)
                 DbResultT.failure[Unit](NotFoundFailure404(Product, oldProduct.model.slug))
               }
               case Right(_) ⇒ ().pure[DbResultT]
             }
           }
         )
      albums ← * <~ ImageManager.getAlbumsForProduct(oldProduct.model.reference)

      fullSkus    ← * <~ ProductSkuLinks.queryRightByLeft(oldProduct.model)
      productSkus ← * <~ fullSkus.map(SkuManager.illuminateSku)

      variants     ← * <~ ProductVariantLinks.queryRightByLeft(oldProduct.model)
      fullVariants ← * <~ variants.map(VariantManager.zipVariantWithValues)

      hasVariants = variants.nonEmpty

      variantAndSkus ← * <~ getVariantsWithRelatedSkus(fullVariants)
      (variantSkus, variantResponses) = variantAndSkus
      skus                            = if (hasVariants) variantSkus else productSkus
      _ ← * <~ failIf(
           checkActive && !skus.exists(_.isActive), {
             logger.warn(
               s"Product variants for product with id=${oldProduct.model.slug} is archived or inactive")
             NotFoundFailure404(Product, oldProduct.model.slug)
           }
         )
      taxons ← * <~ TaxonomyManager.getAssignedTaxons(oldProduct.model)
    } yield ProductResponse.build(illuminated, albums, skus, variantResponses, taxons)

  def updateProduct(productId: ProductReference, payload: UpdateProductPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      oc: OC,
      au: AU,
      apis: Apis): DbResultT[ProductResponse] = {

    val formAndShadow = FormAndShadow.fromPayload(Product.kind, payload.attributes)

    val payloadSkus = payload.skus.getOrElse(Seq.empty)

    for {
      _          ← * <~ validateUpdate(payload)
      oldProduct ← * <~ Products.mustFindFullByReference(productId)

      _ ← * <~ skusToBeUnassociatedMustNotBePresentInCarts(oldProduct.model.id, payloadSkus)

      mergedAttrs = oldProduct.shadow.attributes.merge(formAndShadow.shadow.attributes)
      updated ← * <~ ObjectUtils.update(oldProduct.form.id,
                                        oldProduct.shadow.id,
                                        formAndShadow.form.attributes,
                                        mergedAttrs,
                                        force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(oldProduct.model, updated.shadow, commit, payload.slug)

      albums ← * <~ updateAssociatedAlbums(updatedHead, payload.albums)

      variantLinks ← * <~ ProductVariantLinks.filterLeft(oldProduct.model).result

      hasVariants = variantLinks.nonEmpty || payload.variants.exists(_.nonEmpty)

      updatedSkus ← * <~ findOrCreateSkusForProduct(oldProduct.model, payloadSkus, !hasVariants)

      variants ← * <~ updateAssociatedVariants(updatedHead, payload.variants)
      _        ← * <~ validateSkuMatchesVariants(updatedSkus, variants)

      variantAndSkus ← * <~ getVariantsWithRelatedSkus(variants)
      (variantSkus, variantResponses) = variantAndSkus
      taxons ← * <~ TaxonomyManager.getAssignedTaxons(oldProduct.model)
      response = ProductResponse.build(
        IlluminatedProduct.illuminate(oc, updatedHead, updated.form, updated.shadow),
        albums,
        if (hasVariants) variantSkus else updatedSkus,
        variantResponses,
        taxons)
      _ ← * <~ LogActivity()
           .fullProductUpdated(Some(au.model), response, ObjectContextResponse.build(oc))
    } yield response

  }

  def archiveByContextAndId(
      productId: ProductReference)(implicit ec: EC, db: DB, oc: OC): DbResultT[ProductResponse] = {
    val payload = Map("activeFrom" → (("v" → JNull) ~ ("t" → JString("datetime"))),
                      "activeTo" → (("v" → JNull) ~ ("t" → JString("datetime"))))

    val newFormAttrs   = ObjectForm.fromPayload(Product.kind, payload).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(payload).attributes

    for {
      productObject ← * <~ Products.mustFindFullByReference(productId)
      _             ← * <~ productObject.model.mustNotBePresentInCarts
      mergedAttrs = productObject.shadow.attributes.merge(newShadowAttrs)
      inactive ← * <~ ObjectUtils.update(productObject.form.id,
                                         productObject.shadow.id,
                                         newFormAttrs,
                                         mergedAttrs,
                                         force = true)
      commit      ← * <~ ObjectUtils.commit(inactive)
      updatedHead ← * <~ updateHead(productObject.model, inactive.shadow, commit, None)

      archiveResult ← * <~ Products.update(updatedHead, updatedHead.copy(archivedAt = Some(Instant.now)))

      albumLinks      ← * <~ ProductAlbumLinks.filterLeft(archiveResult).result
      _               ← * <~ albumLinks.map(l ⇒ ProductAlbumLinks.update(l, l.copy(archivedAt = Some(Instant.now))))
      albums          ← * <~ ImageManager.getAlbumsForProduct(ProductReference(inactive.form.id))
      skuLinks        ← * <~ ProductSkuLinks.filterLeft(archiveResult).result
      _               ← * <~ skuLinks.map(l ⇒ ProductSkuLinks.update(l, l.copy(archivedAt = Some(Instant.now))))
      updatedSkus     ← * <~ ProductSkuLinks.queryRightByLeft(archiveResult)
      skus            ← * <~ updatedSkus.map(SkuManager.illuminateSku)
      variantLinks    ← * <~ ProductVariantLinks.filterLeft(archiveResult).result
      _               ← * <~ variantLinks.map(l ⇒ ProductVariantLinks.update(l, l.copy(archivedAt = Some(Instant.now))))
      updatedVariants ← * <~ ProductVariantLinks.queryRightByLeft(archiveResult)
      variants        ← * <~ updatedVariants.map(VariantManager.zipVariantWithValues)
      variantAndSkus  ← * <~ getVariantsWithRelatedSkus(variants)
      (variantSkus, variantResponses) = variantAndSkus
      taxons ← * <~ TaxonomyManager.getAssignedTaxons(productObject.model)
    } yield
      ProductResponse.build(
        product = IlluminatedProduct.illuminate(oc, archiveResult, inactive.form, inactive.shadow),
        albums = albums,
        if (variantLinks.length > 0) variantSkus else skus,
        variantResponses,
        taxons
      )
  }

  private def getVariantsWithRelatedSkus(variants: Seq[FullVariant])(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[(Seq[SkuResponse], Seq[IlluminatedVariantResponse])] = {
    val variantValueIds = variants
      .flatMap { case (_, variantValue) ⇒ variantValue }
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

  private def validateCreate(payload: CreateProductPayload): ValidatedNel[Failure, CreateProductPayload] = {
    val maxSkus = payload.variants
      .getOrElse(Seq.empty)
      .map(_.values.getOrElse(Seq.empty).length.max(1))
      .product

    (notEmpty(payload.skus, "SKUs") |@| lesserThanOrEqual(payload.skus.length, maxSkus, "number of SKUs") |@|
      validateSlug(payload.slug, true)).map {
      case _ ⇒ payload
    }
  }

  private def validateUpdate(payload: UpdateProductPayload): ValidatedNel[Failure, UpdateProductPayload] =
    payload.slug.fold(ok)(value ⇒ validateSlug(value)).map { case _ ⇒ payload }

  private def validateSlug(slug: String, forProductCreate: Boolean = false): ValidatedNel[Failure, Unit] =
    if (slug.isEmpty && forProductCreate || slug.exists(_.isLetter)) {
      ok
    } else {
      Validated.invalidNel(ProductFailures.SlugShouldHaveLetters(slug))
    }

  private def validateSkuMatchesVariants(
      skus: Seq[SkuResponse],
      variants: Seq[(FullObject[Variant], Seq[FullObject[VariantValue]])]): ValidatedNel[Failure, Unit] = {
    val maxSkus = variants.map { case (_, values) ⇒ values.length.max(1) }.product

    lesserThanOrEqual(skus.length, maxSkus, "number of SKUs for given variants").map {
      case _ ⇒ Unit
    }
  }

  private def updateAssociatedVariants(product: Product, variantsPayload: Option[Seq[VariantPayload]])(
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

  private def updateAssociatedAlbums(product: Product, albumsPayload: Option[Seq[AlbumPayload]])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[AlbumResponse]] =
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
                         maybeCommit: Option[ObjectCommit],
                         newSlug: Option[String])(implicit ec: EC): DbResultT[Product] = {

    def withNewSlug =
      (product: Product) ⇒ newSlug.fold(product)(value ⇒ product.copy(slug = value))
    def withCommit =
      (product: Product) ⇒
        maybeCommit.fold(product)(commit ⇒ product.copy(shadowId = shadow.id, commitId = commit.id))

    val newProduct = withNewSlug.andThen(withCommit)(product)

    if (newProduct != product) Products.update(product, newProduct)
    else product.pure[DbResultT]
  }

  private def findOrCreateSkusForProduct(
      product: Product,
      skuPayloads: Seq[SkuPayload],
      createLinks: Boolean = true)(implicit ec: EC, db: DB, oc: OC, au: AU, apis: Apis) =
    skuPayloads.map { payload ⇒
      val albumPayloads = payload.albums.getOrElse(Seq.empty)

      for {
        code ← * <~ SkuManager.mustGetSkuCode(payload)
        sku  ← * <~ Skus.filterByContextAndCode(oc.id, code).one.dbresult
        up ← * <~ sku
              .map { foundSku ⇒
                if (foundSku.archivedAt.isEmpty) {
                  for {
                    existingSku ← * <~ SkuManager.updateSkuInner(foundSku, payload)
                    _           ← * <~ SkuManager.findOrCreateAlbumsForSku(existingSku.model, albumPayloads)
                    _ ← * <~ ProductSkuLinks.syncLinks(product,
                                                       if (createLinks) Seq(existingSku.model)
                                                       else Seq.empty)
                  } yield existingSku
                } else {
                  DbResultT.failure(LinkInactiveSkuFailure(Product, product.id, code))
                }
              }
              .getOrElse {
                for {
                  newSku ← * <~ SkuManager.createSkuInner(oc, payload)
                  _      ← * <~ SkuManager.findOrCreateAlbumsForSku(newSku.model, albumPayloads)
                  _ ← * <~ ProductSkuLinks.syncLinks(product,
                                                     if (createLinks) Seq(newSku.model)
                                                     else Seq.empty)
                } yield newSku
              }
        albums ← * <~ ImageManager.getAlbumsForSkuInner(code, oc)
      } yield SkuResponse.buildLite(IlluminatedSku.illuminate(oc, up), albums)
    }

  private def findOrCreateVariantsForProduct(
      product: Product,
      payload: Seq[VariantPayload])(implicit ec: EC, db: DB, oc: OC, au: AU): DbResultT[Seq[FullVariant]] =
    for {
      variants ← * <~ payload.map(VariantManager.updateOrCreateVariant(oc, _))
      _ ← * <~ ProductVariantLinks.syncLinks(product, variants.map {
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

  def mustFindProductByContextAndFormId404(contextId: Int, formId: Int)(implicit ec: EC,
                                                                        db: DB): DbResultT[Product] =
    Products
      .filter(_.contextId === contextId)
      .filter(_.formId === formId)
      .mustFindOneOr(ProductFormNotFoundForContext(formId, contextId))

  def getContextsForProduct(formId: Int)(implicit ec: EC, db: DB): DbResultT[Seq[ObjectContextResponse]] =
    for {
      products   ← * <~ Products.filterByFormId(formId).result
      contextIds ← * <~ products.map(_.contextId)
      contexts   ← * <~ ObjectContexts.filter(_.id.inSet(contextIds)).sortBy(_.id).result
    } yield contexts.map(ObjectContextResponse.build)

  def mustFindFullProductByFormId(
      productId: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[FullObject[Product]] =
    ObjectManager.getFullObject(mustFindProductByContextAndFormId404(oc.id, productId))

  def mustFindFullProductById(productId: Int)(implicit ec: EC, db: DB): DbResultT[FullObject[Product]] =
    ObjectManager.getFullObject(Products.mustFindById404(productId))

  // This is an inefficient intensely querying method that does the trick
  private def skusToBeUnassociatedMustNotBePresentInCarts(productId: Int, payloadSkus: Seq[SkuPayload])(
      implicit ec: EC,
      db: DB): DbResultT[Unit] =
    for {
      skuIdsForProduct ← * <~ ProductSkuLinks.filter(_.leftId === productId).result.flatMap {
                          case links @ Seq(_) ⇒
                            links.map(_.rightId).pure[DBIO]
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
      skuCodesFromPayload = payloadSkus.map(ps ⇒ SkuManager.getSkuCode(ps.attributes)).flatten
      skuCodesToBeGone    = skuCodesForProduct.diff(skuCodesFromPayload)
      _ ← * <~ (skuCodesToBeGone.map { codeToUnassociate ⇒
           for {
             skuToUnassociate ← * <~ Skus.mustFindByCode(codeToUnassociate)
             _                ← * <~ skuToUnassociate.mustNotBePresentInCarts
           } yield ()
         })
    } yield ()
}
