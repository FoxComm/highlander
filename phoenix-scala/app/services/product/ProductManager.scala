package services.product

import java.time.Instant

import cats.data.{ValidatedNel, _}
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import failures.ArchiveFailures._
import failures.ProductFailures._
import failures._
import models.account._
import models.inventory._
import models.objects._
import models.product._
import org.json4s.JsonDSL._
import org.json4s._
import payloads.ImagePayloads.AlbumPayload
import payloads.ProductOptionPayloads._
import payloads.ProductPayloads._
import payloads.ProductVariantPayloads._
import responses.AlbumResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import responses.AlbumResponses._
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductOptionResponses.ProductOptionResponse
import responses.ProductResponses._
import responses.ProductVariantResponses._
import services.LogActivity
import services.image.ImageManager
import services.image.ImageManager.FullAlbumWithImages
import services.inventory.ProductVariantManager
import services.objects.ObjectManager
import services.taxonomy.TaxonomyManager
import services.variant.ProductOptionManager
import services.variant.ProductOptionManager._
import slick.driver.PostgresDriver.api._
import utils.Validation._
import utils.aliases._
import utils.apis.Apis
import utils.db._

object ProductManager extends LazyLogging {

  def createProduct(admin: User, payload: CreateProductPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      oc: OC,
      apis: Apis,
      au: AU): DbResultT[ProductResponse.Root] = {

    val form                  = ObjectForm.fromPayload(Product.kind, payload.attributes)
    val shadow                = ObjectShadow.fromPayload(payload.attributes)
    val productOptionPayloads = payload.options.getOrElse(Seq.empty).distinct
    val hasOptions            = productOptionPayloads.nonEmpty
    val albumPayloads         = payload.albums.getOrElse(Seq.empty)

    for {
      _     ← * <~ payload.validate
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

      albums                     ← * <~ findOrCreateAlbumsForProduct(product, albumPayloads)
      productVariants            ← * <~ findOrCreateVariantsForProduct(product, payload.variants, !hasOptions)
      productOptions             ← * <~ findOrCreateOptionsForProduct(product, productOptionPayloads)
      productOptionsWithVariants ← * <~ getProductOptionsWithRelatedVariants(productOptions)
      (productOptionVariants, productOptionResponses) = productOptionsWithVariants
      taxons ← * <~ TaxonomyManager.getAssignedTaxons(product)
      response = ProductResponse.build(
          IlluminatedProduct.illuminate(oc, product, ins.form, ins.shadow),
          albums.map(AlbumResponse.build),
          if (hasOptions) productOptionVariants else productVariants,
          productOptionResponses,
          taxons)
      _ ← * <~ LogActivity
           .fullProductCreated(Some(admin), response, ObjectContextResponse.build(oc))
    } yield response

  }

  def getProduct(productId: ProductReference, checkActive: Boolean = false)(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[ProductResponse.Root] =
    for {
      oldProduct ← * <~ Products.mustFindFullByReference(productId)
      illuminated = IlluminatedProduct
        .illuminate(oc, oldProduct.model, oldProduct.form, oldProduct.shadow)
      _ ← * <~ doOrMeh(checkActive, {
           illuminated.mustBeActive match {
             case Xor.Left(err) ⇒ {
               logger.warn(err.toString)
               DbResultT.failure(NotFoundFailure404(Product, oldProduct.model.slug))
             }
             case Xor.Right(_) ⇒ DbResultT.unit
           }
         })
      albums ← * <~ ImageManager.getAlbumsForProduct(oldProduct.model.reference)

      fullVariants ← * <~ ProductVariantLinks.queryRightByLeft(oldProduct.model)
      _ ← * <~ failIf(
             checkActive && fullVariants
               .filter(variant ⇒ IlluminatedVariant.illuminate(oc, variant).mustBeActive.isRight)
               .isEmpty, {
               logger.warn(
                   s"Product variants for product with id=${oldProduct.model.slug} is archived or inactive")
               NotFoundFailure404(Product, oldProduct.model.slug)
             })
      productVariants ← * <~ fullVariants.map(ProductVariantManager.illuminateVariant)

      productOptions ← * <~ ProductOptionLinks.queryRightByLeft(oldProduct.model)
      fullVariants   ← * <~ productOptions.map(ProductOptionManager.zipVariantWithValues)

      hasOptions = productOptions.nonEmpty

      productOptionsWithVariants ← * <~ getProductOptionsWithRelatedVariants(fullVariants)
      (productOptionVariants, productOptionResponses) = productOptionsWithVariants

      taxons ← * <~ TaxonomyManager.getAssignedTaxons(oldProduct.model)
    } yield
      ProductResponse.build(illuminated,
                            albums,
                            if (hasOptions) productOptionVariants else productVariants,
                            productOptionResponses,
                            taxons)

  def updateProduct(productId: ProductReference, payload: UpdateProductPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      oc: OC,
      apis: Apis,
      au: AU): DbResultT[ProductResponse.Root] = {

    val formAndShadow = FormAndShadow.fromPayload(Product.kind, payload.attributes)

    val productVariantPayloads = payload.variants.getOrElse(Seq.empty)

    for {
      _          ← * <~ validateUpdate(payload)
      oldProduct ← * <~ Products.mustFindFullByReference(productId)

      _ ← * <~ productVariantsToBeUnassociatedMustNotBePresentInCarts(oldProduct.model.id,
                                                                      productVariantPayloads)

      mergedAttrs = oldProduct.shadow.attributes.merge(formAndShadow.shadow.attributes)
      updated ← * <~ ObjectUtils.update(oldProduct.form.id,
                                        oldProduct.shadow.id,
                                        formAndShadow.form.attributes,
                                        mergedAttrs,
                                        force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(oldProduct.model, updated.shadow, commit, payload.slug)

      albums ← * <~ updateAssociatedAlbums(updatedHead, payload.albums)

      productOptionLinks ← * <~ ProductOptionLinks.filterLeft(oldProduct.model).result

      hasOptions = productOptionLinks.nonEmpty || payload.options.exists(_.nonEmpty)

      updatedProductVariants ← * <~ findOrCreateVariantsForProduct(oldProduct.model,
                                                                   productVariantPayloads,
                                                                   !hasOptions)

      variants ← * <~ updateAssociatedOptions(updatedHead, payload.options)
      _        ← * <~ validateVariantsMatchesProductOptions(updatedProductVariants, variants)

      productOptionWithVariants ← * <~ getProductOptionsWithRelatedVariants(variants)
      (productOptionVariants, variantResponses) = productOptionWithVariants
      taxons ← * <~ TaxonomyManager.getAssignedTaxons(oldProduct.model)
      response = ProductResponse.build(
          IlluminatedProduct.illuminate(oc, updatedHead, updated.form, updated.shadow),
          albums,
          if (hasOptions) productOptionVariants else updatedProductVariants,
          variantResponses,
          taxons)
      _ ← * <~ LogActivity
           .fullProductUpdated(Some(au.model), response, ObjectContextResponse.build(oc))
    } yield response

  }

  def archiveByContextAndId(productId: ProductReference)(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[ProductResponse.Root] = {
    val payload = Map("activeFrom" → (("v" → JNull) ~ ("type" → JString("datetime"))),
                      "activeTo" → (("v" → JNull) ~ ("type" → JString("datetime"))))

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

      archiveResult ← * <~ Products.update(updatedHead,
                                           updatedHead.copy(archivedAt = Some(Instant.now)))

      albumLinks ← * <~ ProductAlbumLinks.filter(_.leftId === archiveResult.id).result
      _ ← * <~ albumLinks.map { link ⇒
           ProductAlbumLinks.deleteById(link.id,
                                        DbResultT.unit,
                                        id ⇒ NotFoundFailure400(ProductAlbumLinks, id))
         }
      albums              ← * <~ ImageManager.getAlbumsForProduct(ProductReference(inactive.form.id))
      productVariantLinks ← * <~ ProductVariantLinks.filter(_.leftId === archiveResult.id).result
      _ ← * <~ productVariantLinks.map { link ⇒
           ProductVariantLinks.deleteById(link.id,
                                          DbResultT.unit,
                                          id ⇒ NotFoundFailure400(ProductVariantLink, id))
         }
      updatedProductVariants ← * <~ ProductVariantLinks.queryRightByLeft(archiveResult)
      productVariantsDirect ← * <~ updatedProductVariants.map(
                                 ProductVariantManager.illuminateVariant)
      variantLinks ← * <~ ProductOptionLinks.filter(_.leftId === archiveResult.id).result
      _ ← * <~ variantLinks.map { link ⇒
           ProductOptionLinks.deleteById(link.id,
                                         DbResultT.unit,
                                         id ⇒ NotFoundFailure400(ProductVariantLink, link.id))
         }
      updatedVariants           ← * <~ ProductOptionLinks.queryRightByLeft(archiveResult)
      productOptions            ← * <~ updatedVariants.map(ProductOptionManager.zipVariantWithValues)
      productOptionsAndVariants ← * <~ getProductOptionsWithRelatedVariants(productOptions)
      (productVariants, productOptionResponses) = productOptionsAndVariants
      taxons ← * <~ TaxonomyManager.getAssignedTaxons(productObject.model)
    } yield
      ProductResponse.build(
          product =
            IlluminatedProduct.illuminate(oc, archiveResult, inactive.form, inactive.shadow),
          albums = albums,
          if (variantLinks.nonEmpty) productVariants else productVariantsDirect,
          productOptionResponses,
          taxons
      )
  }

  private def getProductOptionsWithRelatedVariants(
      productOptions: Seq[FullProductOption])(implicit ec: EC, db: DB, oc: OC)
    : DbResultT[(Seq[ProductVariantResponse.Partial], Seq[ProductOptionResponse.Root])] = {
    val optionIds = productOptions.flatMap { case (_, optionValue) ⇒ optionValue }.map(_.model.id)
    for {
      optionValueToVariantIdMap ← * <~ ProductOptionManager.mapOptionValuesToVariantIds(optionIds)
      variantIds = optionValueToVariantIdMap.values.toSet.flatten
      productVariants ← * <~ variantIds.toList.map(ProductVariantManager.getPartialByFormId)
      illuminated = productOptions.map {
        case (fullOption, values) ⇒
          val variant = IlluminatedProductOption.illuminate(oc, fullOption)
          ProductOptionResponse.buildLite(variant, values, optionValueToVariantIdMap)
      }
    } yield (productVariants.toSeq, illuminated)
  }

  private def validateCreate(
      payload: CreateProductPayload): ValidatedNel[Failure, CreateProductPayload] = {
    val maxProductVariants =
      payload.options.getOrElse(Seq.empty).map(_.values.getOrElse(Seq.empty).length.max(1)).product

    (notEmpty(payload.variants, "Product variants") |@| lesserThanOrEqual(
            payload.variants.length,
            maxProductVariants,
            "number of product variants") |@|
          validateSlug(payload.slug, true)).map {
      case _ ⇒ payload
    }
  }

  private def validateUpdate(
      payload: UpdateProductPayload): ValidatedNel[Failure, UpdateProductPayload] =
    payload.slug.fold(ok)(value ⇒ validateSlug(value)).map { case _ ⇒ payload }

  private def validateSlug(slug: String,
                           forProductCreate: Boolean = false): ValidatedNel[Failure, Unit] = {
    if (slug.isEmpty && forProductCreate || slug.exists(_.isLetter)) {
      ok
    } else {
      Validated.invalidNel(ProductFailures.SlugShouldHaveLetters(slug))
    }
  }

  private def validateVariantsMatchesProductOptions(
      variants: Seq[ProductVariantResponse.Partial],
      options: Seq[(FullObject[ProductOption], Seq[FullObject[ProductOptionValue]])])
    : ValidatedNel[Failure, Unit] = {
    val maxOptions = options.map { case (_, values) ⇒ values.length.max(1) }.product

    lesserThanOrEqual(variants.length, maxOptions, "number of product variants for given options").map {
      case _ ⇒ Unit
    }
  }

  private def updateAssociatedOptions(product: Product,
                                      optionsPayload: Option[Seq[ProductOptionPayload]])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[FullProductOption]] =
    optionsPayload match {
      case Some(payloads) ⇒
        findOrCreateOptionsForProduct(product, payloads)
      case None ⇒
        for {
          productOptions ← * <~ ProductOptionLinks.queryRightByLeft(product)
          fullOptions    ← * <~ productOptions.map(ProductOptionManager.zipVariantWithValues)
        } yield fullOptions
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
                         maybeCommit: Option[ObjectCommit],
                         newSlug: Option[String])(implicit ec: EC): DbResultT[Product] = {

    def withNewSlug =
      (product: Product) ⇒ newSlug.fold(product)(value ⇒ product.copy(slug = value))
    def withCommit =
      (product: Product) ⇒
        maybeCommit.fold(product)(commit ⇒
              product.copy(shadowId = shadow.id, commitId = commit.id))

    val newProduct = withNewSlug.andThen(withCommit)(product)

    if (newProduct != product) Products.update(product, newProduct)
    else DbResultT.good(product)
  }

  private def findOrCreateVariantsForProduct(
      product: Product,
      variantPayloads: Seq[ProductVariantPayload],
      createLinks: Boolean = true)(implicit ec: EC, db: DB, oc: OC, au: AU, apis: Apis) =
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
                DbResultT.failure(LinkArchivedVariantFailure(Product, product.id, code))
              }
            }.getOrElse {
              for {
                newVariant ← * <~ ProductVariantManager.createInner(oc, payload)
                _ ← * <~ ProductVariantManager.findOrCreateAlbumsForVariant(newVariant.model,
                                                                            albumPayloads)
                _ ← * <~ ProductVariantLinks.syncLinks(product,
                                                       if (createLinks) Seq(newVariant.model)
                                                       else Seq.empty)
              } yield newVariant
            }
        albums     ← * <~ ImageManager.getAlbumsForVariantInner(up.form.id)
        skuMapping ← * <~ ProductVariantSkus.mustFindByVariantFormId(up.form.id)
        options    ← * <~ ProductVariantManager.optionValuesForVariant(up.model)
      } yield
        ProductVariantResponse.buildPartial(IlluminatedVariant.illuminate(oc, up),
                                            albums,
                                            skuMapping.skuId,
                                            skuMapping.skuCode,
                                            options)
    }

  private def findOrCreateOptionsForProduct(product: Product, payload: Seq[ProductOptionPayload])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[FullProductOption]] =
    for {
      productOptions ← * <~ payload.map(ProductOptionManager.updateOrCreate)
      _ ← * <~ ProductOptionLinks.syncLinks(product, productOptions.map {
           case (option, _) ⇒ option.model
         })
    } yield productOptions

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
  // TODO @anna migrate to variant form id
  private def productVariantsToBeUnassociatedMustNotBePresentInCarts(
      productId: Int,
      payloadVariants: Seq[ProductVariantPayload])(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      productVariantIdsForProduct ← * <~ ProductVariantLinks
                                     .filter(_.leftId === productId)
                                     .result
                                     .flatMap {
                                       case links @ Seq(_) ⇒
                                         lift(links.map(_.rightId))
                                       case _ ⇒
                                         for {
                                           optionLinks ← ProductOptionLinks
                                                          .filter(_.leftId === productId)
                                                          .result
                                           variantIds = optionLinks.map(_.rightId)
                                           optionValueLinks ← ProductOptionValueLinks
                                                               .filter(_.leftId.inSet(variantIds))
                                                               .result
                                           optionValueIds = optionValueLinks.map(_.rightId)
                                           optionValueToVariantLinks ← ProductValueVariantLinks
                                                                        .filter(_.leftId.inSet(
                                                                                optionValueIds))
                                                                        .result
                                         } yield optionValueToVariantLinks.map(_.rightId)
                                     }
      productSkus ← * <~ ProductVariants
                     .filter(_.id.inSet(productVariantIdsForProduct))
                     .map(_.code)
                     .result
      payloadSkus = payloadVariants
        .map(ps ⇒ ProductVariantManager.getSkuCode(ps.attributes))
        .flatten
      skuCodesToBeGone = productSkus.diff(payloadSkus)
      _ ← * <~ (skuCodesToBeGone.map { codeToUnassociate ⇒
               for {
                 skuToUnassociate ← * <~ ProductVariants.mustFindByCode(codeToUnassociate)
                 _                ← * <~ skuToUnassociate.mustNotBePresentInCarts
               } yield {}
             })
    } yield {}
}
