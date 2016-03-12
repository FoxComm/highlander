package services

import scala.concurrent.ExecutionContext

import models.inventory._
import models.product._
import models.objects._
import responses.ProductResponses._
import slick.driver.PostgresDriver.api._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import payloads.{CreateProductForm, UpdateProductForm, CreateProductShadow, 
  UpdateProductShadow, CreateObjectContext, UpdateObjectContext,
  CreateFullProductForm, UpdateFullProductForm, CreateFullProductShadow, 
  UpdateFullProductShadow, CreateFullSkuForm, UpdateFullSkuForm, CreateSkuForm, 
  CreateSkuShadow, UpdateFullSkuShadow, CreateFullProduct, UpdateFullProduct}

import org.json4s.JsonAST.{JValue, JString, JObject, JField, JNothing}

import ProductFailure._
import utils.aliases._
import utils.IlluminateAlgorithm
import cats.data.NonEmptyList
import cats.implicits._


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

  def createFullProduct(payload: CreateFullProduct, contextName: String)
    (implicit ec: EC, db: DB): Result[FullProductResponse.Root] = (for {

    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    productForm ← * <~ ObjectForms.create(ObjectForm(kind = Product.kind,
      attributes = payload.form.product.attributes))
    productShadow ← * <~ ObjectShadows.create(ObjectShadow(formId = productForm.id,
      attributes = payload.shadow.product.attributes))
    productCommit  ← * <~ ObjectCommits.create(ObjectCommit(formId = productForm.id, 
      shadowId = productShadow.id))
    _    ← * <~ validateShadow(productForm, productShadow)
    product     ← * <~ Products.create(Product(contextId = context.id, 
      formId = productForm.id, shadowId = productShadow.id, commitId = productCommit.id))
    skuData ← * <~ createSkuData(context, productShadow.id, 
      payload.form.skus, payload.shadow.skus)

  } yield FullProductResponse.build(product, productForm, productShadow, skuData)).runTxn()

  def updateFullProduct(productId: Int, payload: UpdateFullProduct, contextName: String)
    (implicit ec: EC, db: DB): Result[FullProductResponse.Root] = (for {

    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    product       ← * <~ Products.filter(_.contextId === context.id).
      filter(_.formId === productId).one.mustFindOr(
        ProductNotFoundForContext(productId, context.id)) 
    updatedFormShadow ← * <~ updateObjectFormAndShadow(product.formId, 
      product.shadowId, payload.form.product.attributes, 
      payload.shadow.product.attributes)
    productForm   = updatedFormShadow._1
    productShadow = updatedFormShadow._2
    previousCommit ← * <~ ObjectCommits.mustFindById404(product.commitId)
    productCommit  ← * <~ ObjectCommits.update(ObjectCommit(formId = productForm.id, 
      shadowId = productShadow.id, previousId = oldCommit.id.some))
    product     ← * <~ Products.update(product, product.copy(
      shadowId = productShadow.id, commitId = productCommit.id))
    skuData ← * <~ updateSkuData(context, productShadow.id, 
      payload.form.skus, payload.shadow.skus)

  } yield FullProductResponse.build(product, productForm, productShadow, skuData)).runTxn()

  def getIlluminatedFullProduct(productId: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedFullProductResponse.Root] = (for {

    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
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
    })).run()

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
      if(f.code === s.code) Seq.empty
      else Seq(SkuShadowNotFoundInPayload(f.code))
    })

  private def validateSkuPayload2(skuGroup : Seq[(UpdateFullSkuForm, UpdateFullSkuShadow)]) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] =
    failIfErrors(skuGroup.flatMap { case (f, s) ⇒ 
      if(f.code === s.code) Seq.empty
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

    skuLinks     ← * <~ ObjectLinks.filter(_.leftId === productShadowId).result
    skuShadowIds ← * <~ skuLinks.map(_.rightId)
    skuShadows   ← * <~ ObjectShadows.filter(_.id.inSet(skuShadowIds)).result
    skuFormIds   ← * <~ skuShadows.map(_.formId)
    skuForms     ← * <~ ObjectForms.filter(_.id.inSet(skuFormIds)).result
    skus         ← * <~ Skus.filter(_.formId.inSet(skuFormIds)).result

  } yield (skus.zip(skuForms.zip(skuShadows))).map{ case (a, b) ⇒ (a, b._1, b._2)}

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
    formPayloads: Seq[UpdateFullSkuForm], shadowPayloads: Seq[UpdateFullSkuShadow])
  (implicit ec: EC, db: DB) : DbResultT[Seq[(Sku, ObjectForm, ObjectShadow)]] = for {

    skuGroup    ← * <~ groupSkuFormsAndShadows2(formPayloads, shadowPayloads)
    _            ← * <~ validateSkuPayload2(skuGroup) 
    skuData ← * <~ DbResultT.sequence( 
      skuGroup map { case (f, sh) ⇒  updateSku(context, productShadowId, f, sh)}
    )
  } yield skuData

  private def updateSku(context: ObjectContext, productShadowId: Int, 
    formPayload: UpdateFullSkuForm, shadowPayload: UpdateFullSkuShadow)
  (implicit ec: EC, db: DB): DbResultT[(Sku, ObjectForm, ObjectShadow)] = {
    require(formPayload.code == shadowPayload.code)
    for {
      sku   ← * <~ Skus.filter(_.contextId === context.id).
      filter(_.code === formPayload.code).one.mustFindOr(
        SkuNotFoundForContext(formPayload.code, context.name)) 
      updatedFormShadow  ← * <~ updateObjectFormAndShadow(
        sku.formId, sku.shadowId, formPayload.attributes, shadowPayload.attributes)
      form = updatedFormShadow._1
      shadow = updatedFormShadow._2
      link ← * <~ ObjectLinks.create(ObjectLink(leftId = productShadowId, 
        rightId = shadow.id))
    } yield (sku, form, shadow)
  }

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

  private def updateObjectFormAndShadow(formId: Int, shadowId: Int, 
    updatedForm: JValue, updatedShadow: JValue)
    (implicit ec: EC, db: DB): DbResultT[(ObjectForm, ObjectShadow)] = for {

    oldForm     ← * <~ ObjectForms.mustFindById404(formId)
    oldShadow   ← * <~ ObjectShadows.mustFindById404(shadowId)
    newFormPair ← * <~ ObjectUtils.updateForm(oldForm.attributes, updatedForm)
    keyMap      ← * <~ newFormPair._1
    newFormAttributes   = newFormPair._2
    newShadowAttributes ← * <~ ObjectUtils.newShadow(oldShadow.attributes, keyMap)
    form        ← * <~ ObjectForms.update(oldForm, oldForm.copy(attributes = 
      newFormAttributes))
    shadow      ← * <~ ObjectShadows.create(oldShadow.copy(formId = formId, 
        attributes = newShadowAttributes))
    _    ← * <~ validateShadow(form, shadow)
  } yield (form, shadow)


}
