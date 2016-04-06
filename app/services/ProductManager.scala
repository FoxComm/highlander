package services

import models.inventory._
import models.product._
import models.objects._
import responses.ProductResponses._
import slick.driver.PostgresDriver.api._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import payloads.{CreateFullProduct, CreateFullProductForm, CreateFullProductShadow, CreateFullSkuForm, CreateObjectContext, CreateProductForm, CreateProductShadow, CreateSkuForm, CreateSkuShadow, UpdateFullProduct, UpdateFullProductForm, UpdateFullProductShadow, UpdateFullSkuForm, UpdateFullSkuShadow, UpdateObjectContext, UpdateProductForm, UpdateProductShadow}
import org.json4s.JsonAST.{JField, JNothing, JObject, JString, JValue}
import org.json4s.jackson.JsonMethods._
import utils.aliases._
import utils.IlluminateAlgorithm
import cats.data.NonEmptyList
import cats.implicits._
import failures.Failure
import failures.NotFoundFailure400
import failures.ProductFailures._
import failures.ObjectFailures._
import java.time.Instant

import models.StoreAdmin

object ProductManager {

  def getForm(id: Int)
    (implicit ec: EC, db: DB): Result[ProductFormResponse.Root] = (for {
    form       ← * <~ ObjectForms.mustFindById404(id)
    product    ← * <~ Products.filter(_.formId === id).one.mustFindOr(
      ProductFormNotFound(id))
  } yield ProductFormResponse.build(product, form)).run()

  def getShadow(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[ProductShadowResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    product ← * <~ Products.filter(_.contextId === context.id).
      filter(_.formId === id).one.
      mustFindOr(ProductNotFoundForContext(id, context.id))
    shadow  ← * <~ ObjectShadows.mustFindById404(product.shadowId)
  } yield ProductShadowResponse.build(shadow)).run()

  def getShadow(id: Int)
    (implicit ec: EC, db: DB): Result[ProductFormResponse.Root] = (for {
    form       ← * <~ ObjectForms.mustFindById404(id)
    product    ← * <~ Products.filter(_.formId === id).one.mustFindOr(
      ProductFormNotFound(id))
  } yield ProductFormResponse.build(product, form)).run()

  def getIlluminatedProduct(productId: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedProductResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    product       ← * <~ Products.filter(_.contextId === context.id).
      filter(_.id === productId).one.
        mustFindOr(ProductNotFoundForContext(productId, context.id)) 
    form       ← * <~ ObjectForms.mustFindById404(product.formId)
    shadow       ← * <~ ObjectShadows.mustFindById404(product.shadowId)
  } yield IlluminatedProductResponse.build(
    IlluminatedProduct.illuminate(context, product, form, shadow))).run()

  def getFullProduct(productId: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[FullProductResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    product       ← * <~ Products.filter(_.contextId === context.id).
      filter(_.formId === productId).one.mustFindOr(
        ProductNotFoundForContext(productId, context.id)) 
    productForm    ← * <~ ObjectForms.mustFindById404(product.formId)
    productShadow  ← * <~ ObjectShadows.mustFindById404(product.shadowId)
    skuData       ← * <~ getSkuData(productShadow.id)
  } yield FullProductResponse.build(product, productForm, productShadow, skuData)).run()

  def createFullProduct(admin: StoreAdmin, payload: CreateFullProduct, contextName: String)
    (implicit ec: EC, db: DB): Result[FullProductResponse.Root] = (for {

    context       ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    productForm   ← * <~ ObjectForms.create(ObjectForm(kind = Product.kind,
      attributes = payload.form.product.attributes))
    productShadow ← * <~ ObjectShadows.create(ObjectShadow(formId = productForm.id,
      attributes = payload.shadow.product.attributes))
    productCommit ← * <~ ObjectCommits.create(ObjectCommit(formId = productForm.id,
      shadowId = productShadow.id))
    _             ← * <~ validateShadow(productForm, productShadow)
    product       ← * <~ Products.create(Product(contextId = context.id,
      formId = productForm.id, shadowId = productShadow.id, commitId = productCommit.id))
    skuData       ← * <~ createSkuData(context, productShadow.id,
      payload.form.skus, payload.shadow.skus)
    response      = FullProductResponse.build(product, productForm, productShadow, skuData)
    _             ← * <~ LogActivity.productCreated(Some(admin), response)
  } yield response).runTxn()

  def updateFullProduct(productId: Int, payload: UpdateFullProduct, contextName: String)
    (implicit ec: EC, db: DB): Result[FullProductResponse.Root] = (for {

    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    product       ← * <~ Products.filter(_.contextId === context.id).
      filter(_.formId === productId).one.mustFindOr(
        ProductNotFoundForContext(productId, context.id)) 
    updatedFormShadow ← * <~ ObjectManager.updateObjectFormAndShadow(product.formId,
      product.shadowId, payload.form.product.attributes, 
      payload.shadow.product.attributes)
    (productForm, productShadow, productChanged)  = updatedFormShadow
    updatedSkuData ← * <~ updateSkuData(context, productShadow.id, 
      payload.form.skus, payload.shadow.skus, productChanged)
    skusChanged ← * <~ anyChanged(updatedSkuData.map(_._4))
    skuData  ← * <~ updatedSkuData.map( t ⇒ (t._1, t._2, t._3))
    product ← * <~ commitProduct(product, productForm, productShadow, productChanged || skusChanged)
  } yield FullProductResponse.build(product, productForm, productShadow, skuData)).runTxn()


  def getIlluminatedFullProductByContextName(productId: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedFullProductResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    result ← * <~ getIlluminatedFullProductInner(productId, context)
  } yield result).run()

  def getIlluminatedFullProductByContext(productId: Int, context: ObjectContext)
    (implicit ec: EC, db: DB): Result[IlluminatedFullProductResponse.Root] = (for {
    result ← * <~ getIlluminatedFullProductInner(productId, context)
  } yield result).run()

  def getIlluminatedFullProductInner(productId: Int, context: ObjectContext)
    (implicit ec: EC, db: DB): DbResultT[IlluminatedFullProductResponse.Root] = for {

    product       ← * <~ Products.filter(_.contextId === context.id).
      filter(_.formId === productId).one.mustFindOr(
        ProductNotFoundForContext(productId, context.id)) 
      
    productForm   ← * <~ ObjectForms.mustFindById404(product.formId)
    productShadow  ← * <~ ObjectShadows.mustFindById404(product.shadowId)
    skuData ← * <~ getSkuData(productShadow.id)

  } yield IlluminatedFullProductResponse.build(
    IlluminatedProduct.illuminate(context, product, productForm, productShadow),
    skuData.map { 
      case (s, f, sh) ⇒ IlluminatedSku.illuminate(context, s, f, sh)
    })

  def getIlluminatedFullProductAtCommit(productId: Int, contextName: String, commitId: Int)
    (implicit ec: EC, db: DB): Result[IlluminatedFullProductResponse.Root] = (for {

    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    product       ← * <~ Products.filter(_.contextId === context.id).
      filter(_.formId === productId).one.mustFindOr(
        ProductNotFoundForContext(productId, context.id)) 
    commit       ← * <~ ObjectCommits.filter(_.id === commitId).
      filter(_.formId === productId).one.mustFindOr(
        ProductNotFoundAtCommit(productId, commitId)) 
    productForm   ← * <~ ObjectForms.mustFindById404(commit.formId)
    productShadow  ← * <~ ObjectShadows.mustFindById404(commit.shadowId)
    skuData ← * <~ getSkuData(productShadow.id)

  } yield IlluminatedFullProductResponse.build(
    IlluminatedProduct.illuminate(context, product, productForm, productShadow),
    skuData.map { 
      case (s, f, sh) ⇒ IlluminatedSku.illuminate(context, s, f, sh)
    })).run()

  private def anyChanged(changes: Seq[Boolean]) : Boolean = 
    changes.contains(true)

  private def commitProduct(product: Product, productForm: ObjectForm, 
    productShadow: ObjectShadow, shouldCommit: Boolean) 
    (implicit ec: EC, db: DB): DbResultT[Product] = 
      if(shouldCommit) for {
        newCommit ← * <~ ObjectManager.createCommit(product.commitId, productForm.id, productShadow.id)
        product   ← * <~ Products.update(product, product.copy(
          shadowId = productShadow.id, commitId = newCommit.id))
      } yield product
    else DbResultT.pure(product)

  private def failIfErrors(errors: Seq[Failure])
  (implicit ec: EC, db: DB) : DbResultT[Unit] =  {
    errors match {
      case Nil ⇒ DbResultT.pure(Unit)
      case head ::tail ⇒ DbResultT.leftLift(NonEmptyList(head, tail))
    }
  }

  private def validateSkuPayload(skuGroup : Seq[(CreateFullSkuForm, CreateSkuShadow)]) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] =
    failIfErrors(skuGroup.flatMap { case (f, s) ⇒ 
      if(f.code == s.code) Seq.empty
      else Seq(SkuShadowNotFoundInPayload(f.code))
    })

  private def validateSkuPayload2(skuGroup : Seq[(UpdateFullSkuForm, UpdateFullSkuShadow)]) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] =
    failIfErrors(skuGroup.flatMap { case (f, s) ⇒ 
      if(f.code == s.code) Seq.empty
      else Seq(SkuShadowNotFoundInPayload(f.code))
    })

  private def validateShadow(form: ObjectForm, shadow: ObjectShadow) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] = 
    failIfErrors(IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes))

  private def validateShadow(product: Product, form: ObjectForm, shadow: ObjectShadow) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] = 
    failIfErrors(ProductValidator.validate(product, form, shadow))

  private def createSku(context: ObjectContext, productShadowId: Int, 
    formPayload: CreateFullSkuForm, shadowPayload: CreateSkuShadow)
  (implicit ec: EC, db: DB) = { 
    require(formPayload.code == shadowPayload.code)

    for {
      form    ← * <~ ObjectForms.create(ObjectForm(kind = Sku.kind, 
        attributes = formPayload.attributes))
      shadow  ← * <~ ObjectShadows.create(
        ObjectShadow(formId = form.id, attributes = shadowPayload.attributes))
      _    ← * <~ SkuManager.validateShadow(form, shadow)
      _    ← * <~ ObjectLinks.create(ObjectLink(leftId = productShadowId, 
        rightId = shadow.id))
      commit  ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      sku ← * <~ Skus.create(Sku(contextId = context.id, code = formPayload.code, 
        formId = form.id, shadowId = shadow.id, commitId = commit.id))
    } yield (sku, form, shadow)
  }
    
  private def getSkuData(productShadowId: Int)(implicit ec: EC, db: DB) : 
    DbResultT[Seq[(Sku, ObjectForm, ObjectShadow)]] = for {

    links     ← * <~ ObjectLinks.filter(_.leftId === productShadowId).result
    shadowIds ← * <~ links.map(_.rightId)
    shadows   ← * <~ ObjectShadows.filter(_.id.inSet(shadowIds)).sortBy(_.formId).result
    formIds   ← * <~ shadows.map(_.formId)
    forms     ← * <~ ObjectForms.filter(_.id.inSet(formIds)).sortBy(_.id).result
    skus      ← * <~ Skus.filter(_.formId.inSet(formIds)).sortBy(_.formId).result

  } yield (skus.zip(forms.zip(shadows))).map{ case (a, b) ⇒ (a, b._1, b._2)}

  private def createSkuData(context: ObjectContext, productShadowId: Int, 
    formPayloads: Seq[CreateFullSkuForm], shadowPayloads: Seq[CreateSkuShadow])
  (implicit ec: EC, db: DB) : DbResultT[Seq[(Sku, ObjectForm, ObjectShadow)]] = for {

    skuGroup    ← * <~ groupSkuFormsAndShadows(formPayloads, shadowPayloads)
    _            ← * <~ validateSkuPayload(skuGroup) 
    skuData ← * <~ DbResultT.sequence( 
      skuGroup map { case (f, sh) ⇒  createSku(context, productShadowId, f, sh)}
    )
  } yield skuData

  private def updateSkuData(context: ObjectContext, productShadowId: Int, 
    formPayloads: Seq[UpdateFullSkuForm], shadowPayloads: Seq[UpdateFullSkuShadow], productChanged: Boolean)
  (implicit ec: EC, db: DB) : DbResultT[Seq[(Sku, ObjectForm, ObjectShadow, Boolean)]] = for {

    skuGroup    ← * <~ groupSkuFormsAndShadows2(formPayloads, shadowPayloads)
    _            ← * <~ validateSkuPayload2(skuGroup) 
    skuData ← * <~ DbResultT.sequence( 
      skuGroup map { case (f, sh) ⇒  updateSku(context, productShadowId, f, sh, productChanged)}
    )
  } yield skuData

  private def updateSku(context: ObjectContext, productShadowId: Int, 
    formPayload: UpdateFullSkuForm, shadowPayload: UpdateFullSkuShadow, productChanged: Boolean)
  (implicit ec: EC, db: DB): DbResultT[(Sku, ObjectForm, ObjectShadow, Boolean)] = {
    require(formPayload.code == shadowPayload.code)
    for {
      sku   ← * <~ Skus.filterByContextAndCode(context.id, formPayload.code).one.mustFindOr(
        SkuNotFoundForContext(formPayload.code, context.name)) 
      updatedFormShadow  ← * <~ ObjectManager.updateObjectFormAndShadow(
        sku.formId, sku.shadowId, formPayload.attributes, shadowPayload.attributes)
      (form, shadow, skuChanged) = updatedFormShadow
      _ ← * <~ updateLink(productShadowId, sku.shadowId, shadow.id, productChanged, skuChanged)
      sku  ← * <~ SkuManager.commitSku(sku, form, shadow, skuChanged)
    } yield (sku, form, shadow, skuChanged)
  }

  private def updateLink(productShadowId: Int, oldSkuShadowId: Int, skuShadowId: Int,
    productChanged: Boolean, skuChanged: Boolean)
  (implicit ec: EC, db: DB): DbResultT[Unit] = 
    //Create a new link a product changes.
    if(productChanged) 
      for {
      _ ← * <~ ObjectLinks.create(ObjectLink(leftId = productShadowId, rightId = skuShadowId)) 
    } yield Unit 
    //If the product didn't change but the sku changed, update the link
    //This is because we never want two skus of the same type pointing to 
    //the same sku shadow.
    else if(skuChanged) for {
      link ← * <~ ObjectLinks.findByLeftRight(productShadowId, oldSkuShadowId).one.mustFindOr(
        ObjectLinkCannotBeFound(productShadowId, oldSkuShadowId))
      _ ← * <~ ObjectLinks.update(link, link.copy(leftId = productShadowId, rightId = skuShadowId)) 
    } yield Unit 
    //otherwise nothing changed so do nothing.
  else DbResultT.pure(Unit)

  private def groupSkuFormsAndShadows(forms: Seq[CreateFullSkuForm], shadows: Seq[CreateSkuShadow]) = {
    val sortedForms = forms.sortBy(_.code)
    val sortedShadows = shadows.sortBy(_.code)
    sortedForms zip sortedShadows
  }

  private def groupSkuFormsAndShadows2(forms: Seq[UpdateFullSkuForm], shadows: Seq[UpdateFullSkuShadow]) = {
    val sortedForms = forms.sortBy(_.code)
    val sortedShadows = shadows.sortBy(_.code)
    sortedForms zip sortedShadows
  }
}
