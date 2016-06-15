package services.product

import cats.data._
import failures.Failure
import failures.ObjectFailures._
import failures.ProductFailures._
import models.StoreAdmin
import models.image._
import models.inventory._
import models.objects._
import models.product._
import payloads.ImagePayloads.CreateAlbumPayload
import payloads.ProductPayloads._
import payloads.SkuPayloads._
import payloads.VariantPayloads._
import responses.ImageResponses.AlbumResponse
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductResponses._
import services.{LogActivity, Result}
import services.image.ImageManager
import services.inventory.SkuManager
import services.objects.ObjectManager
import services.variant.VariantManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._
import utils.Validation
import utils.Validation._

object ProductManager {

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  // NEW SIMPLE ENDPOINTS

  def createProduct(payload: CreateProductPayload)(
      implicit ec: EC, db: DB, oc: OC): DbResultT[ProductResponse.Root] = {

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
      ProductResponse.build(
          product = IlluminatedProduct.illuminate(oc, product, ins.form, ins.shadow),
          skus = skus.map(sku ⇒ IlluminatedSku.illuminate(oc, sku)),
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

      skuLinks ← * <~ ProductSkuLinks.filter(_.leftId === product.id).result
      skus     ← * <~ skuLinks.map(link ⇒ mustFindFullSkuById(link.rightId))

      variantLinks ← * <~ ProductVariantLinks.filter(_.leftId === product.id).result
      variants     ← * <~ variantLinks.map(link ⇒ mustFindFullVariantById(link.rightId))
    } yield
      ProductResponse.build(product = IlluminatedProduct.illuminate(oc, product, form, shadow),
                            skus = skus.map(sku ⇒ IlluminatedSku.illuminate(oc, sku)),
                            variants = variants.map {
                              case (fullVariant, values) ⇒
                                (IlluminatedVariant.illuminate(oc, fullVariant), values)
                            },
                            variantMap = Map.empty)

  def updateProduct(productId: Int, payload: UpdateProductPayload)(
      implicit ec: EC, db: DB, oc: OC): DbResultT[ProductResponse.Root] = {

    val newFormAttrs   = ObjectForm.fromPayload(Product.kind, payload.attributes).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(payload.attributes).attributes

    for {
      product   ← * <~ mustFindProductByContextAndId404(oc.id, productId)
      oldForm   ← * <~ ObjectForms.mustFindById404(product.formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(product.shadowId)

      mergedAttrs = oldShadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils.update(
                   oldForm.id, oldShadow.id, newFormAttrs, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(product, updated.shadow, commit)

      skus ← * <~ updateAssociatedSkus(updatedHead, oldShadow.id, payload.skus)

      variants ← * <~ updateAssociatedVariants(updatedHead, oldShadow.id, payload.variants)

      _ ← * <~ updateAssociatedAlbums(updatedHead, oldShadow.id)

      fullProduct = FullObject(updatedHead, updated.form, updated.shadow)
      _ ← * <~ validateUpdate(fullProduct, skus, variants)
    } yield
      ProductResponse.build(
          product = IlluminatedProduct.illuminate(oc, updatedHead, updated.form, updated.shadow),
          skus = skus.map(sku ⇒ IlluminatedSku.illuminate(oc, sku)),
          variants = variants.map {
            case (fullVariant, values) ⇒
              (IlluminatedVariant.illuminate(oc, fullVariant), values)
          },
          variantMap = Map.empty)
  }

  private def validateUpdate(product: FullObject[Product],
                             skus: Seq[FullObject[Sku]],
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

  private def updateAssociatedSkus(product: Product,
                                   oldProductShadowId: Int,
                                   skusPayload: Option[Seq[SkuPayload]])(
      implicit ec: EC, db: DB, oc: OC) = {

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

  private def updateAssociatedVariants(product: Product,
                                       oldProductShadowId: Int,
                                       variantsPayload: Option[Seq[VariantPayload]])(
      implicit ec: EC, db: DB, oc: OC) = {

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

  private def updateAssociatedVariant(newShadowId: Int, variantLink: ObjectLink)(
      implicit ec: EC, db: DB, oc: OC) =
    for {
      link ← * <~ ObjectUtils.updateAssociatedRight(Variants, variantLink, newShadowId)
      valLinks ← * <~ ObjectLinks
                  .findByLeftAndType(variantLink.rightId, ObjectLink.VariantValue)
                  .result
      _       ← * <~ ObjectUtils.updateAssociatedRights(VariantValues, valLinks, link.rightId)
      variant ← * <~ mustFindFullVariantByShadowId(link.rightId)
    } yield variant

  private def updateAssociatedAlbums(product: Product, oldProductShadowId: Int)(
      implicit ec: EC, db: DB, oc: OC) = {

    for {
      existingLinks ← * <~ ObjectLinks
                       .findByLeftAndType(oldProductShadowId, ObjectLink.ProductAlbum)
                       .result
      _ ← * <~ ObjectUtils.updateAssociatedRights(Albums, existingLinks, product.shadowId)
    } yield {}
  }

  private def updateHead(
      product: Product, shadow: ObjectShadow, maybeCommit: Option[ObjectCommit])(
      implicit ec: EC): DbResult[Product] = maybeCommit match {
    case Some(commit) ⇒
      Products.update(product, product.copy(shadowId = shadow.id, commitId = commit.id))
    case None ⇒
      DbResult.good(product)
  }

  private def findOrCreateSkuForProduct(product: Product, payload: SkuPayload)(
      implicit ec: EC, db: DB, oc: OC) = {

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
    } yield sku
  }

  private def findOrCreateVariantForProduct(product: Product, payload: VariantPayload)(
      implicit ec: EC, db: DB, oc: OC) = {

    for {
      variant ← * <~ VariantManager.updateOrCreateVariant(oc, payload)
      (fullVariant, _) = variant
      _ ← * <~ ObjectLinks.create(ObjectLink(leftId = product.shadowId,
                                             rightId = fullVariant.shadow.id,
                                             linkType = ObjectLink.ProductVariant))
    } yield variant
  }

  private def mustFindFullSkuById(id: Int)(implicit ec: EC, db: DB): DbResultT[FullObject[Sku]] =
    for {
      sku    ← * <~ Skus.filter(_.id === id).mustFindOneOr(SkuNotFound(id))
      shadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      form   ← * <~ ObjectForms.mustFindById404(sku.formId)
    } yield FullObject(sku, form, shadow)

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

  //
  /////////////////////////////////////////////////////////////////////////////////////////////////////

  def getForm(id: Int)(implicit ec: EC, db: DB): Result[ProductFormResponse.Root] =
    (for {
      // guard to make sure the form is a product.
      product ← * <~ Products.filter(_.formId === id).mustFindOneOr(ProductFormNotFound(id))
      form    ← * <~ ObjectForms.mustFindById404(id)
    } yield ProductFormResponse.build(product, form)).run()

  def getShadow(
      id: Int, contextName: String)(implicit ec: EC, db: DB): Result[ProductShadowResponse.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      product ← * <~ Products
                 .filter(_.contextId === context.id)
                 .filter(_.formId === id)
                 .mustFindOneOr(ProductNotFoundForContext(id, context.id))
      shadow ← * <~ ObjectShadows.mustFindById404(product.shadowId)
    } yield ProductShadowResponse.build(shadow)).run()

  def getShadow(id: Int)(implicit ec: EC, db: DB): Result[ProductFormResponse.Root] =
    (for {
      form    ← * <~ ObjectForms.mustFindById404(id)
      product ← * <~ Products.filter(_.formId === id).mustFindOneOr(ProductFormNotFound(id))
    } yield ProductFormResponse.build(product, form)).run()

  def getIlluminatedProduct(productId: Int, contextName: String)(
      implicit ec: EC, db: DB): Result[IlluminatedProductResponse.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      product ← * <~ Products
                 .filter(_.contextId === context.id)
                 .filter(_.formId === productId)
                 .mustFindOneOr(ProductNotFoundForContext(productId, context.id))
      form   ← * <~ ObjectForms.mustFindById404(product.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(product.shadowId)
    } yield
      IlluminatedProductResponse.build(
          IlluminatedProduct.illuminate(context, product, form, shadow))).run()

  def getFullProduct(productId: Int, contextName: String)(
      implicit ec: EC, db: DB): Result[FullProductResponse.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      product ← * <~ Products
                 .filter(_.contextId === context.id)
                 .filter(_.formId === productId)
                 .mustFindOneOr(ProductNotFoundForContext(productId, context.id))
      productForm   ← * <~ ObjectForms.mustFindById404(product.formId)
      productShadow ← * <~ ObjectShadows.mustFindById404(product.shadowId)
      skuData       ← * <~ getSkuData(productShadow.id)
    } yield FullProductResponse.build(product, productForm, productShadow, skuData)).run()

  def createFullProduct(admin: StoreAdmin, payload: CreateFullProduct, contextName: String)(
      implicit ec: EC, db: DB, ac: AC): Result[FullProductResponse.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      productForm ← * <~ ObjectForm(kind = Product.kind,
                                    attributes = payload.form.product.attributes)
      productShadow ← * <~ ObjectShadow(attributes = payload.shadow.product.attributes)
      ins           ← * <~ ObjectUtils.insert(productForm, productShadow)
      product ← * <~ Products.create(Product(contextId = context.id,
                                             formId = ins.form.id,
                                             shadowId = ins.shadow.id,
                                             commitId = ins.commit.id))
      skuData ← * <~ createSkuData(context, ins.shadow.id, payload.form.skus, payload.shadow.skus)
      productResponse = FullProductResponse.build(product, ins.form, ins.shadow, skuData)
      contextResp     = ObjectContextResponse.build(context)
      _ ← * <~ LogActivity.fullProductCreated(Some(admin), productResponse, contextResp)
    } yield productResponse).runTxn()

  def updateFullProduct(
      admin: StoreAdmin, productId: Int, payload: UpdateFullProduct, contextName: String)(
      implicit ec: EC, db: DB, ac: AC): Result[FullProductResponse.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      product ← * <~ Products
                 .filter(_.contextId === context.id)
                 .filter(_.formId === productId)
                 .mustFindOneOr(ProductNotFoundForContext(productId, context.id))
      updatedProduct ← * <~ ObjectUtils.update(product.formId,
                                               product.shadowId,
                                               payload.form.product.attributes,
                                               payload.shadow.product.attributes)
      updatedSkuData ← * <~ updateSkuData(context,
                                          product.shadowId,
                                          updatedProduct.shadow.id,
                                          payload.form.skus,
                                          payload.shadow.skus)
      skusChanged ← * <~ anyChanged(updatedSkuData.map { case (_, isUpdated) ⇒ isUpdated })
      skuData     ← * <~ updatedSkuData.map { case (fullSku, _)              ⇒ fullSku }
      commit ← * <~ ObjectUtils.commit(updatedProduct.form,
                                       updatedProduct.shadow,
                                       updatedProduct.updated || skusChanged)
      product ← * <~ updateProductHead(product, updatedProduct.shadow, commit)
      productResponse = FullProductResponse.build(
          product, updatedProduct.form, updatedProduct.shadow, skuData)
      contextResp = ObjectContextResponse.build(context)
      _ ← * <~ LogActivity.fullProductUpdated(Some(admin), productResponse, contextResp)
    } yield productResponse).runTxn()

  def addAlbumToProduct(
      admin: StoreAdmin, productId: Int, payload: CreateAlbumPayload, contextName: String)(
      implicit ec: EC, db: DB, ac: AC): Result[AlbumResponse.Root] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      product ← * <~ mustFindProductByContextAndId404(context.id, productId)
      album   ← * <~ ImageManager.createAlbumInner(payload, context)
      images  ← * <~ Image.buildFromAlbum(album)
      link ← * <~ ObjectLinks.create(ObjectLink(leftId = product.shadowId,
                                                rightId = album.shadow.id,
                                                linkType = ObjectLink.ProductAlbum))
    } yield AlbumResponse.build(album, images)).runTxn()

  def mustFindProductByContextAndId404(contextId: Int, productId: Int)(
      implicit ec: EC, db: DB): DbResult[Product] =
    Products
      .filter(_.contextId === contextId)
      .filter(_.formId === productId)
      .mustFindOneOr(ProductNotFoundForContext(productId, contextId))

  def getIlluminatedFullProductByContextName(productId: Int, contextName: String)(
      implicit ec: EC, db: DB): Result[IlluminatedFullProductResponse.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      result ← * <~ getIlluminatedFullProductInner(productId, context)
    } yield result).run()

  def getIlluminatedFullProductByContext(productId: Int, context: ObjectContext)(
      implicit ec: EC, db: DB): Result[IlluminatedFullProductResponse.Root] =
    getIlluminatedFullProductInner(productId, context).run()

  def getIlluminatedFullProductInner(productId: Int, context: ObjectContext)(
      implicit ec: EC, db: DB): DbResultT[IlluminatedFullProductResponse.Root] =
    for {
      product ← * <~ Products
                 .filter(_.contextId === context.id)
                 .filter(_.formId === productId)
                 .mustFindOneOr(ProductNotFoundForContext(productId, context.id))
      productForm        ← * <~ ObjectForms.mustFindById404(product.formId)
      productShadow      ← * <~ ObjectShadows.mustFindById404(product.shadowId)
      skuData            ← * <~ getSkuData(productShadow.id)
      variants           ← * <~ VariantManager.findVariantsByProduct(product)
      fullVariantMap     ← * <~ getFullVariantMapForSkus(skuData, variants)
      variantMap         ← * <~ fullVariantMap.mapValues(_.map(_.value))
      variantsWithValues ← * <~ combineVariantsAndValues(variants, fullVariantMap)
    } yield
      IlluminatedFullProductResponse.build(
          p = IlluminatedProduct.illuminate(context, product, productForm, productShadow),
          skus = skuData.map(sku ⇒ IlluminatedSku.illuminate(context, sku)),
          variants = variantsWithValues.map {
            case (vars, vals) ⇒ (IlluminatedVariant.illuminate(context, vars), vals)
          },
          variantMap = variantMap)

  def getIlluminatedFullProductAtCommit(productId: Int, contextName: String, commitId: Int)(
      implicit ec: EC, db: DB): Result[IlluminatedFullProductResponse.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      product ← * <~ Products
                 .filter(_.contextId === context.id)
                 .filter(_.formId === productId)
                 .mustFindOneOr(ProductNotFoundForContext(productId, context.id))
      commit ← * <~ ObjectCommits
                .filter(_.id === commitId)
                .filter(_.formId === productId)
                .mustFindOneOr(ProductNotFoundAtCommit(productId, commitId))
      productForm        ← * <~ ObjectForms.mustFindById404(commit.formId)
      productShadow      ← * <~ ObjectShadows.mustFindById404(commit.shadowId)
      skuData            ← * <~ getSkuData(productShadow.id)
      variants           ← * <~ VariantManager.findVariantsByProduct(product)
      fullVariantMap     ← * <~ getFullVariantMapForSkus(skuData, variants)
      variantMap         ← * <~ fullVariantMap.mapValues(_.map(_.value))
      variantsWithValues ← * <~ combineVariantsAndValues(variants, fullVariantMap)
    } yield
      IlluminatedFullProductResponse.build(
          p = IlluminatedProduct.illuminate(context, product, productForm, productShadow),
          skus = skuData.map(sku ⇒ IlluminatedSku.illuminate(context, sku)),
          variants = variantsWithValues.map {
            case (vars, vals) ⇒ (IlluminatedVariant.illuminate(context, vars), vals)
          },
          variantMap = variantMap)).run()

  def getContextsForProduct(
      formId: Int)(implicit ec: EC, db: DB): Result[Seq[ObjectContextResponse.Root]] =
    (for {
      products   ← * <~ Products.filterByFormId(formId).result
      contextIds ← * <~ products.map(_.contextId)
      contexts   ← * <~ ObjectContexts.filter(_.id.inSet(contextIds)).sortBy(_.id).result
    } yield contexts.map(ObjectContextResponse.build)).run()

  // Iterates through all SKU data and gets all VariantValues (and corresponding
  // Variant shadow IDs) that are used by the set of SKUs. The return set is
  // constrained by the set of Variants used on the project.
  private def getFullVariantMapForSkus(
      skus: Seq[FullObject[Sku]], variants: Seq[FullObject[Variant]])(implicit ec: EC) = {
    val empty = DbResultT.pure(Map.empty[String, Seq[VariantValueMapping]])
    for {
      fullVariantMap ← * <~ skus.foldLeft(empty) { (acc, sku) ⇒
                        for {
                          values ← * <~ SkuManager.findVariantValuesForSkuInProduct(sku.model,
                                                                                    variants)
                          current ← * <~ acc
                        } yield current + (sku.model.code → values)
                      }
    } yield fullVariantMap
  }

  private def combineVariantsAndValues(
      variants: Seq[FullObject[Variant]], mapping: Map[String, Seq[VariantValueMapping]]) = {
    val combinedMap = mapping.foldLeft(Seq.empty[VariantValueMapping]) {
      case (acc, (_, mappings)) ⇒ acc ++ mappings
    }

    val mapPartition = combinedMap.foldLeft(Map.empty[Int, Seq[FullObject[VariantValue]]]) {
      (acc, mapItem) ⇒
        val valueList = acc.get(mapItem.variantShadowId) match {
          case Some(currentValues) ⇒
            if (currentValues.contains(mapItem.value)) currentValues
            else currentValues :+ mapItem.value
          case None ⇒
            Seq(mapItem.value)
        }

        acc + (mapItem.variantShadowId → valueList)
    }

    variants.map { variant ⇒
      val values = mapPartition.getOrElse(variant.shadow.id, Seq.empty)
      (variant, values)
    }
  }

  private def anyChanged(changes: Seq[Boolean]): Boolean =
    changes.contains(true)

  private def updateProductHead(
      product: Product, productShadow: ObjectShadow, maybeCommit: Option[ObjectCommit])(
      implicit ec: EC): DbResult[Product] = maybeCommit match {
    case Some(commit) ⇒
      Products.update(product, product.copy(shadowId = productShadow.id, commitId = commit.id))
    case None ⇒ DbResult.good(product)
  }

  private def validateSkuPayload(
      skuGroup: Seq[(CreateFullSkuForm, CreateSkuShadow)])(implicit ec: EC): DbResultT[Unit] =
    ObjectUtils.failIfErrors(
        skuGroup.flatMap {
      case (f, s) ⇒
        if (f.code == s.code) Seq.empty
        else Seq(SkuShadowNotFoundInPayload(f.code))
    })

  private def validateSkuPayload2(
      skuGroup: Seq[(UpdateFullSkuForm, UpdateFullSkuShadow)])(implicit ec: EC): DbResultT[Unit] =
    ObjectUtils.failIfErrors(
        skuGroup.flatMap {
      case (f, s) ⇒
        if (f.code == s.code) Seq.empty
        else Seq(SkuShadowNotFoundInPayload(f.code))
    })

  private def validateShadow(product: Product, form: ObjectForm, shadow: ObjectShadow)(
      implicit ec: EC): DbResultT[Unit] =
    ObjectUtils.failIfErrors(ProductValidator.validate(product, form, shadow))

  private def createSku(context: ObjectContext,
                        productShadowId: Int,
                        formPayload: CreateFullSkuForm,
                        shadowPayload: CreateSkuShadow)(implicit ec: EC) = {
    require(formPayload.code == shadowPayload.code)
    for {
      form ← * <~ ObjectForms.create(
                ObjectForm(kind = Sku.kind, attributes = formPayload.attributes))
      shadow ← * <~ ObjectShadows.create(
                  ObjectShadow(formId = form.id, attributes = shadowPayload.attributes))
      _ ← * <~ SkuManager.validateShadow(form, shadow)
      _ ← * <~ ObjectLinks.create(ObjectLink(leftId = productShadowId,
                                             rightId = shadow.id,
                                             linkType = ObjectLink.ProductSku))
      commit ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      sku ← * <~ Skus.create(
               Sku(contextId = context.id,
                   code = formPayload.code,
                   formId = form.id,
                   shadowId = shadow.id,
                   commitId = commit.id))
    } yield FullObject(sku, form, shadow)
  }

  private def getSkuData(productShadowId: Int)(implicit ec: EC): DbResultT[Seq[FullObject[Sku]]] =
    for {
      links     ← * <~ ObjectLinks.findByLeftAndType(productShadowId, ObjectLink.ProductSku).result
      shadowIds ← * <~ links.map(_.rightId)
      shadows   ← * <~ ObjectShadows.filter(_.id.inSet(shadowIds)).sortBy(_.formId).result
      formIds   ← * <~ shadows.map(_.formId)
      forms     ← * <~ ObjectForms.filter(_.id.inSet(formIds)).sortBy(_.id).result
      skus      ← * <~ Skus.filter(_.formId.inSet(formIds)).sortBy(_.formId).result
    } yield
      skus.zip(forms.zip(shadows)).map {
        case (sku, (form, shadow)) ⇒ FullObject(sku, form, shadow)
      }

  private def createSkuData(context: ObjectContext,
                            productShadowId: Int,
                            formPayloads: Seq[CreateFullSkuForm],
                            shadowPayloads: Seq[CreateSkuShadow])(
      implicit ec: EC): DbResultT[Seq[FullObject[Sku]]] =
    for {
      skuGroup ← * <~ groupSkuFormsAndShadows(formPayloads, shadowPayloads)
      _        ← * <~ validateSkuPayload(skuGroup)
      skuData  ← * <~ skuGroup.map { case (f, sh) ⇒ createSku(context, productShadowId, f, sh) }
    } yield skuData

  private def updateSkuData(context: ObjectContext,
                            oldProductShadowId: Int,
                            productShadowId: Int,
                            formPayloads: Seq[UpdateFullSkuForm],
                            shadowPayloads: Seq[UpdateFullSkuShadow])(
      implicit ec: EC, db: DB): DbResultT[Seq[(FullObject[Sku], Boolean)]] =
    for {
      skuGroup ← * <~ groupSkuFormsAndShadows2(formPayloads, shadowPayloads)
      _        ← * <~ validateSkuPayload2(skuGroup)
      skuData ← * <~ skuGroup.map {
                 case (f, sh) ⇒
                   updateSku(context, oldProductShadowId, productShadowId, f, sh)
               }
    } yield skuData

  private def updateSku(context: ObjectContext,
                        oldProductShadowId: Int,
                        productShadowId: Int,
                        formPayload: UpdateFullSkuForm,
                        shadowPayload: UpdateFullSkuShadow)(
      implicit ec: EC, db: DB): DbResultT[(FullObject[Sku], Boolean)] = {
    require(formPayload.code == shadowPayload.code)
    for {
      sku ← * <~ Skus
             .filterByContextAndCode(context.id, formPayload.code)
             .mustFindOneOr(SkuNotFoundForContext(formPayload.code, context.id))
      updatedSku ← * <~ ObjectUtils.update(
                      sku.formId, sku.shadowId, formPayload.attributes, shadowPayload.attributes)
      _ ← * <~ ObjectUtils.updateLink(oldProductShadowId,
                                      productShadowId,
                                      sku.shadowId,
                                      updatedSku.shadow.id,
                                      ObjectLink.ProductSku)
      commit ← * <~ ObjectUtils.commit(updatedSku.form, updatedSku.shadow, updatedSku.updated)
      sku    ← * <~ SkuManager.updateSkuHead(sku, updatedSku.shadow, commit)
    } yield (FullObject(sku, updatedSku.form, updatedSku.shadow), updatedSku.updated)
  }

  private def groupSkuFormsAndShadows(
      forms: Seq[CreateFullSkuForm], shadows: Seq[CreateSkuShadow]) = {
    val sortedForms   = forms.sortBy(_.code)
    val sortedShadows = shadows.sortBy(_.code)
    sortedForms.zip(sortedShadows)
  }

  private def groupSkuFormsAndShadows2(
      forms: Seq[UpdateFullSkuForm], shadows: Seq[UpdateFullSkuShadow]) = {
    val sortedForms   = forms.sortBy(_.code)
    val sortedShadows = shadows.sortBy(_.code)
    sortedForms.zip(sortedShadows)
  }
}
